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

package org.springframework.cloud.appbroker.workflow.instance;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackingServicesUpdateValidatorServiceTest {

	private final BackingServicesUpdateValidatorService backingServicesUpdateValidatorService = new BackingServicesUpdateValidatorService();

	private BackingService mysql;

	private BackingService postgresql;

	@BeforeEach
	void setUp() {
		mysql = BackingService
			.builder()
			.name("mysql")
			.plan("10mb")
			.serviceInstanceName("my-mysql-service-instance")
			.build();
		postgresql = BackingService
			.builder()
			.name("postgresql")
			.plan("a-plan")
			.serviceInstanceName("my-pg-service-instance")
			.build();
	}

	@Test
	void emitsAnErrorWhenRejected() {
		//Given a rejected case (tested in "rejectsWhenNewBackingServicesCreated")
		Mono<List<BackingService>> requestedBackingServices = backingServicesUpdateValidatorService.validatePlanUpdate(
			Mono.just(Lists.newArrayList(mysql)),
			Mono.just(Lists.newArrayList(mysql, postgresql))
		);

		StepVerifier.create(requestedBackingServices)
			//then
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void validatesWhenNoChanges() {
		List<BackingService> validatedServices = backingServicesUpdateValidatorService.validateAndMutatePlanUpdate(
			Lists.newArrayList(mysql),
			Lists.newArrayList(mysql));
		assertThat(validatedServices).isEqualTo(Lists.newArrayList(mysql));
	}

	@Test
	void validatesAndSetPreviousPlanWhenPlanUpdates() {
		//given
		BackingService mysql10Mb = BackingService
			.builder()
			.name("mysql")
			.plan("10mb")
			.serviceInstanceName("my-service-instance")
			.build();
		BackingService mysql100Mb = BackingService
			.builder()
			.name("mysql")
			.plan("100mb")
			.serviceInstanceName("my-service-instance")
			.build();

		//when
		List<BackingService> validatedServices = backingServicesUpdateValidatorService.validateAndMutatePlanUpdate(
			Lists.newArrayList(mysql10Mb),
			Lists.newArrayList(mysql100Mb));

		//Then
		BackingService mysql100MbWithPreviousPlan = BackingService
			.builder()
			.name("mysql")
			.plan("100mb")
			.previousPlan("10mb")
			.serviceInstanceName("my-service-instance")
			.build();
		assertThat(validatedServices).isEqualTo(Lists.newArrayList(mysql100MbWithPreviousPlan));

	}

	@Test
	void rejectsWhenNewBackingServicesCreated() {
		assertThrows(Exception.class, () -> backingServicesUpdateValidatorService.validateAndMutatePlanUpdate(
			Lists.newArrayList(mysql),
			Lists.newArrayList(mysql, postgresql)));
	}

	@Test
	void rejectsWhenChangesType() {
		BackingService mysql = BackingService
			.builder()
			.name("mysql")
			.plan("10mb")
			.serviceInstanceName("my-service-instance")
			.build();
		BackingService pg = BackingService
			.builder()
			.name("postgresql")
			.plan("10mb")
			.serviceInstanceName("my-service-instance")
			.build();
		assertThrows(Exception.class, () -> backingServicesUpdateValidatorService.validateAndMutatePlanUpdate(
			Lists.newArrayList(mysql),
			Lists.newArrayList(pg)));
	}

	@Test
	void rejectsWhenChangeServiceInstanceName() {
		BackingService mysql = BackingService
			.builder()
			.name("mysql")
			.plan("10mb")
			.serviceInstanceName("my-service-instance")
			.build();
		BackingService pg = BackingService
			.builder()
			.name("mysql")
			.plan("10mb")
			.serviceInstanceName("my-unrelated-service-instance")
			.build();
		assertThrows(Exception.class, () -> backingServicesUpdateValidatorService.validateAndMutatePlanUpdate(
			Lists.newArrayList(mysql),
			Lists.newArrayList(pg)));
	}

}
