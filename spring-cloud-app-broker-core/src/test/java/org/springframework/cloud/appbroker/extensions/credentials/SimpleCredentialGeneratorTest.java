/*
 * Copyright 2016-2018 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCredentialGeneratorTest {

	@Test
	void generateString() {
		SimpleCredentialGenerator generator = new SimpleCredentialGenerator();

		StepVerifier.create(generator.generateString(null, null, null, 12, true, false, false, false))
					.assertNext(s -> assertThat(s).matches("^[A-Z]{12}$"))
					.verifyComplete();

		StepVerifier.create(generator.generateString(null, null, null, 12, false, true, false, false))
					.assertNext(s -> assertThat(s).matches("^[a-z]{12}$"))
					.verifyComplete();

		StepVerifier.create(generator.generateString(null, null, null, 12, false, false, true, false))
					.assertNext(s -> assertThat(s).matches("^[0-9]{12}$"))
					.verifyComplete();

		StepVerifier.create(generator.generateString(null, null, null, 12, false, false, false, true))
					.assertNext(s -> assertThat(s).matches("^[\\p{Punct}]{12}$"))
					.verifyComplete();

		StepVerifier.create(generator.generateString(null, null, null, 12, true, true, true, true))
					.assertNext(s -> assertThat(s).matches("^[a-zA-Z0-9\\p{Punct}]{12}$"))
					.verifyComplete();

		StepVerifier.create(generator.generateString(null, null, null, 12, false, false, false, false))
					.assertNext(s -> assertThat(s).matches("^[a-zA-Z0-9\\p{Punct}]{12}$"))
					.verifyComplete();
	}

	@Test
	void generateUser() {
		SimpleCredentialGenerator generator = new SimpleCredentialGenerator();

		StepVerifier.create(generator.generateUser(null, null, null, 10, true, true, true, true))
					.assertNext(user -> {
						assertThat(user.getT1().length()).isEqualTo(10);
						assertThat(user.getT2().length()).isEqualTo(10);
					})
					.verifyComplete();
	}
}