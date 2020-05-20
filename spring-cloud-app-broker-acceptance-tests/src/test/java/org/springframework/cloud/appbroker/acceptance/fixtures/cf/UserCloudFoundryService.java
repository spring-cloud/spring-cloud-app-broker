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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.util.Map;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration.USER_CLIENT_ID;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration.USER_CLIENT_SECRET;

@Service
public final class UserCloudFoundryService {

	private static final Logger LOG = LoggerFactory.getLogger(UserCloudFoundryService.class);

	private final CloudFoundryOperations cloudFoundryOperations;
	private final String targetOrg;
	private final String targetSpace;

	public UserCloudFoundryService(CloudFoundryOperations cloudFoundryOperations, CloudFoundryProperties cloudFoundryProperties) {
		DefaultCloudFoundryOperations sourceCloudFoundryOperations =
			(DefaultCloudFoundryOperations) cloudFoundryOperations;

		this.targetOrg = cloudFoundryProperties.getDefaultOrg() + "-instances";
		this.targetSpace = cloudFoundryProperties.getDefaultSpace();

		ClientCredentialsGrantTokenProvider tokenProvider = ClientCredentialsGrantTokenProvider.builder()
			.clientId(USER_CLIENT_ID)
			.clientSecret(USER_CLIENT_SECRET)
			.build();

		ReactorCloudFoundryClient cloudFoundryClient = ReactorCloudFoundryClient.builder()
			.from((ReactorCloudFoundryClient) sourceCloudFoundryOperations.getCloudFoundryClient())
			.tokenProvider(tokenProvider)
			.build();

		this.cloudFoundryOperations = DefaultCloudFoundryOperations
			.builder()
			.from(sourceCloudFoundryOperations)
			.space(targetSpace)
			.organization(targetOrg)
			.cloudFoundryClient(cloudFoundryClient)
			.build();
	}

	public String getOrgName() {
		return this.targetOrg;
	}

	public String getSpaceName() {
		return this.targetSpace;
	}

	public Mono<Void> deleteServiceInstance(String serviceInstanceName) {
		return getServiceInstance(serviceInstanceName)
			.flatMap(si -> cloudFoundryOperations.services().deleteInstance(DeleteServiceInstanceRequest.builder()
				.name(si.getName())
				.build())
				.doOnSuccess(v -> LOG.info("Success deleting service instance. serviceInstanceName={}",
					serviceInstanceName))
				.doOnError(e -> LOG.error(String.format("Error deleting service instance. serviceInstanceName=%s, " +
					"error=%s", serviceInstanceName, e.getMessage()), e))
				.onErrorResume(e -> Mono.empty()))
			.doOnError(e -> LOG.warn(String.format("Error getting service instance. serviceInstanceName=%s, " +
				"error=%s", serviceInstanceName, e.getMessage()), e))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> createServiceInstance(String planName,
		String serviceName,
		String serviceInstanceName,
		Map<String, Object> parameters) {
		return cloudFoundryOperations.services().createInstance(CreateServiceInstanceRequest.builder()
			.planName(planName)
			.serviceName(serviceName)
			.serviceInstanceName(serviceInstanceName)
			.parameters(parameters)
			.build())
			.doOnSuccess(item -> LOG.info("Success creating service instance. serviceInstanceName={}",
				serviceInstanceName))
			.doOnError(e -> LOG.error(String.format("Error creating service instance. serviceInstanceName=%s, " +
				"error=%s", serviceInstanceName, e.getMessage()), e));
	}

	public Mono<Void> updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		return cloudFoundryOperations.services()
			.updateInstance(UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.build())
			.doOnSuccess(item -> LOG.info("Updated service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error updating service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName) {
		return cloudFoundryOperations.services()
			.getInstance(GetServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.build())
			.doOnSuccess(item -> LOG.info("Got service instance " + serviceInstanceName))
			.doOnError(error -> LOG.error("Error getting service instance " + serviceInstanceName + ": " + error));
	}
}
