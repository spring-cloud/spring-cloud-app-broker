/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.util.CollectionUtils;

public class CreateServiceInstanceRequest {

	private final String serviceInstanceName;
	private final String name;
	private final String plan;
	private final Map<String, Object> parameters;
	private final Map<String, String> properties;

	CreateServiceInstanceRequest(String serviceInstanceName,
								 String name,
								 String plan,
								 Map<String, Object> parameters,
								 Map<String, String> properties) {
		this.serviceInstanceName = serviceInstanceName;
		this.name = name;
		this.plan = plan;
		this.parameters = parameters;
		this.properties = properties;
	}

	public static CreateServiceInstanceRequestBuilder builder() {
		return new CreateServiceInstanceRequestBuilder();
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public String getName() {
		return name;
	}

	public String getPlan() {
		return plan;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static class CreateServiceInstanceRequestBuilder {

		private String serviceInstanceName;
		private String name;
		private String plan;
		private final Map<String, Object> parameters = new HashMap<>();
		private final Map<String, String> properties = new HashMap<>();

		CreateServiceInstanceRequestBuilder() {
		}

		public CreateServiceInstanceRequestBuilder serviceInstanceName(String serviceInstanceName) {
			this.serviceInstanceName = serviceInstanceName;
			return this;
		}

		public CreateServiceInstanceRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		public CreateServiceInstanceRequestBuilder plan(String plan) {
			this.plan = plan;
			return this;
		}

		public CreateServiceInstanceRequestBuilder parameters(String key, String value) {
			if (key != null && value != null) {
				this.parameters.put(key, value);
			}
			return this;
		}

		public CreateServiceInstanceRequestBuilder parameters(Map<String, Object> parameters) {
			if (!CollectionUtils.isEmpty(parameters)) {
				this.parameters.putAll(parameters);
			}
			return this;
		}

		public CreateServiceInstanceRequestBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public CreateServiceInstanceRequest build() {
			return new CreateServiceInstanceRequest(serviceInstanceName, name, plan, parameters, properties);
		}
	}

}
