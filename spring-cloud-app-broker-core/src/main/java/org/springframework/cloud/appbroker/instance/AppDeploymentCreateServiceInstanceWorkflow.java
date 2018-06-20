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

import java.util.Set;

import org.springframework.cloud.appbroker.instance.app.BackingAppDeploymentPlan;
import org.springframework.cloud.appbroker.instance.create.CreateServiceRequestContext;
import org.springframework.cloud.appbroker.instance.create.DefaultCreateServiceBrokerResponseBuilder;
import org.springframework.cloud.appbroker.workflow.CreateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

public class AppDeploymentCreateServiceInstanceWorkflow implements CreateServiceInstanceWorkflow {

	private Set<BackingAppDeploymentPlan> deploymentPlans;
	private DefaultCreateServiceBrokerResponseBuilder createServiceBrokerResponseBuilder;
	private CreateServiceRequestContext requestContext;

	public AppDeploymentCreateServiceInstanceWorkflow(Set<BackingAppDeploymentPlan> deploymentPlans,
													  DefaultCreateServiceBrokerResponseBuilder createServiceBrokerResponseBuilder) {
		this.deploymentPlans = deploymentPlans;
		this.createServiceBrokerResponseBuilder = createServiceBrokerResponseBuilder;
		this.requestContext = requestContext;
	}

	@Override
	public CreateServiceInstanceResponse perform(CreateServiceInstanceRequest requestData) {
		deploymentPlans.forEach(plan -> plan.getBackingAppDeployer().deploy(plan.getBackingAppParameters(), requestContext));
		return createServiceBrokerResponseBuilder.apply(requestContext);
	}

}
