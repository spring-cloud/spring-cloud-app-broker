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

public class BackingService {

	private String serviceInstanceName;

	private String name; // a service definition

	private String plan;

	private String previousPlan; // Necessary to trigger update on plan mutation.

	private Map<String, Object> parameters;

	private Map<String, String> properties;

	private List<ParametersTransformerSpec> parametersTransformers;

	private boolean rebindOnUpdate;

	private BackingService() {
	}

	public BackingService(String serviceInstanceName,
		String name,
		String plan,
		String previousPlan,
		Map<String, Object> parameters,
		Map<String, String> properties,
		List<ParametersTransformerSpec> parametersTransformers,
		boolean rebindOnUpdate) {
		this.serviceInstanceName = serviceInstanceName;
		this.name = name;
		this.plan = plan;
		this.previousPlan = previousPlan;
		this.parameters = parameters;
		this.properties = properties;
		this.parametersTransformers = parametersTransformers;
		this.rebindOnUpdate = rebindOnUpdate;
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

	public String getPreviousPlan() {
		return previousPlan;
	}

	public void setPreviousPlan(String previousPlan) {
		this.previousPlan = previousPlan;
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

	/**
	 * Returns the requested updated plan if any. This enables to not trigger backing service when no plan update was
	 * requested.
	 */
	public String getUpdatedPlanIfAny() {
		if (getPlan() == null) {
			return null; //should only happen in incomplete unit tests
		}

		boolean planWasUpdated = getPreviousPlan() != null && !getPlan().equals(getPreviousPlan());
		if (planWasUpdated) {
			return getPlan();
		}
		return null;
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
			Objects.equals(previousPlan, that.previousPlan) &&
			Objects.equals(parameters, that.parameters) &&
			Objects.equals(properties, that.properties) &&
			Objects.equals(parametersTransformers, that.parametersTransformers) &&
			rebindOnUpdate == that.rebindOnUpdate;
	}

	@Override
	public int hashCode() {
		return Objects
			.hash(serviceInstanceName, name, plan, previousPlan, parameters, properties, parametersTransformers,
				rebindOnUpdate);
	}

	@Override
	public String toString() {
		return "BackingService{" +
			"serviceInstanceName='" + serviceInstanceName + '\'' +
			", name='" + name + '\'' +
			", plan='" + plan + '\'' +
			", previousPlan='" + previousPlan + '\'' +
			", parameters=" + parameters +
			", properties=" + properties +
			", parametersTransformers=" + parametersTransformers +
			", rebindOnUpdate=" + rebindOnUpdate +
			'}';
	}

	public static BackingServiceBuilder builder() {
		return new BackingServiceBuilder();
	}

	public static final class BackingServiceBuilder {

		private String serviceInstanceName;

		private String name;

		private String plan;

		private String previousPlan;

		private final Map<String, Object> parameters = new HashMap<>();

		private final Map<String, String> properties = new HashMap<>();

		private final List<ParametersTransformerSpec> parameterTransformers = new ArrayList<>();

		private boolean rebindOnUpdate;

		private BackingServiceBuilder() {
		}

		public BackingServiceBuilder backingService(BackingService backingService) {
			return this.serviceInstanceName(backingService.getServiceInstanceName())
				.name(backingService.getName())
				.plan(backingService.getPlan())
				.previousPlan(backingService.getPreviousPlan())
				.parameters(backingService.getParameters())
				.properties(backingService.getProperties())
				.parameterTransformers(backingService.getParametersTransformers())
				.rebindOnUpdate(backingService.isRebindOnUpdate());
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

		public BackingServiceBuilder previousPlan(String previousPlan) {
			this.previousPlan = previousPlan;
			return this;
		}

		public BackingServiceBuilder parameters(Map<String, Object> parameters) {
			if (!CollectionUtils.isEmpty(parameters)) {
				this.parameters.putAll(parameters);
			}
			return this;
		}

		public BackingServiceBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public BackingServiceBuilder parameterTransformers(List<ParametersTransformerSpec> parameterTransformers) {
			if (!CollectionUtils.isEmpty(parameterTransformers)) {
				this.parameterTransformers.addAll(parameterTransformers);
			}
			return this;
		}

		public BackingServiceBuilder parameterTransformers(ParametersTransformerSpec... parameterTransformers) {
			if (parameterTransformers != null) {
				this.parameterTransformers(Arrays.asList(parameterTransformers));
			}
			return this;
		}

		public BackingServiceBuilder rebindOnUpdate(boolean rebindOnUpdate) {
			this.rebindOnUpdate = rebindOnUpdate;
			return this;
		}

		public BackingService build() {
			return new BackingService(serviceInstanceName, name, plan, previousPlan, parameters, properties,
				parameterTransformers,
				rebindOnUpdate);
		}

	}

}
