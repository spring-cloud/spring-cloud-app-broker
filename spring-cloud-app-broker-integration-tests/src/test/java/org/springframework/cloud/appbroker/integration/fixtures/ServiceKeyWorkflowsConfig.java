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

package org.springframework.cloud.appbroker.integration.fixtures;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.workflow.ServiceKeyCreateServiceBindingWorkflow;
import org.springframework.cloud.appbroker.workflow.ServiceKeyDeleteServiceBindingWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceKeyWorkflowsConfig {

	@Bean
	@ConditionalOnProperty(name="service-bindings-as-service-keys")
	public ServiceKeyCreateServiceBindingWorkflow serviceKeyCreateServiceBindingWorkflow(BrokeredServices brokeredServices,
		BackingServicesProvisionService backingServicesProvisionService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		TargetService targetService) {
		return new ServiceKeyCreateServiceBindingWorkflow(brokeredServices, backingServicesProvisionService,
			servicesParametersTransformationService, targetService);
	}

	@Bean
	@ConditionalOnProperty(name="service-bindings-as-service-keys")
	public ServiceKeyDeleteServiceBindingWorkflow serviceKeyDeleteServiceBindingWorkflow(BrokeredServices brokeredServices,
		BackingServicesProvisionService backingServicesProvisionService,
		TargetService targetService) {
		return new ServiceKeyDeleteServiceBindingWorkflow(brokeredServices, backingServicesProvisionService, targetService);
	}
}
