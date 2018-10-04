package org.springframework.cloud.appbroker.deployer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackingApplicationTest {

	@Test
	void stringRepresentationShouldNotExposeSensitiveInformationFromTheEnvironment() {
		BackingApplication backingApp = BackingApplication
			.builder()
			.name("Test")
			.environment("privateKey", "secret-private-key")
			.environment("databasePassword", "password")
			.build();

		String backingAppAsString = backingApp.toString();

		assertThat(backingAppAsString).doesNotContain("secret-private-key");
		assertThat(backingAppAsString).doesNotContain("password");

		assertThat(backingAppAsString).contains("privateKey=<value hidden>");
		assertThat(backingAppAsString).contains("databasePassword=<value hidden>");

		assertThat(backingApp.getEnvironment().get("privateKey")).isEqualTo("secret-private-key");
		assertThat(backingApp.getEnvironment().get("databasePassword")).isEqualTo("password");

		backingApp.setEnvironment(null);
		assertThat(backingApp.toString()).isNotEmpty();
	}
}