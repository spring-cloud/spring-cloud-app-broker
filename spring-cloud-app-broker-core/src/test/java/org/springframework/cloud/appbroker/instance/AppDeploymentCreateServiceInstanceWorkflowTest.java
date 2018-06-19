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

package org.springframework.cloud.appbroker.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.instance.app.BackingAppDeployer;
import org.springframework.cloud.appbroker.instance.app.BackingAppDeploymentPlan;
import org.springframework.cloud.appbroker.instance.create.CreateServiceRequestContext;
import org.springframework.cloud.appbroker.instance.create.DefaultCreateServiceBrokerResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppDeploymentCreateServiceInstanceWorkflowTest {
	@Mock
	private BackingAppDeployer deployer;

	@Test
	public void deployOneApp() {
		// this is exposing a Law of Demeter issue in the design: this Workflow should only be dealing with
		// objects it is injected with (BackingAppDeploymentPlans), not objects that are a level of indirection
		// removed (BackingAppDeployer)
		when(deployer.deploy(isNull(), any(CreateServiceRequestContext.class)))
			.thenReturn(Mono.just("test"));

		Set<BackingAppDeploymentPlan> plans = Collections.singleton(new BackingAppDeploymentPlan(deployer, null));
		DefaultCreateServiceBrokerResponseBuilder responseBuilder = new DefaultCreateServiceBrokerResponseBuilder();
		AppDeploymentCreateServiceInstanceWorkflow workflow =
			new AppDeploymentCreateServiceInstanceWorkflow(plans, responseBuilder);

		CreateServiceInstanceResponse response = workflow.perform(CreateServiceInstanceRequest.builder().build());
		assertThat(response).isNotNull();
	}

}