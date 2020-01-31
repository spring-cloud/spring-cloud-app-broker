package com.example.appbroker;

import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.data.annotation.Id;

class ServiceInstanceBinding {

	@Id
	private Long id;

	private String bindingId;

	private String serviceInstanceId;

	private String description;

	private OperationState operationState;

	public ServiceInstanceBinding() {

	}

	public ServiceInstanceBinding(String bindingId, String serviceInstanceId, String description,
			OperationState operationState) {
		this.bindingId = bindingId;
		this.serviceInstanceId = serviceInstanceId;
		this.description = description;
		this.operationState = operationState;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBindingId() {
		return bindingId;
	}

	public void setBindingId(String bindingId) {
		this.bindingId = bindingId;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public OperationState getOperationState() {
		return operationState;
	}

	public void setOperationState(OperationState operationState) {
		this.operationState = operationState;
	}

}
