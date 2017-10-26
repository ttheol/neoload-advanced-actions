package com.neotys.ps.aws.monitoring;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.google.common.base.Optional;
import com.neotys.extensions.action.Action;
import com.neotys.extensions.action.ActionParameter;
import com.neotys.extensions.action.engine.ActionEngine;

public final class CloudWatchMonitoringAction implements Action{

    private static final ImageIcon LOGO_ICON = new ImageIcon(CloudWatchMonitoringAction.class.getResource("AWS-icon.png"));
    

    @Override
	public String getType() {
		return "AWS CloudWatch Monitoring";
	}

	@Override
	public List<ActionParameter> getDefaultActionParameters() {
        ArrayList<ActionParameter> parameters = new ArrayList<ActionParameter>();
        parameters.add(new ActionParameter("AWS URL", "https://monitoring.eu-west-1.amazonaws.com"));
        parameters.add(new ActionParameter("accessKey", ""));
        parameters.add(new ActionParameter("secretKey", ""));
        return parameters;
	}

	@Override
	public Class<? extends ActionEngine> getEngineClass() {
		return CloudWatchMonitoringActionEngine.class;
	}

	@Override
	public Icon getIcon() {
		return LOGO_ICON;
	}

	@Override
	public String getDescription() {
		StringBuilder description = new StringBuilder();
        description.append("Monitors AWS CloudWatch\n\n");
        description.append("AWS URL : Endpoint to access the AWS metrics to trace. This endpoint defines the region to retrieve. http://docs.aws.amazon.com/general/latest/gr/rande.html#cw_region for complete list of endpoints.\n");
        description.append("\tExample : https://monitoring.eu-west-1.amazonaws.com for Ireland region\n\n");
        description.append("accessKey: Access Key to connect to your AWS account\n");
        description.append("\tExample : AKIAIOSFODNN7EXAMPLE\n\n");
        description.append("secretKey: Secret Access Key generated from the AWS IAM console\n");
        description.append("\tExample : wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\n\n");
        description.append("(optional) Period : Granularity, in minutes, of the returned datapoints. Detailed monitoring in AWS lets you have all your metrics with one minute granularity. Basic monitoring limits the following EC2 metrics to a 5-minute granularity :\n");
        description.append("\t- CPUUtilization\n");
        description.append("\t- DiskReadBytes\n");
        description.append("\t- DiskReadOps\n");
        description.append("\t- DiskWriteBytes\n");
        description.append("\t- DiskWriteOps\n");
        description.append("\t- NetworkIn\n");
        description.append("\t- NetworkOut\n");
        description.append("\tDefault value : 1\n\n");
        description.append("(optional) Offset : Number of minutes to retrieve from the AWS monitoring on each request\n");
        description.append("\tDefault value : 6\n\n");
        description.append("(optional) Data Exchange API client : hostname of the controller\n");
        description.append("\tDefault Value : localhost\n\n");
        description.append("\nExecution time of this custom action depends on the number of counters available for your credentials in AWS. To be sure to have no gap in your monitoring, you need to  : \n");
        description.append("\t- Make sure you set a pacing for the action container, equal to the Offset\n");
        description.append("\t- Make sure the execution time of this action is faster than the Offset\n");
        
        return description.toString();
	}

	@Override
	public String getDisplayName() {
		return this.getType();
	}

	@Override
	public String getDisplayPath() {
		return "Monitoring/Cloud";
	}

	@Override
	public Optional<String> getMinimumNeoLoadVersion() {
		return Optional.of("5.1");
	}

	@Override
	public Optional<String> getMaximumNeoLoadVersion() {
		return Optional.absent();
	}
}
