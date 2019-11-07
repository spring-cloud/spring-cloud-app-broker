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

package org.springframework.cloud.appbroker.deployer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DefaultBackingAppDeploymentServiceTest {

	@Mock
	private DeployerClient deployerClient;

	private BackingAppDeploymentService backingAppDeploymentService;

	private BackingApplications backingApps;

	@BeforeEach
	void setUp() {
		backingAppDeploymentService = new DefaultBackingAppDeploymentService(deployerClient);
		backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("testApp1")
				.path("https://myfiles/app1.jar")
				.build())
			.backingApplication(BackingApplication.builder()
				.name("testApp2")
				.path("https://myfiles/app2.jar")
				.build())
			.build();
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void shouldDeployApplications() {
		doReturn(Mono.just("app1"))
			.when(deployerClient).deploy(backingApps.get(0), "instance-id");
		doReturn(Mono.just("app2"))
			.when(deployerClient).deploy(backingApps.get(1), "instance-id");

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("app1");
		expectedValues.add("app2");

		StepVerifier.create(backingAppDeploymentService.deploy(backingApps, "instance-id"))
			// deployments are run in parallel, so the order of completion is not predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void shouldUndeployApplications() {
		doReturn(Mono.just("deleted1"))
			.when(deployerClient).undeploy(backingApps.get(0));
		doReturn(Mono.just("deleted2"))
			.when(deployerClient).undeploy(backingApps.get(1));

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("deleted1");
		expectedValues.add("deleted2");

		StepVerifier.create(backingAppDeploymentService.undeploy(backingApps))
			// deployments are run in parallel, so the order of completion is not predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

}
