/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.autoconfigure;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFoundryClientAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CloudFoundryClientAutoConfiguration.class));

	@Test
	@Disabled
	void clientIsCreatedWithPasswordGrantConfiguration() {
		this.contextRunner
			.withPropertyValues(
				"spring.cloud.appbroker.cf.apiHost=https://api.example.com",
				"spring.cloud.appbroker.cf.username=user",
				"spring.cloud.appbroker.cf.password=secret"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(CloudFoundryProperties.class);
				assertThat(context).hasSingleBean(ReactorCloudFoundryClient.class);
				assertThat(context).hasSingleBean(ReactorDopplerClient.class);
				assertThat(context).hasSingleBean(ReactorUaaClient.class);
				assertThat(context).hasSingleBean(CloudFoundryOperations.class);
				assertThat(context).hasSingleBean(DefaultConnectionContext.class);
				assertThat(context).hasSingleBean(PasswordGrantTokenProvider.class);
			});
	}

	@Test
	void clientIsNotCreatedWithoutConfiguration() {
		this.contextRunner
			.run((context) -> {
				assertThat(context).doesNotHaveBean(CloudFoundryProperties.class);
				assertThat(context).doesNotHaveBean(ReactorCloudFoundryClient.class);
				assertThat(context).doesNotHaveBean(ReactorDopplerClient.class);
				assertThat(context).doesNotHaveBean(ReactorUaaClient.class);
				assertThat(context).doesNotHaveBean(CloudFoundryOperations.class);
				assertThat(context).doesNotHaveBean(ConnectionContext.class);
				assertThat(context).doesNotHaveBean(TokenProvider.class);
			});
	}

}