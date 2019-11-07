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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesResponse;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstances;
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.client.v2.spaces.DeleteSpaceRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceResponse;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v2.spaces.SpaceResource;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.organizations.OrganizationQuota;
import org.cloudfoundry.operations.organizations.Organizations;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.CreateServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.DeleteServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.cloud.appbroker.deployer.UpdateServiceInstanceRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.cloud.appbroker.deployer.DeploymentProperties.TARGET_PROPERTY_KEY;
import static org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties.DEFAULT_API_POLLING_TIMEOUT_SECONDS;

@SuppressWarnings("UnassignedFluxMonoInstance")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudFoundryAppDeployerTest {

	private static final String APP_NAME = "test-app";

	private static final String APP_PATH = "test.jar";

	private static final String SERVICE_INSTANCE_ID = "service-instance-id";
	private static final long DEFAULT_COMPLETION_DURATION = Duration.ofSeconds(DEFAULT_API_POLLING_TIMEOUT_SECONDS).getSeconds();

	private static final int EXPECTED_MANIFESTS = 1;

	private AppDeployer appDeployer;

	@Mock
	private Applications operationsApplications;

	@Mock
	private Services operationsServices;

	@Mock
	private Organizations operationsOrganizations;

	@Mock
	private Spaces operationsSpaces;

	@Mock
	private ServiceInstances clientServiceInstances;

	@Mock
	private org.cloudfoundry.client.v2.spaces.Spaces clientSpaces;

	@Mock
	private org.cloudfoundry.client.v2.organizations.Organizations clientOrganizations;

	@Mock
	private CloudFoundryOperations cloudFoundryOperations;

	@Mock
	private CloudFoundryClient cloudFoundryClient;

	@Mock
	private CloudFoundryOperationsUtils operationsUtils;

	@Mock
	private ResourceLoader resourceLoader;

	private CloudFoundryDeploymentProperties deploymentProperties;

	@BeforeEach
	void setUp() {
		deploymentProperties = new CloudFoundryDeploymentProperties();
		CloudFoundryTargetProperties targetProperties = new CloudFoundryTargetProperties();
		targetProperties.setDefaultOrg("default-org");
		targetProperties.setDefaultSpace("default-space");

		given(operationsApplications.pushManifest(any())).willReturn(Mono.empty());
		given(resourceLoader.getResource(APP_PATH)).willReturn(new FileSystemResource(APP_PATH));

		given(cloudFoundryOperations.spaces()).willReturn(operationsSpaces);
		given(cloudFoundryOperations.applications()).willReturn(operationsApplications);
		given(cloudFoundryOperations.services()).willReturn(operationsServices);
		given(cloudFoundryOperations.organizations()).willReturn(operationsOrganizations);
		given(cloudFoundryClient.serviceInstances()).willReturn(clientServiceInstances);
		given(cloudFoundryClient.spaces()).willReturn(clientSpaces);
		given(cloudFoundryClient.organizations()).willReturn(clientOrganizations);
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(cloudFoundryOperations));
		given(operationsUtils.getOperationsForSpace(anyString())).willReturn(Mono.just(cloudFoundryOperations));
		given(operationsUtils.getOperationsForOrgAndSpace(anyString(), anyString()))
			.willReturn(Mono.just(cloudFoundryOperations));

		appDeployer = new CloudFoundryAppDeployer(deploymentProperties, cloudFoundryOperations, cloudFoundryClient,
			operationsUtils, targetProperties, resourceLoader);
	}

	@Test
	void deployAppWithPlatformDefaults() {
		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.build();

		then(operationsApplications).should().pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithPropertiesInRequest() {
		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.property(DeploymentProperties.COUNT_PROPERTY_KEY, "3")
			.property(DeploymentProperties.MEMORY_PROPERTY_KEY, "2G")
			.property(DeploymentProperties.DISK_PROPERTY_KEY, "3G")
			.property(CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY, "http")
			.property(CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY, "/healthcheck")
			.property(CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY, "buildpack")
			.property(CloudFoundryDeploymentProperties.DOMAINS_PROPERTY, "domain1,domain2")
			.property(DeploymentProperties.HOST_PROPERTY_KEY, "host")
			.property(CloudFoundryDeploymentProperties.NO_ROUTE_PROPERTY, "true")
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.instances(3)
			.memory(2048)
			.disk(3072)
			.healthCheckType(ApplicationHealthCheck.HTTP)
			.healthCheckHttpEndpoint("/healthcheck")
			.buildpack("buildpack")
			.domains("domain2", "domain1") // domains is a list so order matters
			.host("host")
			.noRoute(true)
			.build();

		then(operationsApplications).should().pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithDefaultProperties() {
		deploymentProperties.setCount(3);
		deploymentProperties.setMemory("2G");
		deploymentProperties.setDisk("3G");
		deploymentProperties.setBuildpack("buildpack");
		deploymentProperties.setHealthCheck(ApplicationHealthCheck.HTTP);
		deploymentProperties.setHealthCheckHttpEndpoint("/healthcheck");
		deploymentProperties.setDomains(singleton("domain"));
		deploymentProperties.setHost("host");

		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.instances(3)
			.memory(2048)
			.disk(3072)
			.healthCheckType(ApplicationHealthCheck.HTTP)
			.healthCheckHttpEndpoint("/healthcheck")
			.buildpack("buildpack")
			.domain("domain")
			.host("host")
			.build();

		then(operationsApplications).should().pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithRequestOverridingDefaultProperties() {
		deploymentProperties.setCount(3);
		deploymentProperties.setMemory("2G");
		deploymentProperties.setDisk("3G");
		deploymentProperties.setBuildpack("buildpack1");
		deploymentProperties.setHealthCheck(ApplicationHealthCheck.HTTP);
		deploymentProperties.setHealthCheckHttpEndpoint("/healthcheck1");
		deploymentProperties.setDomains(singleton("domain1"));
		deploymentProperties.setHost("host1");

		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.property(DeploymentProperties.COUNT_PROPERTY_KEY, "5")
			.property(DeploymentProperties.MEMORY_PROPERTY_KEY, "4G")
			.property(DeploymentProperties.DISK_PROPERTY_KEY, "5G")
			.property(CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY, "port")
			.property(CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY, "/healthcheck2")
			.property(CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY, "buildpack2")
			.property(CloudFoundryDeploymentProperties.DOMAINS_PROPERTY, "domain2")
			.property(DeploymentProperties.HOST_PROPERTY_KEY, "host2")
			.property(CloudFoundryDeploymentProperties.NO_ROUTE_PROPERTY, "true")
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.instances(5)
			.memory(4096)
			.disk(5120)
			.healthCheckType(ApplicationHealthCheck.PORT)
			.healthCheckHttpEndpoint("/healthcheck2")
			.buildpack("buildpack2")
			.domains("domain2", "domain1")
			.host("host2")
			.noRoute(true)
			.build();

		then(operationsApplications).should()
			.pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithBothDomainAndDomainsProperties() {
		deploymentProperties.setCount(3);
		deploymentProperties.setMemory("2G");
		deploymentProperties.setDisk("3G");
		deploymentProperties.setBuildpack("buildpack1");
		deploymentProperties.setHealthCheck(ApplicationHealthCheck.HTTP);
		deploymentProperties.setHealthCheckHttpEndpoint("/healthcheck1");
		deploymentProperties.setHost("host1");
		deploymentProperties.setDomains(singleton("default-domain"));

		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.property(DeploymentProperties.COUNT_PROPERTY_KEY, "5")
			.property(DeploymentProperties.MEMORY_PROPERTY_KEY, "4G")
			.property(DeploymentProperties.DISK_PROPERTY_KEY, "5G")
			.property(CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY, "port")
			.property(CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY, "/healthcheck2")
			.property(CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY, "buildpack2")
			.property(CloudFoundryDeploymentProperties.DOMAIN_PROPERTY, "domain1")
			.property(CloudFoundryDeploymentProperties.DOMAINS_PROPERTY, "domain2")
			.property(DeploymentProperties.HOST_PROPERTY_KEY, "host2")
			.property(CloudFoundryDeploymentProperties.NO_ROUTE_PROPERTY, "true")
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.instances(5)
			.memory(4096)
			.disk(5120)
			.healthCheckType(ApplicationHealthCheck.PORT)
			.healthCheckHttpEndpoint("/healthcheck2")
			.buildpack("buildpack2")
			.domains("domain1", "default-domain", "domain2")
			.host("host2")
			.noRoute(true)
			.build();

		then(operationsApplications).should().pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithEnvironmentUsingSpringAppJson() {
		deploymentProperties.setUseSpringApplicationJson(true);

		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.property(CloudFoundryDeploymentProperties.JAVA_OPTS_PROPERTY_KEY, "-Xms512m -Xmx1024m")
			.environment("ENV_VAR_1", "value1")
			.environment("ENV_VAR_2", "value2")
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson(
			"\"ENV_VAR_2\":\"value2\",\"ENV_VAR_1\":\"value1\"")
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.environmentVariable("JAVA_OPTS", "-Xms512m -Xmx1024m")
			.build();

		then(operationsApplications).should().pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithEnvironmentNotUsingSpringAppJson() {
		deploymentProperties.setUseSpringApplicationJson(false);

		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.property(CloudFoundryDeploymentProperties.JAVA_OPTS_PROPERTY_KEY, "-Xms512m -Xmx1024m")
			.environment("ENV_VAR_1", "value1")
			.environment("ENV_VAR_2", "value2")
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifest()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.environmentVariable("JAVA_OPTS", "-Xms512m -Xmx1024m")
			.environmentVariable("spring.cloud.appbroker.service-instance-id", SERVICE_INSTANCE_ID)
			.environmentVariable("ENV_VAR_1", "value1")
			.environmentVariable("ENV_VAR_2", "value2")
			.build();

		then(operationsApplications).should().pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithStartOption() {
		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.property(CloudFoundryDeploymentProperties.START_PROPERTY_KEY, "false")
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(operationsApplications).should().pushManifest(argThat(PushApplicationManifestRequest::getNoStart));
	}

	@Test
	void deployAppWithStartTrueByDefault() {
		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(operationsApplications).should().pushManifest(argThat(r -> !r.getNoStart()));
	}

	@Test
	void deleteServiceInstanceShouldUnbindServices() {
		given(operationsServices.deleteInstance(
			org.cloudfoundry.operations.services.DeleteServiceInstanceRequest.builder()
				.name("service-instance-name")
				.completionTimeout(Duration.ofSeconds(DEFAULT_COMPLETION_DURATION))
				.build()))
			.willReturn(Mono.empty());

		given(operationsServices.getInstance(GetServiceInstanceRequest.builder().name("service-instance-name").build()))
			.willReturn(Mono.just(ServiceInstance.builder()
				.id("siid")
				.type(ServiceInstanceType.MANAGED)
				.name("service-instance-name")
				.applications("app1", "app2")
				.build()));

		given(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.serviceInstanceName("service-instance-name")
			.applicationName("app1")
			.build()))
			.willReturn(Mono.empty());

		given(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.serviceInstanceName("service-instance-name")
			.applicationName("app2")
			.build()))
			.willReturn(Mono.empty());

		DeleteServiceInstanceRequest request =
			DeleteServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.properties(emptyMap())
				.build();

		StepVerifier.create(
			appDeployer.deleteServiceInstance(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
			.verifyComplete();

	}

	@Test
	void deleteServiceInstanceWithTarget() {
		given(operationsServices.deleteInstance(
			org.cloudfoundry.operations.services.DeleteServiceInstanceRequest.builder()
				.name("service-instance-name")
				.completionTimeout(Duration.ofSeconds(DEFAULT_COMPLETION_DURATION))
				.build()))
			.willReturn(Mono.empty());

		given(operationsServices.getInstance(GetServiceInstanceRequest.builder().name("service-instance-name").build()))
			.willReturn(Mono.just(ServiceInstance.builder()
				.id("siid")
				.type(ServiceInstanceType.MANAGED)
				.name("service-instance-name")
				.applications("app1", "app2")
				.build()));

		given(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.serviceInstanceName("service-instance-name")
			.applicationName("app1")
			.build()))
			.willReturn(Mono.empty());

		given(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.serviceInstanceName("service-instance-name")
			.applicationName("app2")
			.build()))
			.willReturn(Mono.empty());

		given(operationsOrganizations
			.get(
				OrganizationInfoRequest
					.builder()
					.name("default-org")
					.build()))
			.willReturn(Mono.just(
				OrganizationDetail
					.builder()
					.id("default-org-id")
					.name("default-org")
					.quota(OrganizationQuota
						.builder()
						.id("quota-id")
						.instanceMemoryLimit(0)
						.organizationId("default-org-id")
						.name("quota")
						.paidServicePlans(false)
						.totalMemoryLimit(0)
						.totalRoutes(0)
						.totalServiceInstances(0)
						.build())
					.build()));

		given(clientOrganizations
			.listSpaces(ListOrganizationSpacesRequest
				.builder()
				.name("service-instance-id")
				.organizationId("default-org-id")
				.page(1)
				.build()))
			.willReturn(Mono.just(ListOrganizationSpacesResponse
				.builder()
				.resource(SpaceResource
					.builder()
					.entity(SpaceEntity
						.builder()
						.name("service-instance-id")
						.build())
					.metadata(Metadata
						.builder()
						.id("service-instance-space-id")
						.build())
					.build())
				.build()));

		given(clientSpaces
			.delete(DeleteSpaceRequest
				.builder()
				.spaceId("service-instance-space-id")
				.build()))
			.willReturn(Mono.empty());

		DeleteServiceInstanceRequest request =
			DeleteServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.properties(emptyMap())
				.properties(singletonMap(TARGET_PROPERTY_KEY, "service-instance-id"))
				.build();

		StepVerifier.create(
			appDeployer.deleteServiceInstance(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
			.verifyComplete();
	}

	@Test
	void createServiceInstance() {
		given(operationsServices.createInstance(
			org.cloudfoundry.operations.services.CreateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.serviceName("db-service")
				.planName("standard")
				.completionTimeout(Duration.ofSeconds(100)) //expect per app configured duration to be received and not
				// CloudFoundryDeployment default
				.parameters(emptyMap())
				.build()))
			.willReturn(Mono.empty());

		Map<String, String> userProvidedTimeoutForBrokeredApp =
			singletonMap(CloudFoundryDeploymentProperties.API_POLLING_TIMEOUT_PROPERTY_KEY, "100");
		CreateServiceInstanceRequest request =
			CreateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.name("db-service")
				.plan("standard")
				.properties(userProvidedTimeoutForBrokeredApp)
				.parameters(emptyMap())
				.build();

		StepVerifier.create(
			appDeployer.createServiceInstance(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
			.verifyComplete();
	}

	@Test
	void createServiceInstanceWithTarget() {
		given(operationsOrganizations
			.get(
				OrganizationInfoRequest
					.builder()
					.name("default-org")
					.build()))
			.willReturn(Mono.just(
				OrganizationDetail
					.builder()
					.id("default-org-id")
					.name("default-org")
					.quota(OrganizationQuota
						.builder()
						.id("quota-id")
						.instanceMemoryLimit(0)
						.organizationId("default-org-id")
						.name("quota")
						.paidServicePlans(false)
						.totalMemoryLimit(0)
						.totalRoutes(0)
						.totalServiceInstances(0)
						.build())
					.build()));

		given(clientOrganizations
			.listSpaces(ListOrganizationSpacesRequest
				.builder()
				.name("service-instance-id")
				.organizationId("default-org-id")
				.page(1)
				.build()))
			.willReturn(Mono.empty());

		given(clientSpaces
			.create(CreateSpaceRequest
				.builder()
				.organizationId("default-org-id")
				.name("service-instance-id")
				.build()))
			.willReturn(Mono.empty());

		given(operationsServices
			.createInstance(
				org.cloudfoundry.operations.services.CreateServiceInstanceRequest
					.builder()
					.serviceInstanceName("service-instance-name")
					.serviceName("db-service")
					.planName("standard")
					.completionTimeout(Duration.ofSeconds(DEFAULT_COMPLETION_DURATION))
					.parameters(emptyMap())
					.build()))
			.willReturn(Mono.empty());

		CreateServiceInstanceRequest request =
			CreateServiceInstanceRequest
				.builder()
				.serviceInstanceName("service-instance-name")
				.name("db-service")
				.plan("standard")
				.parameters(emptyMap())
				.properties(singletonMap(TARGET_PROPERTY_KEY, "service-instance-id"))
				.build();

		StepVerifier.create(
			appDeployer.createServiceInstance(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
			.verifyComplete();
	}

	@Test
	void updateServiceInstanceUpdatesWithParametersUpdatesBackingServices() {
		Map<String, Object> parameters = singletonMap("param1", "value");

		given(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(parameters)
				.completionTimeout(Duration.ofSeconds(DEFAULT_COMPLETION_DURATION))
				.build()))
			.willReturn(Mono.empty());

		UpdateServiceInstanceRequest request =
			UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(parameters)
				.build();

		StepVerifier.create(
			appDeployer.updateServiceInstance(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
			.verifyComplete();
	}

	@Test
	void updateServiceInstanceRebindsBackingAppWhenRequired() {
		given(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.completionTimeout(Duration.ofSeconds(DEFAULT_COMPLETION_DURATION))
				.build()))
			.willReturn(Mono.empty());

		given(operationsServices.getInstance(GetServiceInstanceRequest.builder()
			.name("service-instance-name")
			.build()))
			.willReturn(Mono.just(ServiceInstance.builder()
				.name("service-instance-name")
				.id("service-instance-guid")
				.type(ServiceInstanceType.MANAGED)
				.applications("app1", "app2")
				.build()));

		given(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.applicationName("app1")
			.serviceInstanceName("service-instance-name")
			.build()))
			.willReturn(Mono.empty());

		given(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.applicationName("app2")
			.serviceInstanceName("service-instance-name")
			.build()))
			.willReturn(Mono.empty());

		given(operationsServices.bind(BindServiceInstanceRequest.builder()
			.applicationName("app1")
			.serviceInstanceName("service-instance-name")
			.build()))
			.willReturn(Mono.empty());

		given(operationsServices.bind(BindServiceInstanceRequest.builder()
			.applicationName("app2")
			.serviceInstanceName("service-instance-name")
			.build()))
			.willReturn(Mono.empty());

		UpdateServiceInstanceRequest request =
			UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.rebindOnUpdate(true)
				.build();

		StepVerifier.create(
			appDeployer.updateServiceInstance(request))
			.verifyComplete();
	}

	@Test
	void updateServiceInstanceDoesNothingWithoutParametersNorPlan() {
		given(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.build()))
			.willReturn(Mono.empty());

		UpdateServiceInstanceRequest request =
			UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.build();

		StepVerifier.create(
			appDeployer.updateServiceInstance(request))
			.verifyComplete();
	}


	@Test
	void updateServiceInstanceWithPlanUpdatesBackingService() {
		given(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.planName("premium")
				.build()))
			.willReturn(Mono.empty());

		UpdateServiceInstanceRequest request =
			UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.plan("premium")
				.build();

		StepVerifier.create(
			appDeployer.updateServiceInstance(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
			.verifyComplete();
	}

	@Test
	void getServiceInstanceById() {
		given(operationsServices.getInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(ServiceInstance.builder()
				.id("foo-service-instance-id")
				.name("my-foo-service")
				.service("foo-service")
				.plan("foo-plan")
				.type(ServiceInstanceType.MANAGED)
				.build()));

		given(clientServiceInstances
			.get(any(org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.entity(ServiceInstanceEntity.builder()
					.name("my-foo-service")
					.spaceId("foo-space-id")
					.build())
				.build()));

		given(clientSpaces.get(GetSpaceRequest.builder()
			.spaceId("foo-space-id")
			.build()))
			.willReturn(Mono.just(GetSpaceResponse.builder()
				.entity(SpaceEntity.builder()
					.name("foo-space")
					.organizationId("foo-organization-id")
					.build())
				.build()));

		given(clientOrganizations.get(GetOrganizationRequest.builder()
			.organizationId("foo-organization-id")
			.build()))
			.willReturn(Mono.just(GetOrganizationResponse.builder()
				.entity(OrganizationEntity.builder()
					.name("foo-organization")
					.build())
				.build()));

		org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest request =
			org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest
				.builder()
				.serviceInstanceId("foo-service-instance-id")
				.properties(emptyMap())
				.build();

		StepVerifier.create(appDeployer.getServiceInstance(request))
			.assertNext(response -> {
				assertThat(response.getName()).isEqualTo("my-foo-service");
				assertThat(response.getService()).isEqualTo("foo-service");
				assertThat(response.getPlan()).isEqualTo("foo-plan");
			})
			.verifyComplete();

		then(operationsUtils).should()
			.getOperationsForOrgAndSpace(argThat("foo-organization"::equals), argThat("foo-space"::equals));
		then(cloudFoundryClient).should().serviceInstances();
		then(clientServiceInstances).should()
			.get(argThat(req -> "foo-service-instance-id".equals(req.getServiceInstanceId())));
		then(cloudFoundryClient).should().spaces();
		then(cloudFoundryClient).should().organizations();
		then(clientSpaces).should().get(argThat(req -> "foo-space-id".equals(req.getSpaceId())));
		then(clientOrganizations).should().get(argThat(req -> "foo-organization-id".equals(req.getOrganizationId())));
		then(cloudFoundryOperations).should().services();
		then(operationsServices).should().getInstance(argThat(req -> "my-foo-service".equals(req.getName())));
		then(cloudFoundryClient).shouldHaveNoMoreInteractions();
		then(cloudFoundryOperations).shouldHaveNoMoreInteractions();
		then(operationsUtils).shouldHaveNoMoreInteractions();
	}

	@Test
	void getServiceInstanceByName() {
		given(operationsServices.getInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(ServiceInstance.builder()
				.id("foo-service-instance-id")
				.name("my-foo-service")
				.service("foo-service")
				.plan("foo-plan")
				.type(ServiceInstanceType.MANAGED)
				.build()));

		org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest request = org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest
			.builder()
			.name("my-foo-service")
			.build();

		StepVerifier.create(appDeployer.getServiceInstance(request))
			.assertNext(response -> {
				assertThat(response.getName()).isEqualTo("my-foo-service");
				assertThat(response.getService()).isEqualTo("foo-service");
				assertThat(response.getPlan()).isEqualTo("foo-plan");
			})
			.verifyComplete();

		then(operationsUtils).should().getOperations(argThat(CollectionUtils::isEmpty));
		then(cloudFoundryOperations).should().services();
		then(operationsServices).should().getInstance(argThat(req -> "my-foo-service".equals(req.getName())));
		then(cloudFoundryClient).shouldHaveNoInteractions();
		then(cloudFoundryOperations).shouldHaveNoMoreInteractions();
		then(operationsUtils).shouldHaveNoMoreInteractions();
	}

	@Test
	void getServiceInstanceByNameAndSpace() {
		given(operationsServices.getInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(ServiceInstance.builder()
				.id("foo-service-instance-id")
				.name("my-foo-service")
				.service("foo-service")
				.plan("foo-plan")
				.type(ServiceInstanceType.MANAGED)
				.build()));

		org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest request = org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest
			.builder()
			.name("my-foo-service")
			.properties(singletonMap(TARGET_PROPERTY_KEY, "foo-space"))
			.build();

		StepVerifier.create(appDeployer.getServiceInstance(request))
			.assertNext(response -> {
				assertThat(response.getName()).isEqualTo("my-foo-service");
				assertThat(response.getService()).isEqualTo("foo-service");
				assertThat(response.getPlan()).isEqualTo("foo-plan");
			})
			.verifyComplete();

		then(operationsUtils).should().getOperations(
			argThat(argument -> "foo-space".equals(argument.get(TARGET_PROPERTY_KEY))));
		then(cloudFoundryOperations).should().services();
		then(operationsServices).should().getInstance(argThat(req -> "my-foo-service".equals(req.getName())));
		then(cloudFoundryClient).shouldHaveNoInteractions();
		then(cloudFoundryOperations).shouldHaveNoMoreInteractions();
		then(operationsUtils).shouldHaveNoMoreInteractions();
	}

	private ApplicationManifest.Builder baseManifest() {
		return ApplicationManifest.builder()
			.services(new ArrayList<>());
	}

	private ApplicationManifest.Builder baseManifestWithSpringAppJson() {
		return ApplicationManifest.builder()
			.environmentVariable("SPRING_APPLICATION_JSON",
				"{\"spring.cloud.appbroker.service-instance-id\":\"" + SERVICE_INSTANCE_ID + "\"}")
			.services(new ArrayList<>());
	}

	private ApplicationManifest.Builder baseManifestWithSpringAppJson(String json) {
		return ApplicationManifest.builder()
			.environmentVariable("SPRING_APPLICATION_JSON",
				"{" + json + ",\"spring.cloud.appbroker.service-instance-id\":\"" + SERVICE_INSTANCE_ID + "\"}")
			.services(new ArrayList<>());
	}

	private ArgumentMatcher<PushApplicationManifestRequest> matchesManifest(ApplicationManifest expectedManifest) {
		return new ArgumentMatcher<PushApplicationManifestRequest>() {
			@Override
			public boolean matches(PushApplicationManifestRequest request) {
				if (request.getManifests().size() == EXPECTED_MANIFESTS) {
					return request.getManifests().get(0).equals(expectedManifest);
				}

				return false;
			}

			@Override
			public String toString() {
				return expectedManifest.toString();
			}
		};
	}

}
