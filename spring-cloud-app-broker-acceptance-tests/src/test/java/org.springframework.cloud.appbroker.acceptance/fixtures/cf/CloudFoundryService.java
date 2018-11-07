/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.organizations.CreateOrganizationRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.serviceadmin.CreateServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.DeleteServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.EnableServiceAccessRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.operations.spaces.Spaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class CloudFoundryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryService.class);

	private final CloudFoundryOperations cloudFoundryOperations;
	private final CloudFoundryProperties cloudFoundryProperties;

	public CloudFoundryService(CloudFoundryOperations cloudFoundryOperations,
							   CloudFoundryProperties cloudFoundryProperties) {
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.cloudFoundryProperties = cloudFoundryProperties;
	}

	public Mono<Void> enableServiceBrokerAccess(String serviceName) {
		return loggingMono(
			cloudFoundryOperations
				.serviceAdmin()
				.enableServiceAccess(EnableServiceAccessRequest.builder()
					.serviceName(serviceName)
					.build()));
	}

	public Mono<Void> createServiceBroker(String brokerName, String sampleBrokerAppName) {
		return loggingMono(
			getApplicationRoute(sampleBrokerAppName)
				.flatMap(url -> cloudFoundryOperations
					.serviceAdmin()
					.create(CreateServiceBrokerRequest.builder()
						.name(brokerName)
						.username("user")
						.password("password")
						.url(url)
						.build())));
	}

	private Mono<String> getApplicationRoute(String appName) {
		return loggingMono(
			cloudFoundryOperations
				.applications()
				.get(GetApplicationRequest.builder().name(appName).build())
				.map(ApplicationDetail::getUrls)
				.flatMapMany(Flux::fromIterable)
				.next()
				.map(url -> "https://" + url));
	}

	public Mono<Void> pushBrokerApp(String appName, Path appPath, String... backingAppProperties) {
		return loggingMono(
			cloudFoundryOperations
				.applications()
				.pushManifest(PushApplicationManifestRequest
					.builder()
					.manifest(ApplicationManifest
						.builder()
						.putAllEnvironmentVariables(appBrokerDeployerEnvironmentVariables())
						.putAllEnvironmentVariables(appBrokerCatalogEnvironmentVariables())
						.putAllEnvironmentVariables(backingAppEnvironmentVariables(backingAppProperties))
						.name(appName)
						.path(appPath)
						.memory(1024)
						.build())
					.build()));
	}

	public Mono<Void> deleteApp(String appName) {
		return loggingMono(cloudFoundryOperations
				.applications()
				.delete(DeleteApplicationRequest.builder().name(appName).build()))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceBroker(String brokerName) {
		return loggingMono(cloudFoundryOperations
				.serviceAdmin()
				.delete(DeleteServiceBrokerRequest.builder().name(brokerName).build()))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceInstance(String serviceInstanceName) {
		return loggingMono(
			getServiceInstanceFromList(serviceInstanceName)
				.flatMap(si ->
					cloudFoundryOperations
						.services()
						.deleteInstance(DeleteServiceInstanceRequest.builder().name(si.getName()).build())))
			.onErrorResume(e -> Mono.empty());
	}

	private Mono<ServiceInstanceSummary> getServiceInstanceFromList(String serviceInstanceName) {
		return cloudFoundryOperations
			.services()
			.listInstances()
			.filter(si -> si.getName().equals(serviceInstanceName))
			.next();
	}

	public Mono<Void> createServiceInstance(String planName,
											String serviceName,
											String serviceInstanceName,
											Map<String, Object> parameters) {
		return loggingMono(
			cloudFoundryOperations
				.services()
				.createInstance(CreateServiceInstanceRequest
					.builder()
					.planName(planName)
					.serviceName(serviceName)
					.serviceInstanceName(serviceInstanceName)
					.parameters(parameters)
					.build()));
	}

	public Mono<Void> updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		return loggingMono(
			cloudFoundryOperations
				.services()
				.updateInstance(UpdateServiceInstanceRequest
					.builder()
					.serviceInstanceName(serviceInstanceName)
					.parameters(parameters)
					.build()));
	}

	public Mono<ServiceInstanceSummary> getServiceInstance(String serviceInstanceName) {
		return loggingMono(
			getServiceInstanceFromList(serviceInstanceName));
	}

	public Mono<ServiceInstanceSummary> getServiceInstance(String serviceInstanceName, String space) {
		return loggingMono(
			createOperationsForSpace(space)
				.services()
				.listInstances()
				.filter(si -> si.getName().equals(serviceInstanceName))
				.next());
	}

	public Mono<List<ApplicationSummary>> getApplications() {
		return loggingMono(
			cloudFoundryOperations.applications().list().collectList());
	}

	public Mono<List<String>> getSpaces() {
		return this.cloudFoundryOperations.spaces().list().map(SpaceSummary::getName).collectList();
	}

	public Mono<ApplicationSummary> getApplicationSummaryByNameAndSpace(String appName, String space) {
		return loggingFlux(createOperationsForSpace(space)
			.applications()
			.list()
			.filter(applicationSummary -> applicationSummary.getName().equals(appName))
		).single();
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironmentByAppName(String appName) {
		return loggingMono(
			cloudFoundryOperations
				.applications()
				.getEnvironments(GetApplicationEnvironmentsRequest.builder().name(appName).build()));
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironmentByAppNameAndSpace(String appName, String space) {
		return loggingMono(
			createOperationsForSpace(space)
				.applications()
				.getEnvironments(GetApplicationEnvironmentsRequest.builder().name(appName).build()));
	}

	public Mono<SpaceSummary> getOrCreateDefaultSpace() {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();

		Spaces spaceOperations = DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.cloudFoundryOperations)
			.organization(cloudFoundryProperties.getDefaultOrg())
			.build()
			.spaces();

		final String defaultSpace = cloudFoundryProperties.getDefaultSpace();
		return loggingMono(
			getDefaultSpace(spaceOperations)
				.switchIfEmpty(spaceOperations
					.create(CreateSpaceRequest
						.builder()
						.name(defaultSpace)
						.organization(defaultOrg)
						.build())
					.then(getDefaultSpace(spaceOperations))));
	}

	public Mono<OrganizationSummary> getOrCreateDefaultOrganization() {
		Organizations organizationOperations = cloudFoundryOperations.organizations();

		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return loggingMono(getDefaultOrg(organizationOperations)
				.switchIfEmpty(organizationOperations
					.create(CreateOrganizationRequest
						.builder()
						.organizationName(defaultOrg)
						.build())
					.then(getDefaultOrg(organizationOperations))));
	}

	private CloudFoundryOperations createOperationsForSpace(String space) {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) cloudFoundryOperations)
			.organization(defaultOrg)
			.space(space)
			.build();
	}

	private Mono<OrganizationSummary> getDefaultOrg(Organizations orgOperations) {
		return orgOperations
			.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultOrg()))
			.next();
	}

	private Mono<SpaceSummary> getDefaultSpace(Spaces spaceOperations) {
		return spaceOperations
			.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultSpace()))
			.next();
	}

	private Map<String, String> appBrokerDeployerEnvironmentVariables() {
		Map<String, String> deployerVariables = new HashMap<>();
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.api-host",
			cloudFoundryProperties.getApiHost());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.api-port",
			String.valueOf(cloudFoundryProperties.getApiPort()));
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.default-org",
			cloudFoundryProperties.getDefaultOrg());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.default-space",
			cloudFoundryProperties.getDefaultSpace());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.skip-ssl-validation",
			String.valueOf(cloudFoundryProperties.isSkipSslValidation()));
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.properties.memory", "1024M");

		if (cloudFoundryProperties.getUsername() == null || cloudFoundryProperties.getPassword() == null) {
			deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.client-id",
				CloudFoundryClientConfiguration.ACCEPTANCE_TEST_OAUTH_CLIENT_ID);
			deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.client-secret",
				CloudFoundryClientConfiguration.ACCEPTANCE_TEST_OAUTH_CLIENT_SECRET);
		} else {
			deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.username",
				cloudFoundryProperties.getUsername());
			deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.password",
				cloudFoundryProperties.getPassword());
		}

		return deployerVariables;
	}

	private Map<String, String> appBrokerCatalogEnvironmentVariables() {
		Map<String, String> catalogVariables = new HashMap<>();
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].id", "example-service-id");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].name", "example");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].description", "A simple example");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].bindable", "true");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].tags[0]", "example");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].id", "standard-plan-id");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable", "true");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].name", "standard");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].description", "A simple plan");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].free", "true");
		return catalogVariables;
	}

	private Map<String, String> backingAppEnvironmentVariables(String... backingAppProperties) {
		Map<String, String> backingAppVariables = new HashMap<>();
		for (String appProperty : backingAppProperties) {
			final String[] appPropertyKeyValue = appProperty.split("=");
			if (appPropertyKeyValue.length == 2) {
				backingAppVariables.put(appPropertyKeyValue[0], appPropertyKeyValue[1]);
			} else {
				throw new IllegalArgumentException(format("Backing app property '%s' is incorrectly formatted",
					Arrays.toString(appPropertyKeyValue)));
			}
		}
		return backingAppVariables;
	}

	private <T> Mono<T> loggingMono(Mono<T> publisher) {
		if (LOGGER.isDebugEnabled()) {
			return publisher.log();
		} else {
			return publisher;
		}
	}

	private <T> Flux<T> loggingFlux(Flux<T> publisher) {
		if (LOGGER.isDebugEnabled()) {
			return publisher.log();
		} else {
			return publisher;
		}
	}
}
