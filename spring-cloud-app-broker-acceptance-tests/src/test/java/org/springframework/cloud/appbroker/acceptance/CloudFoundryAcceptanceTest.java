/*
 * Copyright 2002-2020 the original author or authors.
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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLException;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.cloud.appbroker.acceptance.fixtures.uaa.UaaService;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration.APP_BROKER_CLIENT_AUTHORITIES;
import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration.APP_BROKER_CLIENT_SECRET;

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
abstract class CloudFoundryAcceptanceTest {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryAcceptanceTest.class);

	protected static final String PLAN_NAME = "standard";

	protected static final String BACKING_APP_PATH = "classpath:backing-app.jar";

	@Autowired
	protected CloudFoundryService cloudFoundryService;

	@Autowired
	private UaaService uaaService;

	@Autowired
	private AcceptanceTestProperties acceptanceTestProperties;

	private final WebClient webClient = getSslIgnoringWebClient();

	protected abstract String testSuffix();

	protected abstract String appServiceName();

	protected abstract String backingServiceName();

	private String testBrokerAppName() {
		return "test-broker-app-" + testSuffix();
	}

	private String serviceBrokerName() {
		return "test-broker-" + testSuffix();
	}

	private String brokerClientId() {
		return appServiceName();
	}

	@BeforeEach
	void setUp(BrokerProperties brokerProperties) {
		String[] openServiceBrokerProperties = {
			"spring.cloud.openservicebroker.catalog.services[0].id=" + UUID.randomUUID().toString(),
			"spring.cloud.openservicebroker.catalog.services[0].name=" + appServiceName(),
			"spring.cloud.openservicebroker.catalog.services[0].description=A service that deploys a backing app",
			"spring.cloud.openservicebroker.catalog.services[0].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=" + UUID.randomUUID().toString(),
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].name=standard",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].description=A simple plan",
			"spring.cloud.openservicebroker.catalog.services[0].plans[0].free=true",
			"spring.cloud.openservicebroker.catalog.services[1].id=" + UUID.randomUUID().toString(),
			"spring.cloud.openservicebroker.catalog.services[1].name=" + backingServiceName(),
			"spring.cloud.openservicebroker.catalog.services[1].description=A backing service that can be bound to backing apps",
			"spring.cloud.openservicebroker.catalog.services[1].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].id=" + UUID.randomUUID().toString(),
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].name=standard",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].bindable=true",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].description=A simple plan",
			"spring.cloud.openservicebroker.catalog.services[1].plans[0].free=true"
		};

		List<String> appBrokerProperties = new ArrayList<>();
		appBrokerProperties.addAll(Arrays.asList(openServiceBrokerProperties));
		appBrokerProperties.addAll(brokerProperties.getProperties());

		blockingSubscribe(initializeBroker(appBrokerProperties));
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
	public void tearDown() {
		blockingSubscribe(cloudFoundryService.getOrCreateDefaultOrganization()
			.map(OrganizationSummary::getId)
			.flatMap(orgId -> cloudFoundryService.getOrCreateDefaultSpace()
				.map(SpaceSummary::getId)
				.flatMap(spaceId -> cleanup(orgId, spaceId))));
	}

	private Mono<Void> initializeBroker(List<String> appBrokerProperties) {
		return cloudFoundryService
			.getOrCreateDefaultOrganization()
			.map(OrganizationSummary::getId)
			.flatMap(orgId -> cloudFoundryService
				.getOrCreateDefaultSpace()
				.map(SpaceSummary::getId)
				.flatMap(spaceId -> cleanup(orgId, spaceId)
					.then(uaaService.createClient(
						brokerClientId(),
						APP_BROKER_CLIENT_SECRET,
						APP_BROKER_CLIENT_AUTHORITIES))
					.then(cloudFoundryService.associateAppBrokerClientWithOrgAndSpace(brokerClientId(), orgId, spaceId))
					.then(cloudFoundryService
						.pushBrokerApp(testBrokerAppName(), getTestBrokerAppPath(), brokerClientId(),
							appBrokerProperties))
					.then(cloudFoundryService.createServiceBroker(serviceBrokerName(), testBrokerAppName()))
					.then(cloudFoundryService.enableServiceBrokerAccess(appServiceName()))
					.then(cloudFoundryService.enableServiceBrokerAccess(backingServiceName()))));
	}

	private Mono<Void> cleanup(String orgId, String spaceId) {
		return cloudFoundryService.deleteServiceBroker(serviceBrokerName())
			.then(cloudFoundryService.deleteApp(testBrokerAppName()))
			.then(cloudFoundryService.removeAppBrokerClientFromOrgAndSpace(brokerClientId(), orgId, spaceId))
			.onErrorResume(e -> Mono.empty());
	}

	protected void createServiceInstance(String serviceInstanceName) {
		createServiceInstance(serviceInstanceName, Collections.emptyMap());
	}

	protected void createServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		createServiceInstance(appServiceName(), PLAN_NAME, serviceInstanceName, parameters);
	}

	protected void createServiceInstance(String serviceName,
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

	protected void updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
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

	protected void deleteServiceInstance(String serviceInstanceName) {
		blockingSubscribe(cloudFoundryService.deleteServiceInstance(serviceInstanceName));
	}

	protected ServiceInstance getServiceInstance(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName).block();
	}

	protected ServiceInstance getServiceInstance(String serviceInstanceName, String space) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName, space).block();
	}

	protected String getServiceInstanceGuid(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName)
			.map(ServiceInstance::getId)
			.block();
	}

	private Mono<ServiceInstance> getServiceInstanceMono(String serviceInstanceName) {
		return cloudFoundryService.getServiceInstance(serviceInstanceName);
	}

	protected Optional<ApplicationSummary> getApplicationSummary(String appName) {
		return cloudFoundryService
			.getApplications()
			.flatMapMany(Flux::fromIterable)
			.filter(applicationSummary -> appName.equals(applicationSummary.getName()))
			.next()
			.blockOptional();
	}

	protected Optional<ApplicationSummary> getApplicationSummary(String appName, String space) {
		return cloudFoundryService.getApplication(appName, space).blockOptional();
	}

	private ApplicationEnvironments getApplicationEnvironment(String appName) {
		return cloudFoundryService.getApplicationEnvironment(appName).block();
	}

	private ApplicationEnvironments getApplicationEnvironment(String appName, String space) {
		return cloudFoundryService.getApplicationEnvironment(appName, space).block();
	}

	protected DocumentContext getSpringAppJson(String appName) {
		ApplicationEnvironments env = getApplicationEnvironment(appName);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	protected DocumentContext getSpringAppJson(String appName, String space) {
		ApplicationEnvironments env = getApplicationEnvironment(appName, space);
		String saj = (String) env.getUserProvided().get("SPRING_APPLICATION_JSON");
		return JsonPath.parse(saj);
	}

	protected List<String> getSpaces() {
		return cloudFoundryService.getSpaces().block();
	}

	protected Optional<GetClientResponse> getUaaClient(String clientId) {
		return uaaService.getUaaClient(clientId)
			.blockOptional();
	}

	protected void createDomain(String domain) {
		cloudFoundryService.createDomain(domain).block();
	}

	protected void deleteDomain(String domain) {
		cloudFoundryService.deleteDomain(domain).block();
	}

	private Path getTestBrokerAppPath() {
		return Paths.get(acceptanceTestProperties.getBrokerAppPath(), "");
	}

	private <T> void blockingSubscribe(Mono<? super T> publisher) {
		CountDownLatch latch = new CountDownLatch(1);
		publisher.subscribe(System.out::println, t -> {
			if (LOG.isDebugEnabled()) {
				LOG.debug("error subscribing to publisher", t);
			}
			latch.countDown();
		}, latch::countDown);
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected Mono<String> manageApps(String serviceInstanceName, String operation) {
		return cloudFoundryService
			.getServiceInstance(serviceInstanceName)
			.map(ServiceInstance::getId)
			.flatMap(serviceInstanceId ->
				cloudFoundryService
					.getApplicationRoute(testBrokerAppName())
					.flatMap(appRoute ->
						webClient.get()
							.uri(URI.create(appRoute + "/" + operation + "/" + serviceInstanceId))
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
						if (LOG.isDebugEnabled()) {
							LOG.debug("problem ignoring SSL in WebClient", e);
						}
					}
				})))
			.build();
	}

	protected Mono<List<ApplicationDetail>> getApplications(String app1, String app2) {
		return Flux.merge(cloudFoundryService.getApplication(app1),
			cloudFoundryService.getApplication(app2))
			.parallel()
			.runOn(Schedulers.parallel())
			.sequential()
			.collectList();
	}

}
