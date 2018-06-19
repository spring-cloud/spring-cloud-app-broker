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

import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import reactor.core.publisher.Mono;

public interface ReactiveAppDeployer {
	/**
	 * Deploy an app using an {@link AppDeploymentRequest}. The returned id is
	 * later used with {@link #undeploy(String)} or {@link #status(String)} to
	 * undeploy an app or check its status, respectively.
	 *
	 * Implementations may perform this operation asynchronously; therefore a
	 * successful deployment may not be assumed upon return. To determine the
	 * status of a deployment, invoke {@link #status(String)}.
	 *
	 * @param request the app deployment request
	 * @return the deployment id for the app
	 * @throws IllegalStateException if the app has already been deployed
	 */
	Mono<String> deploy(AppDeploymentRequest request);

	/**
	 * Un-deploy an app using its deployment id. Implementations may perform
	 * this operation asynchronously; therefore a successful un-deployment may
	 * not be assumed upon return. To determine the status of a deployment,
	 * invoke {@link #status(String)}.
	 *
	 * @throws IllegalStateException if the app has not been deployed
	 */
	Mono<Void> undeploy(String id);

	/**
	 * Return the {@link AppStatus} for an app represented by a deployment id.
	 *
	 * @param id the app deployment id, as returned by {@link #deploy}
	 * @return the app deployment status
	 */
	Mono<AppStatus> status(String id);

	/**
	 * Return the environment info for this deployer.
	 *
	 * @return the runtime environment info
	 */
	RuntimeEnvironmentInfo environmentInfo();
}