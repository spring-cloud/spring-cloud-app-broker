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

package org.springframework.cloud.appbroker.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.stream.Stream;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.logcache.v1.ReactorLogCacheClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryAppManager;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryOAuth2Client;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryOperationsUtils;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.appbroker.manager.AppManager;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration support for deploying apps to Cloud Foundry
 */
@Configuration
@ConditionalOnProperty(CloudFoundryAppDeployerAutoConfiguration.PROPERTY_PREFIX + ".api-host")
@EnableConfigurationProperties
public class CloudFoundryAppDeployerAutoConfiguration {

	protected static final String PROPERTY_PREFIX = "spring.cloud.appbroker.deployer.cloudfoundry";

	/**
	 * Provide a {@link CloudFoundryDeploymentProperties} bean
	 *
	 * @return the bean
	 */
	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".properties")
	public CloudFoundryDeploymentProperties cloudFoundryDeploymentProperties() {
		return new CloudFoundryDeploymentProperties();
	}

	/**
	 * Provide a {@link CloudFoundryTargetProperties} bean
	 *
	 * @return the bean
	 */
	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX)
	public CloudFoundryTargetProperties cloudFoundryTargetProperties() {
		return new CloudFoundryTargetProperties();
	}

	/**
	 * Provide a {@link AppDeployer} bean
	 *
	 * @param deploymentProperties the CloudFoundryDeploymentProperties bean
	 * @param cloudFoundryOperations the CloudFoundryOperations bean
	 * @param cloudFoundryClient the CloudFoundryClient bean
	 * @param operationsUtils the CloudFoundryOperationsUtils bean
	 * @param targetProperties the CloudFoundryTargetProperties bean
	 * @param resourceLoader the ResourceLoader bean
	 * @return the bean
	 */
	@Bean
	public AppDeployer cloudFoundryAppDeployer(CloudFoundryDeploymentProperties deploymentProperties,
		CloudFoundryOperations cloudFoundryOperations, CloudFoundryClient cloudFoundryClient,
		CloudFoundryOperationsUtils operationsUtils, CloudFoundryTargetProperties targetProperties,
		ResourceLoader resourceLoader) {
		return new CloudFoundryAppDeployer(deploymentProperties, cloudFoundryOperations, cloudFoundryClient,
			operationsUtils, targetProperties, resourceLoader);
	}

	/**
	 * Provide an {@link AppManager} bean
	 *
	 * @param cloudFoundryOperationsUtils the CloudFoundryOperationsUtils bean
	 * @return the bean
	 */
	@Bean
	public AppManager cloudFoundryAppManager(CloudFoundryOperationsUtils cloudFoundryOperationsUtils) {
		return new CloudFoundryAppManager(cloudFoundryOperationsUtils);
	}

	/**
	 * Provide an {@link OAuth2Client} bean
	 *
	 * @param uaaClient the UaaClient bean
	 * @return the bean
	 */
	@Bean
	public OAuth2Client cloudFoundryOAuth2Client(@UaaClientQualifier UaaClient uaaClient) {
		return new CloudFoundryOAuth2Client(uaaClient);
	}

	/**
	 * Provide a {@link ReactorCloudFoundryClient} bean
	 *
	 * @param connectionContext the ConnectionContext bean
	 * @param tokenProvider the TokenProvider bean
	 * @return the bean
	 */
	@Bean
	public ReactorCloudFoundryClient cloudFoundryClient(@ConnectionContextQualifier ConnectionContext connectionContext,
		@TokenQualifier TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	/**
	 * Provide a {@link CloudFoundryOperations} bean
	 *
	 * @param properties the CloudFoundryTargetProperties bean
	 * @param client the CloudFoundryClient bean
	 * @param dopplerClient the DopplerClient bean
	 * @param uaaClient the UaaClient bean
	 * @return the bean
	 */
	@Bean
	public CloudFoundryOperations cloudFoundryOperations(CloudFoundryTargetProperties properties,
		CloudFoundryClient client, DopplerClient dopplerClient, @UaaClientQualifier UaaClient uaaClient) {
		return DefaultCloudFoundryOperations.builder()
			.cloudFoundryClient(client)
			.dopplerClient(dopplerClient)
			.uaaClient(uaaClient)
			.organization(properties.getDefaultOrg())
			.space(properties.getDefaultSpace())
			.build();
	}

	/**
	 * Provide a {@link CloudFoundryOperationsUtils} bean
	 *
	 * @param operations the CloudFoundryOperations bean
	 * @return the bean
	 */
	@Bean
	public CloudFoundryOperationsUtils cloudFoundryOperationsUtils(CloudFoundryOperations operations) {
		return new CloudFoundryOperationsUtils(operations);
	}

	/**
	 * Provide a {@link DefaultConnectionContext} bean
	 *
	 * @param properties the CloudFoundryTargetProperties bean
	 * @return the bean
	 */
	@Bean
	@ConnectionContextQualifier
	public DefaultConnectionContext connectionContext(CloudFoundryTargetProperties properties) {
		return DefaultConnectionContext.builder()
			.apiHost(properties.getApiHost())
			.port(Optional.ofNullable(properties.getApiPort()))
			.skipSslValidation(properties.isSkipSslValidation())
			.secure(properties.isSecure())
			.build();
	}

	/**
	 * Provide a {@link ReactorDopplerClient} bean
	 *
	 * @param connectionContext the ConnectionContext bean
	 * @param tokenProvider the TokenProvider bean
	 * @return the bean
	 */
	@Bean
	public ReactorDopplerClient dopplerClient(@ConnectionContextQualifier ConnectionContext connectionContext,
		@TokenQualifier TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	/**
	 * Provide a {@link LogCacheClient} bean
	 *
	 * @param connectionContext the ConnectionContext bean
	 * @param tokenProvider the TokenProvider bean
	 * @return the bean
	 */
	@Bean
	public LogCacheClient logCacheClient(@ConnectionContextQualifier ConnectionContext connectionContext,
		@TokenQualifier TokenProvider tokenProvider) {
		return ReactorLogCacheClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	/**
	 * Provide a {@link TokenProvider} bean
	 *
	 * @param properties the CloudFoundryTargetProperties bean
	 * @return the bean
	 */
	@TokenQualifier
	@Bean
	public TokenProvider uaaTokenProvider(CloudFoundryTargetProperties properties) {
		boolean isClientIdAndSecretSet = Stream.of(properties.getClientId(), properties.getClientSecret())
			.allMatch(StringUtils::hasText);
		boolean isUsernameAndPasswordSet = Stream.of(properties.getUsername(), properties.getPassword())
			.allMatch(StringUtils::hasText);
		if (isClientIdAndSecretSet && isUsernameAndPasswordSet) {
			throw new IllegalStateException(
				String.format("(%1$s.client_id / %1$s.client_secret) must not be set when\n" +
					"(%1$s.username / %1$s.password) are also set", PROPERTY_PREFIX));
		}
		else if (isClientIdAndSecretSet) {
			return ClientCredentialsGrantTokenProvider.builder()
				.clientId(properties.getClientId())
				.clientSecret(properties.getClientSecret())
				.identityZoneSubdomain(properties.getIdentityZoneSubdomain())
				.build();
		}
		else if (isUsernameAndPasswordSet) {
			return PasswordGrantTokenProvider.builder()
				.password(properties.getPassword())
				.username(properties.getUsername())
				.build();
		}
		else {
			throw new IllegalStateException(
				String.format("Either (%1$s.client_id and %1$s.client_secret) or\n" +
					"(%1$s.username and %1$s.password) properties must be set", PROPERTY_PREFIX));
		}
	}

	/**
	 * Provide a {@link ReactorUaaClient} bean
	 *
	 * @param connectionContext the ConnectionContext bean
	 * @param tokenProvider the TokenProvider bean
	 * @return the bean
	 */
	@UaaClientQualifier
	@Bean
	public ReactorUaaClient uaaClient(@ConnectionContextQualifier ConnectionContext connectionContext,
		@TokenQualifier TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
			.connectionContext(connectionContext)
			.tokenProvider(tokenProvider)
			.build();
	}

	@Qualifier
	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TokenQualifier {

		String value() default "appBrokerTokenProvider";

	}

	@Qualifier
	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface UaaClientQualifier {

		String value() default "appBrokerUaaClientQualifier";

	}

	@Qualifier
	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ConnectionContextQualifier {

		String value() default "appBrokerConnectionContextQualifier";

	}

}
