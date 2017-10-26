package com.neotys.ps.aws.monitoring;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.olingo.odata2.api.exception.ODataException;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.util.json.JSONException;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;
import com.neotys.rest.dataexchange.client.DataExchangeAPIClient;
import com.neotys.rest.dataexchange.client.DataExchangeAPIClientFactory;
import com.neotys.rest.dataexchange.model.EntryBuilder;
import com.neotys.rest.error.NeotysAPIException;

public final class CloudWatchMonitoringActionEngine implements ActionEngine {

	private String AWSEndpoint;
	private String awsAccessKey;
	private String awsSecretKey;
	private int period = 60;
	private String dataExchangeHost = "localhost";
	private int startOffset = 6;
	private static int datapointsCount;

	private void parseParameters(List<ActionParameter> parameters) {
		
		for (ActionParameter parameter : parameters) {
			switch (parameter.getName().toLowerCase()) {
			case "aws url":
				AWSEndpoint = parameter.getValue();
				break;
			case "accesskey":
				awsAccessKey = parameter.getValue();
				break;
			case "secretkey":
				awsSecretKey = parameter.getValue();
				break;
			case "period":
				period = Integer.parseInt(parameter.getValue())*60;
				break;
			case "offset":
				startOffset = Integer.parseInt(parameter.getValue());
				break;
			case "data exchange api client":
				dataExchangeHost = parameter.getValue();
				break;
			default :
				break;
			}
		}
	}

	public static void traceMetrics (AmazonCloudWatchClient awsClient,ListMetricsResult listMetrics, Date startTime, Date endTime, int period, DataExchangeAPIClient dataExchangeAPIClient) throws JSONException, GeneralSecurityException, IOException, ODataException, URISyntaxException, NeotysAPIException{
		List<Metric> metrics = listMetrics.getMetrics();
		List<String> stats = Arrays.asList("Average") ;
		
		//Parcours des métriques
		for (int i = 0; i < metrics.size(); i++){
			String metricName = metrics.get(i).getMetricName();
			String namespace = metrics.get(i).getNamespace();
			List<Dimension> metricDimensions = metrics.get(i).getDimensions();
			GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
			statRequest.setMetricName(metricName);
			statRequest.setNamespace(namespace);
			statRequest.setEndTime(endTime);
			statRequest.setStartTime(startTime);
			statRequest.setPeriod(period);
			statRequest.setStatistics(stats);
			
			//Parcours des dimensions
			for (int j = 0; j < metricDimensions.size(); j++) {
				String dimensionName = metricDimensions.get(j).getName();
				String dimensionValue = metricDimensions.get(j).getValue();
				
				statRequest.setDimensions(Arrays.asList(metricDimensions.get(j)));
				
				GetMetricStatisticsResult statMetrics = awsClient.getMetricStatistics(statRequest);
				
				List<Datapoint> datapoints = statMetrics.getDatapoints();
				//Parcours des points de mesure
				
				for (int k = 0; k < datapoints.size(); k++){
					long timestamp = datapoints.get(k).getTimestamp().getTime();
					
					EntryBuilder eb = new EntryBuilder(Arrays.asList(namespace,dimensionName,dimensionValue,metricName), timestamp);
					
					eb.unit(datapoints.get(k).getUnit());
					eb.value(datapoints.get(k).getAverage());
					
					dataExchangeAPIClient.addEntry(eb.build());
					datapointsCount += 1;
				}
			}
		}

	}
	
	@Override
	public SampleResult execute(Context context,
			List<ActionParameter> parameters) {

		SampleResult result = new SampleResult();
		StringBuilder resultString = new StringBuilder();
		
		parseParameters(parameters);
			
		DataExchangeAPIClient dataExchangeAPIClient ;
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
		AmazonCloudWatchClient awsClient = new AmazonCloudWatchClient(awsCredentials);
		awsClient.setEndpoint(AWSEndpoint);
		
		Date endDate = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(endDate);
		endDate = calendar.getTime();
		calendar.add(Calendar.MINUTE, - startOffset);
		Date startDate = calendar.getTime();
		datapointsCount=0;
		
		try {
				
			dataExchangeAPIClient = DataExchangeAPIClientFactory.newClient("http://"+ dataExchangeHost +":7400/DataExchange/v1/Service.svc/");			
	
			//Récupération de la première page de métriques
			ListMetricsRequest listRequest = new ListMetricsRequest();
			String nextToken = "";
	
			//Parcours de toutes les métriques
			while (nextToken!=null) {

				ListMetricsResult availableMetrics = awsClient.listMetrics(listRequest);
				
				//Tracage des statistiques
				traceMetrics(awsClient,availableMetrics, startDate, endDate, period, dataExchangeAPIClient);
	
				nextToken = availableMetrics.getNextToken();
				listRequest.setNextToken(nextToken);
			}
			} catch (GeneralSecurityException | IOException | ODataException
					| URISyntaxException | NeotysAPIException | JSONException e) {
				e.printStackTrace();
			}
		
		
		float executionTime = (new Date().getTime()) - endDate.getTime(); 
		resultString.append(datapointsCount + " datapoints retrieved in "+ executionTime/1000 +" seconds.\n");
		result.setResponseContent(resultString.toString());
		return result;
	}

	@Override
	public void stopExecute() {
		// TODO Auto-generated method stub

	}

}
