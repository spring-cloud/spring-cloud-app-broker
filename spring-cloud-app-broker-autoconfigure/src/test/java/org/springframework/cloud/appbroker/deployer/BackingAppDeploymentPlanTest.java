package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.DeployerApplication.DeployerApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BackingAppDeploymentPlanTest {

	private BackingAppDeploymentPlan backingAppDeploymentPlan;
	@Mock
	private DeployerClient deployerClient;

	@Test
	void shouldExecuteTheDeploymentPlan() {
		//Given that a deployment plan with properties and a client
		BackingAppDeployProperties deployProperties = new BackingAppDeployProperties();
		backingAppDeploymentPlan = new BackingAppDeploymentPlan(deployProperties, deployerClient);

		//When
		when(deployerClient.deploy(any())).thenReturn(Mono.just("running"));
		final String testAppStatus = backingAppDeploymentPlan.execute(DeployerApplicationBuilder.builder().withAppName("testapp1").build());
		//then
		String expectedStatus = "running";
		assertThat(testAppStatus).isEqualTo(expectedStatus);
	}
}