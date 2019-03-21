/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.cloud.appbroker.acceptance.fixtures.uaa.UaaService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
	CloudFoundryClientConfiguration.class,
	CloudFoundryService.class,
	UaaService.class,
	HealthListener.class,
	RestTemplate.class
})
@ExtendWith(SpringExtension.class)
@ExtendWith(BrokerPropertiesParameterResolver.class)
@EnableConfigurationProperties(AcceptanceTestProperties.class)
class CloudFoundryAcceptanceTest {

	static final String TEST_BROKER_APP_NAME = "test-broker-app";
	private static final String SERVICE_BROKER_NAME = "test-broker";
	
	static final String APP_SERVICE_NAME = "app-service";
	static final String BACKING_SERVICE_NAME = "backing-service";
	static final String PLAN_NAME = "standard";
	static final String BACKING_APP_PATH = "classpath:backing-app.jar";

	@Autowired
	protected CloudFoundryService cloudFoundryService;

	@Autowired
	private UaaService uaaService;

	@Autowired
	private AcceptanceTestProperties acceptanceTestProperties;

	@BeforeEach
	void setUp(BrokerProperties brokerProperties) {
		blockingSubscribe(initializeBroker(brokerProperties.getProperties()));
	}

	@BeforeEach
	void configureJsonPath() {
		Configuration.setDefaults(new Configuration.Defaults() {
			private final JsonProvider jacksonJsonProvider = new JacksonJsonProvider();
			private final MappingProvider jacksonMappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jacksonJsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return jacksonMappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}

	@AfterEach
	void tearDown() {
		blockingSubscribe(cleanup());
	}

	private Mono<Void> initializeBroker(String... appBrokerProperties) {
		return cleanup()
			.then(
				cloudFoundryService
					.getOrCreateDefaultOrganization()
					.then(cloudFoundryService.getOrCreateDefaultSpace())
					.then(cloudFoundryService.pushBrokerApp(TEST_BROKER_APP_NAME, getTestBrokerAppPath(), appBrokerProperties))
					.then(cloudFoundryService.createServiceBroker(SERVICE_BROKER_NAME, TEST_BROKER_APP_NAME))
					.then(cloudFoundryService.enableServiceBrokerAccess(APP_SERVICE_NAME))
					.then(cloudFoundryService.enableServiceBrokerAccess(BACKING_SERVICE_NAME)));
	}

	private Mono<Void> cleanup() {
		return cloudFoundryService.deleteServiceBroker(SERVICE_BROKER_NAME)
			.then(cloudFoundryService.deleteApp(TEST_BROKER_APP_NAME));
	}

	void createServiceInstance(String serviceInstanceName) {
		createServiceInstance(serviceInstanceName, Collections.emptyMap());
	}

	void createServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		createServiceInstance(APP_SERVICE_NAME, PLAN_NAME, serviceInstanceName, parameters);
	}

	void createServiceInstance(String serviceName,
							   String planName,
							   String serviceInstanceName,
							   Map<String, Object> parameters) {
		cloudFoundryService.createServiceInstance(planName, serviceName, serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap(serviceInstance -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Create service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	void updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		cloudFoundryService.updateServiceInstance(serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap(serviceInstance -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Update service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	void deleteServiceInstance(String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceInstance(serviceInstanceName));
	}

	ServiceInstance getServiceInstance(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName).block();
	}

	ServiceInstance getServiceInstance(String serviceInstanceName, String space) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName, space).block();
	}

	String getServiceInstanceGuid(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName)
			.map(ServiceInstance::getId)
			.block();
	}

	private Mono<ServiceInstance> getServiceInstanceMono(String serviceInstanceName) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName);
	}

	Optional<ApplicationSummary> getApplicationSummary(String appName) {
		return cloudFoundryService
			.getApplications()
			.flatMapMany(Flux::fromIterable)
			.filter(applicationSummary -> appName.equals(applicationSummary.getName()))
			.next()
			.blockOptional();
	}

	Optional<ApplicationSummary> getApplicationSummary(String appName, String space) {
		return cloudFoundryService.getApplication(appName, space).blockOptional();
	}

	ApplicationEnvironments getApplicationEnvironment(String appName) {
		return cloudFoundryService.getApplicationEnvironment(appName).block();
	}

	ApplicationEnvironments getApplicationEnvironment(String appName, String space) {
		return cloudFoundryService.getApplicationEnvironment(appName, space).block();
	}

	String getTestBrokerAppRoute() {
		return cloudFoundryService.getApplicationRoute(TEST_BROKER_APP_NAME).block();
	}

	DocumentContext getSpringAppJson(String appName) {
		ApplicationEnvironments env = getApplicationEnvironment(appName);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	DocumentContext getSpringAppJson(String appName, String space) {
		ApplicationEnvironments env = getApplicationEnvironment(appName, space);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	List<String> getSpaces() {
		return cloudFoundryService.getSpaces().block();
	}

	Optional<GetClientResponse> getUaaClient(String clientId) {
		return uaaService.getUaaClient(clientId)
			.blockOptional();
	}

	private Path getTestBrokerAppPath() {
		return Paths.get(acceptanceTestProperties.getBrokerAppPath(), "");
	}

	private <T> void blockingSubscribe(Mono<? super T> publisher) {
		CountDownLatch latch = new CountDownLatch(1);
		publisher.subscribe(System.out::println, t -> {
			t.printStackTrace();
			latch.countDown();
		}, latch::countDown);
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}