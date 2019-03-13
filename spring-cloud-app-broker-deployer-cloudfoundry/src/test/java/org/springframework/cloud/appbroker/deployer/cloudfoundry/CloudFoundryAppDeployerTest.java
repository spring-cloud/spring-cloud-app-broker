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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstances;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceResponse;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("UnassignedFluxMonoInstance")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudFoundryAppDeployerTest {

	private static final String APP_NAME = "test-app";
	private static final String APP_PATH = "test.jar";
	private static final String SERVICE_INSTANCE_ID = "service-instance-id";

	private AppDeployer appDeployer;

	@Mock
	private Applications operationsApplications;

	@Mock
	private Services operationsServices;

	@Mock
	private Spaces operationsSpaces;

	@Mock
	private ServiceInstances clientServiceInstances;

	@Mock
	private org.cloudfoundry.client.v2.spaces.Spaces clientSpaces;

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

		when(operationsApplications.pushManifest(any())).thenReturn(Mono.empty());
		when(resourceLoader.getResource(APP_PATH)).thenReturn(new FileSystemResource(APP_PATH));

		when(cloudFoundryOperations.spaces()).thenReturn(operationsSpaces);
		when(cloudFoundryOperations.applications()).thenReturn(operationsApplications);
		when(cloudFoundryOperations.services()).thenReturn(operationsServices);
		when(cloudFoundryClient.serviceInstances()).thenReturn(clientServiceInstances);
		when(cloudFoundryClient.spaces()).thenReturn(clientSpaces);
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(cloudFoundryOperations));
		when(operationsUtils.getOperationsForSpace(anyString())).thenReturn(Mono.just(cloudFoundryOperations));

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

		verify(operationsApplications).pushManifest(argThat(matchesManifest(expectedManifest)));
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
			.property(CloudFoundryDeploymentProperties.DOMAIN_PROPERTY, "domain")
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
			.domain("domain")
			.host("host")
			.noRoute(true)
			.build();

		verify(operationsApplications).pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithDefaultProperties() {
		deploymentProperties.setCount(3);
		deploymentProperties.setMemory("2G");
		deploymentProperties.setDisk("3G");
		deploymentProperties.setBuildpack("buildpack");
		deploymentProperties.setHealthCheck(ApplicationHealthCheck.HTTP);
		deploymentProperties.setHealthCheckHttpEndpoint("/healthcheck");
		deploymentProperties.setDomain("domain");
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

		verify(operationsApplications).pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithRequestOverridingDefaultProperties() {
		deploymentProperties.setCount(3);
		deploymentProperties.setMemory("2G");
		deploymentProperties.setDisk("3G");
		deploymentProperties.setBuildpack("buildpack1");
		deploymentProperties.setHealthCheck(ApplicationHealthCheck.HTTP);
		deploymentProperties.setHealthCheckHttpEndpoint("/healthcheck1");
		deploymentProperties.setDomain("domain1");
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
			.property(CloudFoundryDeploymentProperties.DOMAIN_PROPERTY, "domain2")
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
			.domain("domain2")
			.host("host2")
			.noRoute(true)
			.build();

		verify(operationsApplications).pushManifest(argThat(matchesManifest(expectedManifest)));
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

		ApplicationManifest expectedManifest = baseManifestWithSpringAppJson("\"ENV_VAR_2\":\"value2\",\"ENV_VAR_1\":\"value1\"")
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.environmentVariable("JAVA_OPTS", "-Xms512m -Xmx1024m")
			.build();

		verify(operationsApplications).pushManifest(argThat(matchesManifest(expectedManifest)));
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

		verify(operationsApplications).pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deleteServiceInstanceShouldUnbindServices() {
		when(operationsServices.deleteInstance(
			org.cloudfoundry.operations.services.DeleteServiceInstanceRequest.builder()
																			 .name("service-instance-name")
																			 .build()))
			.thenReturn(Mono.empty());

		when(operationsServices.getInstance(GetServiceInstanceRequest.builder().name("service-instance-name").build()))
			.thenReturn(Mono.just(ServiceInstance.builder()
												 .id("siid")
												 .type(ServiceInstanceType.MANAGED)
												 .name("service-instance-name")
												 .applications("app1", "app2")
												 .build()));

		when(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
														 .serviceInstanceName("service-instance-name")
														 .applicationName("app1")
														 .build()))
			.thenReturn(Mono.empty());

		when(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
														 .serviceInstanceName("service-instance-name")
														 .applicationName("app2")
														 .build()))
			.thenReturn(Mono.empty());

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
	void createServiceInstance() {
		when(operationsServices.createInstance(
			org.cloudfoundry.operations.services.CreateServiceInstanceRequest.builder()
																			 .serviceInstanceName("service-instance-name")
																			 .serviceName("db-service")
																			 .planName("standard")
																			 .parameters(emptyMap())
																			 .build()))
			.thenReturn(Mono.empty());

		CreateServiceInstanceRequest request =
			CreateServiceInstanceRequest.builder()
										.serviceInstanceName("service-instance-name")
										.name("db-service")
										.plan("standard")
										.parameters(emptyMap())
										.build();

		StepVerifier.create(
			appDeployer.createServiceInstance(request))
					.assertNext(response -> assertThat(response.getName()).isEqualTo("service-instance-name"))
					.verifyComplete();
	}

	@Test
	void updateServiceInstanceUpdatesWithParameters() {
		Map<String, Object> parameters = Collections.singletonMap("param1", "value");

		when(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(parameters)
				.build()))
			.thenReturn(Mono.empty());

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
	void updateServiceInstanceRebindsWhenRequired() {
		when(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.build()))
			.thenReturn(Mono.empty());

		when(operationsServices.getInstance(GetServiceInstanceRequest.builder()
			.name("service-instance-name")
			.build()))
			.thenReturn(Mono.just(ServiceInstance.builder()
				.name("service-instance-name")
				.id("service-instance-guid")
				.type(ServiceInstanceType.MANAGED)
				.applications("app1", "app2")
				.build()));

		when(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.applicationName("app1")
			.serviceInstanceName("service-instance-name")
			.build()))
			.thenReturn(Mono.empty());

		when(operationsServices.unbind(UnbindServiceInstanceRequest.builder()
			.applicationName("app2")
			.serviceInstanceName("service-instance-name")
			.build()))
			.thenReturn(Mono.empty());

		when(operationsServices.bind(BindServiceInstanceRequest.builder()
			.applicationName("app1")
			.serviceInstanceName("service-instance-name")
			.build()))
			.thenReturn(Mono.empty());

		when(operationsServices.bind(BindServiceInstanceRequest.builder()
			.applicationName("app2")
			.serviceInstanceName("service-instance-name")
			.build()))
			.thenReturn(Mono.empty());

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
	void updateServiceInstanceDoesNothingWithoutParameters() {
		when(operationsServices.updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName("service-instance-name")
				.parameters(emptyMap())
				.build()))
			.thenReturn(Mono.empty());

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
	void getServiceInstanceById() {
		when(operationsServices.getInstance(any(GetServiceInstanceRequest.class)))
			.thenReturn(Mono.just(ServiceInstance.builder()
				.id("foo-service-instance-id")
				.name("my-foo-service")
				.service("foo-service")
				.plan("foo-plan")
				.type(ServiceInstanceType.MANAGED)
				.build()));

		when(clientServiceInstances
			.get(any(org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest.class)))
			.thenReturn(Mono.just(GetServiceInstanceResponse.builder()
				.entity(ServiceInstanceEntity.builder()
					.name("my-foo-service")
					.spaceId("foo-space-id")
					.build())
				.build()));

		when(clientSpaces.get(GetSpaceRequest.builder()
			.spaceId("foo-space-id")
			.build()))
			.thenReturn(Mono.just(GetSpaceResponse.builder()
				.entity(SpaceEntity.builder()
					.name("foo-space")
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

		verify(operationsUtils).getOperationsForSpace(argThat("foo-space"::equals));
		verify(cloudFoundryClient).serviceInstances();
		verify(clientServiceInstances).get(argThat(req -> "foo-service-instance-id".equals(req.getServiceInstanceId())));
		verify(cloudFoundryClient).spaces();
		verify(clientSpaces).get(argThat(req -> "foo-space-id".equals(req.getSpaceId())));
		verify(cloudFoundryOperations).services();
		verify(operationsServices).getInstance(argThat(req -> "my-foo-service".equals(req.getName())));
		verifyNoMoreInteractions(cloudFoundryClient, cloudFoundryOperations, operationsUtils);
	}

	@Test
	void getServiceInstanceByName() {
		when(operationsServices.getInstance(any(GetServiceInstanceRequest.class)))
			.thenReturn(Mono.just(ServiceInstance.builder()
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

		verify(operationsUtils).getOperations(argThat(CollectionUtils::isEmpty));
		verify(cloudFoundryOperations).services();
		verify(operationsServices).getInstance(argThat(req -> "my-foo-service".equals(req.getName())));
		verifyZeroInteractions(cloudFoundryClient);
		verifyNoMoreInteractions(cloudFoundryOperations, operationsUtils);
	}

	@Test
	void getServiceInstanceByNameAndSpace() {
		when(operationsServices.getInstance(any(GetServiceInstanceRequest.class)))
			.thenReturn(Mono.just(ServiceInstance.builder()
				.id("foo-service-instance-id")
				.name("my-foo-service")
				.service("foo-service")
				.plan("foo-plan")
				.type(ServiceInstanceType.MANAGED)
				.build()));

		org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest request = org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest
			.builder()
			.name("my-foo-service")
			.properties(Collections.singletonMap(DeploymentProperties.TARGET_PROPERTY_KEY, "foo-space"))
			.build();

		StepVerifier.create(appDeployer.getServiceInstance(request))
			.assertNext(response -> {
				assertThat(response.getName()).isEqualTo("my-foo-service");
				assertThat(response.getService()).isEqualTo("foo-service");
				assertThat(response.getPlan()).isEqualTo("foo-plan");
			})
			.verifyComplete();

		verify(operationsUtils).getOperations(
			argThat(argument -> "foo-space".equals(argument.get(DeploymentProperties.TARGET_PROPERTY_KEY))));
		verify(cloudFoundryOperations).services();
		verify(operationsServices).getInstance(argThat(req -> "my-foo-service".equals(req.getName())));
		verifyZeroInteractions(cloudFoundryClient);
		verifyNoMoreInteractions(cloudFoundryOperations, operationsUtils);
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
				if (request.getManifests().size() == 1) {
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