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

package org.springframework.cloud.appbroker.instance.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBackingAppDeployerTest {
	@Mock
	private ReactiveAppDeployer appDeployer;

	@Mock
	private ResourceLoader resourceLoader;

	@Test
	public void test() {
		when(resourceLoader.getResource("test-path"))
			.thenReturn(new ByteArrayResource("resource".getBytes()));

		when(appDeployer.deploy(any(AppDeploymentRequest.class)))
			.thenReturn(Mono.just("deployed-app-id"));

		BackingAppParameters backingAppParameters = new BackingAppParameters("test-app", "test-path", null);

		DefaultBackingAppDeployer backingAppDeployer = new DefaultBackingAppDeployer(appDeployer, resourceLoader);
		Mono<String> appId = backingAppDeployer.deploy(backingAppParameters, null);

		assertThat(appId.block()).isEqualTo("deployed-app-id");
	}
}