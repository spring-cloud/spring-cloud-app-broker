package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;

public interface DeleteServiceInstanceWorkflow<REQ extends DeleteServiceInstanceRequest, RES extends DeleteServiceInstanceResponse>
	extends AppBrokerWorkflow<REQ, RES> {
}
