package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

public class UpdateServiceInstanceDefaultWorkflow implements UpdateServiceInstanceWorkflow<UpdateServiceInstanceRequest, UpdateServiceInstanceResponse> {

	@Override
	public UpdateServiceInstanceResponse perform(UpdateServiceInstanceRequest requestData) {
		return null;
	}
}
