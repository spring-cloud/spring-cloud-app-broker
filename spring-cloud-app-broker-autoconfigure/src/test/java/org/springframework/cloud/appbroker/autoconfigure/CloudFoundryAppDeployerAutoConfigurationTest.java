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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFoundryAppDeployerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CloudFoundryAppDeployerAutoConfiguration.class));

	@Test
	void clientIsCreatedWithPasswordGrantConfiguration() {
		this.contextRunner
			.withPropertyValues(
				"spring.cloud.appbroker.deployer.cloudfoundry.api-host=api.example.com",
				"spring.cloud.appbroker.deployer.cloudfoundry.api-port=443",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-org=example-org",
				"spring.cloud.appbroker.deployer.cloudfoundry.default-space=example-space",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret",
				"spring.cloud.appbroker.deployer.cloudfoundry.properties.memory=2G",
				"spring.cloud.appbroker.deployer.cloudfoundry.properties.count=3",
				"spring.cloud.appbroker.deployer.cloudfoundry.properties.buildpack=example-buildpack",
				"spring.cloud.appbroker.deployer.cloudfoundry.properties.domain=example.com"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(CloudFoundryTargetProperties.class);
				CloudFoundryTargetProperties targetProperties = context.getBean(CloudFoundryTargetProperties.class);
				assertThat(targetProperties.getApiHost()).isEqualTo("api.example.com");
				assertThat(targetProperties.getApiPort()).isEqualTo(443);
				assertThat(targetProperties.getDefaultOrg()).isEqualTo("example-org");
				assertThat(targetProperties.getDefaultSpace()).isEqualTo("example-space");

				assertThat(context).hasSingleBean(CloudFoundryDeploymentProperties.class);
				CloudFoundryDeploymentProperties deploymentProperties = context.getBean(CloudFoundryDeploymentProperties.class);
				assertThat(deploymentProperties.getMemory()).isEqualTo("2G");
				assertThat(deploymentProperties.getCount()).isEqualTo(3);
				assertThat(deploymentProperties.getBuildpack()).isEqualTo("example-buildpack");
				assertThat(deploymentProperties.getDomain()).isEqualTo("example.com");

				assertThat(context).hasSingleBean(AppDeployer.class);

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
				assertThat(context).doesNotHaveBean(CloudFoundryTargetProperties.class);
				assertThat(context).doesNotHaveBean(CloudFoundryDeploymentProperties.class);
				assertThat(context).doesNotHaveBean(ReactorCloudFoundryClient.class);
				assertThat(context).doesNotHaveBean(ReactorDopplerClient.class);
				assertThat(context).doesNotHaveBean(ReactorUaaClient.class);
				assertThat(context).doesNotHaveBean(CloudFoundryOperations.class);
				assertThat(context).doesNotHaveBean(ConnectionContext.class);
				assertThat(context).doesNotHaveBean(TokenProvider.class);
			});
	}

}