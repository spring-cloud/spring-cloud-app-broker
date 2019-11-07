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

import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class SimpleCredentialGenerator implements CredentialGenerator {

	private static final String UPPERCASE_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static final String LOWERCASE_ALPHA = "abcdefghijklmnopqrstuvwxyz";

	private static final String DIGITS = "0123456789";

	private static final String SPECIAL_CHARACTERS = "~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?";

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public Mono<Tuple2<String, String>> generateUser(String applicationId, String serviceInstanceId, String descriptor,
		int length,
		boolean includeUppercaseAlpha, boolean includeLowercaseAlpha,
		boolean includeNumeric, boolean includeSpecial) {
		return generateString(applicationId, serviceInstanceId, descriptor, length,
			includeUppercaseAlpha, includeLowercaseAlpha, includeNumeric, includeSpecial)
			.flatMap(username -> generateString(applicationId, serviceInstanceId, descriptor, length,
				includeUppercaseAlpha, includeLowercaseAlpha, includeNumeric, includeSpecial)
				.map(password -> Tuples.of(username, password)));
	}

	@Override
	public Mono<String> generateString(String applicationId, String serviceInstanceId, String descriptor, int length,
		boolean includeUppercaseAlpha, boolean includeLowercaseAlpha,
		boolean includeNumeric, boolean includeSpecial) {
		StringBuilder builder = new StringBuilder();

		if (includeUppercaseAlpha) {
			builder.append(UPPERCASE_ALPHA);
		}

		if (includeLowercaseAlpha) {
			builder.append(LOWERCASE_ALPHA);
		}

		if (includeNumeric) {
			builder.append(DIGITS);
		}

		if (includeSpecial) {
			builder.append(SPECIAL_CHARACTERS);
		}

		if (builder.length() == 0) {
			builder.append(UPPERCASE_ALPHA)
				.append(LOWERCASE_ALPHA)
				.append(DIGITS)
				.append(SPECIAL_CHARACTERS);
		}

		char[] chars = builder.toString().toCharArray();

		return Mono.just(RandomStringUtils.random(length, 0, chars.length - 1, false, false, chars, secureRandom));
	}

}
