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

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudFoundryAppDeployerTest {

	private static final String APP_NAME = "test-app";
	private static final String APP_PATH = "test.jar";

	private AppDeployer appDeployer;

	@Mock
	private Applications applications;

	@Mock
	private CloudFoundryOperations cloudFoundryOperations;

	@Mock
	private ResourceLoader resourceLoader;

	private CloudFoundryDeploymentProperties deploymentProperties;

	@BeforeEach
	void setUp() {
		deploymentProperties = new CloudFoundryDeploymentProperties();

		when(applications.pushManifest(any()))
			.thenReturn(Mono.empty());
		when(cloudFoundryOperations.applications())
			.thenReturn(applications);
		when(resourceLoader.getResource(APP_PATH))
			.thenReturn(new FileSystemResource(APP_PATH));

		appDeployer = new CloudFoundryAppDeployer(
			new CloudFoundryTargetProperties(), deploymentProperties, cloudFoundryOperations, resourceLoader);
	}

	@Test
	void deployAppWithPlatformDefaults() {
		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.build();
		
		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifest()
			.name(APP_NAME)
			.path(new File(APP_PATH).toPath())
			.build();

		verify(applications).pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	@Test
	void deployAppWithPropertiesInRequest() {
		DeployApplicationRequest request = DeployApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
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

		ApplicationManifest expectedManifest = baseManifest()
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

		verify(applications).pushManifest(argThat(matchesManifest(expectedManifest)));
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
			.build();

		StepVerifier.create(appDeployer.deploy(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		ApplicationManifest expectedManifest = baseManifest()
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

		verify(applications).pushManifest(argThat(matchesManifest(expectedManifest)));
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

		ApplicationManifest expectedManifest = baseManifest()
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

		verify(applications).pushManifest(argThat(matchesManifest(expectedManifest)));
	}

	private ApplicationManifest.Builder baseManifest() {
		return ApplicationManifest.builder()
			.environmentVariable("SPRING_APPLICATION_INDEX", "${vcap.application.instance_index}")
			.environmentVariable("SPRING_CLOUD_APPLICATION_GUID", "${vcap.application.name}:${vcap.application.instance_index}")
			.environmentVariable("SPRING_APPLICATION_JSON", "{}")
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