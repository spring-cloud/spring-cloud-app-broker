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

package org.springframework.cloud.appbroker.instance.app;

import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.appbroker.instance.create.CreateServiceRequestContext;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Mono;

public class DefaultBackingAppDeployer implements BackingAppDeployer, ResourceLoaderAware {
	private ReactiveAppDeployer deployer;
	private ResourceLoader resourceLoader;

	public DefaultBackingAppDeployer(ReactiveAppDeployer deployer) {
		this.deployer = deployer;
	}

	DefaultBackingAppDeployer(ReactiveAppDeployer deployer, ResourceLoader resourceLoader) {
		this.deployer = deployer;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Mono<String> deploy(BackingAppParameters backingAppParameters,
							   CreateServiceRequestContext createServiceRequestContext) {
		AppDeploymentRequest request = buildDeploymentRequest(backingAppParameters);
		return deployer.deploy(request);
	}

	private AppDeploymentRequest buildDeploymentRequest(BackingAppParameters backingAppParameters) {
		AppDefinition appDefinition = new AppDefinition(backingAppParameters.getName(),
			backingAppParameters.getProperties());
		Resource resource = resourceLoader.getResource(backingAppParameters.getPath());

		return new AppDeploymentRequest(appDefinition, resource);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
