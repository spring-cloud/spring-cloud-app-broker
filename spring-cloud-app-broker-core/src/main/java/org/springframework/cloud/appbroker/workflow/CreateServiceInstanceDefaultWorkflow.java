package org.springframework.cloud.appbroker.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.CreateServiceRequestContext;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.DefaultCreateServiceBrokerResponseBuilder;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy.BackingAppDeployer;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy.BackingAppParameters;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

public class CreateServiceInstanceDefaultWorkflow implements CreateServiceInstanceWorkflow<CreateServiceInstanceRequest, CreateServiceInstanceResponse> {

	private BackingAppDeployer backingAppDeployer;
	private BackingAppParameters backingAppParameters;
	private DefaultCreateServiceBrokerResponseBuilder createServiceBrokerResponseBuilder;

	@Autowired
	public CreateServiceInstanceDefaultWorkflow(BackingAppDeployer backingAppDeployer,
												BackingAppParameters backingAppParameters,
												DefaultCreateServiceBrokerResponseBuilder createServiceBrokerResponseBuilder) {

		this.backingAppDeployer = backingAppDeployer;
		this.backingAppParameters = backingAppParameters;
		this.createServiceBrokerResponseBuilder = createServiceBrokerResponseBuilder;
	}

	@Override
	public CreateServiceInstanceResponse perform(CreateServiceInstanceRequest requestData) {
		CreateServiceRequestContext requestContext = new CreateServiceRequestContext(requestData);
		backingAppDeployer.accept(backingAppParameters, requestContext);
		return createServiceBrokerResponseBuilder.apply(requestContext);
	}

}
