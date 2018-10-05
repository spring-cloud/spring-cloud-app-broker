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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.security.SecureRandom;

public class SimpleCredentialGenerator implements CredentialGenerator {

	private static final String UPPERCASE_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String LOWERCASE_ALPHA = "abcdefghijklmnopqrstuvwxyz";
	private static final String DIGITS = "0123456789";
	private static final String SPECIAL_CHARACTERS = "~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?";

	private final SecureRandom secureRandom = new SecureRandom();
	
	@Override
	public Pair<String, String> generateUser(String applicationId, String serviceInstanceId, int length,
											 boolean includeUppercaseAlpha, boolean includeLowercaseAlpha,
											 boolean includeNumeric, boolean includeSpecial) {
		return Pair.of(
			generateString(applicationId, serviceInstanceId, length,
				includeUppercaseAlpha, includeLowercaseAlpha, includeNumeric, includeSpecial),
			generateString(applicationId, serviceInstanceId, length,
				includeUppercaseAlpha, includeLowercaseAlpha, includeNumeric, includeSpecial));
	}

	@Override
	public String generateString(String applicationId, String serviceInstanceId, int length,
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

		if (builder.toString().length() == 0) {
			builder.append(UPPERCASE_ALPHA)
				.append(LOWERCASE_ALPHA)
				.append(DIGITS)
				.append(SPECIAL_CHARACTERS);
		}

		char[] chars = builder.toString().toCharArray();

		return RandomStringUtils.random(length, 0, chars.length - 1, false, false, chars, secureRandom);
	}
}
