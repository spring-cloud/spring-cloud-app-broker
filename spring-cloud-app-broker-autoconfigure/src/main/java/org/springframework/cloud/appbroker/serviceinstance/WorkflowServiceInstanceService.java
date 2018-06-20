package org.springframework.cloud.appbroker.serviceinstance;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;


/**
 * TODO This should be in the App Broker core subproject
 * A {@code ServiceInstanceService} that delegates to a set of discrete Workflow objects for each service broker
 * operation.
 */
@Service
public class WorkflowServiceInstanceService implements ServiceInstanceService {

	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;

	public WorkflowServiceInstanceService(ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow) {
		this.provisionServiceInstanceWorkflow = provisionServiceInstanceWorkflow;
	}

	@Override
	public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
		provisionServiceInstanceWorkflow.provision(request);

		return CreateServiceInstanceResponse.builder().build();
	}

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return null;
	}

	@Override
	public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
		return null;
	}

	@Override
	public GetServiceInstanceResponse getServiceInstance(GetServiceInstanceRequest request) {
		return null;
	}

	@Override
	public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
		return null;
	}
}
