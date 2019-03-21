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

package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.ParametersTransformerSpec;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class BackingApplicationsParametersTransformationServiceTest {

	@Test
	void transformParametersWithNoBackingApps() {
		BackingApplicationsParametersTransformationService service =
			new BackingApplicationsParametersTransformationService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications.builder()
																	 .build();

		StepVerifier
			.create(service.transformParameters(backingApplications, new HashMap<>()))
			.expectNext(backingApplications)
			.verifyComplete();
	}

	@Test
	void transformParametersWithNoTransformers() {
		BackingApplicationsParametersTransformationService service =
			new BackingApplicationsParametersTransformationService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications
			.builder()
			.backingApplication(BackingApplication.builder().build())
			.build();

		StepVerifier
			.create(service.transformParameters(backingApplications, new HashMap<>()))
			.expectNext(backingApplications)
			.verifyComplete();
	}

	@Test
	void transformParametersWithUnknownTransformer() {
		BackingApplicationsParametersTransformationService service =
			new BackingApplicationsParametersTransformationService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications
			.builder()
			.backingApplication(BackingApplication
				.builder()
				.name("misconfigured-app")
				.parameterTransformers(ParametersTransformerSpec.builder()
																.name("unknown-transformer")
																.build())
				.build())
			.build();

		StepVerifier
			.create(service.transformParameters(backingApplications, new HashMap<>()))
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(ServiceBrokerException.class)
				.hasMessageContaining("unknown-transformer"))
			.verify();
	}

	@Test
	void transformParametersWithTransformers() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("key1", "value1");
		parameters.put("key2", "value2");

		BackingApplication app1 = BackingApplication.builder()
													.name("app1")
													.parameterTransformers(ParametersTransformerSpec
														.builder()
														.name("transformer1")
														.build())
													.build();
		BackingApplication app2 = BackingApplication
			.builder()
			.name("app2")
			.parameterTransformers(ParametersTransformerSpec
					.builder()
					.name("transformer1")
					.arg("arg1", "value1")
					.arg("arg2", 5)
					.build(),
				ParametersTransformerSpec
					.builder()
					.name("transformer2")
					.build())
			.build();
		BackingApplications backingApplications = BackingApplications.builder()
																	 .backingApplication(app1)
																	 .backingApplication(app2)
																	 .build();

		TestFactory factory1 = new TestFactory("transformer1");
		TestFactory factory2 = new TestFactory("transformer2");

		BackingApplicationsParametersTransformationService service =
			new BackingApplicationsParametersTransformationService(
				Arrays.asList(factory1, factory2));

		StepVerifier
			.create(service.transformParameters(backingApplications, parameters))
			.expectNextMatches(transformedBackingApplications -> {
				Map<String, Object> app1ExpectedTransformedEnvironment = new HashMap<>(app1.getEnvironment());
				app1ExpectedTransformedEnvironment.put("0", "transformer1");

				Map<String, Object> app2ExpectedTransformedEnvironment = new HashMap<>(app2.getEnvironment());
				app2ExpectedTransformedEnvironment.put("0", "transformer1");
				app2ExpectedTransformedEnvironment.put("1", "transformer2");

				assertAll("unexpected transformation results",
					() -> assertThat(transformedBackingApplications.size()).isEqualTo(backingApplications.size()),
					() -> assertThat(transformedBackingApplications.get(0).getEnvironment()).isEqualTo(app1ExpectedTransformedEnvironment),
					() -> assertThat(transformedBackingApplications.get(1).getEnvironment()).isEqualTo(app2ExpectedTransformedEnvironment)
				);

				return true;
			})
			.verifyComplete();

		assertThat(factory1.getActualParameters()).isEqualTo(parameters);
		assertThat(factory2.getActualParameters()).isEqualTo(parameters);

		assertThat(factory1.getActualConfig()).isEqualTo(new Config("value1", 5));
		assertThat(factory2.getActualConfig()).isEqualTo(new Config(null, null));
	}

	public static class TestFactory extends ParametersTransformerFactory<BackingApplication, Config> {

		private final String name;

		private Map<String, Object> actualParameters;
		private Config actualConfig;

		TestFactory(String name) {
			super(Config.class);
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public ParametersTransformer<BackingApplication> create(Config config) {
			this.actualConfig = config;
			return this::doTransform;
		}

		private Mono<BackingApplication> doTransform(BackingApplication backingApplication,
													 Map<String, Object> parameters) {
			this.actualParameters = parameters;
			backingApplication.addEnvironment(Integer.toString(backingApplication.getEnvironment().size()), getName());
			return Mono.just(backingApplication);
		}

		Map<String, Object> getActualParameters() {
			return actualParameters;
		}

		Config getActualConfig() {
			return actualConfig;
		}
	}

	@SuppressWarnings("unused")
	public static class Config {

		private String arg1;
		private Integer arg2;

		private Config() {
		}

		Config(String arg1, Integer arg2) {
			this.arg1 = arg1;
			this.arg2 = arg2;
		}

		public String getArg1() {
			return arg1;
		}

		public void setArg1(String arg1) {
			this.arg1 = arg1;
		}

		public int getArg2() {
			return arg2;
		}

		public void setArg2(int arg2) {
			this.arg2 = arg2;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Config)) {
				return false;
			}
			Config config = (Config) o;
			return Objects.equals(arg2, config.arg2) &&
				Objects.equals(arg1, config.arg1);
		}

		@Override
		public int hashCode() {
			return Objects.hash(arg1, arg2);
		}

		@Override
		public String toString() {
			return "Config{" +
				"arg1='" + arg1 + '\'' +
				", arg2=" + arg2 +
				'}';
		}
	}
}