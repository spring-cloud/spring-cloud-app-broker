/*
 * Copyright 2016-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import org.springframework.http.HttpEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class AppManagementAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_1 = "app-1";

	private static final String APP_2 = "app-2";

	private static final String SI_NAME = "si-managed";

	private final WebClient webClient = getSslIgnoringWebClient();

	@BeforeEach
	void setUp() {
		StepVerifier.create(cloudFoundryService.deleteServiceInstance(SI_NAME))
			.verifyComplete();

		StepVerifier.create(cloudFoundryService.createServiceInstance(PLAN_NAME, APP_SERVICE_NAME, SI_NAME, null))
			.verifyComplete();

		StepVerifier.create(cloudFoundryService.getServiceInstance(SI_NAME))
			.assertNext(serviceInstance -> assertThat(serviceInstance.getStatus()).isEqualTo("succeeded"))
			.verifyComplete();
	}

	@AfterEach
	void cleanUp() {
		StepVerifier.create(cloudFoundryService.deleteServiceInstance(SI_NAME))
			.verifyComplete();

		StepVerifier.create(getApplications())
			.verifyError();
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH
	})
	void stopApps() {
		StepVerifier.create(manageApps("stop"))
			.assertNext(result -> assertThat(result).contains("stopping"))
			.verifyComplete();

		StepVerifier.create(getApplications())
			.assertNext(apps -> assertThat(apps).extracting("runningInstances").containsOnly(0))
			.verifyComplete();
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH
	})
	void startApps() {
		StepVerifier.create(cloudFoundryService.stopApplication(APP_1)
			.then(cloudFoundryService.stopApplication(APP_2)))
			.verifyComplete();

		StepVerifier.create(getApplications())
			.assertNext(apps -> assertThat(apps).extracting("runningInstances").containsOnly(0))
			.verifyComplete();

		StepVerifier.create(manageApps("start"))
			.assertNext(result -> assertThat(result).contains("starting"))
			.verifyComplete();

		StepVerifier.create(getApplications())
			.assertNext(apps -> assertThat(apps).extracting("runningInstances").containsOnly(1))
			.verifyComplete();
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH
	})
	void restartApps() {
		List<ApplicationDetail> apps = getApplications().block();
		Date originallySince1 = apps.get(0).getInstanceDetails().get(0).getSince();
		Date originallySince2 = apps.get(1).getInstanceDetails().get(0).getSince();

		StepVerifier.create(manageApps("restart"))
			.assertNext(result -> assertThat(result).contains("restarting"))
			.verifyComplete();

		List<ApplicationDetail> restagedApps = getApplications().block();
		Date since1 = restagedApps.get(0).getInstanceDetails().get(0).getSince();
		Date since2 = restagedApps.get(1).getInstanceDetails().get(0).getSince();
		assertThat(restagedApps).extracting("runningInstances").containsOnly(1);
		assertThat(since1).isAfter(originallySince1);
		assertThat(since2).isAfter(originallySince2);
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH
	})
	void restageApps() throws Exception {
		List<ApplicationDetail> apps = getApplications().block();
		Date originallySince1 = apps.get(0).getInstanceDetails().get(0).getSince();
		Date originallySince2 = apps.get(1).getInstanceDetails().get(0).getSince();
		assertThat(apps).extracting("runningInstances").containsOnly(1);

		StepVerifier.create(manageApps("restage"))
			.assertNext(result -> assertThat(result).contains("restaging"))
			.verifyComplete();

		List<ApplicationDetail> restagedApps = getApplications().block();
		Date since1 = restagedApps.get(0).getInstanceDetails().get(0).getSince();
		Date since2 = restagedApps.get(1).getInstanceDetails().get(0).getSince();
		assertThat(restagedApps).extracting("runningInstances").containsOnly(1);
		assertThat(since1).isAfter(originallySince1);
		assertThat(since2).isAfter(originallySince2);
	}

	private Mono<List<ApplicationDetail>> getApplications() {
		return Flux.merge(cloudFoundryService.getApplication(APP_1),
			cloudFoundryService.getApplication(APP_2))
			.parallel()
			.runOn(Schedulers.parallel())
			.sequential()
			.collectList();
	}

	private Mono<String> manageApps(String operation) {
		return cloudFoundryService.getServiceInstance(SI_NAME)
			.map(ServiceInstance::getId)
			.flatMap(serviceInstanceId -> cloudFoundryService.getApplicationRoute(TEST_BROKER_APP_NAME)
				.flatMap(appRoute -> webClient.get()
					.uri(URI.create(appRoute + "/" + operation +  "/" + serviceInstanceId))
					.exchange()
					.flatMap(clientResponse -> clientResponse.toEntity(String.class))
					.map(HttpEntity::getBody)));
	}

	private WebClient getSslIgnoringWebClient() {
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient
				.create()
				.secure(t -> {
					try {
						t.sslContext(SslContextBuilder
							.forClient()
							.trustManager(InsecureTrustManagerFactory.INSTANCE)
							.build());
					}
					catch (SSLException e) {
						e.printStackTrace();
					}
				})))
			.build();
	}
}