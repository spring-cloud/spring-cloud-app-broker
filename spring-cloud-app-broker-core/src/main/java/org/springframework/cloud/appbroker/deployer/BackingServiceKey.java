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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

public class BackingServiceKey {

	private String serviceInstanceName;

	private String serviceKeyName;

	private Map<String, Object> parameters;

	private Map<String, String> properties;

	private List<ParametersTransformerSpec> parametersTransformers;

	private BackingServiceKey() {
	}

	public BackingServiceKey(String serviceInstanceName,
		String serviceKeyName,
		Map<String, Object> parameters,
		Map<String, String> properties,
		List<ParametersTransformerSpec> parametersTransformers) {
		this.serviceInstanceName = serviceInstanceName;
		this.serviceKeyName = serviceKeyName;
		this.parameters = parameters;
		this.properties = properties;
		this.parametersTransformers = parametersTransformers;
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public void setServiceInstanceName(String serviceInstanceName) {
		this.serviceInstanceName = serviceInstanceName;
	}

	public String getServiceKeyName() {
		return serviceKeyName;
	}

	public void setServiceKeyName(String serviceKeyName) {
		this.serviceKeyName = serviceKeyName;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public void addParameter(String key, Object value) {
		parameters.put(key, value);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public List<ParametersTransformerSpec> getParametersTransformers() {
		return parametersTransformers;
	}

	public void setParametersTransformers(List<ParametersTransformerSpec> parametersTransformers) {
		this.parametersTransformers = parametersTransformers;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BackingServiceKey that = (BackingServiceKey) o;
		return Objects.equals(serviceInstanceName, that.serviceInstanceName) &&
			Objects.equals(serviceKeyName, that.serviceKeyName) &&
			Objects.equals(parameters, that.parameters) &&
			Objects.equals(properties, that.properties) &&
			Objects.equals(parametersTransformers, that.parametersTransformers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceInstanceName, serviceKeyName, parameters, properties, parametersTransformers);
	}

	@Override
	public String toString() {
		return "BackingServiceKey{" +
			"serviceInstanceName='" + serviceInstanceName + '\'' +
			", serviceKeyName='" + serviceKeyName + '\'' +
			", parameters=" + parameters +
			", properties=" + properties +
			", parametersTransformers=" + parametersTransformers +
			'}';
	}

	public static BackingServiceKeyBuilder builder() {
		return new BackingServiceKeyBuilder();
	}

	public static final class BackingServiceKeyBuilder {

		private String serviceInstanceName;

		private String serviceKeyName;

		private final Map<String, Object> parameters = new HashMap<>();

		private final Map<String, String> properties = new HashMap<>();

		private final List<ParametersTransformerSpec> parameterTransformers = new ArrayList<>();

		private BackingServiceKeyBuilder() {
		}

		public BackingServiceKeyBuilder backingService(BackingServiceKey backingService) {
			return this.serviceInstanceName(backingService.getServiceInstanceName())
				.serviceKeyName(backingService.getServiceKeyName())
				.parameters(backingService.getParameters())
				.properties(backingService.getProperties())
				.parameterTransformers(backingService.getParametersTransformers());
		}

		public BackingServiceKeyBuilder serviceInstanceName(String serviceInstanceName) {
			this.serviceInstanceName = serviceInstanceName;
			return this;
		}

		public BackingServiceKeyBuilder serviceKeyName(String serviceKeyName) {
			this.serviceKeyName = serviceKeyName;
			return this;
		}

		public BackingServiceKeyBuilder parameters(Map<String, Object> parameters) {
			if (!CollectionUtils.isEmpty(parameters)) {
				this.parameters.putAll(parameters);
			}
			return this;
		}

		public BackingServiceKeyBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public BackingServiceKeyBuilder parameterTransformers(List<ParametersTransformerSpec> parameterTransformers) {
			if (!CollectionUtils.isEmpty(parameterTransformers)) {
				this.parameterTransformers.addAll(parameterTransformers);
			}
			return this;
		}

		public BackingServiceKeyBuilder parameterTransformers(ParametersTransformerSpec... parameterTransformers) {
			if (parameterTransformers != null) {
				this.parameterTransformers(Arrays.asList(parameterTransformers));
			}
			return this;
		}

		public BackingServiceKey build() {
			return new BackingServiceKey(serviceInstanceName, serviceKeyName, parameters, properties,
				parameterTransformers);
		}

	}

}
