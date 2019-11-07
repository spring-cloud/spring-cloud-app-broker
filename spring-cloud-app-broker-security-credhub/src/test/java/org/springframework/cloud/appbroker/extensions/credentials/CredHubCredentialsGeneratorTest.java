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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.core.credential.CredHubCredentialOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialType;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.password.PasswordCredential;
import org.springframework.credhub.support.password.PasswordParameters;
import org.springframework.credhub.support.user.UserCredential;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CredHubCredentialsGeneratorTest {

	@Mock
	private CredHubOperations credHubOperations;

	@Mock
	private CredHubCredentialOperations credHubCredentialOperations;

	private CredHubCredentialsGenerator generator;

	@BeforeEach
	void setUp() {
		this.generator = new CredHubCredentialsGenerator(credHubOperations);
	}

	@Test
	void passwordParameters() {
		PasswordParameters params = ReflectionTestUtils
			.invokeMethod(generator, "passwordParameters", 42, false, false, false, false);
		assertThat(params.getLength()).isEqualTo(42);
		assertThat(params.getExcludeUpper()).isTrue();
		assertThat(params.getExcludeLower()).isTrue();
		assertThat(params.getExcludeNumber()).isTrue();
		assertThat(params.getIncludeSpecial()).isFalse();
	}

	@Test
	void generateUser() {

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		given(this.credHubCredentialOperations.generate(any()))
			.willReturn(new CredentialDetails<>("id",
				new SimpleCredentialName("app-service"), CredentialType.PASSWORD,
				new UserCredential("username", "password")));

		StepVerifier.create(generator.generateUser("foo", "bar", "hello", 12, false, false, false, false))
			.assertNext(tuple2 -> {
				assertThat(tuple2.getT1()).isEqualTo("username");
				assertThat(tuple2.getT2()).isEqualTo("password");
			})
			.verifyComplete();
	}

	@Test
	void generateString() {

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		given(this.credHubCredentialOperations.generate(any()))
			.willReturn(new CredentialDetails<>("id",
				new SimpleCredentialName("app-service"), CredentialType.PASSWORD,
				new PasswordCredential("password")));

		StepVerifier.create(generator.generateString("foo", "bar", "hello", 12, false, false, false, false))
			.assertNext(password -> assertThat(password).isEqualTo("password"))
			.verifyComplete();
	}

	@Test
	void deleteUser() {
		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		StepVerifier.create(generator.deleteUser("foo", "bar", "hello"))
			.verifyComplete();

		verify(credHubCredentialOperations).deleteByName(new SimpleCredentialName("foo", "bar", "hello"));
		verifyNoMoreInteractions(credHubOperations);
		verifyNoMoreInteractions(credHubCredentialOperations);
	}

	@Test
	void deleteString() {
		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		StepVerifier.create(generator.deleteString("foo", "bar", "hello"))
			.verifyComplete();

		verify(credHubCredentialOperations).deleteByName(new SimpleCredentialName("foo", "bar", "hello"));
		verifyNoMoreInteractions(credHubOperations);
		verifyNoMoreInteractions(credHubCredentialOperations);
	}

}
