/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Map;

public class DeleteServiceKeyRequest {

	private final String serviceInstanceName;
	private final String serviceKeyName;
	private final Map<String, String> properties; // to access space target property


	public DeleteServiceKeyRequest(String serviceInstanceName, String serviceKeyName, Map<String, String> properties) {
		this.serviceInstanceName = serviceInstanceName;
		this.serviceKeyName = serviceKeyName;
		this.properties = properties;
	}

	public static DeleteServiceKeyRequestBuilder builder() {
		return new DeleteServiceKeyRequestBuilder();
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public String getServiceKeyName() {
		return serviceKeyName;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static final class DeleteServiceKeyRequestBuilder {

		private String serviceInstanceName;
		private String serviceKeyName;
		private Map<String, String> properties;

		private DeleteServiceKeyRequestBuilder() {
		}

		public DeleteServiceKeyRequestBuilder serviceInstanceName(String name) {
			this.serviceInstanceName = name;
			return this;
		}

		public DeleteServiceKeyRequest build() {
			return new DeleteServiceKeyRequest(serviceInstanceName, serviceKeyName, properties);
		}

		public DeleteServiceKeyRequestBuilder serviceKeyName(String serviceKeyName) {
			this.serviceKeyName = serviceKeyName;
			return this;
		}

		public DeleteServiceKeyRequestBuilder properties(Map<String, String> properties) {
			this.properties = properties;
			return this;

		}
	}
}
