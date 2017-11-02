package com.neotys.ps.aws.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.google.common.base.Optional;
import com.neotys.extensions.action.Action;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;

public final class CloudWatchIntegrationAction implements Action{
	private static final String BUNDLE_NAME = "com.neotys.ps.aws.integration.bundle";
	private static final String DISPLAY_NAME = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayName");
	private static final String DISPLAY_PATH = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("displayPath");
	private static final ImageIcon ICON_PATH = new ImageIcon(CloudWatchIntegrationAction.class.getResource(ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString("iconFile")));

	@Override
	public String getType() {
		return "CloudWatchIntegration";
	}

	@Override
	public List<ActionParameter> getDefaultActionParameters() {
		final List<ActionParameter> parameters = new ArrayList<ActionParameter>();
        parameters.add(new ActionParameter("AWS Region", "eu-west-1"));
        parameters.add(new ActionParameter("accessKey", ""));
        parameters.add(new ActionParameter("secretKey", ""));
		return parameters;
	}

	@Override
	public Class<? extends ActionEngine> getEngineClass() {
		return CloudWatchIntegrationActionEngine.class;
	}

	@Override
	public Icon getIcon() {
		return ICON_PATH;
	}

	@Override
	public boolean getDefaultIsHit(){
		return false;
	}

	@Override
	public String getDescription() {
		final StringBuilder description = new StringBuilder();
        description.append("Send load test data to CloudWatch\n\n");
        description.append("AWS Region (required): Region where CloudWatch will be accessed. http://docs.aws.amazon.com/general/latest/gr/rande.html#cw_region for complete list of endpoints.\n");
        description.append("\tExample : eu-west-1 for Ireland region\n");
        description.append("accessKey (required): Access Key to connect to your AWS account\n");
        description.append("\tExample : AKIAIOSFODNN7EXAMPLE\n");
        description.append("secretKey (required): Secret Access Key generated from the AWS IAM console\n");
        description.append("\tExample : wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\n");
        description.append("NeoLoad Web Host (optional): Host of your NeoLoad web instance. Default is the SaaS instance\n");
        description.append("\tExample : neoload-api.saas.neotys.com\n");
        description.append("NeoLoad Web Token (required): Token used to connect to your NL web instance\n");
        description.append("\tExample : e6d799df427b8fc3b2998d896db1d5d01c618d5d73aa7b9d\n");
		return description.toString();
	}

	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Override
	public String getDisplayPath() {
		return DISPLAY_PATH;
	}

	@Override
	public Optional<String> getMinimumNeoLoadVersion() {
		return Optional.absent();
	}

	@Override
	public Optional<String> getMaximumNeoLoadVersion() {
		return Optional.absent();
	}
}
