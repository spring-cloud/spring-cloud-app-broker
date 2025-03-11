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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.organizations.RemoveOrganizationManagerRequest;
import org.cloudfoundry.client.v2.organizations.RemoveOrganizationUserRequest;
import org.cloudfoundry.client.v2.privatedomains.DeletePrivateDomainRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperRequest;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.organizations.CreateOrganizationRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationsResponse;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.cloudfoundry.client.v3.roles.CreateRoleRequest;
import org.cloudfoundry.client.v3.roles.ListRolesRequest;
import org.cloudfoundry.client.v3.roles.ListRolesResponse;
import org.cloudfoundry.client.v3.roles.Role;
import org.cloudfoundry.client.v3.roles.RoleRelationships;
import org.cloudfoundry.client.v3.roles.RoleType;
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
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.domains.CreateDomainRequest;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.serviceadmin.CreateServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.DeleteServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.EnableServiceAccessRequest;
import org.cloudfoundry.operations.serviceadmin.UpdateServiceBrokerRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.operations.spaces.Spaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

@Service
public class CloudFoundryService {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryService.class);

	private static final String DEPLOYER_PROPERTY_PREFIX = "spring.cloud.appbroker.deployer.cloudfoundry.";

	private static final String JBP_CONFIG_OPEN_JDK_JRE_ENV_VAR_NAME = "JBP_CONFIG_OPEN_JDK_JRE";

	private static final String JBP_CONFIG_OPEN_JDK_JRE_17 = "{ jre: { version: 17.+ } }";

	private static final int EXPECTED_PROPERTY_PARTS = 2;

	private final CloudFoundryClient cloudFoundryClient;

	private final CloudFoundryOperations cloudFoundryOperations;

	private final CloudFoundryProperties cloudFoundryProperties;

	public CloudFoundryService(CloudFoundryClient cloudFoundryClient, CloudFoundryOperations cloudFoundryOperations,
			CloudFoundryProperties cloudFoundryProperties) {
		this.cloudFoundryClient = cloudFoundryClient;
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.cloudFoundryProperties = cloudFoundryProperties;
	}

	public Mono<Void> enableServiceBrokerAccess(String serviceName) {
		return this.cloudFoundryOperations.serviceAdmin()
			.enableServiceAccess(EnableServiceAccessRequest.builder().serviceName(serviceName).build())
			.doOnSuccess((v) -> LOG.info("Enabled access to service. serviceName={}", serviceName))
			.doOnError((e) -> LOG.error(String.format("Error enabling access to service. serviceName=%s, error=%s",
					serviceName, e.getMessage()), e));
	}

	public Mono<Void> createServiceBroker(String brokerName, String testBrokerAppName) {
		return getApplicationRoute(testBrokerAppName).flatMap((url) -> this.cloudFoundryOperations.serviceAdmin()
			.create(CreateServiceBrokerRequest.builder()
				.name(brokerName)
				.username("user")
				.password("password")
				.url(url)
				.build())
			.doOnSuccess((v) -> LOG.info("Success creating service broker. brokerName={}", brokerName))
			.doOnError((e) -> LOG.error(
					String.format("Error creating service broker. brokerName=%s, error=%s", brokerName, e.getMessage()),
					e)));
	}

	public Mono<Void> updateServiceBroker(String brokerName, String testBrokerAppName) {
		return getApplicationRoute(testBrokerAppName).flatMap((url) -> this.cloudFoundryOperations.serviceAdmin()
			.update(UpdateServiceBrokerRequest.builder()
				.name(brokerName)
				.username("user")
				.password("password")
				.url(url)
				.build())
			.doOnSuccess((v) -> LOG.info("Success updating service broker. brokerName={}", brokerName))
			.doOnError((e) -> LOG.error(
					String.format("Error updating service broker. brokerName=%s, error=%s", brokerName, e.getMessage()),
					e)));
	}

	public Mono<String> getApplicationRoute(String appName) {
		return this.cloudFoundryOperations.applications()
			.get(GetApplicationRequest.builder().name(appName).build())
			.doOnSuccess((item) -> LOG.info("Success getting route for app. appName={}", appName))
			.doOnError((e) -> LOG
				.error(String.format("Error getting route for app. appName=%s, error=%s", appName, e.getMessage()), e))
			.map(ApplicationDetail::getUrls)
			.flatMapMany(Flux::fromIterable)
			.next()
			.map((url) -> "https://" + url);
	}

	public Mono<Void> pushBrokerApp(String appName, Path appPath, String brokerClientId,
			List<String> appBrokerProperties) {
		return this.cloudFoundryOperations.applications()
			.pushManifest(PushApplicationManifestRequest.builder()
				.manifest(ApplicationManifest.builder()
					.environmentVariables(appBrokerDeployerEnvironmentVariables(brokerClientId))
					.putAllEnvironmentVariables(propertiesToEnvironment(appBrokerProperties))
					.name(appName)
					.path(appPath)
					.memory(1024)
					.build())
				.build())
			.doOnSuccess((v) -> LOG.info("Success pushing broker app. appName={}", appName))
			.doOnError((e) -> LOG
				.error(String.format("Error pushing broker app. appName=%s, error=%s", appName, e.getMessage()), e));
	}

	public Mono<Void> updateBrokerApp(String appName, String brokerClientId, List<String> appBrokerProperties) {
		return this.cloudFoundryOperations.applications()
			.get(GetApplicationRequest.builder().name(appName).build())
			.map(ApplicationDetail::getId)
			.flatMap((applicationId) -> this.cloudFoundryClient.applicationsV2()
				.update(UpdateApplicationRequest.builder()
					.applicationId(applicationId)
					.putAllEnvironmentJsons(appBrokerDeployerEnvironmentVariables(brokerClientId))
					.putAllEnvironmentJsons(propertiesToEnvironment(appBrokerProperties))
					.name(appName)
					.memory(1024)
					.build())
				.thenReturn(applicationId))
			.then(this.cloudFoundryOperations.applications()
				.restart(RestartApplicationRequest.builder().name(appName).build()))
			.doOnSuccess((item) -> LOG.info("Updated broker app " + appName))
			.doOnError((error) -> LOG.error("Error updating broker app " + appName + ": " + error))
			.then();
	}

	public Mono<Void> deleteApp(String appName) {
		return this.cloudFoundryOperations.applications()
			.list()
			.filter((app) -> appName.equals(app.getName()))
			.singleOrEmpty()
			.flatMap((app) -> this.cloudFoundryOperations.applications()
				.delete(DeleteApplicationRequest.builder().name(appName).deleteRoutes(true).build()))
			.doOnSuccess((item) -> LOG.info("Success deleting app. appName={}", appName))
			.doOnError((e) -> LOG
				.warn(String.format("Error deleting app. appName=%s, error=%s", appName, e.getMessage()), e))
			.onErrorResume((e) -> Mono.empty());
	}

	public Mono<Void> deleteServiceBroker(String brokerName) {
		return this.cloudFoundryOperations.serviceAdmin()
			.list()
			.filter((serviceBroker) -> brokerName.equals(serviceBroker.getName()))
			.singleOrEmpty()
			.flatMap((serviceBroker) -> this.cloudFoundryOperations.serviceAdmin()
				.delete(DeleteServiceBrokerRequest.builder().name(brokerName).build()))
			.doOnSuccess((item) -> LOG.info("Success deleting service broker. brokerName={}", brokerName))
			.doOnError((e) -> LOG.warn(String.format("Error deleting service broker. brokerName=%s, error=%s ",
					brokerName, e.getMessage()), e))
			.onErrorResume((e) -> Mono.empty());
	}

	public Mono<Void> createBackingServiceInstance(String planName, String serviceName, String serviceInstanceName,
			Map<String, Object> parameters) {
		return this.cloudFoundryOperations.services()
			.createInstance(CreateServiceInstanceRequest.builder()
				.planName(planName)
				.serviceName(serviceName)
				.serviceInstanceName(serviceInstanceName)
				.parameters(parameters)
				.build())
			.doOnSuccess((item) -> LOG.info("Success creating service instance. serviceInstanceName={}",
					serviceInstanceName))
			.doOnError((e) -> LOG
				.error(String.format("Error creating service instance. serviceInstanceName=%s, " + "error=%s",
						serviceInstanceName, e.getMessage()), e));
	}

	public Flux<ServiceInstanceSummary> listServiceInstances() {
		return this.cloudFoundryOperations.services().listInstances();
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName) {
		return getServiceInstance(this.cloudFoundryOperations, serviceInstanceName);
	}

	public Mono<ServiceInstance> getServiceInstance(String serviceInstanceName, String space) {
		return getServiceInstance(createOperationsForSpace(space), serviceInstanceName);
	}

	private Mono<ServiceInstance> getServiceInstance(CloudFoundryOperations operations, String serviceInstanceName) {
		return operations.services()
			.getInstance(GetServiceInstanceRequest.builder().name(serviceInstanceName).build())
			.doOnSuccess(
					(item) -> LOG.info("Success getting service instance. serviceInstanceName={}", serviceInstanceName))
			.doOnError((e) -> LOG
				.error(String.format("Error getting service instance. serviceInstanceName=%s, " + "error=%s",
						serviceInstanceName, e.getMessage()), e));
	}

	public Mono<List<ApplicationSummary>> getApplications() {
		return listApplications(this.cloudFoundryOperations).collectList();
	}

	public Mono<ApplicationDetail> getApplication(String appName) {
		return this.cloudFoundryOperations.applications().get(GetApplicationRequest.builder().name(appName).build());
	}

	public Mono<ApplicationSummary> getApplication(String appName, String space) {
		return listApplications(createOperationsForSpace(space))
			.filter((applicationSummary) -> applicationSummary.getName().equals(appName))
			.single();
	}

	private Flux<ApplicationSummary> listApplications(CloudFoundryOperations operations) {
		return operations.applications()
			.list()
			.doOnComplete(() -> LOG.info("Listed applications"))
			.doOnError((e) -> LOG.error(String.format("Error listing applications. error=%s", e.getMessage()), e));
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironment(String appName) {
		return getApplicationEnvironment(this.cloudFoundryOperations, appName);
	}

	public Mono<ApplicationEnvironments> getApplicationEnvironment(String appName, String space) {
		return getApplicationEnvironment(createOperationsForSpace(space), appName);
	}

	private Mono<ApplicationEnvironments> getApplicationEnvironment(CloudFoundryOperations operations, String appName) {
		return operations.applications()
			.getEnvironments(GetApplicationEnvironmentsRequest.builder().name(appName).build())
			.doOnSuccess((item) -> LOG.info("Success getting environment for application. appName={}", appName))
			.doOnError((e) -> LOG
				.error(String.format("Error getting environment for application. appName=%s, " + "error=%s", appName,
						e.getMessage()), e));
	}

	public Mono<Void> stopApplication(String appName) {
		return this.cloudFoundryOperations.applications().stop(StopApplicationRequest.builder().name(appName).build());
	}

	public Mono<SpaceSummary> getOrCreateDefaultSpace() {
		return getOrCreateSpace(this.cloudFoundryProperties.getDefaultOrg(),
				this.cloudFoundryProperties.getDefaultSpace());
	}

	public Mono<String> getOrCreateDefaultOrg() {
		return getOrCreateOrganization(this.cloudFoundryProperties.getDefaultOrg());
	}

	public Mono<List<String>> getSpaces() {
		return this.cloudFoundryOperations.spaces()
			.list()
			.doOnComplete(() -> LOG.info("Success listing spaces"))
			.doOnError((e) -> LOG.error(String.format("Error listing spaces. error=%s", e.getMessage()), e))
			.map(SpaceSummary::getName)
			.collectList();
	}

	public Mono<SpaceSummary> getOrCreateSpace(String orgName, String spaceName) {
		Spaces spaceOperations = DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.cloudFoundryOperations)
			.organization(orgName)
			.build()
			.spaces();

		return getSpace(spaceOperations, spaceName).switchIfEmpty(
				spaceOperations.create(CreateSpaceRequest.builder().name(spaceName).organization(orgName).build())
					.then(getSpace(spaceOperations, spaceName)));
	}

	public Mono<String> getOrCreateOrganization(String orgName) {
		return cloudFoundryClient.organizationsV3()
			.list(ListOrganizationsRequest.builder().name(orgName).build())
			.flatMapIterable(ListOrganizationsResponse::getResources)
			.cast(Organization.class)
			.singleOrEmpty()
			.switchIfEmpty(cloudFoundryClient.organizationsV3()
				.create(CreateOrganizationRequest.builder().name(orgName).build())
				.cast(Organization.class))
			.map(Organization::getId);
	}

	private Mono<SpaceSummary> getSpace(Spaces spaceOperations, String spaceName) {
		return spaceOperations.list().filter((r) -> r.getName().equals(spaceName)).singleOrEmpty();
	}

	public Mono<Void> associateAppBrokerClientWithOrgAndSpace(String brokerClientId, String orgId, String spaceId) {
		return Mono.justOrEmpty(brokerClientId)
			.flatMap((userId) -> associateOrgUser(orgId, userId).then(associateOrgManager(orgId, userId))
				.then(associateSpaceDeveloper(spaceId, userId)))
			.then();
	}

	public Mono<Void> associateClientWithOrgAndSpace(String clientId, String orgId, String spaceId) {
		return associateOrgUser(orgId, clientId).then(associateSpaceDeveloper(spaceId, clientId)).then();
	}

	public Mono<Void> removeAppBrokerClientFromOrgAndSpace(String brokerClientId, String orgId, String spaceId) {
		return Mono.justOrEmpty(brokerClientId)
			.flatMap((userId) -> removeSpaceDeveloper(spaceId, userId).then(removeOrgManager(orgId, userId))
				.then(removeOrgUser(orgId, userId)));
	}

	public Mono<Void> createDomain(String domain) {
		return this.cloudFoundryOperations.domains()
			.create(CreateDomainRequest.builder()
				.domain(domain)
				.organization(this.cloudFoundryProperties.getDefaultOrg())
				.build())
			.onErrorResume((e) -> Mono.empty());
	}

	public Mono<Void> deleteDomain(String domain) {
		return this.cloudFoundryOperations.domains()
			.list()
			.filter((d) -> d.getName().equals(domain))
			.map(Domain::getId)
			.flatMap((domainId) -> this.cloudFoundryClient.privateDomains()
				.delete(DeletePrivateDomainRequest.builder().privateDomainId(domainId).build()))
			.then();
	}

	private Mono<Void> createOrgRole(RoleType roleType, String orgId, String userId) {
		return cloudFoundryClient.rolesV3()
			.list(ListRolesRequest.builder()
				.type(roleType)
				.organizationId(orgId)
				.userId(userId)
				.build())
			.flatMapIterable(ListRolesResponse::getResources)
			.singleOrEmpty()
			.cast(Role.class)
			.switchIfEmpty(cloudFoundryClient.rolesV3()
				.create(CreateRoleRequest.builder()
					.type(roleType)
					.relationships(RoleRelationships.builder()
						.user(ToOneRelationship.builder()
							.data(Relationship.builder().id(userId).build())
							.build())
						.organization(ToOneRelationship.builder()
							.data(Relationship.builder().id(orgId).build())
							.build())
						.build())
					.build()))
			.then();
	}

	private Mono<Void> associateOrgUser(String orgId, String userId) {
		return createOrgRole(RoleType.ORGANIZATION_USER, orgId, userId);
	}

	private Mono<Void> associateOrgManager(String orgId, String userId) {
		return createOrgRole(RoleType.ORGANIZATION_MANAGER, orgId, userId);
	}

	private Mono<Void> createSpaceRole(RoleType roleType, String spaceId, String userId) {
		return cloudFoundryClient.rolesV3()
			.list(ListRolesRequest.builder()
				.type(roleType)
				.spaceId(spaceId)
				.userId(userId)
				.build())
			.flatMapIterable(ListRolesResponse::getResources)
			.singleOrEmpty()
			.cast(Role.class)
			.switchIfEmpty(cloudFoundryClient.rolesV3()
				.create(CreateRoleRequest.builder()
					.type(roleType)
					.relationships(RoleRelationships.builder()
						.user(ToOneRelationship.builder()
							.data(Relationship.builder().id(userId).build())
							.build())
						.space(ToOneRelationship.builder()
							.data(Relationship.builder().id(spaceId).build())
							.build())
						.build())
					.build()))
			.then();
	}

	private Mono<Void> associateSpaceDeveloper(String spaceId, String userId) {
		return createSpaceRole(RoleType.SPACE_DEVELOPER, spaceId, userId);
	}

	private Mono<Void> removeOrgUser(String orgId, String userId) {
		return this.cloudFoundryClient.organizations()
			.removeUser(RemoveOrganizationUserRequest.builder().organizationId(orgId).userId(userId).build());
	}

	private Mono<Void> removeOrgManager(String orgId, String userId) {
		return this.cloudFoundryClient.organizations()
			.removeManager(RemoveOrganizationManagerRequest.builder().organizationId(orgId).managerId(userId).build());
	}

	private Mono<Void> removeSpaceDeveloper(String spaceId, String userId) {
		return this.cloudFoundryClient.spaces()
			.removeDeveloper(RemoveSpaceDeveloperRequest.builder().spaceId(spaceId).developerId(userId).build());
	}

	private CloudFoundryOperations createOperationsForSpace(String space) {
		final String defaultOrg = this.cloudFoundryProperties.getDefaultOrg();
		return DefaultCloudFoundryOperations.builder()
			.from((DefaultCloudFoundryOperations) this.cloudFoundryOperations)
			.organization(defaultOrg)
			.space(space)
			.build();
	}

	private Map<String, String> appBrokerDeployerEnvironmentVariables(String brokerClientId) {
		Map<String, String> deployerVariables = new HashMap<>();
		deployerVariables.put(JBP_CONFIG_OPEN_JDK_JRE_ENV_VAR_NAME, JBP_CONFIG_OPEN_JDK_JRE_17);
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "api-host", this.cloudFoundryProperties.getApiHost());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "api-port",
				String.valueOf(this.cloudFoundryProperties.getApiPort()));
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "default-org", this.cloudFoundryProperties.getDefaultOrg());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "default-space",
				this.cloudFoundryProperties.getDefaultSpace());
		deployerVariables.put(DEPLOYER_PROPERTY_PREFIX + "skip-ssl-validation",
				String.valueOf(this.cloudFoundryProperties.isSkipSslValidation()));
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
				throw new IllegalArgumentException(String.format("App Broker property '%s' is incorrectly formatted",
						Arrays.toString(propertyKeyValue)));
			}
		}
		return environment;
	}

}
