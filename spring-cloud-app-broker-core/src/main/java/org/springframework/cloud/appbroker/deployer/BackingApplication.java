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
import java.util.Objects;

public class BackingApplication {

	private static final String VALUE_HIDDEN = "<value hidden>";
	
	private String name;
	private String path;
	private Map<String, String> properties;
	private Map<String, String> environment;
	private List<String> services;
	private List<ParametersTransformerSpec> parametersTransformers;
	private List<CredentialProviderSpec> credentialProviders;

	public BackingApplication(BackingApplication backingApplicationToCopy) {
		this.name = backingApplicationToCopy.name;
		this.path = backingApplicationToCopy.path;
		this.properties = backingApplicationToCopy.properties == null
			? new HashMap<>()
			: new HashMap<>(backingApplicationToCopy.properties);
		this.environment = backingApplicationToCopy.environment == null
			? new HashMap<>()
			: new HashMap<>(backingApplicationToCopy.environment);
		this.services = backingApplicationToCopy.services == null
			? new ArrayList<>()
			: new ArrayList<>(backingApplicationToCopy.services);
		this.parametersTransformers = backingApplicationToCopy.parametersTransformers == null
			? new ArrayList<>()
			: new ArrayList<>(backingApplicationToCopy.parametersTransformers);
		this.credentialProviders = backingApplicationToCopy.credentialProviders == null
			? new ArrayList<>()
			: new ArrayList<>(backingApplicationToCopy.credentialProviders);
	}

	private BackingApplication() {
	}

	BackingApplication(String name, String path, Map<String, String> properties,
					   Map<String, String> environment, List<String> services,
					   List<ParametersTransformerSpec> parametersTransformers,
					   List<CredentialProviderSpec> credentialProviders) {
		this.name = name;
		this.path = path;
		this.properties = properties;
		this.environment = environment;
		this.services = services;
		this.parametersTransformers = parametersTransformers;
		this.credentialProviders = credentialProviders;
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

	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	public void addEnvironment(String key, String value) {
		environment.put(key, value);
	}

	public List<String> getServices() {
		return services;
	}

	public void setServices(List<String> services) {
		this.services = services;
	}

	public List<ParametersTransformerSpec> getParametersTransformers() {
		return parametersTransformers;
	}

	public void setParametersTransformers(List<ParametersTransformerSpec> parametersTransformers) {
		this.parametersTransformers = parametersTransformers;
	}

	public List<CredentialProviderSpec> getCredentialProviders() {
		return credentialProviders;
	}

	public void setCredentialProviders(List<CredentialProviderSpec> credentialProviders) {
		this.credentialProviders = credentialProviders;
	}

	public static BackingApplicationBuilder builder() {
		return new BackingApplicationBuilder();
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BackingApplication)) {
			return false;
		}
		BackingApplication that = (BackingApplication) o;
		return Objects.equals(name, that.name) &&
			Objects.equals(path, that.path) &&
			Objects.equals(properties, that.properties) &&
			Objects.equals(environment, that.environment) &&
			Objects.equals(services, that.services) &&
			Objects.equals(parametersTransformers, that.parametersTransformers) &&
			Objects.equals(credentialProviders, that.credentialProviders);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(name, path, properties, environment, services,
			parametersTransformers, credentialProviders);
	}

	@Override
	public String toString() {
		return "BackingApplication{" +
			"name='" + name + '\'' +
			", path='" + path + '\'' +
			", properties=" + properties +
			", environment=" + sanitizeEnvironment(environment) +
			", services=" + services +
			", parametersTransformers=" + parametersTransformers +
			", credentialProviders=" + credentialProviders +
			'}';
	}

	private Map<String, String> sanitizeEnvironment(Map<String, String> environment) {
		if (environment == null) {
			return null;
		}

		HashMap<String, String> sanitizedEnvironment = new HashMap<>();
		environment.forEach((key, value) -> sanitizedEnvironment.put(key, VALUE_HIDDEN));

		return sanitizedEnvironment;
	}

	public static class BackingApplicationBuilder {

		private String name;
		private String path;
		private final Map<String, String> properties = new HashMap<>();
		private final Map<String, String> environment = new HashMap<>();
		private final List<String> services = new ArrayList<>();
		private final List<ParametersTransformerSpec> parameterTransformers = new ArrayList<>();
		private final List<CredentialProviderSpec> credentialProviders = new ArrayList<>();

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

		public BackingApplicationBuilder parameterTransformers(ParametersTransformerSpec... parameterTransformers) {
			this.parameterTransformers.addAll(Arrays.asList(parameterTransformers));
			return this;
		}

		public BackingApplicationBuilder credentialProviders(CredentialProviderSpec... credentialProviders) {
			this.credentialProviders.addAll(Arrays.asList(credentialProviders));
			return this;
		}

		public BackingApplication build() {
			return new BackingApplication(name, path, properties, environment, services,
				parameterTransformers, credentialProviders);
		}
	}
}
