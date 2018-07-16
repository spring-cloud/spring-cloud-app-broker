package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class BackingAppDeploymentServiceTest {

	private static final String STATUS_RUNNING = "running";
	private static final String STATUS_DELETED = "deleted";

	@Mock
	private DeployerClient deployerClient;

	private BackingAppDeploymentService backingAppDeploymentService;
	private BackingApplications backingApps;

	@BeforeEach
	void setUp() {
		backingAppDeploymentService = new BackingAppDeploymentService(deployerClient);
		backingApps = new BackingApplications(
			new BackingApplication("testApp1", "http://myfiles/app1.jar"),
			new BackingApplication("testApp2", "http://myfiles/app2.jar")
		);
	}

	@Test
	void shouldDeployApplications() {
		doReturn(Mono.just(STATUS_RUNNING))
			.when(deployerClient).deploy(backingApps.get(0));
		doReturn(Mono.just(STATUS_RUNNING))
			.when(deployerClient).deploy(backingApps.get(1));

		String deployStatus = backingAppDeploymentService.deploy(backingApps);

		assertThat(deployStatus).isEqualTo(STATUS_RUNNING + "," + STATUS_RUNNING);
	}

	@Test
	void shouldUndeployApplications() {
		doReturn(Mono.just(STATUS_DELETED))
			.when(deployerClient).undeploy(backingApps.get(0));
		doReturn(Mono.just(STATUS_DELETED))
			.when(deployerClient).undeploy(backingApps.get(1));

		String undeployStatus = backingAppDeploymentService.undeploy(backingApps);

		assertThat(undeployStatus).isEqualTo(STATUS_DELETED + "," + STATUS_DELETED);
	}
}