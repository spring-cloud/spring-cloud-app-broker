package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

public interface CreateServiceInstanceWorkflow<REQ extends CreateServiceInstanceRequest, RES extends CreateServiceInstanceResponse>
	extends AppBrokerWorkflow<REQ, RES> {
}
