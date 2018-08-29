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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackingApplication {

	private String name;
	private String path;
	private Map<String, String> properties;
	private Map<String, String> environment;
	private List<String> services;
	private List<String> parametersTransformers;

	private BackingApplication() {
	}

	private BackingApplication(String name, String path, Map<String, String> properties,
							   Map<String, String> environment, List<String> services,
							   List<String> parametersTransformers) {
		this.name = name;
		this.path = path;
		this.properties = properties;
		this.environment = environment;
		this.services = services;
		this.parametersTransformers = parametersTransformers;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	public List<String> getServices() {
		return services;
	}

	public void setServices(List<String> services) {
		this.services = services;
	}

	public List<String> getParametersTransformers() {
		return parametersTransformers;
	}

	public void setParametersTransformers(List<String> parametersTransformers) {
		this.parametersTransformers = parametersTransformers;
	}

	public static BackingApplicationBuilder builder() {
		return new BackingApplicationBuilder();
	}

	public static class BackingApplicationBuilder {

		private String name;
		private String path;
		private Map<String, String> properties = new HashMap<>();
		private Map<String, String> environment = new HashMap<>();
		private List<String> services = new ArrayList<>();
		private List<String> parameterTransformers = new ArrayList<>();

		BackingApplicationBuilder() {
		}

		public BackingApplicationBuilder name(String name) {
			this.name = name;
			return this;
		}

		public BackingApplicationBuilder path(String path) {
			this.path = path;
			return this;
		}

		public BackingApplicationBuilder property(String key, String value) {
			this.properties.put(key, value);
			return this;
		}

		public BackingApplicationBuilder properties(Map<String, String> properties) {
			this.properties.putAll(properties);
			return this;
		}

		public BackingApplicationBuilder environment(String key, String value) {
			this.environment.put(key, value);
			return this;
		}

		public BackingApplicationBuilder environment(Map<String, String> environment) {
			this.environment.putAll(environment);
			return this;
		}

		public BackingApplicationBuilder services(String... services) {
			this.services.addAll(Arrays.asList(services));
			return this;
		}

		public BackingApplicationBuilder parameterTransformers(String... parameterTransformer) {
			this.parameterTransformers.addAll(Arrays.asList(parameterTransformer));
			return this;
		}

		public BackingApplication build() {
			return new BackingApplication(name, path, properties, environment, services, parameterTransformers);
		}
	}
}
