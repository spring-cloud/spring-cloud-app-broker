/*
 * Copyright 2016-2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance.logging;


import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.spaces.GetSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceDetail;
import reactor.core.publisher.Flux;

import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.stereotype.Component;

@Component
class BackingApplicationIdsProvider implements ApplicationIdsProvider {

	private final CloudFoundryClient cloudFoundryClient;

	private final CloudFoundryOperations cloudFoundryOperations;

	public BackingApplicationIdsProvider(CloudFoundryClient cloudFoundryClient,
		CloudFoundryOperations cloudFoundryOperations) {
		this.cloudFoundryClient = cloudFoundryClient;
		this.cloudFoundryOperations = cloudFoundryOperations;
	}

	@Override
	public Flux<String> getApplicationIds(String serviceInstanceId) {
		return cloudFoundryOperations.spaces().get(GetSpaceRequest.builder().name(serviceInstanceId).build())
			.map(SpaceDetail::getId)
			.flatMap(spaceId ->
				cloudFoundryClient.applicationsV3()
					.list(ListApplicationsRequest.builder().spaceIds(spaceId).build())
			)
			.flatMapMany(
				listApplicationsResponse -> Flux.fromIterable(listApplicationsResponse.getResources())
					.map(ApplicationResource::getId));
	}

}
