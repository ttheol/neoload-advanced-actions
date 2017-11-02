package com.neotys.ps.aws.integration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CloudWatchIntegrationActionTest {
	@Test
	public void shouldReturnType() {
		final CloudWatchIntegrationAction action = new CloudWatchIntegrationAction();
		assertEquals("CloudWatchIntegration", action.getType());
	}

}
