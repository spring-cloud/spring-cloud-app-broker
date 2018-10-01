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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@AutoConfigureAfter(CloudFoundryClientAutoConfiguration.class)
public class AppDeployerAutoConfiguration {

	@Bean
	@ConditionalOnBean(CloudFoundryOperations.class)
	public AppDeployer cloudFoundryAppDeployer(CloudFoundryOperations cloudFoundryOperations,
											   ResourceLoader resourceLoader) {
		CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties = new CloudFoundryDeploymentProperties();
		return new CloudFoundryAppDeployer(cloudFoundryDeploymentProperties,
			cloudFoundryOperations, resourceLoader);
	}
}
