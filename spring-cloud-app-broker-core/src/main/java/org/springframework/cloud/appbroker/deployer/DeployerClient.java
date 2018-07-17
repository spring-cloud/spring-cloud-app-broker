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
import java.util.HashMap;
import java.util.Map;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

public class DeployerClient implements ResourceLoaderAware {

	private static final String SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SERVICES_KEY = "spring.cloud.deployer.cloudfoundry.services";

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

	Mono<String> deploy(BackingApplication backingApplication) {
		AppDeploymentRequest appDeploymentRequest = createAppDeploymentRequest(backingApplication);
		Mono<String> deployedApplicationId = appDeployer.deploy(appDeploymentRequest);
		deployedApplicationId.block();
		return Mono.just("running");
	}

	Mono<String> undeploy(BackingApplication backingApplication) {
		Mono<Void> undeploy = appDeployer.undeploy(backingApplication.getName());
		undeploy.block();
		return Mono.just("deleted");
	}

	private AppDeploymentRequest createAppDeploymentRequest(BackingApplication backingApplication) {
		AppDefinition appDefinition =
			new AppDefinition(backingApplication.getName(), backingApplication.getEnvironment());
		
		Resource resource = getResource(backingApplication.getPath());

		Map<String, String> deploymentProperties = createDeploymentProperties(backingApplication);

		return new AppDeploymentRequest(appDefinition, resource, deploymentProperties);
	}

	private Map<String, String> createDeploymentProperties(BackingApplication backingApplication) {
		Map<String, String> deploymentProperties = new HashMap<>();
		if (backingApplication.getProperties() != null) {
			deploymentProperties.putAll(backingApplication.getProperties());
		}

		if (backingApplication.getServices() != null) {
			Map<String, String> services = Collections.singletonMap(SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SERVICES_KEY,
				StringUtils.collectionToCommaDelimitedString(backingApplication.getServices()));
			deploymentProperties.putAll(services);
		}
		return deploymentProperties;
	}

	private Resource getResource(String path) {
		return resourceLoader.getResource(path);
	}
}
