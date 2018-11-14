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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.clients.CreateClientRequest;
import org.cloudfoundry.uaa.clients.CreateClientResponse;
import org.cloudfoundry.uaa.clients.DeleteClientRequest;
import org.cloudfoundry.uaa.clients.DeleteClientResponse;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class CloudFoundryOAuth2Client implements OAuth2Client {
	private final UaaClient uaaClient;

	public CloudFoundryOAuth2Client(UaaClient uaaClient) {
		this.uaaClient = uaaClient;
	}

	@Override
	public Mono<CreateOAuth2ClientResponse> createClient(CreateOAuth2ClientRequest request) {
		return uaaClient.clients()
			.create(mapCreateRequest(request))
			.map(this::mapCreateResponse);
	}

	@Override
	public Mono<DeleteOAuth2ClientResponse> deleteClient(DeleteOAuth2ClientRequest request) {
		return uaaClient.clients()
			.delete(mapDeleteRequest(request))
			.map(this::mapDeleteResponse);
	}

	private CreateClientRequest mapCreateRequest(CreateOAuth2ClientRequest request) {
		return CreateClientRequest.builder()
			.clientId(request.getClientId())
			.clientSecret(request.getClientSecret())
			.name(request.getClientName())
			.scopes(request.getScopes())
			.authorities(request.getAuthorities())
			.authorizedGrantTypes(mapStringToGrantType(request.getGrantTypes()))
			.identityZoneSubdomain(request.getIdentityZoneSubdomain())
			.identityZoneId(request.getIdentityZoneId())
			.build();
	}

	private CreateOAuth2ClientResponse mapCreateResponse(CreateClientResponse response) {
		return CreateOAuth2ClientResponse.builder()
			.clientId(response.getClientId())
			.clientName(response.getName())
			.scopes(response.getScopes())
			.authorities(response.getAuthorities())
			.grantTypes(mapGrantTypeToString(response.getAuthorizedGrantTypes()))
			.build();
	}

	private DeleteClientRequest mapDeleteRequest(DeleteOAuth2ClientRequest request) {
		return DeleteClientRequest.builder()
			.clientId(request.getClientId())
			.identityZoneSubdomain(request.getIdentityZoneSubdomain())
			.identityZoneId(request.getIdentityZoneId())
			.build();
	}

	private DeleteOAuth2ClientResponse mapDeleteResponse(DeleteClientResponse response) {
		return DeleteOAuth2ClientResponse.builder()
			.clientId(response.getClientId())
			.clientName(response.getName())
			.scopes(response.getScopes())
			.authorities(response.getAuthorities())
			.grantTypes(mapGrantTypeToString(response.getAuthorizedGrantTypes()))
			.build();
	}

	private List<GrantType> mapStringToGrantType(List<String> grantTypes) {
		if (CollectionUtils.isEmpty(grantTypes)) {
			return null;
		}

		return grantTypes.stream()
			.map(GrantType::from)
			.collect(Collectors.toList());
	}

	private List<String> mapGrantTypeToString(List<GrantType> grantTypes) {
		if (CollectionUtils.isEmpty(grantTypes)) {
			return null;
		}
		
		return grantTypes.stream()
			.map(GrantType::getValue)
			.collect(Collectors.toList());
	}
}
