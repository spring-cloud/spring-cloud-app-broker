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

package org.springframework.cloud.appbroker.acceptance.fixtures.uaa;

import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.clients.CreateClientRequest;
import org.cloudfoundry.uaa.clients.DeleteClientRequest;
import org.cloudfoundry.uaa.clients.GetClientRequest;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;

@Service
public class UaaService {

	private static final Logger LOG = LoggerFactory.getLogger(UaaService.class);

	private final UaaClient uaaClient;

	public UaaService(UaaClient uaaClient) {
		this.uaaClient = uaaClient;
	}

	public Mono<GetClientResponse> getUaaClient(String clientId) {
		return uaaClient.clients().get(GetClientRequest
			.builder()
			.clientId(clientId)
			.build())
			.onErrorResume(e -> Mono.empty());
	}

	public Mono<Void> createClient(String clientId, String clientSecret, String... authorities) {
		return uaaClient.clients().delete(DeleteClientRequest
			.builder()
			.clientId(clientId)
			.build())
			.doOnError(error -> LOG.warn("Error deleting client: " + clientId + " with error: " + error))
			.onErrorResume(e -> Mono.empty())
			.then(uaaClient.clients().create(CreateClientRequest
				.builder()
				.clientId(clientId)
				.clientSecret(clientSecret)
				.authorizedGrantType(GrantType.CLIENT_CREDENTIALS)
				.authorities(authorities)
				.build())
				.doOnError(error -> LOG.error("Error creating client: " + clientId + " with error: " + error))
				.onErrorResume(e -> Mono.empty()))
			.then();
	}

}
