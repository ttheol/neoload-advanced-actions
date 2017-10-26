package com.neotys.ps.iot.coap.client;

import java.util.List;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;

public final class CoAPClientActionEngine implements ActionEngine {

	private static String server;
	private static Integer port;
	private static String path;	
	private static String method;
	private static String scheme;
	private static boolean confirmable;
	private static String payload;
	
	private static void parseParameters(List<ActionParameter> parameters){
		
		//Setting default value
		scheme="coap";
		confirmable=true;		
		payload="";
		
		for (ActionParameter temp:parameters){
			switch (temp.getName().toLowerCase()) {
			case "method":
				method = temp.getValue().toUpperCase();
				break;
			case "server":
				server = temp.getValue();
				break;
			case "port":
				port = Integer.parseInt(temp.getValue());
				break;
			case "path":
				path = temp.getValue();
				break;
			case "confirmable":
				confirmable=Boolean.parseBoolean(temp.getValue().toLowerCase());				
				break;
			case "payload":
				payload=temp.getValue();
			default :
				break;
			}
		}
	}
	
	private static CoapResponse sendGet(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;
		
		//Send the request
		response = client.get();
		
		//Return the response
		return response;
	}
	
	private static CoapResponse sendPost(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;
		
		//Send the request
		response = client.post(payload, MediaTypeRegistry.TEXT_PLAIN);
		
		//Return the response
		return response;
	}
	
	private static CoapResponse sendPut(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;

		//Send the request
		response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
		
		//Return the response
		return response;
	}
	
	private static CoapResponse sendDelete(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;
		
		//Send the request
		response = client.delete();
		
		//Return the response
		return response;
	}
	
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		final SampleResult sampleResult = new SampleResult();
		final StringBuilder requestBuilder = new StringBuilder();
		final StringBuilder responseBuilder = new StringBuilder();
		parseParameters(parameters);
		
		//Instantiate the client
		CoapClient client = new CoapClient(scheme,server,port,path);
		if (confirmable) {
			client.useCONs();
		} else {
			client.useNONs();
		}
		
		appendLineToStringBuilder(requestBuilder,String.format("%s %s",method,client.getURI()));
		appendLineToStringBuilder(requestBuilder,String.format("\n%s",payload));
		sampleResult.setRequestContent(requestBuilder.toString());
		
		sampleResult.sampleStart();

		CoapResponse response=null;
		//Send the request
		switch (method) {
		case "GET":
			response = sendGet(client);
			break;
		case "POST":
			response = sendPost(client);
			break;
		case "PUT":
			response = sendPut(client);
			break;
		case "DELETE":
			response = sendDelete(client);
			break;
		default:
			response = sendGet(client);
			break;
		}
		
		if (response != null) {
			appendLineToStringBuilder(responseBuilder, Utils.prettyPrint(response));
		} else {
			return getErrorResult(context,sampleResult,"No response","NL-CoAPClient-01",null);
		}
			
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
	private static SampleResult getErrorResult(final Context context, final SampleResult result, final String errorMessage, final String statusCode, final Exception exception) {
		result.setError(true);
		result.setStatusCode(statusCode);
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

}
