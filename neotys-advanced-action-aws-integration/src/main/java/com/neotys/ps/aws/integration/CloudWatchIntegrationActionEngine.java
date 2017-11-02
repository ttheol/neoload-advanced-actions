package com.neotys.ps.aws.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.InvalidParameterValueException;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.base.Charsets;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;

import io.swagger.client.ApiException;
import io.swagger.client.api.ResultsApi;
import io.swagger.client.model.ElementDefinitions;
import io.swagger.client.model.ElementValues;
import io.swagger.client.model.TestDefinition;
import io.swagger.client.model.TestStatistics;

public final class CloudWatchIntegrationActionEngine implements ActionEngine {

	private static String awsRegion;
	private static String awsAccessKey;
	private static String awsSecretKey;
	private static String nlWebHost;
	private static String nlWebToken;
	
	private static void parseParameters(Context context, List<ActionParameter> parameters) {
		//Initialise the parameters
		nlWebHost = "neoload-api.saas.neotys.com";
		nlWebToken = context.getAccountToken();
		
		//Parse the advanced actions parameters
		for (ActionParameter temp:parameters) {
			switch (temp.getName().toLowerCase()) {
			case "aws region" :
				awsRegion = temp.getValue();
				break;
			case "accesskey":
				awsAccessKey = temp.getValue();
				break;
			case "secretkey":
				awsSecretKey = temp.getValue();
				break;
			case "neoload web host":
				nlWebHost = temp.getValue();
				break;
			case "neoload web token":
				nlWebToken = temp.getValue();
				break;
			default:
				break;
			}
		}
	}
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		final SampleResult sampleResult = new SampleResult();
		final StringBuilder requestBuilder = new StringBuilder();
		final StringBuilder responseBuilder = new StringBuilder();
		final String awsNameSpace = "NeoLoad";
		
		//Parse the parameters
		parseParameters(context, parameters);
		appendLineToStringBuilder(requestBuilder,String.format("AWS Region:%s",awsRegion));

		//Instantiate a NL Web API client		
		String nlWebPath = String.format("https://%s/v1",nlWebHost);		
		ResultsApi nlWebClient = new ResultsApi(nlWebPath,nlWebToken);
		
		//Instantiate an AWS Client						
		AmazonCloudWatch cwClient = createNewAWSClient(awsAccessKey, awsSecretKey, awsRegion);

		//Wait for the test to start
				
		//Get the Test Id
		String basePath = nlWebClient.getApiClient().getBasePath().toString();
		appendLineToStringBuilder(requestBuilder,String.format("Base URL:%s",basePath));
		String testId = context.getTestId();
		appendLineToStringBuilder(requestBuilder,String.format("Test Id:%s",testId));

		//Initialise the dimensions for AWS filtering		
		Collection<Dimension> dimensions = new ArrayList<Dimension>();
		addDimension(dimensions,"TestId",testId);
		
		try {
			
			TestDefinition definition = nlWebClient.getTest(testId);	
			Double lgCount = new Double(definition.getLgCount());
			String authorName = convertStringToASCII(definition.getAuthor());
			String projectName = convertStringToASCII(definition.getProject());
			String scenarioName = convertStringToASCII(definition.getScenario());
			String testName = convertStringToASCII(definition.getName());
			
			//Log the name of the test
			appendLineToStringBuilder(requestBuilder,String.format("Name:%s",testName));			
			appendLineToStringBuilder(requestBuilder,String.format("Description:%s",definition.getDescription()));
			appendLineToStringBuilder(requestBuilder,String.format("Author:%s",authorName));
			appendLineToStringBuilder(requestBuilder,String.format("Termination Reason:%s",definition.getTerminationReason()));
			appendLineToStringBuilder(requestBuilder,String.format("LG count:%s",lgCount.intValue()));
			appendLineToStringBuilder(requestBuilder,String.format("Project Name:%s",projectName));
			appendLineToStringBuilder(requestBuilder,String.format("Scenario Name:%s",scenarioName));
			appendLineToStringBuilder(requestBuilder,String.format("Status:%s",definition.getStatus()));
			appendLineToStringBuilder(requestBuilder,String.format("Quality Status:%s",definition.getQualityStatus()));
			appendLineToStringBuilder(requestBuilder,String.format("Start Date:%tc",new Date(definition.getStartDate())));
			appendLineToStringBuilder(requestBuilder,String.format("Duration:%tT",new Date(definition.getDuration())));
			sampleResult.setRequestContent(requestBuilder.toString());
			
			//Add the test properties to the dimensions
			addDimension(dimensions,"Author",authorName);
			addDimension(dimensions,"Project",projectName);
			addDimension(dimensions,"Scenario",scenarioName);
			addDimension(dimensions,"Test",testName);
			
			final Collection<Dimension> baseDimensions = new ArrayList<Dimension>(dimensions);
						
			//Send the number of LGs to AWS
			AWSPutRequest(cwClient, baseDimensions, awsNameSpace, "Number of LGs", lgCount, StandardUnit.Count);
			
			addDimension(dimensions,"Type","Statistics");
			
			//Get the latest test statistics
			TestStatistics stats = nlWebClient.getTestStatistics(testId);
			int countVU = stats.getLastVirtualUserCount();			
			float requestsPerSec = stats.getLastRequestCountPerSecond();
			
			appendLineToStringBuilder(responseBuilder,String.format("Number of Virtual Users: %d",countVU));			
			appendLineToStringBuilder(responseBuilder,String.format("Requests per second: %f",requestsPerSec));
			
			//Send the number of VUs to CloudWatch
			AWSPutRequest(cwClient, dimensions, awsNameSpace, "VU", (double) countVU, StandardUnit.Count);			
			
			//Send the requests per sec to CloudWatch
			AWSPutRequest(cwClient, dimensions, awsNameSpace, "Requests per second", (double) requestsPerSec, StandardUnit.CountSecond);
			
			//Get the transaction list
			ElementDefinitions transactions = nlWebClient.getTestElements(testId, "TRANSACTION");
			
			//Print the transaction names and response times
			for (int i=0 ; i < transactions.size(); i++) {
				//Get the statistics on the transaction
				ElementValues transactionStat = nlWebClient.getTestElementsValues(testId, transactions.get(i).getId());
				double avgDuration = transactionStat.getAvgDuration()/1000;
				
				//Reset the dimensions
				dimensions.clear();
				dimensions.addAll(baseDimensions);
				
				//Add a new dimension
				addDimension(dimensions,"Type","Transaction");
				
				//If the current transaction is the all transactions
				if (transactions.get(i).getType().equals("ALL_TRANSACTIONS")) {
					//Log the avg response time for all transactions in seconds
					appendLineToStringBuilder(responseBuilder,String.format("All transactions (Avg Duration): %f sec",avgDuration));
					
					addDimension(dimensions,"Transaction Name","All");
					AWSPutRequest(cwClient, dimensions, awsNameSpace, "Response time", avgDuration, StandardUnit.Seconds);
					
				} else {
					//Get the name of the transaction
					String transactionName = transactions.get(i).getName();
					//Get the path
					List<String> path = transactions.get(i).getPath();
					
					//Get the context
					String scriptName = path.get(0);
					String baseContainer = path.get(1);
					
					StringBuilder transactionPath = new StringBuilder();
					for (String s : path) {
						transactionPath.append(s);						
						transactionPath.append("/");
					}
					
					//Shave the last /
					transactionPath.deleteCharAt(transactionPath.length()-1);
					
					//Add the path to the dimensions
					addDimension(dimensions,"Full Path",transactionPath.toString());					
					addDimension(dimensions,"Script",scriptName);
					addDimension(dimensions,"Action",baseContainer);
					addDimension(dimensions,"TransactionName",transactionName);
					
					//Log the avg response time for the transaction in seconds
					appendLineToStringBuilder(responseBuilder,String.format("%s (Avg Duration): %f sec",transactionPath.toString(),avgDuration));
					AWSPutRequest(cwClient, dimensions, awsNameSpace, "Response time", avgDuration, StandardUnit.Seconds);
				}
			}
								
		} catch (InvalidParameterValueException e) {
			return getErrorResult(context,sampleResult,e.getErrorMessage(),String.format("NL-CloudWatchIntegration-%s",e.getErrorCode()),e);
		} catch (ApiException e) {
			return getErrorResult(context,sampleResult,e.getMessage(),String.format("NL-CloudWatchIntegration-%d",e.getCode()),e);
		} catch (NullPointerException e) {
			return getErrorResult(context,sampleResult,e.getMessage(),String.format("NL-CloudWatchIntegration-%d",e.getCause().toString()),e);
		}
		
		
		sampleResult.sampleStart();

		sampleResult.sampleEnd();

		sampleResult.setResponseContent(responseBuilder.toString());
		return sampleResult;
	}

	private void appendLineToStringBuilder(final StringBuilder sb, final String line){
		sb.append(line).append("\n");
	}

	/**
	 * This method allows to easily create an error result and log exception.
	 */
	private static SampleResult getErrorResult(final Context context, final SampleResult result, final String errorMessage, final String errorCode, final Exception exception) {
		result.setError(true);
		result.setStatusCode(errorCode);
		result.setResponseContent(errorMessage);
		if(exception != null){
			context.getLogger().error(errorMessage, exception);
		} else{
			context.getLogger().error(errorMessage);
		}
		return result;
	}

	@Override
	public void stopExecute() {
		// TODO add code executed when the test have to stop.
	}
	


	private String convertStringToASCII(String source) {
		String s = source;
		byte[] b = s.getBytes(Charsets.US_ASCII);
		return new String (b);
		
	}
	
	private AmazonCloudWatch createNewAWSClient(String accessKey, String secretKey, String region) {
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);				
		AmazonCloudWatch client = AmazonCloudWatchClientBuilder.standard()		
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
				.withRegion(region)
				.build();
		
		return client;
	}
	
	private void addDimension(java.util.Collection<Dimension> dimensions, String dimensionName, String dimensionValue) {
		dimensions.add(
				new Dimension()
				.withName(dimensionName)
				.withValue(dimensionValue));
	}

	private void AWSPutRequest(AmazonCloudWatch client, java.util.Collection<Dimension> dimensions, String nameSpace, String metricName, Double value, StandardUnit unit) {
		MetricDatum datum = new MetricDatum()
				.withDimensions(dimensions)
				.withMetricName(metricName)
				.withUnit(unit)
				.withValue(value);			
		
		PutMetricDataRequest request = new PutMetricDataRequest()
				.withNamespace(nameSpace)
				.withMetricData(datum);
		
		client.putMetricData(request);
	}
}