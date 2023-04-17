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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

/**
 * An application deployed as part of the service provisioning process
 */
@SuppressWarnings("PMD.GodClass")
public class BackingApplication {

	private static final String VALUE_HIDDEN = "<value hidden>";

	private String name;

	private String path;

	private Map<String, String> properties;

	private Map<String, Object> environment;

	private List<ServicesSpec> services;

	private List<ParametersTransformerSpec> parametersTransformers;

	private BackingApplication() {
	}

	/**
	 * Construct a new {@link BackingApplication}
	 *
	 * @param name the name of the application
	 * @param path the path to the application
	 * @param properties the properties
	 * @param environment the environment variables
	 * @param services the services required by the application
	 * @param parametersTransformers the parameter transformers
	 */
	public BackingApplication(String name, String path,
		Map<String, String> properties,
		Map<String, Object> environment,
		List<ServicesSpec> services,
		List<ParametersTransformerSpec> parametersTransformers) {
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

	/**
	 * Add a single property
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}

	public Map<String, Object> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, Object> environment) {
		this.environment = environment;
	}

	/**
	 * Add a single environment value
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void addEnvironment(String key, Object value) {
		environment.put(key, value);
	}

	public List<ServicesSpec> getServices() {
		return services;
	}

	public void setServices(List<ServicesSpec> services) {
		this.services = services;
	}

	public List<ParametersTransformerSpec> getParametersTransformers() {
		return parametersTransformers;
	}

	public void setParametersTransformers(List<ParametersTransformerSpec> parametersTransformers) {
		this.parametersTransformers = parametersTransformers;
	}

	/**
	 * Create a builder that provides a fluent API for constructing a {@literal BackingApplication}.
	 *
	 * @return the builder
	 */
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
			Objects.equals(parametersTransformers, that.parametersTransformers);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(name, path, properties, environment, services, parametersTransformers);
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
			'}';
	}

	private Map<String, Object> sanitizeEnvironment(Map<String, Object> environment) {
		if (environment == null) {
			return null;
		}

		HashMap<String, Object> sanitizedEnvironment = new HashMap<>();
		environment.forEach((key, value) -> sanitizedEnvironment.put(key, VALUE_HIDDEN));

		return sanitizedEnvironment;
	}

	/**
	 * Provides a fluent API for constructing a {@literal BackingApplication}.
	 */
	public static final class BackingApplicationBuilder {

		private String name;

		private String path;

		private final Map<String, String> properties = new HashMap<>();

		private final Map<String, Object> environment = new HashMap<>();

		private final List<ServicesSpec> services = new ArrayList<>();

		private final List<ParametersTransformerSpec> parameterTransformers = new ArrayList<>();

		private BackingApplicationBuilder() {
		}

		/**
		 * Build a backing application based on another application definition
		 *
		 * @param backingApplication the backing application from which to copy properties
		 * @return the builder
		 */
		public BackingApplicationBuilder backingApplication(BackingApplication backingApplication) {
			this.name(backingApplication.getName())
				.path(backingApplication.getPath())
				.properties(backingApplication.getProperties())
				.environment(backingApplication.getEnvironment());
			if (!CollectionUtils.isEmpty(backingApplication.getServices())) {
				this.services(backingApplication.getServices().stream()
					.map(spec -> ServicesSpec.builder()
						.spec(spec)
						.build())
					.collect(Collectors.toList()));
			}
			if (!CollectionUtils.isEmpty(backingApplication.getParametersTransformers())) {
				this.parameterTransformers(backingApplication.getParametersTransformers().stream()
					.map(spec -> ParametersTransformerSpec.builder()
						.spec(spec)
						.build())
					.collect(Collectors.toList()));
			}
			return this;
		}

		/**
		 * The name of the application
		 *
		 * @param name the name
		 * @return the builder
		 */
		public BackingApplicationBuilder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * The path to the application
		 *
		 * @param path the path
		 * @return the builder
		 */
		public BackingApplicationBuilder path(String path) {
			this.path = path;
			return this;
		}

		/**
		 * Properties that describe the application
		 *
		 * @param key the property key
		 * @param value the property value
		 * @return the builder
		 */
		public BackingApplicationBuilder property(String key, String value) {
			if (key != null && value != null) {
				this.properties.put(key, value);
			}
			return this;
		}

		/**
		 * Properties that describe the application
		 *
		 * @param properties the properties
		 * @return the builder
		 */
		public BackingApplicationBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		/**
		 * Environment variables to be set for the application
		 *
		 * @param key the env var key
		 * @param value the env var value
		 * @return the builder
		 */
		public BackingApplicationBuilder environment(String key, String value) {
			if (key != null && value != null) {
				this.environment.put(key, value);
			}
			return this;
		}

		/**
		 * Environment variables to be set for the application
		 *
		 * @param environment the env vars
		 * @return the builder
		 */
		public BackingApplicationBuilder environment(Map<String, Object> environment) {
			if (!CollectionUtils.isEmpty(environment)) {
				this.environment.putAll(environment);
			}
			return this;
		}

		/**
		 * Services required by the application
		 *
		 * @param services the services
		 * @return the builder
		 */
		public BackingApplicationBuilder services(List<ServicesSpec> services) {
			if (!CollectionUtils.isEmpty(services)) {
				this.services.addAll(services);
			}
			return this;
		}

		/**
		 * Services required by the application
		 *
		 * @param services the services
		 * @return the builder
		 */
		public BackingApplicationBuilder services(ServicesSpec... services) {
			if (services != null) {
				this.services(Arrays.asList(services));
			}
			return this;
		}

		/**
		 * Parameter transformers for the application
		 *
		 * @param parameterTransformers the parameter transformers
		 * @return the builder
		 */
		public BackingApplicationBuilder parameterTransformers(List<ParametersTransformerSpec> parameterTransformers) {
			if (!CollectionUtils.isEmpty(parameterTransformers)) {
				this.parameterTransformers.addAll(parameterTransformers);
			}
			return this;
		}

		/**
		 * Parameter transformers for the application
		 *
		 * @param parameterTransformers the parameter transformers
		 * @return the builder
		 */
		public BackingApplicationBuilder parameterTransformers(ParametersTransformerSpec... parameterTransformers) {
			if (parameterTransformers != null) {
				this.parameterTransformers(Arrays.asList(parameterTransformers));
			}
			return this;
		}

		/**
		 * Construct a {@link BackingApplication} from the provided values.
		 *
		 * @return the newly constructed {@literal BackingApplication}
		 */
		public BackingApplication build() {
			return new BackingApplication(name, path, properties, environment, services, parameterTransformers);
		}

	}

}
