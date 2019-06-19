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

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.kubernetes.KubernetesAppDeployer;
import org.springframework.cloud.appbroker.deployer.kubernetes.KubernetesAppManager;
import org.springframework.cloud.appbroker.deployer.kubernetes.KubernetesOAuth2Client;
import org.springframework.cloud.appbroker.deployer.kubernetes.KubernetesTargetProperties;
import org.springframework.cloud.appbroker.manager.AppManager;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@ConditionalOnProperty(KubernetesAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".master-url")
@EnableConfigurationProperties
public class KubernetesAppDeployerAutoConfiguration {

	static final String PROPERTY_PREFIX = "spring.cloud.appbroker.deployer.kubernetes";

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX)
	KubernetesTargetProperties kubernetesTargetProperties() {
		return new KubernetesTargetProperties();
	}

	@Bean
	AppDeployer kubernetesAppDeployer(KubernetesClient kubernetesClient,
									  KubernetesTargetProperties targetProperties,
									  ResourceLoader resourceLoader) {
		return new KubernetesAppDeployer(kubernetesClient, targetProperties, resourceLoader);
	}

	@Bean
	AppManager kubernetesAppManager() {
		return new KubernetesAppManager();
	}

	@Bean
	OAuth2Client kubernetesOAuth2Client() {
		return new KubernetesOAuth2Client();
	}

	@Bean
	KubernetesClient kubernetesClient(KubernetesTargetProperties targetProperties) {
		Config config =
				new ConfigBuilder()
						.withMasterUrl(targetProperties.getMasterUrl())
						.build();
		return new DefaultKubernetesClient(config);
	}

}
