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
import java.util.Map;

import reactor.core.publisher.Flux;

public interface BackingServicesProvisionService {

	Flux<String> createServiceInstance(List<BackingService> backingServices);

	Flux<String> updateServiceInstance(List<BackingService> backingServices);

	Flux<String> deleteServiceInstance(List<BackingService> backingServices);

	/**
	 * Create service keys
	 * @return A flux of credentials associated with each created service key
	 */
	Flux<Map<String, Object>> createServiceKeys(List<BackingServiceKey> backingServiceKeys);

	/**
	 * Delete service keys
	 * @return a Flux of service key names that were deleted
	 */
	Flux<String> deleteServiceKeys(List<BackingServiceKey> backingServiceKeys);
}
