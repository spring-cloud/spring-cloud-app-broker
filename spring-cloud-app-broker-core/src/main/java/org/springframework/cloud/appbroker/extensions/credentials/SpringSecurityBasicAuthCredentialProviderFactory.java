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

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import org.springframework.cloud.appbroker.deployer.BackingApplication;

public class SpringSecurityBasicAuthCredentialProviderFactory extends
	CredentialProviderFactory<CredentialGenerationConfig> {

	private static final String CREDENTIAL_DESCRIPTOR = "basic";

	static final String SPRING_SECURITY_USER_NAME_KEY = "spring.security.user.name";
	static final String SPRING_SECURITY_USER_PASSWORD_KEY = "spring.security.user.password";

	private final CredentialGenerator credentialGenerator;

	public SpringSecurityBasicAuthCredentialProviderFactory(CredentialGenerator credentialGenerator) {
		super(CredentialGenerationConfig.class);
		this.credentialGenerator = credentialGenerator;
	}

	@Override
	public CredentialProvider create(CredentialGenerationConfig config) {
		return new CredentialProvider() {
			@Override
			public Mono<BackingApplication> addCredentials(BackingApplication backingApplication,
														   String serviceInstanceGuid) {
				return generateCredentials(config, backingApplication, serviceInstanceGuid)
					.flatMap(user -> addUserToEnvironment(backingApplication, user))
					.thenReturn(backingApplication);
			}

			@Override
			public Mono<BackingApplication> deleteCredentials(BackingApplication backingApplication,
															  String serviceInstanceGuid) {
				return credentialGenerator.deleteUser(backingApplication.getName(), serviceInstanceGuid, CREDENTIAL_DESCRIPTOR)
										  .thenReturn(backingApplication);
			}
		};
	}

	private Mono<Tuple2<String, String>> generateCredentials(CredentialGenerationConfig config,
															 BackingApplication backingApplication,
															 String serviceInstanceGuid) {
		return credentialGenerator.generateUser(backingApplication.getName(), serviceInstanceGuid, CREDENTIAL_DESCRIPTOR,
			config.getLength(), config.isIncludeUppercaseAlpha(), config.isIncludeLowercaseAlpha(),
			config.isIncludeNumeric(), config.isIncludeSpecial());
	}

	private Mono<Void> addUserToEnvironment(BackingApplication backingApplication, Tuple2<String, String> user) {
		backingApplication.addEnvironment(SPRING_SECURITY_USER_NAME_KEY, user.getT1());
		backingApplication.addEnvironment(SPRING_SECURITY_USER_PASSWORD_KEY, user.getT2());
		return Mono.empty();
	}
}
