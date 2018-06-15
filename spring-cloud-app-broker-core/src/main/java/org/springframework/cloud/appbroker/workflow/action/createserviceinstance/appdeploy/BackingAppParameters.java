package org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy;

import java.util.Map;

public class BackingAppParameters {

	private final String name;

	private final Map<String, String> properties;

	public BackingAppParameters(String name, Map<String, String> properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}
}
