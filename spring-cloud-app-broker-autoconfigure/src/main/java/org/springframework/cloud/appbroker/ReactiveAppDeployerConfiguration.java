/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryReactiveAppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.NoOpAppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.AppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudFoundryProperties.class)
public class ReactiveAppDeployerConfiguration {

	@Bean
	public AppNameGenerator noOpApplicationNameGenerator() {
		return new NoOpAppNameGenerator();
	}

	@Bean
	public CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties() {
		CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties = new CloudFoundryDeploymentProperties();
		cloudFoundryDeploymentProperties.setDisk("1024M");
		return cloudFoundryDeploymentProperties;
	}

	@Bean
	public ReactiveAppDeployer cloudFoundryReactiveAppDeployer(AppNameGenerator noOpApplicationNameGenerator,
															   CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties,
															   CloudFoundryOperations defaultCloudFoundryOperations) {
		return new CloudFoundryReactiveAppDeployer(noOpApplicationNameGenerator, cloudFoundryDeploymentProperties, defaultCloudFoundryOperations, null);
	}
}
