/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cloud.appbroker.extensions.targets;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.TargetSpec;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class TargetServiceTest {

	private TargetService targetService;

	@BeforeEach
	void setUp() {
		targetService = new TargetService(
			Arrays.asList(new SpacePerServiceInstance(), new ServiceInstanceGuidSuffix()));
	}

	@Test
	void shouldAddProperties() {
		// given an app with a target
		TargetSpec targetSpec = TargetSpec.builder().name("SpacePerServiceInstance").build();
		BackingApplication backingApplication = BackingApplication.builder().name("app-name").build();

		//when add gets called
		List<BackingApplication> updatedBackingApplications =
			targetService
				.addToBackingApplications(singletonList(backingApplication), targetSpec, "service-id")
				.block();

		//then a host and space are added
		BackingApplication updatedBackingApplication = updatedBackingApplications.get(0);
		assertThat(updatedBackingApplication.getName()).isEqualTo("app-name");
		assertThat(updatedBackingApplication.getProperties().get("host")).isEqualTo("app-name-service-id");
		assertThat(updatedBackingApplication.getProperties().get("target")).isEqualTo("service-id");
	}

	@Test
	void shouldAddPropertiesToAllBackingApps() {
		// given an app with a target
		TargetSpec targetSpec = TargetSpec.builder().name("SpacePerServiceInstance").build();
		BackingApplication backingApplication1 = BackingApplication.builder().name("app-name1").build();
		BackingApplication backingApplication2 = BackingApplication.builder().name("app-name2").build();

		//when add gets called
		List<BackingApplication> updatedBackingApplications =
			targetService
				.addToBackingApplications(Lists.newArrayList(backingApplication1, backingApplication2), targetSpec,
					"service-id")
				.block();

		//then a host and space are added
		BackingApplication updatedBackingApplication1 = updatedBackingApplications.get(0);
		assertThat(updatedBackingApplication1.getName()).isEqualTo("app-name1");
		assertThat(updatedBackingApplication1.getProperties().get("host")).isEqualTo("app-name1-service-id");
		assertThat(updatedBackingApplication1.getProperties().get("target")).isEqualTo("service-id");

		BackingApplication updatedBackingApplication2 = updatedBackingApplications.get(1);
		assertThat(updatedBackingApplication2.getName()).isEqualTo("app-name2");
		assertThat(updatedBackingApplication2.getProperties().get("host")).isEqualTo("app-name2-service-id");
		assertThat(updatedBackingApplication2.getProperties().get("target")).isEqualTo("service-id");
	}

	@Test
	void shouldAddShortNameAsServiceInstanceGuidSuffix() {
		final String appName = "boo";
		final String serviceInstanceId = UUID.randomUUID().toString();
		final TargetSpec targetSpec = TargetSpec.builder().name("ServiceInstanceGuidSuffix").build();
		final BackingApplication backingApplication = BackingApplication.builder().name(appName).build();

		StepVerifier.create(targetService.addToBackingApplications(singletonList(backingApplication), targetSpec,
				serviceInstanceId))
				.consumeNextWith(backingApplications -> {
					String name = backingApplications.get(0).getName();
					assertThat(name.length()).isLessThan(50).isGreaterThan(serviceInstanceId.length());
					assertThat(name).contains(serviceInstanceId).contains(appName);
				})
				.verifyComplete();
	}

	@Test
	void shouldAddLongNameAsServiceInstanceGuidSuffix() {
		final String appName = "this-is-a-much-longer-app-name-that-will-require-truncation";
		final String serviceInstanceId = UUID.randomUUID().toString();
		final TargetSpec targetSpec = TargetSpec.builder().name("ServiceInstanceGuidSuffix").build();
		final BackingApplication backingApplication = BackingApplication.builder().name(appName).build();

		StepVerifier.create(targetService.addToBackingApplications(singletonList(backingApplication), targetSpec,
				serviceInstanceId))
				.consumeNextWith(backingApplications -> {
					String name = backingApplications.get(0).getName();
					assertThat(name.length()).isEqualTo(50);
					assertThat(name).contains(serviceInstanceId);
				})
				.verifyComplete();
	}

}
