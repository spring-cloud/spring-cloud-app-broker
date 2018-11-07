/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.deployer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BackingService {

	private String serviceInstanceName;
	private String name;
	private String plan;
	private Map<String, String> parameters;
	private Map<String, String> properties;

	private BackingService() {
	}

	BackingService(String serviceInstanceName, String name, String plan, Map<String, String> parameters, Map<String, String> properties) {
		this.serviceInstanceName = serviceInstanceName;
		this.name = name;
		this.plan = plan;
		this.parameters = parameters;
		this.properties = properties;
	}

	BackingService(BackingService backingServiceToCopy) {
		this.serviceInstanceName = backingServiceToCopy.serviceInstanceName;
		this.name = backingServiceToCopy.name;
		this.plan = backingServiceToCopy.plan;
		this.parameters = backingServiceToCopy.parameters == null
			? new HashMap<>()
			: new HashMap<>(backingServiceToCopy.parameters);
		this.properties = backingServiceToCopy.properties == null
			? new HashMap<>()
			: new HashMap<>(backingServiceToCopy.properties);
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public void setServiceInstanceName(String serviceInstanceName) {
		this.serviceInstanceName = serviceInstanceName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BackingService that = (BackingService) o;
		return Objects.equals(serviceInstanceName, that.serviceInstanceName) &&
			Objects.equals(name, that.name) &&
			Objects.equals(plan, that.plan) &&
			Objects.equals(parameters, that.parameters) &&
			Objects.equals(properties, that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceInstanceName, name, plan, parameters, properties);
	}

	@Override
	public String toString() {
		return "BackingService{" +
			"serviceInstanceName='" + serviceInstanceName + '\'' +
			", name='" + name + '\'' +
			", plan='" + plan + '\'' +
			", parameters=" + parameters +
			", properties=" + properties +
			'}';
	}

	public static BackingServiceBuilder builder() {
		return new BackingServiceBuilder();
	}

	public static final class BackingServiceBuilder {

		private String serviceInstanceName;
		private String name;
		private String plan;
		private Map<String, String> parameters = new HashMap<>();
		private Map<String, String> properties = new HashMap<>();

		BackingServiceBuilder() {
		}

		public BackingService build() {
			return new BackingService(serviceInstanceName, name, plan, parameters, properties);
		}

		public BackingServiceBuilder serviceInstanceName(String serviceInstanceName) {
			this.serviceInstanceName = serviceInstanceName;
			return this;
		}

		public BackingServiceBuilder name(String name) {
			this.name = name;
			return this;
		}

		public BackingServiceBuilder plan(String plan) {
			this.plan = plan;
			return this;
		}

		public BackingServiceBuilder parameters(Map<String, String> parameters) {
			this.parameters = parameters;
			return this;
		}

		public BackingServiceBuilder properties(Map<String, String> properties) {
			this.properties = properties;
			return this;
		}
	}

}
