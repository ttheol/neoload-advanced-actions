package com.neotys.ps.iot.coap.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CoAPClientActionTest {
	@Test
	public void shouldReturnType() {
		final CoAPClientAction action = new CoAPClientAction();
		assertEquals("CoAPClient", action.getType());
	}

}
