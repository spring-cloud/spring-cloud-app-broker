package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;

public class GetLastOperationDefaultWorkflow implements GetLastOperationWorkflow<GetLastServiceOperationRequest, GetLastServiceOperationResponse> {

	@Override
	public GetLastServiceOperationResponse perform(GetLastServiceOperationRequest requestData) {
		return null;
	}
}
