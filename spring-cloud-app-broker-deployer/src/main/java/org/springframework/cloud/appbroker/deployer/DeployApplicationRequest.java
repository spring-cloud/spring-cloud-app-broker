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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

public class DeployApplicationRequest {

	private final String name;

	private final String path;

	private final Map<String, String> properties;

	private final Map<String, Object> environment;

	private final List<String> services;

	private final String serviceInstanceId;

	DeployApplicationRequest(String name, String path, Map<String, String> properties,
							 Map<String, Object> environment, List<String> services,
							 String serviceInstanceId) {
		this.name = name;
		this.path = path;
		this.properties = properties;
		this.environment = environment;
		this.services = services;
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public Map<String, Object> getEnvironment() {
		return environment;
	}

	public List<String> getServices() {
		return services;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public static DeployApplicationRequestBuilder builder() {
		return new DeployApplicationRequestBuilder();
	}

	public static class DeployApplicationRequestBuilder {

		private String name;
		private String path;
		private final Map<String, String> properties = new HashMap<>();
		private final Map<String, Object> environment = new HashMap<>();
		private final List<String> services = new ArrayList<>();
		private String serviceInstanceId;

		DeployApplicationRequestBuilder() {
		}

		public DeployApplicationRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		public DeployApplicationRequestBuilder path(String path) {
			this.path = path;
			return this;
		}

		public DeployApplicationRequestBuilder property(String key, String value) {
			if (key != null && value != null) {
				this.properties.put(key, value);
			}
			return this;
		}

		public DeployApplicationRequestBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public DeployApplicationRequestBuilder environment(String key, String value) {
			if (key != null && value != null) {
				this.environment.put(key, value);
			}
			return this;
		}

		public DeployApplicationRequestBuilder environment(Map<String, Object> environment) {
			if (!CollectionUtils.isEmpty(environment)) {
				this.environment.putAll(environment);
			}
			return this;
		}

		public DeployApplicationRequestBuilder service(String service) {
			if (service != null) {
				this.services.add(service);
			}
			return this;
		}

		public DeployApplicationRequestBuilder services(List<String> services) {
			if (!CollectionUtils.isEmpty(services)) {
				this.services.addAll(services);
			}
			return this;
		}

		public DeployApplicationRequestBuilder serviceInstanceId(String serviceInstanceId) {
			this.serviceInstanceId = serviceInstanceId;
			return this;
		}

		public DeployApplicationRequest build() {
			return new DeployApplicationRequest(name, path, properties, environment, services, serviceInstanceId);
		}
	}

}
