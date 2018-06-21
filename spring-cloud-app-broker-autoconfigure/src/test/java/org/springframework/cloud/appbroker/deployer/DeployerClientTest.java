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

package org.springframework.cloud.appbroker.deployer;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryReactiveAppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.NoOpAppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import reactor.core.publisher.Mono;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeployerClientTest {

	private DeployerClient deployerClient;

	@Mock
	private CloudFoundryOperations cloudFoundryOperations;
	@Mock
	private Applications applications;

	@BeforeEach
	void setUp() {
		NoOpAppNameGenerator applicationNameGenerator = new NoOpAppNameGenerator();
		CloudFoundryDeploymentProperties deploymentProperties = new CloudFoundryDeploymentProperties();
		deploymentProperties.setDisk("1024M");

		when(applications.pushManifest(any())).thenReturn(Mono.empty());
		when(cloudFoundryOperations.applications()).thenReturn(applications);

		ReactiveAppDeployer reactiveAppDeployer = new CloudFoundryReactiveAppDeployer(applicationNameGenerator, deploymentProperties, cloudFoundryOperations, null);
		deployerClient = new DeployerClient(reactiveAppDeployer);
	}

	@Test
	void shouldDeployAppByName() {
		DeployerApplication deployerApplication = new DeployerApplication();
		deployerApplication.setAppName("helloworld");
		deployerApplication.setPath("http://myfiles/app.jar");

		//when I call deploy an app with a given name
		Mono<String> lastState = deployerClient.deploy(deployerApplication);

		//then
		assertThat(lastState.block()).isEqualTo("running");

		Mockito.verify(applications).pushManifest(expectedPushManifestRequest());
	}

	private static PushApplicationManifestRequest expectedPushManifestRequest() {
		// FIXME
		return PushApplicationManifestRequest.builder().build();
	}

}