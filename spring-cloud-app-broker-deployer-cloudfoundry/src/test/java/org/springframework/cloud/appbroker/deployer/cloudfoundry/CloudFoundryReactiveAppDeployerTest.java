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

import java.net.MalformedURLException;
import java.util.Collections;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudFoundryReactiveAppDeployerTest {

	private static final int APP_MEMORY = 1024;
	private static final String APP_MEMORY_STRING = "1024M";
	private static final String APP_NAME = "test-app";
	private static final String APP_PATH = "test.jar";
	private ReactiveAppDeployer appDeployer;
	@Mock
	private Applications applications;
	@Mock
	private CloudFoundryOperations cloudFoundryOperations;

	@BeforeEach
	void setUp() {
		NoOpAppNameGenerator applicationNameGenerator = new NoOpAppNameGenerator();
		CloudFoundryDeploymentProperties deploymentProperties = new CloudFoundryDeploymentProperties();
		deploymentProperties.setDisk(APP_MEMORY_STRING);

		when(applications.pushManifest(any())).thenReturn(Mono.empty());
		when(cloudFoundryOperations.applications()).thenReturn(applications);

		appDeployer = new CloudFoundryReactiveAppDeployer(applicationNameGenerator,
			deploymentProperties, cloudFoundryOperations, null);
	}

	@Test
	void shouldDeployAppWithNameAndPath() throws MalformedURLException {
		AppDefinition appDefinition = new AppDefinition(APP_NAME, Collections.emptyMap());
		Resource resource = new FileUrlResource(APP_PATH);
		AppDeploymentRequest request = new AppDeploymentRequest(appDefinition, resource);
		appDeployer.deploy(request);

		verify(applications).pushManifest(argThat(matchesExpectedManifest()));
	}

	private ArgumentMatcher<PushApplicationManifestRequest> matchesExpectedManifest() {
		return request -> {
			if (request.getManifests().size() == 1) {
				ApplicationManifest manifest = request.getManifests().get(0);
				return APP_NAME.equals(manifest.getName())
					&& APP_PATH.equals(manifest.getPath().toString())
					&& manifest.getDisk() == APP_MEMORY;
			}

			return false;
		};
	}

}