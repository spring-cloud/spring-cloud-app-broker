package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackingAppDeploymentServiceTest {

	private static final String STATUS = "running";

	@Mock
	private DeployerClient deployerClient;

	@Test
	void shouldExecuteTheDeploymentPlan() {
		//Given that a deployment plan with properties and a client
		BackingAppDeploymentService backingAppDeploymentService = new BackingAppDeploymentService(deployerClient);

		//When
		when(deployerClient.deploy(any())).thenReturn(Mono.just(STATUS));
		BackingAppProperties backingApplication = new BackingAppProperties("testApp1", "http://myfiles/app.jar");
		String testAppStatus = backingAppDeploymentService.execute(backingApplication);

		//then
		assertThat(testAppStatus).isEqualTo(STATUS);
	}
}