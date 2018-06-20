package org.springframework.cloud.appbroker.serviceinstance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.appbroker.deployer.DeployerApplication.DeployerApplicationBuilder;
import org.springframework.cloud.appbroker.deployer.DeployerClient;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProvisionServiceInstanceWorkflowTest {

	private AppBrokerCreateInstanceProperties appBrokerCreateInstanceProperties;
	@Mock
	private DeployerClient deployerClient;
	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;

	@Test
	void shouldProvisionDefaultServiceInstance() {
		// given that properties contains app name
		appBrokerCreateInstanceProperties = new AppBrokerCreateInstanceProperties();
		appBrokerCreateInstanceProperties.setAppName("helloworldapp");
		provisionServiceInstanceWorkflow = new ProvisionServiceInstanceWorkflow(appBrokerCreateInstanceProperties, deployerClient);

		// when
		provisionServiceInstanceWorkflow.provision(null);

		//then deployer should be called with the application name
		verify(deployerClient, times(1))
			.deploy(DeployerApplicationBuilder.builder().withAppName("helloworldapp").build());

	}
}