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

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.CreateOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientRequest;
import org.springframework.cloud.appbroker.oauth2.DeleteOAuth2ClientResponse;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;

public class SpringSecurityOAuth2CredentialProviderFactory extends
	CredentialProviderFactory<SpringSecurityOAuth2CredentialProviderFactory.Config> {

	private static final String CREDENTIAL_DESCRIPTOR = "oauth2";

	static final String SPRING_SECURITY_OAUTH2_REGISTRATION_KEY = "spring.security.oauth2.client.registration.";
	static final String SPRING_SECURITY_OAUTH2_CLIENT_ID_KEY = ".client-id";
	static final String SPRING_SECURITY_OAUTH2_CLIENT_SECRET_KEY = ".client-secret";

	private final CredentialGenerator credentialGenerator;
	private final OAuth2Client oAuth2Client;

	public SpringSecurityOAuth2CredentialProviderFactory(CredentialGenerator credentialGenerator,
														 OAuth2Client oAuth2Client) {
		super(Config.class);
		this.credentialGenerator = credentialGenerator;
		this.oAuth2Client = oAuth2Client;
	}

	@Override
	public CredentialProvider create(Config config) {
		return new CredentialProvider() {
			@Override
			public Mono<BackingApplication> addCredentials(BackingApplication backingApplication,
														   String serviceInstanceGuid) {
				return generateCredentials(config, backingApplication, serviceInstanceGuid)
					.flatMap(client -> addClientToEnvironment(config, backingApplication, client))
					.flatMap(client -> createOAuth2Client(config, client))
					.flatMap(response -> Mono.just(backingApplication));
			}

			@Override
			public Mono<BackingApplication> deleteCredentials(BackingApplication backingApplication,
															  String serviceInstanceGuid) {
				return credentialGenerator.deleteString(backingApplication.getName(), serviceInstanceGuid, CREDENTIAL_DESCRIPTOR)
					.then(generateClientId(config, backingApplication, serviceInstanceGuid))
					.flatMap(clientId -> deleteOAuth2Client(config, clientId)
						.flatMap(response -> Mono.just(backingApplication)));
			}
		};
	}

	private Mono<Tuple2<String, String>> generateCredentials(Config config,
															 BackingApplication backingApplication,
															 String serviceInstanceGuid) {
		return generateClientId(config, backingApplication, serviceInstanceGuid)
			.flatMap(id -> generateClientSecret(config, backingApplication, serviceInstanceGuid)
				.map(secret -> Tuples.of(id, secret)));
	}

	private Mono<Tuple2<String, String>> addClientToEnvironment(Config config,
																BackingApplication backingApplication,
																Tuple2<String, String> client) {
		String registrationKey = SPRING_SECURITY_OAUTH2_REGISTRATION_KEY + config.getRegistration();

		backingApplication.addEnvironment(registrationKey + SPRING_SECURITY_OAUTH2_CLIENT_ID_KEY, client.getT1());
		backingApplication.addEnvironment(registrationKey + SPRING_SECURITY_OAUTH2_CLIENT_SECRET_KEY, client.getT2());

		return Mono.just(client);
	}

	private Mono<String> generateClientId(Config config, BackingApplication backingApplication,
									String serviceInstanceGuid) {
		return Mono.defer(() -> {
			if (config.clientId == null) {
				return Mono.just(backingApplication.getName() + "-" + serviceInstanceGuid);
			}
			return Mono.just(config.getClientId());
		});
	}

	private Mono<String> generateClientSecret(Config config, BackingApplication backingApplication,
										String serviceInstanceGuid) {
		return credentialGenerator.generateString(backingApplication.getName(), serviceInstanceGuid,
			CREDENTIAL_DESCRIPTOR, config.getLength(), config.isIncludeUppercaseAlpha(), config.isIncludeLowercaseAlpha(),
			config.isIncludeNumeric(), config.isIncludeSpecial());
	}

	private Mono<CreateOAuth2ClientResponse> createOAuth2Client(Config config, Tuple2<String, String> client) {
		CreateOAuth2ClientRequest.CreateOAuth2ClientRequestBuilder builder = CreateOAuth2ClientRequest.builder()
			.clientId(client.getT1())
			.clientSecret(client.getT2())
			.clientName(config.getClientName())
			.identityZoneSubdomain(config.getIdentityZoneSubdomain())
			.identityZoneId(config.getIdentityZoneId());

		if (config.getScopes() != null) {
			builder.scopes(config.getScopes());
		}

		if (config.getAuthorities() != null) {
			builder.authorities(config.getAuthorities());
		}

		if (config.getGrantTypes() != null) {
			builder.grantTypes(config.getGrantTypes());
		}

		return oAuth2Client.createClient(builder.build());
	}

	private Mono<DeleteOAuth2ClientResponse> deleteOAuth2Client(Config config, String clientId) {
		DeleteOAuth2ClientRequest request = DeleteOAuth2ClientRequest.builder()
			.clientId(clientId)
			.identityZoneSubdomain(config.getIdentityZoneSubdomain())
			.identityZoneId(config.getIdentityZoneId())
			.build();

		return oAuth2Client.deleteClient(request);
	}

	@SuppressWarnings("WeakerAccess")
	public static class Config extends CredentialGenerationConfig {
		private String registration;
		private String clientId;
		private String clientName;
		private String[] scopes;
		private String[] authorities;
		private String[] grantTypes;
		private String identityZoneSubdomain;
		private String identityZoneId;

		public String getRegistration() {
			return registration;
		}

		public void setRegistration(String registration) {
			this.registration = registration;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientName() {
			return clientName;
		}

		public void setClientName(String clientName) {
			this.clientName = clientName;
		}

		public String[] getScopes() {
			return scopes;
		}

		public void setScopes(String... scopes) {
			this.scopes = scopes;
		}

		public String[] getAuthorities() {
			return authorities;
		}

		public void setAuthorities(String... authorities) {
			this.authorities = authorities;
		}

		public String[] getGrantTypes() {
			return grantTypes;
		}

		public void setGrantTypes(String... grantTypes) {
			this.grantTypes = grantTypes;
		}

		public String getIdentityZoneSubdomain() {
			return identityZoneSubdomain;
		}

		public void setIdentityZoneSubdomain(String identityZoneSubdomain) {
			this.identityZoneSubdomain = identityZoneSubdomain;
		}

		public String getIdentityZoneId() {
			return identityZoneId;
		}

		public void setIdentityZoneId(String identityZoneId) {
			this.identityZoneId = identityZoneId;
		}
	}
}
