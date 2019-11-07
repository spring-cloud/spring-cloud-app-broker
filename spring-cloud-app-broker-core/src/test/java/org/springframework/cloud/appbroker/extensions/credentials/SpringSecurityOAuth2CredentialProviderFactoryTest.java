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

package org.springframework.cloud.appbroker.extensions.credentials;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityOAuth2CredentialProviderFactory.SPRING_SECURITY_OAUTH2_CLIENT_ID_KEY;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityOAuth2CredentialProviderFactory.SPRING_SECURITY_OAUTH2_CLIENT_SECRET_KEY;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityOAuth2CredentialProviderFactory.SPRING_SECURITY_OAUTH2_REGISTRATION_KEY;

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

		given(credentialGenerator.generateString(backingApplication.getName(), "service-instance-id", "oauth2",
			8, true, false, true, false))
			.willReturn(Mono.just("test-secret"));

		given(oAuth2Client.createClient(buildCreateOAuth2Request("test-id")))
			.willReturn(Mono.just(CreateOAuth2ClientResponse.builder().build()));

		StepVerifier
			.create(provider.addCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		assertEnvironmentContainsProperties(backingApplication, "test-id");
	}

	@Test
	void addCredentialsWithoutClientId() {
		createProvider(null);

		BackingApplication backingApplication = BackingApplication.builder()
			.name("test-app")
			.build();
		String clientId = backingApplication.getName() + "-" + "service-instance-id";

		given(credentialGenerator.generateString(backingApplication.getName(), "service-instance-id", "oauth2",
			8, true, false, true, false))
			.willReturn(Mono.just("test-secret"));

		given(oAuth2Client.createClient(buildCreateOAuth2Request(clientId)))
			.willReturn(Mono.just(CreateOAuth2ClientResponse.builder().build()));

		StepVerifier
			.create(provider.addCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		assertEnvironmentContainsProperties(backingApplication, clientId);
	}

	private void assertEnvironmentContainsProperties(BackingApplication backingApplication, String id) {
		Map<String, Object> environment = backingApplication.getEnvironment();

		assertThat(environment)
			.containsEntry(SPRING_SECURITY_OAUTH2_REGISTRATION_KEY + CLIENT_REGISTRATION +
				SPRING_SECURITY_OAUTH2_CLIENT_ID_KEY, id)
			.containsEntry(SPRING_SECURITY_OAUTH2_REGISTRATION_KEY + CLIENT_REGISTRATION +
				SPRING_SECURITY_OAUTH2_CLIENT_SECRET_KEY, "test-secret");
	}

	@Test
	void deleteCredentialsWithClientId() {
		createProvider("test-id");

		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		given(oAuth2Client.deleteClient(buildDeleteOAuth2Request("test-id")))
			.willReturn(Mono.just(DeleteOAuth2ClientResponse.builder().build()));

		given(credentialGenerator.deleteString(backingApplication.getName(), "service-instance-id", "oauth2"))
			.willReturn(Mono.empty());

		StepVerifier
			.create(provider.deleteCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		then(credentialGenerator).should().deleteString(backingApplication.getName(), "service-instance-id", "oauth2");
		then(credentialGenerator).shouldHaveNoMoreInteractions();
	}

	@Test
	void deleteCredentialsWithoutClientId() {
		createProvider(null);

		BackingApplication backingApplication = BackingApplication.builder()
			.name("test-app")
			.build();
		String clientId = backingApplication.getName() + "-" + "service-instance-id";

		given(oAuth2Client.deleteClient(buildDeleteOAuth2Request(clientId)))
			.willReturn(Mono.just(DeleteOAuth2ClientResponse.builder().build()));

		given(credentialGenerator.deleteString(backingApplication.getName(), "service-instance-id", "oauth2"))
			.willReturn(Mono.empty());

		StepVerifier
			.create(provider.deleteCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		then(credentialGenerator).should().deleteString(backingApplication.getName(), "service-instance-id", "oauth2");
		then(credentialGenerator).shouldHaveNoMoreInteractions();
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
