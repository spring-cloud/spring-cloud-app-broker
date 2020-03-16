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
