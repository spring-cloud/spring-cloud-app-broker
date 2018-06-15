package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;

public class GetServiceInstanceDefaultWorkflow implements GetServiceInstanceWorkflow<GetServiceInstanceRequest, GetServiceInstanceResponse> {

	@Override
	public GetServiceInstanceResponse perform(GetServiceInstanceRequest requestData) {
		return null;
	}
}
