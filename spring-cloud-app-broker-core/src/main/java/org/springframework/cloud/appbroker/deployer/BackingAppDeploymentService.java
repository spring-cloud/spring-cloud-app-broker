/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * This interface is implemented by service brokers to process requests to deploy, update, and undeploy backing
 * applications associated with a service instance.
 */
public interface BackingAppDeploymentService {

	/**
	 * Deploy the backing applications and associate with the service instance
	 *
	 * @param backingApps a collection of backing applications
	 * @param serviceInstanceId the service instance ID
	 * @return a set of strings, where each corresponds to an application e.g. the application name
	 */
	Flux<String> deploy(List<BackingApplication> backingApps, String serviceInstanceId);

	/**
	 * Update the backing applications and associate with the service instance
	 *
	 * @param backingApps a collection of backing applications
	 * @param serviceInstanceId the service instance ID
	 * @return a set of strings, where each corresponds to an application. e.g. the application name
	 */
	Flux<String> update(List<BackingApplication> backingApps, String serviceInstanceId);

	/**
	 * Undeploy the backing applications
	 *
	 * @param backingApps a collection of backing applications
	 * @return a set of strings, where each corresponds to an application. e.g. the application name
	 */
	Flux<String> undeploy(List<BackingApplication> backingApps);

}
