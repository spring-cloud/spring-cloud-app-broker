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

	private CreateInstanceProperties createInstanceProperties;
	@Mock
	private DeployerClient deployerClient;
	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;

	@Test
	void shouldProvisionDefaultServiceInstance() {
		// given that properties contains app name
		createInstanceProperties = new CreateInstanceProperties();
		createInstanceProperties.setAppName("helloworldapp");
		provisionServiceInstanceWorkflow = new ProvisionServiceInstanceWorkflow(createInstanceProperties, deployerClient);

		// when
		provisionServiceInstanceWorkflow.provision(null);

		//then deployer should be called with the application name
		verify(deployerClient, times(1))
			.deploy(DeployerApplicationBuilder.builder().withAppName("helloworldapp").build());
	}
}