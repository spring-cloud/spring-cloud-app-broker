/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.util.Optional;

import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.UaaException;
import org.cloudfoundry.uaa.clients.Clients;
import org.cloudfoundry.uaa.clients.CreateClientRequest;
import org.cloudfoundry.uaa.clients.DeleteClientRequest;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(CloudFoundryProperties.class)
public class CloudFoundryClientConfiguration {

	static final String ACCEPTANCE_TEST_OAUTH_CLIENT_ID = "acceptance-test-client";
	static final String ACCEPTANCE_TEST_OAUTH_CLIENT_SECRET = "acceptance-test-client-secret";
	private static final String[] ACCEPTANCE_TEST_OAUTH_CLIENT_AUTHORITIES = {
		"openid", "cloud_controller.admin", "cloud_controller.read", "cloud_controller.write",
		"clients.read", "clients.write"
	};

	@Bean
	CloudFoundryOperations cloudFoundryOperations(CloudFoundryProperties properties, CloudFoundryClient client,
												  DopplerClient dopplerClient, UaaClient uaaClient) {
		return DefaultCloudFoundryOperations.builder()
			.cloudFoundryClient(client)
			.dopplerClient(dopplerClient)
			.uaaClient(uaaClient)
			.organization(properties.getDefaultOrg())
			.space(properties.getDefaultSpace())
			.build();
	}

	@Bean
	CloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	ConnectionContext connectionContext(CloudFoundryProperties properties) {
		return DefaultConnectionContext.builder()
			.apiHost(properties.getApiHost())
			.port(Optional.ofNullable(properties.getApiPort()))
			.skipSslValidation(properties.isSkipSslValidation())
			.secure(properties.isSecure())
			.build();
	}

	@Bean
	DopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	UaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	@ConditionalOnProperty({CloudFoundryProperties.PROPERTY_PREFIX + ".username",
		CloudFoundryProperties.PROPERTY_PREFIX + ".password"})
	PasswordGrantTokenProvider passwordTokenProvider(CloudFoundryProperties properties) {
		return PasswordGrantTokenProvider.builder()
			.password(properties.getPassword())
			.username(properties.getUsername())
			.build();
	}

	@Bean
	@ConditionalOnProperty({CloudFoundryProperties.PROPERTY_PREFIX + ".client-id",
		CloudFoundryProperties.PROPERTY_PREFIX + ".client-secret"})
	ClientCredentialsGrantTokenProvider clientTokenProvider(ConnectionContext connectionContext,
															CloudFoundryProperties properties) {

		Clients uaaClients = buildTempUaaClient(connectionContext, properties).clients();

		uaaClients.delete(DeleteClientRequest.builder()
			.clientId(ACCEPTANCE_TEST_OAUTH_CLIENT_ID)
			.build())
			.onErrorResume(UaaException.class, e -> Mono.empty())
			.onErrorResume(UnknownCloudFoundryException.class, e -> Mono.empty())
			.then(uaaClients.create(CreateClientRequest.builder()
				.clientId(ACCEPTANCE_TEST_OAUTH_CLIENT_ID)
				.clientSecret(ACCEPTANCE_TEST_OAUTH_CLIENT_SECRET)
				.authorizedGrantType(GrantType.CLIENT_CREDENTIALS)
				.authorities(ACCEPTANCE_TEST_OAUTH_CLIENT_AUTHORITIES)
				.build()))
			.block();

		return ClientCredentialsGrantTokenProvider.builder()
			.clientId(ACCEPTANCE_TEST_OAUTH_CLIENT_ID)
			.clientSecret(ACCEPTANCE_TEST_OAUTH_CLIENT_SECRET)
			.identityZoneSubdomain(properties.getIdentityZoneSubdomain())
			.build();
	}

	private UaaClient buildTempUaaClient(ConnectionContext connectionContext, CloudFoundryProperties properties) {
		return ReactorUaaClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(ClientCredentialsGrantTokenProvider.builder()
				.clientId(properties.getClientId())
				.clientSecret(properties.getClientSecret())
				.identityZoneSubdomain(properties.getIdentityZoneSubdomain())
				.build())
			.build();
	}

}
