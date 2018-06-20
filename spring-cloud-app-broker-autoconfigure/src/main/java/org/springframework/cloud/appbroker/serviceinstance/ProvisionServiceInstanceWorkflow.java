package org.springframework.cloud.appbroker.serviceinstance;

import org.springframework.cloud.appbroker.deployer.DeployerApplication;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.stereotype.Service;

//TODO This should be in the App Broker core subproject
@Service
class ProvisionServiceInstanceWorkflow {

	private CreateInstanceProperties createInstanceProperties;

	// TODO this breaks the bounded context very badly. `App deployer client interaction` is a bounded context and
	// 		`service instance provisioning workflow` is another. There should be indirection when communicating
	//		between them
	private DeployerClient deployerClient;

	public ProvisionServiceInstanceWorkflow(CreateInstanceProperties createInstanceProperties,
											DeployerClient deployerClient) {
		this.createInstanceProperties = createInstanceProperties;
		this.deployerClient = deployerClient;
	}

	void provision(Object any) {
		final String appName = createInstanceProperties.getAppName();
		deployerClient.deploy(createDeployerApplication(appName));
	}

	private DeployerApplication createDeployerApplication(String appName) {
		return DeployerApplication.DeployerApplicationBuilder.builder().withAppName(appName).build();
	}
}
