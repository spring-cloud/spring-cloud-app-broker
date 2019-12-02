/*
 * Copyright 2002-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.credhub.core.ReactiveCredHubOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.password.PasswordCredential;
import org.springframework.credhub.support.password.PasswordParameters;
import org.springframework.credhub.support.password.PasswordParametersRequest;
import org.springframework.credhub.support.user.UserCredential;
import org.springframework.credhub.support.user.UserParametersRequest;

public class CredHubCredentialsGenerator implements CredentialGenerator {

	private final ReactiveCredHubOperations credHubOperations;

	public CredHubCredentialsGenerator(ReactiveCredHubOperations credHubOperations) {
		this.credHubOperations = credHubOperations;
	}

	@Override
	public Mono<Tuple2<String, String>> generateUser(String applicationId, String serviceInstanceId, String descriptor,
		int length, boolean includeUppercaseAlpha, boolean includeLowercaseAlpha, boolean includeNumeric,
		boolean includeSpecial) {
		return credHubOperations.credentials().generate(UserParametersRequest.builder()
			.name(new SimpleCredentialName(applicationId, serviceInstanceId, descriptor))
			.parameters(passwordParameters(length, includeUppercaseAlpha, includeLowercaseAlpha, includeNumeric,
				includeSpecial))
			.build(), UserCredential.class)
			.map(CredentialDetails::getValue)
			.map(userCredential -> Tuples.of(userCredential.getUsername(), userCredential.getPassword()));
	}

	@Override
	public Mono<String> generateString(String applicationId, String serviceInstanceId, String descriptor, int length,
		boolean includeUppercaseAlpha, boolean includeLowercaseAlpha, boolean includeNumeric,
		boolean includeSpecial) {
		return credHubOperations.credentials()
			.generate(PasswordParametersRequest.builder()
				.name(new SimpleCredentialName(applicationId, serviceInstanceId, descriptor))
				.parameters(passwordParameters(length, includeUppercaseAlpha, includeLowercaseAlpha, includeNumeric,
					includeSpecial))
				.build(), PasswordCredential.class)
			.map(CredentialDetails::getValue)
			.map(PasswordCredential::getPassword);
	}

	@Override
	public Mono<Void> deleteUser(String applicationId, String serviceInstanceId, String descriptor) {
		return credHubOperations.credentials()
			.deleteByName(new SimpleCredentialName(applicationId, serviceInstanceId, descriptor));
	}

	@Override
	public Mono<Void> deleteString(String applicationId, String serviceInstanceId, String descriptor) {
		return credHubOperations.credentials()
			.deleteByName(new SimpleCredentialName(applicationId, serviceInstanceId, descriptor));
	}

	private PasswordParameters passwordParameters(int length, boolean includeUppercaseAlpha,
		boolean includeLowercaseAlpha, boolean includeNumeric, boolean includeSpecial) {
		return PasswordParameters.builder()
			.length(length)
			.excludeUpper(!includeUppercaseAlpha)
			.excludeLower(!includeLowercaseAlpha)
			.excludeNumber(!includeNumeric)
			.includeSpecial(includeSpecial)
			.build();
	}

}
