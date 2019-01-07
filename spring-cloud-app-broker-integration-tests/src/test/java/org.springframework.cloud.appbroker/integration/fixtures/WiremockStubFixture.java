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

package org.springframework.cloud.appbroker.integration.fixtures;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Metadata;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class WiremockStubFixture {
	private static final String RESPONSES_RESOURCE_PATH = "classpath:/responses/";

	protected WireMock wireMock;

	@Autowired
	private ResourceLoader resourceLoader;

	protected WiremockStubFixture(int port) {
		wireMock = WireMock.create()
			.port(port)
			.build();
	}

	protected StubMapping stubFor(MappingBuilder mappingBuilder) {
		return givenThat(mappingBuilder);
	}

	protected StubMapping givenThat(MappingBuilder mappingBuilder) {
		return wireMock.register(mappingBuilder);
	}

	CloudControllerStubFixture.StringReplacementPair replace(String regex, String replacement) {
		return new CloudControllerStubFixture.StringReplacementPair(regex, replacement);
	}

	String readResponseFromFile(String fileRoot, String prefix) {
		return readTestDataFile(RESPONSES_RESOURCE_PATH + prefix + "/" + fileRoot + ".json");
	}

	private String readTestDataFile(String filePath) {
		try {
			Resource resource = resourceLoader.getResource(filePath);
			InputStreamReader reader = new InputStreamReader(resource.getInputStream());
			return new BufferedReader(reader)
				.lines()
				.collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new RuntimeException("Error loading resource from location " + filePath, e);
		}
	}

	Metadata optionalStubMapping() {
		return Metadata.metadata()
			.attr("optional", true)
			.build();
	}

	class StringReplacementPair {
		private final String regex;
		private final String replacement;

		private StringReplacementPair(String regex, String replacement) {
			this.regex = regex;
			this.replacement = replacement;
		}

		public String getRegex() {
			return regex;
		}

		public String getReplacement() {
			return replacement;
		}
	}
}
