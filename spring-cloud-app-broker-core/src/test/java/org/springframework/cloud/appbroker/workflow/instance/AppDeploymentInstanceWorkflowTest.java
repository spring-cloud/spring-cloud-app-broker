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

package org.springframework.cloud.appbroker.workflow.instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AppDeploymentInstanceWorkflowTest {

	private BackingApplications backingApps;
	private AppDeploymentInstanceWorkflow workflow;

	@BeforeEach
	void setUp() {
		backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("app1")
				.path("https://myfiles/app.jar")
				.build())
			.build();

		BrokeredServices brokeredServices = BrokeredServices.builder()
			.service(BrokeredService.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(backingApps)
				.build())
			.build();

		workflow = new AppDeploymentInstanceWorkflow(brokeredServices);
	}

	@Test
	void acceptWithMatchingService() {
		ServiceDefinition serviceDefinition = buildServiceDefinition("service1", "plan1");
		StepVerifier
			.create(workflow.accept(serviceDefinition, serviceDefinition.getPlans().get(0)))
			.expectNextMatches(value -> value)
			.verifyComplete();
	}

	@Test
	void doNotAcceptWithUnsupportedService() {
		ServiceDefinition serviceDefinition = buildServiceDefinition("unknown-service", "plan1");
		StepVerifier
			.create(workflow.accept(serviceDefinition, serviceDefinition.getPlans().get(0)))
			.expectNextMatches(value -> !value)
			.verifyComplete();
	}

	@Test
	void doNotAcceptWithUnsupportedPlan() {
		ServiceDefinition serviceDefinition = buildServiceDefinition("service1", "unknown-plan");
		StepVerifier
			.create(workflow.accept(serviceDefinition, serviceDefinition.getPlans().get(0)))
			.expectNextMatches(value -> !value)
			.verifyComplete();
	}

	@Test
	void getBackingAppForServiceSucceeds() {
		ServiceDefinition serviceDefinition = buildServiceDefinition("service1", "plan1");
		StepVerifier
			.create(workflow
				.getBackingApplicationsForService(serviceDefinition, serviceDefinition.getPlans().get(0)))
			.assertNext(actual -> assertThat(actual)
				.isEqualTo(backingApps)
				.isNotSameAs(backingApps))
			.verifyComplete();
	}

	@Test
	void getBackingAppForServiceWithUnknownServiceIdDoesNothing() {
		ServiceDefinition serviceDefinition = buildServiceDefinition("unknown-service", "plan1");
		StepVerifier
			.create(workflow
				.getBackingApplicationsForService(serviceDefinition,serviceDefinition.getPlans().get(0)))
		.verifyComplete();
	}

	@Test
	void getBackingAppForServiceWithUnknownPlanIdDoesNothing() {
		ServiceDefinition serviceDefinition = buildServiceDefinition("service1", "unknown-plan");
		StepVerifier
			.create(workflow
				.getBackingApplicationsForService(serviceDefinition, serviceDefinition.getPlans().get(0)))
		.verifyComplete();
	}

	private ServiceDefinition buildServiceDefinition(String serviceName, String planName) {
		return ServiceDefinition.builder()
			.id(serviceName + "-id")
			.name(serviceName)
			.plans(Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.build())
			.build();
	}
}