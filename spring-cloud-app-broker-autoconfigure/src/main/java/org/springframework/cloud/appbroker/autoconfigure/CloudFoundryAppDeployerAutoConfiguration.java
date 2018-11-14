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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryOAuth2Client;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.util.Optional;

@Configuration
@ConditionalOnProperty(CloudFoundryAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".api-host")
@EnableConfigurationProperties
public class CloudFoundryAppDeployerAutoConfiguration {
	static final String PROPERTY_PREFIX = "spring.cloud.appbroker.deployer.cloudfoundry";

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".properties")
	CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties() {
		return new CloudFoundryDeploymentProperties();
	}

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX)
	CloudFoundryTargetProperties cloudFoundryTargetProperties() {
		return new CloudFoundryTargetProperties();
	}

	@Bean
	AppDeployer cloudFoundryAppDeployer(CloudFoundryDeploymentProperties deploymentProperties,
										CloudFoundryOperations cloudFoundryOperations,
										CloudFoundryClient cloudFoundryClient,
										CloudFoundryTargetProperties targetProperties,
										ResourceLoader resourceLoader) {
		return new CloudFoundryAppDeployer(deploymentProperties, cloudFoundryOperations, cloudFoundryClient,
			targetProperties, resourceLoader);
	}

	@Bean
	OAuth2Client cloudFoundryOAuth2Client(UaaClient uaaClient) {
		return new CloudFoundryOAuth2Client(uaaClient);
	}

	@Bean
	ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Bean
	CloudFoundryOperations cloudFoundryOperations(CloudFoundryTargetProperties properties, CloudFoundryClient client,
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
	DefaultConnectionContext connectionContext(CloudFoundryTargetProperties properties) {
		return DefaultConnectionContext.builder()
			.apiHost(properties.getApiHost())
			.port(Optional.ofNullable(properties.getApiPort()))
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
	@ConditionalOnProperty({CloudFoundryAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".username",
		CloudFoundryAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".password"})
	PasswordGrantTokenProvider passwordGrantTokenProvider(CloudFoundryTargetProperties properties) {
		return PasswordGrantTokenProvider.builder()
			.password(properties.getPassword())
			.username(properties.getUsername())
			.build();
	}

	@Bean
	@ConditionalOnProperty({CloudFoundryAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".client-id",
		CloudFoundryAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".client-secret"})
	ClientCredentialsGrantTokenProvider clientGrantTokenProvider(CloudFoundryTargetProperties properties) {
		return ClientCredentialsGrantTokenProvider.builder()
			.clientId(properties.getClientId())
			.clientSecret(properties.getClientSecret())
			.identityZoneSubdomain(properties.getIdentityZoneSubdomain())
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
