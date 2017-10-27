package com.neotys.ps.iot.coap.client;

import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
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
	private static byte[] token;
	
	private static OptionSet parseParameters(List<ActionParameter> parameters){
		
		OptionSet options = new OptionSet();
		//Setting default values
		scheme = "coap";
		confirmable = true;		
		payload = "";
		parameterList = new StringBuilder();
		token=null;
		options.setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		
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
			case "content-format":
				options.setContentFormat(MediaTypeRegistry.parse(temp.getValue()));
				break;
			case "accept":
				options.setAccept(MediaTypeRegistry.parse(temp.getValue()));
				break;
			case "if-none-match":
				options.setIfNoneMatch(true);
				break;
			case "if-match":
				options.addIfMatch(DatatypeConverter.parseHexBinary((temp.getValue())));				
				break;
			case "token":
				token = temp.getValue().getBytes();
				break;
			case "etag":
				options.addETag(DatatypeConverter.parseHexBinary((temp.getValue())));
				break;
			default :
				//If the name of the parameter is unknown, add it to the payload or path
				//If the parameter is the first one
				if (parameterList.length()==0) {
					//Prefix with a ?
					parameterList.append("?");
				} else {
					//Prefix with a &
					parameterList.append("&");
				}
				parameterList.append(temp.getName());
				parameterList.append("=");
				parameterList.append(temp.getValue());
				break;
			}
		}
		return options;
	}	

	
	@Override
	public SampleResult execute(Context context, List<ActionParameter> parameters) {
		final SampleResult sampleResult = new SampleResult();
		final StringBuilder requestBuilder = new StringBuilder();
		final StringBuilder responseBuilder = new StringBuilder();
		OptionSet options = parseParameters(parameters);
				
		//If I have parameters to send
		if (parameterList.length()>0) {
			//Handle the parameters based on the method
			switch (method) {
			case "GET":		//Add the parameters to the path
				path = String.format("%s%s", path,parameterList.toString());
				break;
			case "POST": case "PUT":	//Add the parameters to the payload
				payload = String.format("%s%s",payload,parameterList.toString());
			}
		}
		
		//Instantiate the CoAP client
		CoapClient client = new CoapClient(scheme,server,port,path);
		String uri = client.getURI();
		
		//Instantiate a request and set its options
		Request req12;
		switch (method) {
		case "POST":
			req12 = Request.newPost();
			req12.setPayload(payload);
			break;
		case "PUT":
			req12 = Request.newPut();
			req12.setPayload(payload);
			break;
		case "DELETE":
			req12 = Request.newDelete();
			break;
		default:
			req12 = Request.newGet();			
			break;
		}
		req12.setConfirmable(confirmable);		
		req12.setOptions(options);
		req12.setToken(token);
		
		//Log the request
		appendLineToStringBuilder(requestBuilder,String.format("%s",uri));
		appendLineToStringBuilder(requestBuilder,Utils.prettyPrint(req12));
		appendLineToStringBuilder(requestBuilder,req12.getOptions().toString());
		sampleResult.setRequestContent(requestBuilder.toString());

		CoapResponse response=null;

		sampleResult.sampleStart();

		//Send the request
		response = client.advanced(req12);
		
		sampleResult.sampleEnd();
		
		if (response != null) {
			appendLineToStringBuilder(responseBuilder, Utils.prettyPrint(response));
		} else {
			return getErrorResult(context,sampleResult,"No response","NL-CoAPClient-01",null);
		}
		
		
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
