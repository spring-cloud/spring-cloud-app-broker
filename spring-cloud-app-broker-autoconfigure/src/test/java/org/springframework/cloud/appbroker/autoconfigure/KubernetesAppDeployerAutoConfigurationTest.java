/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.autoconfigure;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.kubernetes.KubernetesTargetProperties;

import static org.assertj.core.api.Assertions.assertThat;

class KubernetesAppDeployerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(KubernetesAppDeployerAutoConfiguration.class));

	@Test
	void clientIsCreatedWithMasterUrl() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.appbroker.deployer.kubernetes.master-url=https://master.example.com"
				)
				.run((context) -> {
					assertThat(context).hasSingleBean(KubernetesTargetProperties.class);
					KubernetesTargetProperties targetProperties = context.getBean(KubernetesTargetProperties.class);
					assertThat(targetProperties.getMasterUrl()).isEqualTo("https://master.example.com");

					assertThat(context).hasSingleBean(AppDeployer.class);

					assertThat(context).hasSingleBean(KubernetesClient.class);
				});
	}

}
