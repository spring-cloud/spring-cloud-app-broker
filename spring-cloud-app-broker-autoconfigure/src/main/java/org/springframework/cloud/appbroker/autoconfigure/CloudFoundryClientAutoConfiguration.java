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

package org.springframework.cloud.appbroker.autoconfigure;

import java.util.Optional;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(CloudFoundryProperties.PROPERTY_PREFIX + ".apiHost")
@EnableConfigurationProperties(CloudFoundryProperties.class)
public class CloudFoundryClientAutoConfiguration {

	@Bean
	ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
										.connectionContext(connectionContext)
										.tokenProvider(tokenProvider)
										.build();
	}

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
	DefaultConnectionContext connectionContext(CloudFoundryProperties properties) {
		Optional<ProxyConfiguration> proxyConfiguration = Optional.empty();
		String proxyHost = properties.getProxyHost();
		int proxyPort = properties.getProxyPort();
		if (proxyHost != null && proxyPort != 0) {
			proxyConfiguration = Optional.of(
				ProxyConfiguration.builder()
								  .host(properties.getProxyHost())
								  .port(properties.getApiPort())
								  .build());
		}
		return DefaultConnectionContext.builder()
									   .apiHost(properties.getApiHost())
									   .port(Optional.ofNullable(properties.getApiPort()))
									   .proxyConfiguration(proxyConfiguration)
									   .skipSslValidation(properties.isSkipSslValidation())
									   .secure(properties.isSecure())
									   .build();
	}

	@Bean
	ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
								   .connectionContext(connectionContext)
								   .tokenProvider(tokenProvider)
								   .build();
	}

	@Bean
	@ConditionalOnProperty({CloudFoundryProperties.PROPERTY_PREFIX + ".username",
		CloudFoundryProperties.PROPERTY_PREFIX + ".password"})
	PasswordGrantTokenProvider tokenProvider(CloudFoundryProperties properties) {
		return PasswordGrantTokenProvider.builder()
										 .password(properties.getPassword())
										 .username(properties.getUsername())
										 .build();
	}

	@Bean
	ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
							   .connectionContext(connectionContext)
							   .tokenProvider(tokenProvider)
							   .build();
	}

}
