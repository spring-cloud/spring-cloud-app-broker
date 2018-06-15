package org.springframework.cloud.appbroker.spi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.workflow.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.GetLastOperationWorkflow;
import org.springframework.cloud.appbroker.workflow.GetServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.UpdateServiceInstanceWorkflow;
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
import org.springframework.stereotype.Component;

@Component
public class ServiceInstanceProvisionService implements ServiceInstanceService {

	private CreateServiceInstanceWorkflow<CreateServiceInstanceRequest, CreateServiceInstanceResponse> createServiceInstanceWorkflow;
	private DeleteServiceInstanceWorkflow<DeleteServiceInstanceRequest, DeleteServiceInstanceResponse> deleteServiceInstanceWorkflow;
	private GetLastOperationWorkflow<GetLastServiceOperationRequest, GetLastServiceOperationResponse> getLastOperationWorkflow;
	private GetServiceInstanceWorkflow<GetServiceInstanceRequest, GetServiceInstanceResponse> getServiceInstanceWorkflow;
	private UpdateServiceInstanceWorkflow<UpdateServiceInstanceRequest, UpdateServiceInstanceResponse> updateServiceInstanceWorkflow;


	@Autowired
	public ServiceInstanceProvisionService(CreateServiceInstanceWorkflow<CreateServiceInstanceRequest, CreateServiceInstanceResponse> createServiceInstanceWorkflow,
										   UpdateServiceInstanceWorkflow<UpdateServiceInstanceRequest, UpdateServiceInstanceResponse> updateServiceInstanceWorkflow,
										   GetServiceInstanceWorkflow<GetServiceInstanceRequest, GetServiceInstanceResponse> getServiceInstanceWorkflow,
										   GetLastOperationWorkflow<GetLastServiceOperationRequest, GetLastServiceOperationResponse> getLastOperationWorkflow,
										   DeleteServiceInstanceWorkflow<DeleteServiceInstanceRequest, DeleteServiceInstanceResponse> deleteServiceInstanceWorkflow) {

		this.createServiceInstanceWorkflow = createServiceInstanceWorkflow;
		this.updateServiceInstanceWorkflow = updateServiceInstanceWorkflow;
		this.getServiceInstanceWorkflow = getServiceInstanceWorkflow;
		this.getLastOperationWorkflow = getLastOperationWorkflow;
		this.deleteServiceInstanceWorkflow = deleteServiceInstanceWorkflow;
	}

	@Override
	public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
		return createServiceInstanceWorkflow.perform(request);
	}

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return deleteServiceInstanceWorkflow.perform(request);
	}

	@Override
	public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
		return getLastOperationWorkflow.perform(request);
	}

	@Override
	public GetServiceInstanceResponse getServiceInstance(GetServiceInstanceRequest request) {
		return getServiceInstanceWorkflow.perform(request);
	}

	@Override
	public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
		return updateServiceInstanceWorkflow.perform(request);
	}
}
