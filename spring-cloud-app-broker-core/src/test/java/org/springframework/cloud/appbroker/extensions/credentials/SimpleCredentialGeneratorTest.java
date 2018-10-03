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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCredentialGeneratorTest {
	@Test
	void generateString() {
		SimpleCredentialGenerator generator = new SimpleCredentialGenerator();

		assertThat(generator.generateString(null, null, 12, true, false, false, false))
			.matches("^[A-Z]{12}$");
		assertThat(generator.generateString(null, null, 12, false, true, false, false))
			.matches("^[a-z]{12}$");
		assertThat(generator.generateString(null, null, 12, false, false, true, false))
			.matches("^[0-9]{12}$");
		assertThat(generator.generateString(null, null, 12, false, false, false, true))
			.matches("^[\\p{Punct}]{12}$");

		assertThat(generator.generateString(null, null, 12, true, true, true, true))
			.matches("^[a-zA-Z0-9\\p{Punct}]{12}$");
		assertThat(generator.generateString(null, null, 12, false, false, false, false))
			.matches("^[a-zA-Z0-9\\p{Punct}]{12}$");
	}

	@Test
	void generateUser() {
		SimpleCredentialGenerator generator = new SimpleCredentialGenerator();

		Pair<String, String> user = generator.generateUser(null, null, 10, true, true, true, true);

		assertThat(user.getLeft().length()).isEqualTo(10);
		assertThat(user.getRight().length()).isEqualTo(10);
	}
}