package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackingAppDeploymentServiceTest {

	private static final String STATUS_RUNNING = "running";
	private static final String STATUS_DELETED = "deleted";

	@Mock
	private DeployerClient deployerClient;

	private BackingAppDeploymentService backingAppDeploymentService;

	@BeforeEach
	void setUp() {
		backingAppDeploymentService = new BackingAppDeploymentService(deployerClient);
	}

	@Test
	void shouldDeployApplication() {
		// given
		BackingAppProperties backingApplication = new BackingAppProperties("testApp1", "http://myfiles/app.jar");
		when(deployerClient.deploy(any())).thenReturn(Mono.just(STATUS_RUNNING));

		// when
		String testAppStatus = backingAppDeploymentService.deploy(backingApplication);

		// then
		assertThat(testAppStatus).isEqualTo(STATUS_RUNNING);

		verify(deployerClient).deploy(eq(backingApplication));
	}

	@Test
	void shouldUndeployApplication() {
		// given
		BackingAppProperties backingApplication = new BackingAppProperties("testApp1", "http://myfiles/app.jar");
		when(deployerClient.undeploy(any())).thenReturn(Mono.just(STATUS_DELETED));

		// when
		String testAppStatus = backingAppDeploymentService.undeploy(backingApplication);

		// then
		assertThat(testAppStatus).isEqualTo(STATUS_DELETED);

		verify(deployerClient).undeploy(eq(backingApplication));
	}
}