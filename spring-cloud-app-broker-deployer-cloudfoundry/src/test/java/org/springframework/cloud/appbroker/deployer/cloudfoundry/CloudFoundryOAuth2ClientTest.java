/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.clients.Clients;
import org.cloudfoundry.uaa.clients.CreateClientRequest;
import org.cloudfoundry.uaa.clients.CreateClientResponse;
import org.cloudfoundry.uaa.clients.DeleteClientRequest;
import org.cloudfoundry.uaa.clients.DeleteClientResponse;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudFoundryOAuth2ClientTest {

	@Mock
	private UaaClient uaaClient;

	@Mock
	private Clients clients;

	private CloudFoundryOAuth2Client oAuth2Client;

	@BeforeEach
	void setUp() {
		when(uaaClient.clients()).thenReturn(clients);

		oAuth2Client = new CloudFoundryOAuth2Client(uaaClient);
	}

	@Test
	void createClient() {
		CreateClientRequest clientsRequest = CreateClientRequest.builder()
			.clientId("test-client")
			.clientSecret("test-secret")
			.name("test-name")
			.scopes("auth1", "auth2")
			.authorities("auth1", "auth2")
			.authorizedGrantTypes(GrantType.CLIENT_CREDENTIALS, GrantType.AUTHORIZATION_CODE,
				GrantType.PASSWORD, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN)
			.identityZoneSubdomain("subdomain")
			.identityZoneId("zoneId")
			.build();

		CreateClientResponse clientsResponse = CreateClientResponse.builder()
			.clientId("test-client")
			.name("test-name")
			.scopes("auth1", "auth2")
			.authorities("auth1", "auth2")
			.authorizedGrantTypes(GrantType.CLIENT_CREDENTIALS, GrantType.AUTHORIZATION_CODE,
				GrantType.PASSWORD, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN)
			.build();

		when(clients.create(clientsRequest))
			.thenReturn(Mono.just(clientsResponse));

		CreateOAuth2ClientRequest request = CreateOAuth2ClientRequest.builder()
			.clientId("test-client")
			.clientSecret("test-secret")
			.clientName("test-name")
			.scopes("auth1", "auth2")
			.authorities("auth1", "auth2")
			.grantTypes("client_credentials", "authorization_code", "password", "implicit", "refresh_token")
			.identityZoneSubdomain("subdomain")
			.identityZoneId("zoneId")
			.build();

		StepVerifier.create(oAuth2Client.createClient(request))
			.assertNext(response -> assertResponse(response.getClientId(), response.getClientName(),
				response.getScopes(), response.getAuthorities(),
				response.getGrantTypes()))
			.verifyComplete();
	}

	@Test
	void deleteClient() {
		DeleteClientRequest clientsRequest = DeleteClientRequest.builder()
			.clientId("test-client")
			.identityZoneSubdomain("subdomain")
			.identityZoneId("zoneId")
			.build();

		DeleteClientResponse clientsResponse = DeleteClientResponse.builder()
			.clientId("test-client")
			.name("test-name")
			.scopes("auth1", "auth2")
			.authorities("auth1", "auth2")
			.authorizedGrantTypes(GrantType.CLIENT_CREDENTIALS, GrantType.AUTHORIZATION_CODE,
				GrantType.PASSWORD, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN)
			.build();

		when(clients.delete(clientsRequest))
			.thenReturn(Mono.just(clientsResponse));

		DeleteOAuth2ClientRequest request = DeleteOAuth2ClientRequest.builder()
			.clientId("test-client")
			.identityZoneSubdomain("subdomain")
			.identityZoneId("zoneId")
			.build();

		StepVerifier.create(oAuth2Client.deleteClient(request))
			.assertNext(response -> assertResponse(response.getClientId(), response.getClientName(),
				response.getScopes(), response.getAuthorities(),
				response.getGrantTypes()))
			.verifyComplete();
	}

	private void assertResponse(String clientId, String name,
								List<String> scopes, List<String> authorities,
								List<String> authorizedGrantTypes) {
		assertThat(clientId).isEqualTo("test-client");
		assertThat(name).isEqualTo("test-name");
		assertThat(scopes).contains("auth1", "auth2");
		assertThat(authorities).contains("auth1", "auth2");
		assertThat(authorizedGrantTypes).contains(
			"client_credentials", "authorization_code", "password", "implicit", "refresh_token");
	}
}