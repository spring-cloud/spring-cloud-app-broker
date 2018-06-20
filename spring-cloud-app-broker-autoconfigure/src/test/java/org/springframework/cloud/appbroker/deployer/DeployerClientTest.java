package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class DeployerClientTest {

	private DeployerClient deployerClient;

	@BeforeEach
	void setUp() {
		deployerClient = new DeployerClient();
	}

	@Test
	void shouldDeployAppByName() {
		DeployerApplication deployerApplication = DeployerApplication.DeployerApplicationBuilder
			.builder().withAppName("helloworld").build();

		//when I call deploy an app with a given name
		Mono<String> lastState = deployerClient.deploy(deployerApplication);

		//then
		assertThat(lastState.block()).isEqualTo("running");
	}
}