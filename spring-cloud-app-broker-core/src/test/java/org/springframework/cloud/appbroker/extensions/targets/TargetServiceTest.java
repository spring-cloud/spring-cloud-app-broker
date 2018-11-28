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

package org.springframework.cloud.appbroker.extensions.targets;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.TargetSpec;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class TargetServiceTest {

	private TargetService targetService;

	@BeforeEach
	void setUp() {
		targetService = new TargetService(singletonList(new SpacePerServiceInstance()));
	}

	@Test
	void shouldAddProperties() {
		// given an app with a target
		TargetSpec targetSpec = TargetSpec.builder().name("SpacePerServiceInstance").build();
		BackingApplication backingApplication = BackingApplication.builder().name("app-name").build();

		//when add gets called
		List<BackingApplication> updatedBackingApplications = targetService.addToBackingApplications(singletonList(backingApplication), targetSpec, "service-id").block();

		//then a host and space are added
		BackingApplication updatedBackingApplication = updatedBackingApplications.get(0);
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
		List<BackingApplication> updatedBackingApplications = targetService.addToBackingApplications(Lists.newArrayList(backingApplication1, backingApplication2), targetSpec, "service-id").block();

		//then a host and space are added
		BackingApplication updatedBackingApplication1 = updatedBackingApplications.get(0);
		assertThat(updatedBackingApplication1.getProperties().get("host")).isEqualTo("app-name1-service-id");
		assertThat(updatedBackingApplication1.getProperties().get("target")).isEqualTo("service-id");

		BackingApplication updatedBackingApplication2 = updatedBackingApplications.get(1);
		assertThat(updatedBackingApplication2.getProperties().get("host")).isEqualTo("app-name2-service-id");
		assertThat(updatedBackingApplication2.getProperties().get("target")).isEqualTo("service-id");
	}
}