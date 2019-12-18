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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationManagerRequest;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationManagerResponse;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationUserRequest;
import org.cloudfoundry.client.v2.organizations.AssociateOrganizationUserResponse;
import org.cloudfoundry.client.v2.organizations.RemoveOrganizationManagerRequest;
import org.cloudfoundry.client.v2.organizations.RemoveOrganizationUserRequest;
import org.cloudfoundry.client.v2.privatedomains.DeletePrivateDomainRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperRequest;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperResponse;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperRequest;
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
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.domains.CreateDomainRequest;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.CreateOrganizationRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.serviceadmin.CreateServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.DeleteServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.EnableServiceAccessRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
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

	private static final String DEPLOYER_PROPERTY_PREFIX = "spring.cloud.appbroker.deployer.cloudfoundry.";

	private static final int EXPECTED_PROPERTY_PARTS = 2;

	private final CloudFoundryClient cloudFoundryClient;

	private final CloudFoundryOperations cloudFoundryOperations;

	private final CloudFoundryProperties cloudFoundryProperties;

	public CloudFoundryService(CloudFoundryClient cloudFoundryClient,
		CloudFoundryOperations cloudFoundryOperations,
		CloudFoundryProperties cloudFoundryProperties) {
		this.cloudFoundryClient = cloudFoundryClient;
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.cloudFoundryProperties = cloudFoundryProperties;
	}

	public Mono<Void> enableServiceBrokerAccess(String serviceName) {
		return cloudFoundryOperations.serviceAdmin()
			.enableServiceAccess(EnableServiceAccessRequest.builder()
				.serviceName(serviceName)
				.build())
			.doOnSuccess(item -> LOGGER.info("Enabled access to service " + serviceName))
			.doOnError(error -> LOGGER.error("Error enabling access to service " + serviceName + ": " + error));
	}

	public Mono<Void> createServiceBroker(String brokerName, String testBrokerAppName) {
		return getApplicationRoute(testBrokerAppName)
			.flatMap(url -> cloudFoundryOperations.serviceAdmin()
				.create(CreateServiceBrokerRequest.builder()
					.name(brokerName)
					.username("user")
					.password("password")
					.url(url)
					.build())
				.doOnSuccess(item -> LOGGER.info("Created service broker " + brokerName))
				.doOnError(error -> LOGGER.error("Error creating service broker " + brokerName + ": " + error)));
	}

	public Mono<String> getApplicationRoute(String appName) {
		return cloudFoundryOperations.applications()
			.get(GetApplicationRequest.builder()
				.name(appName)
				.build())
			.doOnSuccess(item -> LOGGER.info("Got route for app " + appName))
			.doOnError(error -> LOGGER.error("Error getting route for app " + appName + ": " + error))
			.map(ApplicationDetail::getUrls)
			.flatMapMany(Flux::fromIterable)
			.next()
			.map(url -> "https://" + url);
	}

	public Mono<Void> pushBrokerApp(String appName, Path appPath, String brokerClientId,
		List<String> appBrokerProperties) {
		return cloudFoundryOperations.applications()
			.pushManifest(PushApplicationManifestRequest.builder()
				.manifest(ApplicationManifest.builder()
					.putAllEnvironmentVariables(appBrokerDeployerEnvironmentVariables(brokerClientId))
					.putAllEnvironmentVariables(propertiesToEnvironment(appBrokerProperties))
					.name(appName)
					.path(appPath)
					.memory(1024)
					.build())
				.build())
			.doOnSuccess(item -> LOGGER.info("Pushed broker app " + appName))
			.doOnError(error -> LOGGER.error("Error pushing broker app " + appName + ": " + error));
	}

	public Mono<Void> deleteApp(String appName) {
		return cloudFoundryOperations.applications()
			.delete(DeleteApplicationRequest.builder()
				.name(appName)
				.deleteRoutes(true)
				.build())
			.doOnSuccess(item -> LOGGER.info("Deleted app " + appName))
			.doOnError(error -> LOGGER.warn("Error deleting app " + appName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceBroker(String brokerName) {
		return cloudFoundryOperations.serviceAdmin()
			.delete(DeleteServiceBrokerRequest.builder()
				.name(brokerName)
				.build())
			.doOnSuccess(item -> LOGGER.info("Deleted service broker " + brokerName))
			.doOnError(error -> LOGGER.warn("Error deleting service broker " + brokerName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteServiceInstance(String serviceInstanceName) {
		return getServiceInstance(serviceInstanceName)
			.flatMap(si -> cloudFoundryOperations.services()
				.deleteInstance(DeleteServiceInstanceRequest.builder()
					.name(si.getName())
					.build())
				.doOnSuccess(item -> LOGGER.info("Deleted service instance " + serviceInstanceName))
				.doOnError(
					error -> LOGGER.error("Error deleting service instance " + serviceInstanceName + ": " + error))
				.onErrorResume(e -> Mono.empty()))
			.doOnError(error -> LOGGER.warn("Error getting service instance " + serviceInstanceName + ": " + error))
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> createServiceInstance(String planName,
		String serviceName,
		String serviceInstanceName,
		Map<String, Object> parameters) {
		return cloudFoundryOperations.services()
			.createInstance(CreateServiceInstanceRequest.builder()
				.planName(planName)
				.serviceName(serviceName)
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.build())
			.doOnSuccess(item -> LOGGER.info("Created service instance " + serviceInstanceName))
			.doOnError(error -> LOGGER.error("Error creating service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<Void> updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		return cloudFoundryOperations.services()
			.updateInstance(UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.build())
			.doOnSuccess(item -> LOGGER.info("Updated service instance " + serviceInstanceName))
			.doOnError(error -> LOGGER.error("Error updating service instance " + serviceInstanceName + ": " + error));
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName) {
		return getServiceInstance(cloudFoundryOperations, serviceInstanceName);
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName, String space) {
		return getServiceInstance(createOperationsForSpace(space), serviceInstanceName);
	}

	private Mono<ServiceInstance> getServiceInstance(CloudFoundryOperations operations,
		String serviceInstanceName) {
		return operations.services()
			.getInstance(GetServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.build())
			.doOnSuccess(item -> LOGGER.info("Got service instance " + serviceInstanceName))
			.doOnError(error -> LOGGER.error("Error getting service instance " + serviceInstanceName + ": " + error,
				error));
	}

	public Mono<List<ApplicationSummary>> getApplications() {
		return listApplications(cloudFoundryOperations)
			.collectList();
	}

	public Mono<ApplicationDetail> getApplication(String appName) {
		return cloudFoundryOperations.applications().get(GetApplicationRequest.builder()
			.name(appName)
			.build());
	}

	public Mono<ApplicationSummary> getApplication(String appName, String space) {
		return listApplications(createOperationsForSpace(space))
			.filter(applicationSummary -> applicationSummary.getName().equals(appName))
			.single();
	}

	private Flux<ApplicationSummary> listApplications(CloudFoundryOperations operations) {
		return operations.applications()
			.list()
			.doOnComplete(() -> LOGGER.info("Listed applications"))
			.doOnError(error -> LOGGER.error("Error listing applications: " + error));
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironment(String appName) {
		return getApplicationEnvironment(cloudFoundryOperations, appName);
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironment(String appName, String space) {
		return getApplicationEnvironment(createOperationsForSpace(space), appName);
	}

	private Mono<ApplicationEnvironments> getApplicationEnvironment(CloudFoundryOperations operations, String appName) {
		return operations.applications()
			.getEnvironments(GetApplicationEnvironmentsRequest.builder()
				.name(appName)
				.build())
			.doOnSuccess(item -> LOGGER.info("Got environment for application " + appName))
			.doOnError(error -> LOGGER.error("Error getting environment for application " + appName + ": " + error));
	}

	public Mono<Void> stopApplication(String appName) {
		return cloudFoundryOperations.applications().stop(StopApplicationRequest.builder()
			.name(appName)
			.build());
	}

	public Mono<List<String>> getSpaces() {
		return cloudFoundryOperations.spaces()
			.list()
			.doOnComplete(() -> LOGGER.info("Listed spaces"))
			.doOnError(error -> LOGGER.error("Error listing spaces: " + error))
			.map(SpaceSummary::getName)
			.collectList();
	}

	public Mono<SpaceSummary> getOrCreateDefaultSpace() {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();

		Spaces spaceOperations = DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.cloudFoundryOperations)
			.organization(cloudFoundryProperties.getDefaultOrg())
			.build()
			.spaces();

		final String defaultSpace = cloudFoundryProperties.getDefaultSpace();
		return getDefaultSpace(spaceOperations)
			.switchIfEmpty(spaceOperations.create(CreateSpaceRequest
				.builder()
				.name(defaultSpace)
				.organization(defaultOrg)
				.build())
				.then(getDefaultSpace(spaceOperations)));
	}

	public Mono<OrganizationSummary> getOrCreateDefaultOrganization() {
		Organizations organizationOperations = cloudFoundryOperations.organizations();

		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return getDefaultOrg(organizationOperations)
			.switchIfEmpty(organizationOperations
				.create(CreateOrganizationRequest
					.builder()
					.organizationName(defaultOrg)
					.build())
				.then(getDefaultOrg(organizationOperations)));
	}

	private Mono<OrganizationSummary> getDefaultOrg(Organizations orgOperations) {
		return orgOperations.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultOrg()))
			.next();
	}

	private Mono<SpaceSummary> getDefaultSpace(Spaces spaceOperations) {
		return spaceOperations.list()
			.filter(r -> r
				.getName()
				.equals(cloudFoundryProperties.getDefaultSpace()))
			.next();
	}

	public Mono<Void> associateAppBrokerClientWithOrgAndSpace(String brokerClientId, String orgId, String spaceId) {
		return Mono.justOrEmpty(brokerClientId)
			.flatMap(userId -> associateOrgUser(orgId, userId)
				.then(associateOrgManager(orgId, userId))
				.then(associateSpaceDeveloper(spaceId, userId)))
			.then();
	}

	public Mono<Void> removeAppBrokerClientFromOrgAndSpace(String brokerClientId, String orgId, String spaceId) {
		return Mono.justOrEmpty(brokerClientId)
			.flatMap(userId -> removeSpaceDeveloper(spaceId, userId)
				.then(removeOrgManager(orgId, userId))
				.then(removeOrgUser(orgId, userId)));
	}

	public Mono<Void> createDomain(String domain) {
		return cloudFoundryOperations
			.domains()
			.create(CreateDomainRequest
				.builder()
				.domain(domain)
				.organization(cloudFoundryProperties.getDefaultOrg())
				.build())
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> deleteDomain(String domain) {
		return cloudFoundryOperations
			.domains()
			.list()
			.filter(d -> d.getName().equals(domain))
			.map(Domain::getId)
			.flatMap(domainId -> cloudFoundryClient
				.privateDomains()
				.delete(DeletePrivateDomainRequest
					.builder()
					.privateDomainId(domainId)
					.build()))
			.then();
	}

	private Mono<AssociateOrganizationUserResponse> associateOrgUser(String orgId, String userId) {
		return cloudFoundryClient.organizations().associateUser(AssociateOrganizationUserRequest.builder()
			.organizationId(orgId)
			.userId(userId)
			.build());
	}

	private Mono<AssociateOrganizationManagerResponse> associateOrgManager(String orgId, String userId) {
		return cloudFoundryClient.organizations().associateManager(AssociateOrganizationManagerRequest.builder()
			.organizationId(orgId)
			.managerId(userId)
			.build());
	}

	private Mono<AssociateSpaceDeveloperResponse> associateSpaceDeveloper(String spaceId, String userId) {
		return cloudFoundryClient.spaces().associateDeveloper(AssociateSpaceDeveloperRequest.builder()
			.spaceId(spaceId)
			.developerId(userId)
			.build());
	}

	private Mono<Void> removeOrgUser(String orgId, String userId) {
		return cloudFoundryClient.organizations().removeUser(RemoveOrganizationUserRequest.builder()
			.organizationId(orgId)
			.userId(userId)
			.build());
	}

	private Mono<Void> removeOrgManager(String orgId, String userId) {
		return cloudFoundryClient.organizations().removeManager(RemoveOrganizationManagerRequest.builder()
			.organizationId(orgId)
			.managerId(userId)
			.build());
	}

	private Mono<Void> removeSpaceDeveloper(String spaceId, String userId) {
		return cloudFoundryClient.spaces().removeDeveloper(RemoveSpaceDeveloperRequest.builder()
			.spaceId(spaceId)
			.developerId(userId)
			.build());
	}

	private CloudFoundryOperations createOperationsForSpace(String space) {
		final String defaultOrg = cloudFoundryProperties.getDefaultOrg();
		return DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) cloudFoundryOperations)
			.organization(defaultOrg)
			.space(space)
			.build();
	}

	private Map<String, String> appBrokerDeployerEnvironmentVariables(String brokerClientId) {
		Map<String, String> deployerVariables = new HashMap<>();
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "api-host",
			cloudFoundryProperties.getApiHost());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "api-port",
			String.valueOf(cloudFoundryProperties.getApiPort()));
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "default-org",
			cloudFoundryProperties.getDefaultOrg());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "default-space",
			cloudFoundryProperties.getDefaultSpace());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "skip-ssl-validation",
			String.valueOf(cloudFoundryProperties.isSkipSslValidation()));
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "properties.memory", "1024M");
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "client-id", brokerClientId);
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "client-secret",
			CloudFoundryClientConfiguration.APP_BROKER_CLIENT_SECRET);
		return deployerVariables;
	}

	private Map<String, String> propertiesToEnvironment(List<String> properties) {
		Map<String, String> environment = new HashMap<>();
		for (String property : properties) {
			final String[] propertyKeyValue = property.split("=");
			if (propertyKeyValue.length == EXPECTED_PROPERTY_PARTS) {
				environment.put(propertyKeyValue[0], propertyKeyValue[1]);
			}
			else {
				throw new IllegalArgumentException(format("App Broker property '%s' is incorrectly formatted",
					Arrays.toString(propertyKeyValue)));
			}
		}
		return environment;
	}

}
