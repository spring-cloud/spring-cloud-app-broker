/*
 * Copyright 2016-2018. the original author or authors.
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
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.appbroker.acceptance.fixtures.uaa.UaaService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {
	CloudFoundryClientConfiguration.class,
	CloudFoundryService.class,
	UaaService.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(BrokerPropertiesParameterResolver.class)
@EnableConfigurationProperties(AcceptanceTestProperties.class)
class CloudFoundryAcceptanceTest {

	private static final String SAMPLE_BROKER_APP_NAME = "sample-broker";
	private static final String SERVICE_BROKER_NAME = "sample-broker-name";
	private static final String SERVICE_NAME = "example";
	private static final String PLAN_NAME = "standard";
	private static final String SERVICE_INSTANCE_NAME = "my-service";

	@Autowired
	private CloudFoundryService cloudFoundryService;

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

	private Mono<Void> initializeBroker(String... backingAppProperties) {
		return cleanup()
			.then(
				cloudFoundryService
					.getOrCreateDefaultOrganization()
					.then(cloudFoundryService.getOrCreateDefaultSpace())
					.then(cloudFoundryService.pushBrokerApp(SAMPLE_BROKER_APP_NAME, getSampleBrokerAppPath(), backingAppProperties))
					.then(cloudFoundryService.createServiceBroker(SERVICE_BROKER_NAME, SAMPLE_BROKER_APP_NAME))
					.then(cloudFoundryService.enableServiceBrokerAccess(SERVICE_NAME)));
	}

	private Mono<Void> cleanup() {
		return cloudFoundryService
			.deleteServiceInstance(SERVICE_INSTANCE_NAME)
			.then(cloudFoundryService.deleteServiceBroker(SERVICE_BROKER_NAME))
			.then(cloudFoundryService.deleteApp(SAMPLE_BROKER_APP_NAME));
	}

	void setupServiceBrokerForService(String serviceName) {
		String[] properties = new String[] {
			"spring.cloud.openservicebroker.catalog.services[0].id=" + serviceName,
			"spring.cloud.openservicebroker.catalog.services[0].name=" + serviceName,
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=" + serviceName,

			"spring.cloud.appbroker.services[0].service-name=" + serviceName,
			"spring.cloud.appbroker.services[0].plan-name=standard",
			"spring.cloud.appbroker.services[0].apps[0].name=app-" + serviceName,
			"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar"};
		blockingSubscribe(
			cloudFoundryService.pushBrokerApp(serviceName, getSampleBrokerAppPath(), properties)
							   .then(cloudFoundryService.createServiceBroker(serviceName, serviceName))
							   .then(cloudFoundryService.enableServiceBrokerAccess(serviceName)));
	}

	void deleteServiceBrokerForService(String serviceName) {
		blockingSubscribe(
			cloudFoundryService.deleteServiceBroker(serviceName)
							   .then(cloudFoundryService.deleteApp(serviceName)));
	}

	void createServiceInstance() {
		createServiceInstanceWithParameters(Collections.emptyMap());
	}

	void createServiceInstance(String planName,
							   String serviceName,
							   String serviceInstanceName,
							   Map<String, Object> parameters) {
		blockingSubscribe(cloudFoundryService
			.createServiceInstance(
				planName,
				serviceName,
				serviceInstanceName,
				parameters));
	}

	void createServiceInstanceWithParameters(Map<String, Object> parameters) {
		createServiceInstance(PLAN_NAME, SERVICE_NAME, SERVICE_INSTANCE_NAME, parameters);
	}

	void updateServiceInstance(Map<String, Object> parameters) {
		blockingSubscribe(cloudFoundryService.updateServiceInstance(SERVICE_INSTANCE_NAME, parameters));
	}

	void deleteServiceInstance() {
		deleteServiceInstance(SERVICE_INSTANCE_NAME);
	}

	void deleteServiceInstance(String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceInstance(serviceInstanceName));
	}

	Optional<ServiceInstanceSummary> getServiceInstance() {
		return getServiceInstanceMono().blockOptional();
	}

	Optional<ServiceInstanceSummary> getServiceInstance(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName).blockOptional();
	}

	Optional<ServiceInstanceSummary> getServiceInstance(String serviceInstanceName, String space) {
		return getServiceInstanceMono(serviceInstanceName, space).blockOptional();
	}

	Mono<ServiceInstanceSummary> getServiceInstanceMono() {
		return getServiceInstanceMono(SERVICE_INSTANCE_NAME);
	}

	private Mono<ServiceInstanceSummary> getServiceInstanceMono(String serviceInstanceName) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName);
	}

	private Mono<ServiceInstanceSummary> getServiceInstanceMono(String serviceInstanceName, String space) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName, space);
	}

	Optional<ApplicationSummary> getApplicationSummaryByName(String appName) {
		return cloudFoundryService
			.getApplications()
			.flatMapMany(Flux::fromIterable)
			.filter(applicationSummary -> appName.equals(applicationSummary.getName()))
			.next()
			.blockOptional();
	}

	Optional<ApplicationSummary> getApplicationSummaryByNameAndSpace(String appName, String space) {
		return cloudFoundryService.getApplicationSummaryByNameAndSpace(appName, space).blockOptional();
	}

	ApplicationEnvironments getApplicationEnvironmentByName(String appName) {
		return cloudFoundryService.getApplicationEnvironmentByAppName(appName).block();
	}

	DocumentContext getSpringAppJsonByName(String appName) {
		ApplicationEnvironments env = getApplicationEnvironmentByName(appName);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	DocumentContext getSpringAppJsonByNameAndSpace(String appName, String space) {
		ApplicationEnvironments env = getApplicationEnvironmentByNameAndSpace(appName, space);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	ApplicationEnvironments getApplicationEnvironmentByNameAndSpace(String appName, String space) {
		return cloudFoundryService.getApplicationEnvironmentByAppNameAndSpace(appName, space).block();
	}

	List<String> getSpaces() {
		return cloudFoundryService.getSpaces().block();
	}

	Optional<GetClientResponse> getUaaClient(String clientId) {
		return uaaService.getUaaClient(clientId)
			.blockOptional();
	}

	private Path getSampleBrokerAppPath() {
		return Paths.get(acceptanceTestProperties.getSampleBrokerAppPath(), "");
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