package org.springframework.cloud.appbroker.serviceinstance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.appbroker.deployer.BackingAppDeployProperties;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentPlan;
import org.springframework.cloud.appbroker.deployer.DeployerApplication.DeployerApplicationBuilder;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProvisionServiceInstanceWorkflowTest {

	private BackingAppDeployProperties backingAppDeployProperties;
	@Mock
	private BackingAppDeploymentPlan backingAppDeploymentPlan;
	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;

	@Test
	void shouldProvisionDefaultServiceInstance() {
		// given that properties contains app name
		backingAppDeployProperties = new BackingAppDeployProperties();
		backingAppDeployProperties.setAppName("helloworldapp");
		provisionServiceInstanceWorkflow = new ProvisionServiceInstanceWorkflow(backingAppDeployProperties, backingAppDeploymentPlan);

		// when
		provisionServiceInstanceWorkflow.provision();

		//then deployer should be called with the application name
		verify(backingAppDeploymentPlan, times(1))
			.execute(DeployerApplicationBuilder.builder().withAppName("helloworldapp").build());
	}
}