package org.springframework.cloud.appbroker.workflow.createserviceinstance.action;

import java.util.function.Function;

import org.springframework.cloud.appbroker.workflow.createserviceinstance.CreateServiceRequestContext;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

public interface DefaultCreateServiceBrokerResponseBuilder extends Function<CreateServiceRequestContext, CreateServiceInstanceResponse> {

	default CreateServiceInstanceResponse apply(CreateServiceRequestContext createServiceRequestContext) {
		return CreateServiceInstanceResponse.builder()
											.instanceExisted(createServiceRequestContext.getInstanceExisted())
											.build();
	}

}
