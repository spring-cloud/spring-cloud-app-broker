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

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Mono;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeployerClientTest {

	private static final String APP_NAME = "helloworld";
	private static final String APP_ARCHIVE = "app.jar";
	private static final String APP_PATH = "classpath:/jars/" + APP_ARCHIVE;

	private DeployerClient deployerClient;

	@Mock
	private ReactiveAppDeployer appDeployer;

	@Mock
	private ResourceLoader resourceLoader;

	@BeforeEach
	void setUp() {
		deployerClient = new DeployerClient(appDeployer, resourceLoader);
	}

	@Test
	void shouldDeployApp() {
		// given
		when(resourceLoader.getResource(APP_PATH)).thenReturn(new FileSystemResource(APP_PATH));
		when(appDeployer.deploy(any())).thenReturn(Mono.just("appID"));

		BackingApplication application = new BackingApplication(APP_NAME, APP_PATH);

		// when
		Mono<String> lastState = deployerClient.deploy(application);

		// then
		assertThat(lastState.block()).isEqualTo("running");

		verify(appDeployer).deploy(argThat(matchesRequest(APP_NAME, APP_ARCHIVE, Collections.emptyMap())));
	}

	@Test
	@SuppressWarnings("serial")
	void shouldDeployAppWithProperties() {
		// given
		when(resourceLoader.getResource(APP_PATH)).thenReturn(new FileSystemResource(APP_PATH));
		when(appDeployer.deploy(any())).thenReturn(Mono.just("appID"));

		Map<String, String> properties = new HashMap<String, String>() {{
			put("memory", "1G");
			put("instances", "2");
		}};
		BackingApplication application = new BackingApplication(APP_NAME, APP_PATH, properties);

		// when
		Mono<String> lastState = deployerClient.deploy(application);

		// then
		assertThat(lastState.block()).isEqualTo("running");

		verify(appDeployer).deploy(argThat(matchesRequest(APP_NAME, APP_ARCHIVE, properties)));
	}

	@Test
	@SuppressWarnings("serial")
	void shouldDeployAppWithService() {
		// given
		when(resourceLoader.getResource(APP_PATH)).thenReturn(new FileSystemResource(APP_PATH));
		when(appDeployer.deploy(any())).thenReturn(Mono.just("appID"));

		BackingApplication application = new BackingApplication(APP_NAME, APP_PATH, Collections.singletonList("my-db-service"));

		// when
		Mono<String> lastState = deployerClient.deploy(application);

		// then
		assertThat(lastState.block()).isEqualTo("running");

		Map<String, String> properties = new HashMap<String, String>() {{
			put("spring.cloud.deployer.cloudfoundry.services", "my-db-service");
		}};
		verify(appDeployer).deploy(argThat(matchesRequest(APP_NAME, APP_ARCHIVE, properties)));
	}

	@Test
	void shouldUndeployApp() {
		// given
		when(appDeployer.undeploy(any())).thenReturn(Mono.empty());

		BackingApplication application = new BackingApplication(APP_NAME, APP_PATH);

		// when
		Mono<String> lastState = deployerClient.undeploy(application);

		// then
		assertThat(lastState.block()).isEqualTo("deleted");

		verify(appDeployer).undeploy(eq(APP_NAME));
	}

	private ArgumentMatcher<AppDeploymentRequest> matchesRequest(String appName, String appArchive,
																 Map<String, String> properties) {
		return request ->
			request.getDefinition().getName().equals(appName) &&
				request.getResource().getFilename().equals(appArchive) &&
				request.getDeploymentProperties().equals(properties);
	}
}