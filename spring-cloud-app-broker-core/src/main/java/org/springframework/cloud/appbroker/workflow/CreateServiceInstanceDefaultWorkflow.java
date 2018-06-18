package org.springframework.cloud.appbroker.workflow;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.CreateServiceRequestContext;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.DefaultCreateServiceBrokerResponseBuilder;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy.BackingAppDeployer;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy.BackingAppDeploymentPlan;
import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy.BackingAppParameters;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

public class CreateServiceInstanceDefaultWorkflow implements CreateServiceInstanceWorkflow<CreateServiceInstanceRequest, CreateServiceInstanceResponse> {

	private Set<BackingAppDeploymentPlan> deploymentPlans;
	private DefaultCreateServiceBrokerResponseBuilder createServiceBrokerResponseBuilder;

	@Autowired
	public CreateServiceInstanceDefaultWorkflow(Set<BackingAppDeploymentPlan> deploymentPlans,
												DefaultCreateServiceBrokerResponseBuilder createServiceBrokerResponseBuilder) {
		this.deploymentPlans = deploymentPlans;
		this.createServiceBrokerResponseBuilder = createServiceBrokerResponseBuilder;
	}

	@Override
	public CreateServiceInstanceResponse perform(CreateServiceInstanceRequest requestData) {
		CreateServiceRequestContext requestContext = new CreateServiceRequestContext(requestData);
		deploymentPlans.forEach(plan -> plan.getBackingAppDeployer().accept(plan.getBackingAppParameters(), requestContext));
		return createServiceBrokerResponseBuilder.apply(requestContext);
	}

}
