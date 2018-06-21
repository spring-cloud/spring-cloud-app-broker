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

package org.springframework.cloud.appbroker.deployer;

import java.net.MalformedURLException;
import java.util.Collections;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//TODO This should be in the App Broker core subproject

@Component
class DeployerClient {

	private ReactiveAppDeployer reactiveAppDeployer;

	DeployerClient(ReactiveAppDeployer reactiveAppDeployer) {
		this.reactiveAppDeployer = reactiveAppDeployer;
	}

	Mono<String> deploy(DeployerApplication deployerApplication) {

		AppDefinition appDefinition = new AppDefinition(deployerApplication.getAppName(), Collections.emptyMap());
		Resource resource = getResource(deployerApplication.getPath());
		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource);
		Mono<String> deployedApplicationId = reactiveAppDeployer.deploy(appDeploymentRequest);
		deployedApplicationId.block();
		return Mono.just("running");
	}

	private Resource getResource(String path) {
		try {
			return new FileUrlResource(path);
		} catch (MalformedURLException e) {
			throw new RuntimeException("The URL given from the resource is not correct");
		}
	}

}
