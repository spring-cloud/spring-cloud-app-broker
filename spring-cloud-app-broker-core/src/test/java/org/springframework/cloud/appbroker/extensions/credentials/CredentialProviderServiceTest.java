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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.CredentialProviderSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialProviderServiceTest {
	@Test
	void addAndDeleteCredentials() {
		BackingApplication app1 = BackingApplication.builder()
			.name("app1")
			.credentialProviders(CredentialProviderSpec.builder()
				.name("provider1")
				.build())
			.build();
		BackingApplication app2 = BackingApplication.builder()
			.name("app2")
			.credentialProviders(CredentialProviderSpec.builder()
					.name("provider1")
					.build(),
				CredentialProviderSpec.builder()
					.name("provider2")
					.build())
			.build();
		BackingApplications backingApplications = BackingApplications.builder()
			.backingApplication(app1)
			.backingApplication(app2)
			.build();

		TestFactory factory1 = new TestFactory("provider1");
		TestFactory factory2 = new TestFactory("provider2");

		CredentialProviderService service = new CredentialProviderService(Arrays.asList(factory1, factory2));

		StepVerifier
			.create(service.addCredentials(backingApplications, "service-instance-guid"))
			.expectNext(backingApplications)
			.verifyComplete();

		StepVerifier
			.create(service.deleteCredentials(backingApplications, "service-instance-guid"))
			.expectNext(backingApplications)
			.verifyComplete();

		assertThat(app1.getEnvironment()).containsKey("provider1-added");
		assertThat(app1.getEnvironment()).containsKey("provider1-deleted");
		assertThat(app1.getEnvironment()).doesNotContainKey("provider2-added");
		assertThat(app1.getEnvironment()).doesNotContainKey("provider2-deleted");

		assertThat(app2.getEnvironment()).containsKey("provider1-added");
		assertThat(app2.getEnvironment()).containsKey("provider1-deleted");
		assertThat(app2.getEnvironment()).containsKey("provider2-added");
		assertThat(app2.getEnvironment()).containsKey("provider2-deleted");
	}

	@Test
	void addAndDeleteCredentialsWithNoBackingApps() {
		CredentialProviderService service = new CredentialProviderService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications.builder()
			.build();

		StepVerifier
			.create(service.addCredentials(backingApplications, "service-instance-guid"))
			.expectNext(backingApplications)
			.verifyComplete();

		StepVerifier
			.create(service.deleteCredentials(backingApplications, "service-instance-guid"))
			.expectNext(backingApplications)
			.verifyComplete();
	}

	@Test
	void addAndDeleteCredentialsWithNoProviders() {
		CredentialProviderService service = new CredentialProviderService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications.builder()
			.backingApplication(BackingApplication.builder().build())
			.build();

		StepVerifier
			.create(service.addCredentials(backingApplications, "service-instance-guid"))
			.expectNext(backingApplications)
			.verifyComplete();

		StepVerifier
			.create(service.deleteCredentials(backingApplications, "service-instance-guid"))
			.expectNext(backingApplications)
			.verifyComplete();
	}

	public class TestFactory extends CredentialProviderFactory<Object> {
		private String name;

		TestFactory(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public CredentialProvider create(Object config) {
			return new CredentialProvider() {
				@Override
				public Mono<BackingApplication> addCredentials(BackingApplication backingApplication, String serviceInstanceGuid) {
					backingApplication.addEnvironment(name + "-added", "done");
					return Mono.just(backingApplication);
				}

				@Override
				public Mono<BackingApplication> deleteCredentials(BackingApplication backingApplication, String serviceInstanceGuid) {
					backingApplication.addEnvironment(name + "-deleted", "done");
					return Mono.just(backingApplication);
				}
			};
		}
	}
}