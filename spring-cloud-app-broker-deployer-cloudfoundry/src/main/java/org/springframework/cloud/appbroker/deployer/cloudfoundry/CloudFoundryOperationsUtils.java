/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.util.Map;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.deployer.DeploymentProperties;
import org.springframework.util.CollectionUtils;

public class CloudFoundryOperationsUtils {

	private final CloudFoundryOperations operations;

	public CloudFoundryOperationsUtils(CloudFoundryOperations operations) {
		this.operations = operations;
	}

	protected Mono<CloudFoundryOperations> getOperations(Map<String, String> properties) {
		return Mono.fromSupplier(() -> {
			DefaultCloudFoundryOperations.Builder builder = DefaultCloudFoundryOperations.builder()
				.from((DefaultCloudFoundryOperations) this.operations);

			if (!CollectionUtils.isEmpty(properties) && properties.containsKey(DeploymentProperties.ORGANIZATION_PROPERTY_KEY)) {
				builder.organization(properties.get(DeploymentProperties.ORGANIZATION_PROPERTY_KEY));
			}

			if (!CollectionUtils.isEmpty(properties) && properties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
				builder.space(properties.get(DeploymentProperties.TARGET_PROPERTY_KEY));
			}

			return builder.build();
		});
	}

	protected Mono<CloudFoundryOperations> getOperationsForOrgAndSpace(String organization, String space) {
		return Mono.fromSupplier(() -> DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.operations)
			.organization(organization)
			.space(space)
			.build());
	}

}
