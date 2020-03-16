/*
 * Copyright 2002-2020 the original author or authors.
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

public class CreateServiceKeyRequest {

	private final String serviceInstanceName;

	private final String serviceKeyName;

	private final Map<String, Object> parameters;

	private final Map<String, String> properties;


	public CreateServiceKeyRequest(String serviceInstanceName,
		String serviceKeyName,
		Map<String, Object> parameters,
		Map<String, String> properties) {
		this.serviceInstanceName = serviceInstanceName;
		this.serviceKeyName = serviceKeyName;
		this.parameters = parameters;
		this.properties = properties;
	}

	public static CreateServiceKeyRequestBuilder builder() {
		return new CreateServiceKeyRequestBuilder();
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public String getServiceKeyName() {
		return serviceKeyName;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static final class CreateServiceKeyRequestBuilder {

		private String serviceInstanceName;

		private String serviceKeyName;

		private final Map<String, Object> parameters = new HashMap<>();

		private Map<String, String> properties;

		private CreateServiceKeyRequestBuilder() {
		}

		public CreateServiceKeyRequestBuilder serviceInstanceName(String serviceInstanceName) {
			this.serviceInstanceName = serviceInstanceName;
			return this;
		}

		public CreateServiceKeyRequestBuilder serviceKeyName(String serviceKeyName) {
			this.serviceKeyName = serviceKeyName;
			return this;
		}

		public CreateServiceKeyRequestBuilder parameters(String key, String value) {
			if (key != null && value != null) {
				this.parameters.put(key, value);
			}
			return this;
		}

		public CreateServiceKeyRequestBuilder parameters(Map<String, Object> parameters) {
			if (!CollectionUtils.isEmpty(parameters)) {
				this.parameters.putAll(parameters);
			}
			return this;
		}

		public CreateServiceKeyRequestBuilder properties(Map<String, String> properties) {
			this.properties = properties;
			return this;
		}

		public CreateServiceKeyRequest build() {
			return new CreateServiceKeyRequest(serviceInstanceName, serviceKeyName, parameters, properties);
		}

	}

}
