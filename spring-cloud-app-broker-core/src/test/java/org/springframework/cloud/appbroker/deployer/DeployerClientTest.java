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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeployerClientTest {

	private static final String APP_NAME = "helloworld";
	private static final String APP_PATH = "classpath:/jars/app.jar";

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
	void shouldDeployAppByName() {
		when(resourceLoader.getResource(APP_PATH)).thenReturn(new FileSystemResource(APP_PATH));
		when(appDeployer.deploy(any())).thenReturn(Mono.just("appID"));

		BackingAppProperties application = new BackingAppProperties(APP_NAME, APP_PATH);
		Mono<String> lastState = deployerClient.deploy(application);

		assertThat(lastState.block()).isEqualTo("running");

		verify(appDeployer).deploy(argThat(matchesRequest()));
	}

	@Test
	void shouldUndeployAppByName() {
		BackingAppProperties application = new BackingAppProperties(APP_NAME, APP_PATH);
		deployerClient.undeploy(application);

		verify(appDeployer).undeploy(eq(APP_NAME));
	}

	private ArgumentMatcher<AppDeploymentRequest> appDeploymentRequestMatcher() {
		return request ->
			request.getDefinition().getName().equals(APP_NAME) &&
				request.getResource().getFilename().equals("app.jar");
	}

	private ArgumentMatcher<AppDeploymentRequest> matchesRequest() {
		return appDeploymentRequestMatcher();
	}
}