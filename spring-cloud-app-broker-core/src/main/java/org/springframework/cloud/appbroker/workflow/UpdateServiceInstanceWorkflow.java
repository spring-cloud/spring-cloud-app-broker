package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

public interface UpdateServiceInstanceWorkflow<REQ extends UpdateServiceInstanceRequest, RES extends UpdateServiceInstanceResponse>
	extends AppBrokerWorkflow<REQ, RES> {
}
