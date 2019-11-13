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

package org.springframework.cloud.appbroker.extensions.targets;

import java.util.Map;

public class ServiceInstanceGuidSuffix extends TargetFactory<ServiceInstanceGuidSuffix.Config> {

	private static final int CF_SERVICE_NAME_MAX_LENGTH = 50;

	private static final String CONCAT_DELIM = "-";

	public ServiceInstanceGuidSuffix() {
		super(Config.class);
	}

	@Override
	public Target create(Config config) {
		return this::apply;
	}

	private ArtifactDetails apply(Map<String, String> properties, String name, String serviceInstanceId, String backingServiceName, String backingServicePlanName) {
		final int availableLength = calculateAvailableLength(serviceInstanceId);
		String modifiedName = name;
		if (name.length() > calculateAvailableLength(serviceInstanceId)) {
			modifiedName = name.substring(0, availableLength);
		}
		return ArtifactDetails.builder()
			.name(assembleName(modifiedName, serviceInstanceId))
			.properties(properties)
			.build();
	}

	private int calculateAvailableLength(String serviceInstanceId) {
		return CF_SERVICE_NAME_MAX_LENGTH - CONCAT_DELIM.length() - serviceInstanceId.length();
	}

	private String assembleName(String appName, String serviceInstanceId) {
		return appName + CONCAT_DELIM + serviceInstanceId;
	}

	public static class Config {
	}

}
