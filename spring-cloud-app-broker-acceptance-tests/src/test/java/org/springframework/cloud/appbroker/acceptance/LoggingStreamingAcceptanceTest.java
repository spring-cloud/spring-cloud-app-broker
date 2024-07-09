/*
 * Copyright 2002-2020 the original author or authors
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

package org.springframework.cloud.appbroker.acceptance;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingStreamingAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_CREATE_1 = "app-logging-stream-1";

	private static final String SI_NAME = "si-logging-stream";

	private static final String SUFFIX = "logging-stream-instance";

	private static final String APP_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	@Override
	protected String appServiceName() {
		return APP_SERVICE_NAME;
	}

	@Override
	protected String backingServiceName() {
		return BACKING_SERVICE_NAME;
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_CREATE_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance",
		"spring.cloud.appbroker.deployer.cloudfoundry.properties.stack=cflinuxfs4"
	})
	void shouldStreamBackingApplicationLogs() {
		Mono<Object> createServiceInstanceMono = Mono.fromRunnable(() ->
				CompletableFuture.runAsync(() ->
					createServiceInstance(SI_NAME)))
			.subscribeOn(Schedulers.boundedElastic());

		Mono<String> logStreamingMono = Mono.defer(() -> callCLICommand(
			List.of("cf", "service-logs", SI_NAME, "--skip-ssl-validation")
		)).subscribeOn(Schedulers.boundedElastic());

		StepVerifier.create(createServiceInstanceMono
				.then(Mono.delay(Duration.ofSeconds(20)).then())
				.then(getServiceInstanceMono(SI_NAME).retry(5))
				.then(Mono.zip(logStreamingMono, logStreamingMono))
			)
			.assertNext(tuple -> {
				assertThat(tuple.getT1()).isNotEmpty();
				assertThat(tuple.getT1()).contains("Connected, tailing logs for service instance");
				assertThat(tuple.getT1()).contains("[STG/0]");
				assertThat(tuple.getT1()).doesNotContain("websocket: close");

				assertThat(tuple.getT2()).isNotEmpty();
				assertThat(tuple.getT2()).contains("Connected, tailing logs for service instance");
				assertThat(tuple.getT2()).contains("[STG/0]");
				assertThat(tuple.getT2()).doesNotContain("websocket: close");
			})
			.verifyComplete();
	}

	@AfterEach
	void tearDown() {
		deleteServiceInstance(SI_NAME);
	}

}
