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

public class BackingService {

	private String serviceInstanceName;
	private String name;
	private String plan;
	private Map<String, Object> parameters;
	private Map<String, String> properties;
	private List<ParametersTransformerSpec> parametersTransformers;
	private boolean rebindOnUpdate;

	private BackingService() {
	}

	BackingService(String serviceInstanceName,
				   String name,
				   String plan,
				   Map<String, Object> parameters,
				   Map<String, String> properties,
				   List<ParametersTransformerSpec> parametersTransformers,
				   boolean rebindOnUpdate) {
		this.serviceInstanceName = serviceInstanceName;
		this.name = name;
		this.plan = plan;
		this.parameters = parameters;
		this.properties = properties;
		this.parametersTransformers = parametersTransformers;
		this.rebindOnUpdate = rebindOnUpdate;
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
		this.parametersTransformers = backingServiceToCopy.parametersTransformers == null
			? new ArrayList<>()
			: new ArrayList<>(backingServiceToCopy.parametersTransformers);
		this.rebindOnUpdate = backingServiceToCopy.rebindOnUpdate;
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

	public boolean isRebindOnUpdate() {
		return rebindOnUpdate;
	}

	public void setRebindOnUpdate(boolean rebindOnUpdate) {
		this.rebindOnUpdate = rebindOnUpdate;
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
			Objects.equals(properties, that.properties) &&
			Objects.equals(parametersTransformers, that.parametersTransformers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceInstanceName, name, plan, parameters, properties, parametersTransformers);
	}

	@Override
	public String toString() {
		return "BackingService{" +
			"serviceInstanceName='" + serviceInstanceName + '\'' +
			", name='" + name + '\'' +
			", plan='" + plan + '\'' +
			", parameters=" + parameters +
			", properties=" + properties +
			", parametersTransformers=" + parametersTransformers +
			'}';
	}

	public static BackingServiceBuilder builder() {
		return new BackingServiceBuilder();
	}

	public static final class BackingServiceBuilder {

		private String serviceInstanceName;
		private String name;
		private String plan;
		private Map<String, Object> parameters = new HashMap<>();
		private Map<String, String> properties = new HashMap<>();
		private final List<ParametersTransformerSpec> parameterTransformers = new ArrayList<>();
		private boolean rebindOnUpdate;

		BackingServiceBuilder() {
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

		public BackingServiceBuilder parameters(Map<String, Object> parameters) {
			this.parameters = parameters;
			return this;
		}

		public BackingServiceBuilder properties(Map<String, String> properties) {
			this.properties = properties;
			return this;
		}

		public BackingServiceBuilder parameterTransformers(ParametersTransformerSpec... parameterTransformers) {
			this.parameterTransformers.addAll(Arrays.asList(parameterTransformers));
			return this;
		}

		public BackingServiceBuilder rebindOnUpdate(boolean rebindOnUpdate) {
			this.rebindOnUpdate = rebindOnUpdate;
			return this;
		}

		public BackingService build() {
			return new BackingService(serviceInstanceName, name, plan, parameters, properties, parameterTransformers, rebindOnUpdate);
		}
	}

}
