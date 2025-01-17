/*
 * Copyright 2016-2020 the original author or authors.
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
class DefaultBackingAppDeploymentServiceTests {

	@Mock
	private DeployerClient deployerClient;

	private BackingAppDeploymentService backingAppDeploymentService;

	private BackingApplications backingApps;

	@BeforeEach
	void setUp() {
		this.backingAppDeploymentService = new DefaultBackingAppDeploymentService(this.deployerClient);
		this.backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder().name("testApp1").path("https://myfiles/app1.jar").build())
			.backingApplication(BackingApplication.builder().name("testApp2").path("https://myfiles/app2.jar").build())
			.build();
	}

	@Test
	void shouldDeployApplications() {
		doReturn(Mono.just("app1")).when(this.deployerClient).deploy(this.backingApps.get(0), "instance-id");
		doReturn(Mono.just("app2")).when(this.deployerClient).deploy(this.backingApps.get(1), "instance-id");

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("app1");
		expectedValues.add("app2");

		StepVerifier.create(this.backingAppDeploymentService.deploy(this.backingApps, "instance-id"))
			// deployments are run in parallel, so the order of completion is not
			// predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

	@Test
	void shouldPrepareApplicationsForUpdate() {
		doReturn(Mono.just("app1")).when(this.deployerClient).preUpdate(this.backingApps.get(0), "instance-id");
		doReturn(Mono.just("app2")).when(this.deployerClient).preUpdate(this.backingApps.get(1), "instance-id");

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("app1");
		expectedValues.add("app2");

		StepVerifier.create(this.backingAppDeploymentService.prepareForUpdate(this.backingApps, "instance-id"))
			// update preparations are run in parallel, so the order of completion is not
			// predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

	@Test
	void shouldUpdateApplications() {
		doReturn(Mono.just("app1")).when(this.deployerClient).update(this.backingApps.get(0), "instance-id");
		doReturn(Mono.just("app2")).when(this.deployerClient).update(this.backingApps.get(1), "instance-id");

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("app1");
		expectedValues.add("app2");

		StepVerifier.create(this.backingAppDeploymentService.update(this.backingApps, "instance-id"))
			// updates are run in parallel, so the order of completion is not predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

	@Test
	void shouldUndeployApplications() {
		doReturn(Mono.just("deleted1")).when(this.deployerClient).undeploy(this.backingApps.get(0));
		doReturn(Mono.just("deleted2")).when(this.deployerClient).undeploy(this.backingApps.get(1));

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("deleted1");
		expectedValues.add("deleted2");

		StepVerifier.create(this.backingAppDeploymentService.undeploy(this.backingApps))
			// deployments are run in parallel, so the order of completion is not
			// predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

}
