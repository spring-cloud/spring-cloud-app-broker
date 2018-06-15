package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;

public class DeleteServiceInstanceDefaultWorkflow implements DeleteServiceInstanceWorkflow<DeleteServiceInstanceRequest, DeleteServiceInstanceResponse> {

	@Override
	public DeleteServiceInstanceResponse perform(DeleteServiceInstanceRequest requestData) {
		return null;
	}
}
