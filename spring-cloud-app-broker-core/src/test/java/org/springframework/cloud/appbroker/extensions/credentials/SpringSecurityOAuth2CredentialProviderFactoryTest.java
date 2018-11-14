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

package org.springframework.cloud.appbroker.extensions.credentials;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringSecurityOAuth2CredentialProviderFactoryTest {
	private static final String CLIENT_REGISTRATION = "app-client";

	@Mock
	private CredentialGenerator credentialGenerator;

	@Mock
	private OAuth2Client oAuth2Client;

	private CredentialProvider provider;
	private SpringSecurityOAuth2CredentialProviderFactory factory;

	@BeforeEach
	void setUp() {
		factory = new SpringSecurityOAuth2CredentialProviderFactory(credentialGenerator, oAuth2Client);
	}

	@Test
	void addCredentialsWithClientId() {
		createProvider("test-id");

		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		when(credentialGenerator.generateString(backingApplication.getName(), "service-instance-id",
			8, true, false, true, false))
			.thenReturn("test-secret");

		when(oAuth2Client.createClient(buildCreateOAuth2Request("test-id")))
			.thenReturn(Mono.just(CreateOAuth2ClientResponse.builder().build()));

		StepVerifier
			.create(provider.addCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		assertThat(backingApplication.getEnvironment())
			.containsEntry(factory.springSecurityClientIdKey(CLIENT_REGISTRATION), "test-id");
		assertThat(backingApplication.getEnvironment())
			.containsEntry(factory.springSecurityClientSecretKey(CLIENT_REGISTRATION), "test-secret");
	}

	@Test
	void addCredentialsWithoutClientId() {
		createProvider(null);

		BackingApplication backingApplication = BackingApplication.builder()
			.name("test-app")
			.build();
		String clientId = backingApplication.getName() + "-" + "service-instance-id";

		when(credentialGenerator.generateString(backingApplication.getName(), "service-instance-id",
			8, true, false, true, false))
			.thenReturn("test-secret");

		when(oAuth2Client.createClient(buildCreateOAuth2Request(clientId)))
			.thenReturn(Mono.just(CreateOAuth2ClientResponse.builder().build()));

		StepVerifier
			.create(provider.addCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		assertThat(backingApplication.getEnvironment())
			.containsEntry(factory.springSecurityClientIdKey(CLIENT_REGISTRATION), clientId);
		assertThat(backingApplication.getEnvironment())
			.containsEntry(factory.springSecurityClientSecretKey(CLIENT_REGISTRATION), "test-secret");
	}

	@Test
	void deleteCredentialsWithClientId() {
		createProvider("test-id");

		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		when(oAuth2Client.deleteClient(buildDeleteOAuth2Request("test-id")))
			.thenReturn(Mono.just(DeleteOAuth2ClientResponse.builder().build()));

		StepVerifier
			.create(provider.deleteCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		verify(credentialGenerator).deleteString(backingApplication.getName(), "service-instance-id");
		verifyNoMoreInteractions(credentialGenerator);
	}

	@Test
	void deleteCredentialsWithoutClientId() {
		createProvider(null);

		BackingApplication backingApplication = BackingApplication.builder()
			.name("test-app")
			.build();
		String clientId = backingApplication.getName() + "-" + "service-instance-id";

		when(oAuth2Client.deleteClient(buildDeleteOAuth2Request(clientId)))
			.thenReturn(Mono.just(DeleteOAuth2ClientResponse.builder().build()));

		StepVerifier
			.create(provider.deleteCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		verify(credentialGenerator).deleteString(backingApplication.getName(), "service-instance-id");
		verifyNoMoreInteractions(credentialGenerator);
	}

	private void createProvider(String clientId) {
		provider = factory.createWithConfig(config -> {
				config.setRegistration(CLIENT_REGISTRATION);
				config.setClientId(clientId);
				config.setClientName("test-name");
				config.setScopes("scope1");
				config.setAuthorities("auth1");
				config.setGrantTypes("client_credentials");
				config.setIdentityZoneSubdomain("subdomain");
				config.setIdentityZoneId("zoneId");

				config.setLength(8);
				config.setIncludeUppercaseAlpha(true);
				config.setIncludeLowercaseAlpha(false);
				config.setIncludeNumeric(true);
				config.setIncludeSpecial(false);
			});
	}

	private CreateOAuth2ClientRequest buildCreateOAuth2Request(String clientId) {
		return CreateOAuth2ClientRequest.builder()
			.clientId(clientId)
			.clientSecret("test-secret")
			.clientName("test-name")
			.scopes("scope1")
			.authorities("auth1")
			.grantTypes("client_credentials")
			.identityZoneSubdomain("subdomain")
			.identityZoneId("zoneId")
			.build();
	}

	private DeleteOAuth2ClientRequest buildDeleteOAuth2Request(String clientId) {
		return DeleteOAuth2ClientRequest.builder()
			.clientId(clientId)
			.identityZoneSubdomain("subdomain")
			.identityZoneId("zoneId")
			.build();
	}
}
