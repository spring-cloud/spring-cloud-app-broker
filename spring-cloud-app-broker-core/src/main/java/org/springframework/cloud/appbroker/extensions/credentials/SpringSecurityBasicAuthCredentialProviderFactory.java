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

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import reactor.core.publisher.Mono;

public class SpringSecurityBasicAuthCredentialProviderFactory extends
	CredentialProviderFactory<CredentialGenerationConfig> {

	static final String SPRING_SECURITY_USER_NAME = "security.user.name";
	static final String SPRING_SECURITY_USER_PASSWORD = "security.user.password";

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
				generateCredentials(config, backingApplication, serviceInstanceGuid);
				return Mono.just(backingApplication);
			}

			@Override
			public Mono<BackingApplication> deleteCredentials(BackingApplication backingApplication,
															  String serviceInstanceGuid) {
				credentialGenerator.deleteUser(backingApplication.getName(), serviceInstanceGuid);
				return Mono.just(backingApplication);
			}
		};
	}

	private void generateCredentials(CredentialGenerationConfig config,
									 BackingApplication backingApplication,
									 String serviceInstanceGuid) {
		Pair<String, String> user =
			credentialGenerator.generateUser(backingApplication.getName(), serviceInstanceGuid,
				config.getLength(), config.isIncludeUppercaseAlpha(), config.isIncludeLowercaseAlpha(),
				config.isIncludeNumeric(), config.isIncludeSpecial());

		backingApplication.addEnvironment(SPRING_SECURITY_USER_NAME, user.getLeft());
		backingApplication.addEnvironment(SPRING_SECURITY_USER_PASSWORD, user.getRight());
	}
}
