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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityBasicAuthCredentialProviderFactory.SPRING_SECURITY_KEY;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityBasicAuthCredentialProviderFactory.SPRING_SECURITY_USER_KEY;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityBasicAuthCredentialProviderFactory.SPRING_SECURITY_USER_NAME_KEY;
import static org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityBasicAuthCredentialProviderFactory.SPRING_SECURITY_USER_PASSWORD_KEY;

@ExtendWith(MockitoExtension.class)
class SpringSecurityBasicAuthCredentialProviderFactoryTest {

	@Mock
	private CredentialGenerator credentialGenerator;

	private CredentialProvider provider;

	@BeforeEach
	void setUp() {
		provider = new SpringSecurityBasicAuthCredentialProviderFactory(credentialGenerator)
			.createWithConfig(config -> {
				config.setLength(8);
				config.setIncludeUppercaseAlpha(true);
				config.setIncludeLowercaseAlpha(false);
				config.setIncludeNumeric(true);
				config.setIncludeSpecial(false);
			});
	}

	@Test
	@SuppressWarnings("unchecked")
	void addCredentials() {
		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		when(credentialGenerator.generateUser(backingApplication.getName(), "service-instance-id",
			8, true, false, true, false))
			.thenReturn(Pair.of("username", "password"));

		StepVerifier
			.create(provider.addCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		Map<String, Object> environment = backingApplication.getEnvironment();
		Map<String, Object> userProperties = MapUtils.getNestedMap(environment,
			SPRING_SECURITY_KEY, SPRING_SECURITY_USER_KEY);

		assertThat(userProperties)
			.containsEntry(SPRING_SECURITY_USER_NAME_KEY, "username")
			.containsEntry(SPRING_SECURITY_USER_PASSWORD_KEY, "password");
	}

	@Test
	void deleteCredentials() {
		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		StepVerifier
			.create(provider.deleteCredentials(backingApplication, "service-instance-id"))
			.expectNext(backingApplication)
			.verifyComplete();

		verify(credentialGenerator).deleteUser(backingApplication.getName(), "service-instance-id");
		verifyNoMoreInteractions(credentialGenerator);
	}
}