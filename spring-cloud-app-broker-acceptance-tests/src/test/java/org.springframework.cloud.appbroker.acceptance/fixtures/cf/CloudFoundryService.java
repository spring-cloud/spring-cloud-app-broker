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

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.organizations.CreateOrganizationRequest;
import org.cloudfoundry.operations.organizations.DefaultOrganizations;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.serviceadmin.CreateServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.DeleteServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.EnableServiceAccessRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.DefaultSpaces;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.acceptance.AcceptanceTestProperties;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class CloudFoundryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryService.class);

	private CloudFoundryOperations cloudFoundryOperations;
	private CloudFoundryProperties cloudFoundryProperties;
	private CloudFoundryClient cloudFoundryClient;
	private AcceptanceTestProperties acceptanceTestProperties;

	@Autowired
	public CloudFoundryService(CloudFoundryOperations cloudFoundryOperations, CloudFoundryProperties cloudFoundryProperties,
							   CloudFoundryClient cloudFoundryClient, AcceptanceTestProperties acceptanceTestProperties) {
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.cloudFoundryProperties = cloudFoundryProperties;
		this.cloudFoundryClient = cloudFoundryClient;
		this.acceptanceTestProperties = acceptanceTestProperties;
	}

	public Mono<Void> enableServiceBrokerAccess(String serviceName) {
		return loggingMono(
			cloudFoundryOperations
				.serviceAdmin()
				.enableServiceAccess(EnableServiceAccessRequest.builder().serviceName(serviceName).build()));
	}

	public Mono<Void> createServiceBroker(String brokerName, String sampleBrokerAppName) {
		return loggingMono(
			getApplicationRoute(sampleBrokerAppName)
				.flatMap(url -> cloudFoundryOperations
					.serviceAdmin()
					.create(CreateServiceBrokerRequest
						.builder()
						.name(brokerName)
						.username("user")
						.password("password")
						.url(url)
						.build())));
	}

	public Mono<String> getApplicationRoute(String appName) {
		return loggingMono(
			cloudFoundryOperations
				.applications()
				.get(GetApplicationRequest.builder().name(appName).build())
				.map(ApplicationDetail::getUrls)
				.flatMapMany(Flux::fromIterable)
				.next()
				.map(url -> "https://" + url));
	}

	public Mono<Void> pushAppBroker(String appName, Path appPath, String[] backingAppProperties) {
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

	public Mono<Void> deleteBackingApp(String appName) {
		return loggingMono(
			cloudFoundryOperations
				.applications()
				.delete(DeleteApplicationRequest.builder().name(appName).build()));
	}

	public Mono<Void> deleteServiceBroker(String brokerName) {
		return loggingMono(
			cloudFoundryOperations
				.serviceAdmin()
				.delete(DeleteServiceBrokerRequest.builder().name(brokerName).build()));
	}

	public Mono<Void> deleteServiceInstance(String serviceInstanceName) {
		return loggingMono(
			cloudFoundryOperations
				.services()
				.listInstances()
				.filter(si -> si.getName().equals(serviceInstanceName))
				.next()
				.flatMap(si ->
					cloudFoundryOperations
						.services()
						.deleteInstance(DeleteServiceInstanceRequest.builder().name(si.getName()).build())));
	}

	public Mono<Void> createServiceInstance(String planName, String serviceName, String serviceInstanceName) {
		return loggingMono(
			cloudFoundryOperations
				.services()
				.createInstance(CreateServiceInstanceRequest
					.builder()
					.planName(planName)
					.serviceName(serviceName)
					.serviceInstanceName(serviceInstanceName)
					.build()));
	}

	public Mono<List<ApplicationSummary>> getApplications() {
		return loggingMono(
			cloudFoundryOperations.applications().list().collectList());
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironmentByAppName(String appName) {
		return loggingMono(
			cloudFoundryOperations
				.applications()
				.getEnvironments(GetApplicationEnvironmentsRequest.builder().name(appName).build()));
	}


	public Mono<SpaceSummary> getOrCreateDefaultSpace() {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();

		DefaultSpaces spaceOperations = new DefaultSpaces(
			Mono.just(cloudFoundryClient),
			getOrCreateDefaultOrganization().map(OrganizationSummary::getId),
			Mono.just(cloudFoundryProperties.getUsername()));

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
		DefaultOrganizations organizationOperations = new DefaultOrganizations(
			Mono.just(cloudFoundryClient),
			Mono.just(cloudFoundryProperties.getUsername()));

		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return loggingMono(
			getDefaultOrg(organizationOperations)
				.switchIfEmpty(organizationOperations
					.create(CreateOrganizationRequest
						.builder()
						.organizationName(defaultOrg)
						.build())
					.then(getDefaultOrg(organizationOperations))));
	}

	private Mono<OrganizationSummary> getDefaultOrg(DefaultOrganizations orgOperations) {
		return orgOperations
			.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultOrg()))
			.next();
	}

	private Mono<SpaceSummary> getDefaultSpace(DefaultSpaces spaceOperations) {
		return spaceOperations
			.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultSpace()))
			.next();
	}

	private Map<String, String> appBrokerDeployerEnvironmentVariables() {

		Map<String, String> deployerVariables = new HashMap<>();
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.api-host", cloudFoundryProperties.getApiHost());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.api-port", String.valueOf(cloudFoundryProperties.getApiPort()));
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.username", cloudFoundryProperties.getUsername());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.password", cloudFoundryProperties.getPassword());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.default-org", cloudFoundryProperties.getDefaultOrg());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.default-space", cloudFoundryProperties.getDefaultSpace());
		deployerVariables.put("spring.cloud.appbroker.deployer.cloudfoundry.skip-ssl-validation", String.valueOf(cloudFoundryProperties.isSkipSslValidation()));
		deployerVariables.put("spring.cloud.appbroker.apps[0].cloudfoundry.skip-ssl-validation", String.valueOf(cloudFoundryProperties.isSkipSslValidation()));
		return deployerVariables;
	}

	private Map<String, String> appBrokerCatalogEnvironmentVariables() {
		Map<String, String> catalogVariables = new HashMap<>();
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].id", "example-service");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].name", "example");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].description", "A simple example");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].bindable", "true");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].tags[0]", "example");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].id", "simple-plan");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable", "true");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].name", "standard");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].description", "A simple plan");
		catalogVariables.put("spring.cloud.openservicebroker.catalog.services[0].plans[0].free", "true");
		return catalogVariables;
	}

	private Map<String, String> backingAppEnvironmentVariables(String[] backingAppProperties) {
		Map<String, String> backingAppVariables = new HashMap<>();
		for (String appProperty : backingAppProperties) {
			final String[] appPropertyKeyValue = appProperty.split("=");
			if (appPropertyKeyValue.length != 2) {
				throw new RuntimeException(format("Backing app property '%s' is incorrectly formatted", Arrays.toString(appPropertyKeyValue)));
			}
			else backingAppVariables.put(appPropertyKeyValue[0], appPropertyKeyValue[1]);
		}
		return backingAppVariables;
	}

	private <T> Mono<T> loggingMono(Mono<T> publisher) {
		if (LOGGER.isDebugEnabled()) {
			return publisher.log();
		}
		else return publisher;
	}

}
