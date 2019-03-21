/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance.fixtures.uaa;

import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.clients.GetClientRequest;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UaaService {
	private final UaaClient uaaClient;

	public UaaService(UaaClient uaaClient) {
		this.uaaClient = uaaClient;
	}

	public Mono<GetClientResponse> getUaaClient(String clientId) {
		return uaaClient.clients().get(GetClientRequest.builder()
			.clientId(clientId)
			.build())
			.onErrorResume(e -> Mono.empty());
	}
}
