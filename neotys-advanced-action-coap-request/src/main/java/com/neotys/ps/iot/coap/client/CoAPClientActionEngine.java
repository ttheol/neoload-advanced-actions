package com.neotys.ps.iot.coap.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;
import com.neotys.extensions.action.engine.Context;
import com.neotys.extensions.action.engine.SampleResult;

public final class CoAPClientActionEngine implements ActionEngine {

	private static String server;					//Server to send the requests to
	private static Integer port;					//Port of the endpoint
	private static String path;						//Path of the URI
	private static String method;					//CoAP method to use to send the request
	private static String scheme;					//Protocol prefix
	private static boolean confirmable;				//true if the request is confirmable, false otherwise
	private static String payload;					//Payload to send in the PUT or POST
	private static StringBuilder parameterList;		//List of GET, POST or PUT parameters to send with the request
	private static int accept;						//Desired format of the response
	private static int payloadType;					//Type format of the payload. Default is text/plain
	private static boolean ifNoneMatch;				//Set to true if specified in the request. false otherwise
	private static ArrayList<byte[]> ifMatch ;				//etags for the If-Match header
		
	
	private static void parseParameters(List<ActionParameter> parameters){
		
		//Setting default value
		scheme = "coap";
		confirmable = true;		
		payload = "";
		parameterList = new StringBuilder();
		accept = -1;
		payloadType = 0;
		ifNoneMatch = false;
		ifMatch= new ArrayList<byte[]>();
		int i = 0;					//Counter for the If-Match array of etags
		
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
				break;
			case "payloadtype":
				payloadType=MediaTypeRegistry.parse(temp.getValue());
				break;
			case "accept":
				accept = MediaTypeRegistry.parse(temp.getValue());
				break;
			case "if-none-match":
				ifNoneMatch = true;
				break;
			case "if-match":
				ifMatch.add(hexStringToByteArray(temp.getValue()));
				i++;
				break;
			default :
				if (parameterList.length()==0) {
					parameterList.append("?");
				} else {
					parameterList.append("&");
				}
				parameterList.append(temp.getName());
				parameterList.append("=");
				parameterList.append(temp.getValue());
				break;
			}
		}
	}
	
	private static CoapResponse sendGet(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;		

		if (accept==-1) {
			//Send the request
			response = client.get();
		} else {
			response = client.get(accept);
		}
		
		//Return the response
		return response;
	}
	
	private static CoapResponse sendPost(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;
		
		//If the request doesn't specify a return type
		if (accept==-1) {
			//Send a standard request
			response = client.post(payload, payloadType);
		} else {		
			//Use the accept header in the request
			response = client.post(payload, payloadType, accept);
		}
		
		//Return the response
		return response;
	}
	
	private static CoapResponse sendPut(CoapClient client) {
		//Initialise the response
		CoapResponse response=null;		

		//If the If-None-Match header is not specified
		if (!ifNoneMatch) {
			//If the If-Match header is not specified
			if (ifMatch.isEmpty()) {
				//Send the standard request
				response = client.put(payload, payloadType);				
			} else {
								
				//Create an array of etags
				byte[][] etags = new byte[ifMatch.size()][];
				for (int i=0;i < ifMatch.size();i++) {
					//byte[] temp = ifMatch.get(i);
					etags[i] = ifMatch.get(i);
				}
				
				//Use the etags
				response = client.putIfMatch(payload, payloadType, etags);
			}
		} else {
			//Send the request with the If-None-Match header
			response = client.putIfNoneMatch(payload, payloadType);
		}
		
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
		
		//If I have parameters to send
		if (parameterList.length()>0) {
			//Handling the parameters based on the method
			switch (method) {
			case "GET":
				path = String.format("%s%s", path,parameterList.toString());
				break;
			case "POST": case "PUT":
				payload = String.format("%s%s",payload,parameterList.toString());
			}
		}
		
		//Instantiate the client
		CoapClient client = new CoapClient(scheme,server,port,path);
		if (confirmable) {
			client.useCONs();
		} else {
			client.useNONs();
		}
		
		appendLineToStringBuilder(requestBuilder,String.format("%s %s",method,client.getURI()));
		appendLineToStringBuilder(requestBuilder,String.format("%s %d","If-Match count",ifMatch.size()));
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
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

}
