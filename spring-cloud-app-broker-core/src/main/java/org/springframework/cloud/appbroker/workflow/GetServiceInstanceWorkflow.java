package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;

public interface GetServiceInstanceWorkflow<REQ extends GetServiceInstanceRequest, RES extends GetServiceInstanceResponse>
	extends AppBrokerWorkflow<REQ, RES> {
}
