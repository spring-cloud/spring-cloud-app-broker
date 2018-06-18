package org.springframework.cloud.appbroker.workflow.action.createserviceinstance;

import org.springframework.cloud.appbroker.workflow.BrokerRequestContext;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy.BackingAppState;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

public class CreateServiceRequestContext implements BrokerRequestContext {

	private BackingAppState backingAppState;
	private boolean instanceExisted;
	private CreateServiceInstanceRequest originatingRequest;

	public CreateServiceRequestContext(CreateServiceInstanceRequest requestData) {
		this.originatingRequest = requestData;
	}

	public BackingAppState getBackingAppState() {
		return backingAppState;
	}

	public void setBackingAppState(BackingAppState backingAppState) {
		this.backingAppState = backingAppState;
	}

	public boolean getInstanceExisted() {
		return instanceExisted;
	}

	public void setInstanceExisted(boolean instanceExisted) {
		this.instanceExisted = instanceExisted;
	}

	public CreateServiceInstanceRequest getOriginatingRequest() {
		return originatingRequest;
	}

	public void setOriginatingRequest(CreateServiceInstanceRequest originatingRequest) {
		this.originatingRequest = originatingRequest;
	}
}
