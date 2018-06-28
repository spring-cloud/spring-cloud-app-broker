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

import java.util.Collections;

import reactor.core.publisher.Mono;

import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class DeployerClient implements ResourceLoaderAware {

	private ReactiveAppDeployer appDeployer;
	private ResourceLoader resourceLoader;

	public DeployerClient(ReactiveAppDeployer appDeployer) {
		this.appDeployer = appDeployer;
	}

	DeployerClient(ReactiveAppDeployer appDeployer, ResourceLoader resourceLoader) {
		this.appDeployer = appDeployer;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	Mono<String> deploy(BackingAppProperties backingApplication) {
		AppDefinition appDefinition = new AppDefinition(backingApplication.getAppName(), Collections.emptyMap());
		Resource resource = getResource(backingApplication.getPath());
		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource);
		Mono<String> deployedApplicationId = appDeployer.deploy(appDeploymentRequest);
		deployedApplicationId.block();
		return Mono.just("running");
	}

	private Resource getResource(String path) {
		return resourceLoader.getResource(path);
	}
}
