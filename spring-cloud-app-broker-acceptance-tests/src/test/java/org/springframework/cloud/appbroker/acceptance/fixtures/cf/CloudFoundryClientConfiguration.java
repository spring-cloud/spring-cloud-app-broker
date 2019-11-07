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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.util.Optional;

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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudFoundryProperties.class)
public class CloudFoundryClientConfiguration {

	/**
	 * The client secret
	 */
	public static final String APP_BROKER_CLIENT_SECRET = "app-broker-client-secret";

	/**
	 * The client authorities
	 */
	public static final String[] APP_BROKER_CLIENT_AUTHORITIES = {
		"cloud_controller.read", "cloud_controller.write", "clients.write"
	};

	@Bean
	protected CloudFoundryOperations cloudFoundryOperations(CloudFoundryProperties properties,
		CloudFoundryClient client,
		DopplerClient dopplerClient,
		UaaClient uaaClient) {
		return DefaultCloudFoundryOperations.builder()
			.cloudFoundryClient(client)
			.dopplerClient(dopplerClient)
			.uaaClient(uaaClient)
			.organization(properties.getDefaultOrg())
			.space(properties.getDefaultSpace())
			.build();
	}

	@Bean
	protected CloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext,
		@Qualifier("userCredentials") TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	protected ConnectionContext connectionContext(CloudFoundryProperties properties) {
		return DefaultConnectionContext.builder()
			.apiHost(properties.getApiHost())
			.port(Optional.ofNullable(properties.getApiPort()))
			.skipSslValidation(properties.isSkipSslValidation())
			.secure(properties.isSecure())
			.build();
	}

	@Bean
	protected DopplerClient dopplerClient(ConnectionContext connectionContext,
		@Qualifier("userCredentials") TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	protected UaaClient uaaClient(ConnectionContext connectionContext,
		@Qualifier("clientCredentials") TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	@Qualifier("userCredentials")
	@ConditionalOnProperty({
		CloudFoundryProperties.PROPERTY_PREFIX + ".username",
		CloudFoundryProperties.PROPERTY_PREFIX + ".password"
	})
	protected PasswordGrantTokenProvider passwordTokenProvider(CloudFoundryProperties properties) {
		return PasswordGrantTokenProvider.builder()
			.password(properties.getPassword())
			.username(properties.getUsername())
			.build();
	}

	@Bean
	@Qualifier("clientCredentials")
	@ConditionalOnProperty({
		CloudFoundryProperties.PROPERTY_PREFIX + ".client-id",
		CloudFoundryProperties.PROPERTY_PREFIX + ".client-secret"
	})
	protected ClientCredentialsGrantTokenProvider clientTokenProvider(CloudFoundryProperties properties) {
		return ClientCredentialsGrantTokenProvider.builder()
			.clientId(properties.getClientId())
			.clientSecret(properties.getClientSecret())
			.identityZoneSubdomain(properties.getIdentityZoneSubdomain())
			.build();
	}

}
