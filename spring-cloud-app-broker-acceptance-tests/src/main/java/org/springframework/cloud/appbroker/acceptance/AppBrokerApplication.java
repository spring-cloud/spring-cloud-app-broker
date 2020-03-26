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

package org.springframework.cloud.appbroker.acceptance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.appbroker.acceptance.services.NoOpCreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.acceptance.services.NoOpCreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.acceptance.services.NoOpDeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.acceptance.services.NoOpUpdateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.ServiceKeyCreateServiceBindingWorkflow;
import org.springframework.cloud.appbroker.workflow.ServiceKeyDeleteServiceBindingWorkflow;
import org.springframework.context.annotation.Bean;

/**
 * A Spring Boot application for running acceptance tests
 */
@SpringBootApplication
public class AppBrokerApplication {

	/**
	 * main application entry point
	 *
	 * @param args the args
	 */
	public static void main(String[] args) {
		SpringApplication.run(AppBrokerApplication.class, args);
	}

	/**
	 * A no-op CreateServiceInstanceWorkflow bean
	 *
	 * @return the bean
	 */
	@Bean
	public CreateServiceInstanceWorkflow createServiceInstanceWorkflow() {
		return new NoOpCreateServiceInstanceWorkflow();
	}

	/**
	 * A no-op UpdateServiceInstanceWorkflow bean
	 *
	 * @return the bean
	 */
	@Bean
	public UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow() {
		return new NoOpUpdateServiceInstanceWorkflow();
	}

	/**
	 * A no-op DeleteServiceInstanceWorkflow bean
	 *
	 * @return the bean
	 */
	@Bean
	public DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow() {
		return new NoOpDeleteServiceInstanceWorkflow();
	}

//	/**
//	 * A no-op ServiceInstanceBindingService bean
//	 *
//	 * @return the bean
//	 */
//	@Bean
//	public ServiceInstanceBindingService serviceInstanceBindingService() {
//		return new NoOpServiceInstanceBindingService();
//	}
//

	/**
	 * A no-op CreateServiceInstanceAppBindingWorkflow which returns static credentials
	 *
	 * @return the bean
	 */
	@Bean
	public CreateServiceInstanceAppBindingWorkflow createServiceInstanceAppBindingWorkflow() {
		return new NoOpCreateServiceInstanceAppBindingWorkflow();
	}

	@Bean
	@ConditionalOnProperty(name="service-bindings-as-service-keys")
	public ServiceKeyCreateServiceBindingWorkflow serviceKeyCreateServiceBindingWorkflow(
		BrokeredServices brokeredServices,
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
