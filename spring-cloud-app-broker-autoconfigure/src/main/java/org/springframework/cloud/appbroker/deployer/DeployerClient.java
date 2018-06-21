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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

//TODO This should be in the App Broker core subproject
// TODO I should not be public in order to avoid usage outside of it's bounded context (app deployment)
/**
 * The deployer client exists to provide an adapter / anti-corruption layer between the platform deployer component {@see ReactiveAppDeployer}
 * and App Broker workflow components. For rationale {@see https://docs.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer}
 *
 * A simple rule we have been following is;
 * Controller --calls--> Service ✅
 * Service    --calls--> Client  ✅
 * Service    --calls--> Service ⛔️ probably a tight coupling between two bounded contexts
 */
@Component
public class DeployerClient {

	private ReactiveAppDeployer reactiveAppDeployer;

	public DeployerClient(ReactiveAppDeployer reactiveAppDeployer) {
		this.reactiveAppDeployer = reactiveAppDeployer;
	}

	// TODO I should not be public in order to avoid usage outside of it's bounded context (app deployment)
	public Mono<String> deploy(DeployerApplication deployerApplication) {

		AppDefinition appDefinition = new AppDefinition(deployerApplication.getAppName(), Collections.emptyMap());
		Resource resource = null;
		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource);
		return reactiveAppDeployer.deploy(appDeploymentRequest);
	}

}
