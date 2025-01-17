/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
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
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryProperties;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.UserCloudFoundryService;
import org.springframework.cloud.appbroker.acceptance.fixtures.uaa.UaaService;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { CloudFoundryClientConfiguration.class, CloudFoundryService.class,
		UserCloudFoundryService.class, UaaService.class, HealthListener.class, RestTemplate.class })
@ExtendWith(SpringExtension.class)
@ExtendWith(BrokerPropertiesParameterResolver.class)
@EnableConfigurationProperties(AcceptanceTestProperties.class)
abstract class CloudFoundryAcceptanceTests {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryAcceptanceTests.class);

	private static final String BACKING_SERVICE_PLAN_ID = UUID.randomUUID().toString();

	private static final String SERVICE_ID = UUID.randomUUID().toString();

	private static final String PLAN_ID = UUID.randomUUID().toString();

	private static final String BACKING_SERVICE_ID = UUID.randomUUID().toString();

	protected static final String PLAN_NAME = "standard";

	protected static final String BACKING_APP_PATH = "classpath:backing-app.jar";

	@Autowired
	protected CloudFoundryService cloudFoundryService;

	@Autowired
	protected UserCloudFoundryService userCloudFoundryService;

	@Autowired
	private CloudFoundryProperties cloudFoundryProperties;

	@Autowired
	private UaaService uaaService;

	@Autowired
	private AcceptanceTestProperties acceptanceTestProperties;

	private String cfHome;

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
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		List<String> appBrokerProperties = getAppBrokerProperties(brokerProperties);
		blockingSubscribe(initializeUser());
		blockingSubscribe(initializeBroker(appBrokerProperties));
		prepareCLI();
	}

	void setUpForBrokerUpdate(BrokerProperties brokerProperties) {
		List<String> appBrokerProperties = getAppBrokerProperties(brokerProperties);
		blockingSubscribe(updateBroker(appBrokerProperties));
	}

	private List<String> getAppBrokerProperties(BrokerProperties brokerProperties) {
		String[] openServiceBrokerProperties = { "spring.cloud.openservicebroker.catalog.services[0].id=" + SERVICE_ID,
				"spring.cloud.openservicebroker.catalog.services[0].name=" + appServiceName(),
				"spring.cloud.openservicebroker.catalog.services[0].description=A service that deploys a backing app",
				"spring.cloud.openservicebroker.catalog.services[0].bindable=true",
				"spring.cloud.openservicebroker.catalog.services[0].metadata.properties.serviceInstanceLogsEndpoint="
						+ getServiceInstanceLogsEndpoint(),
				"spring.cloud.openservicebroker.catalog.services[0].plans[0].id=" + PLAN_ID,
				"spring.cloud.openservicebroker.catalog.services[0].plans[0].name=standard",
				"spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable=true",
				"spring.cloud.openservicebroker.catalog.services[0].plans[0].description=A simple plan",
				"spring.cloud.openservicebroker.catalog.services[0].plans[0].free=true",
				"spring.cloud.openservicebroker.catalog.services[1].id=" + BACKING_SERVICE_ID,
				"spring.cloud.openservicebroker.catalog.services[1].name=" + backingServiceName(),
				"spring.cloud.openservicebroker.catalog.services[1].description=A backing service that can be bound to backing apps",
				"spring.cloud.openservicebroker.catalog.services[1].bindable=true",
				"spring.cloud.openservicebroker.catalog.services[1].plans[0].id=" + BACKING_SERVICE_PLAN_ID,
				"spring.cloud.openservicebroker.catalog.services[1].plans[0].name=standard",
				"spring.cloud.openservicebroker.catalog.services[1].plans[0].bindable=true",
				"spring.cloud.openservicebroker.catalog.services[1].plans[0].description=A simple plan",
				"spring.cloud.openservicebroker.catalog.services[1].plans[0].free=true" };

		List<String> appBrokerProperties = new ArrayList<>();
		appBrokerProperties.addAll(Arrays.asList(openServiceBrokerProperties));
		appBrokerProperties.addAll(brokerProperties.getProperties());
		return appBrokerProperties;
	}

	private String getServiceInstanceLogsEndpoint() {
		return "https://" + testBrokerAppName() + "." + this.cloudFoundryProperties.getApiHost().substring(4)
				+ "/logs/";
	}

	@BeforeEach
	void configureJsonPath() {
		Configuration.setDefaults(new Configuration.Defaults() {
			private final JsonProvider jacksonJsonProvider = new JacksonJsonProvider();

			private final MappingProvider jacksonMappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return this.jacksonJsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return this.jacksonMappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}

	@AfterEach
	void tearDown(TestInfo testInfo) {
		blockingSubscribe(this.cloudFoundryService.getOrCreateDefaultOrg()
			.map(OrganizationSummary::getId)
			.flatMap((orgId) -> this.cloudFoundryService.getOrCreateDefaultSpace()
				.map(SpaceSummary::getId)
				.flatMap((spaceId) -> cleanup(orgId, spaceId))));
	}

	private Mono<Void> initializeUser() {
		return this.cloudFoundryService.getOrCreateOrganization(this.userCloudFoundryService.getOrgName())
			.map(OrganizationSummary::getId)
			.flatMap((orgId) -> this.cloudFoundryService
				.getOrCreateSpace(this.userCloudFoundryService.getOrgName(),
						this.userCloudFoundryService.getSpaceName())
				.map(SpaceSummary::getId)
				.flatMap((spaceId) -> this.uaaService
					.createClient(CloudFoundryClientConfiguration.USER_CLIENT_ID,
							CloudFoundryClientConfiguration.USER_CLIENT_SECRET,
							CloudFoundryClientConfiguration.USER_CLIENT_AUTHORITIES)
					.then(this.cloudFoundryService.associateClientWithOrgAndSpace(
							CloudFoundryClientConfiguration.USER_CLIENT_ID, orgId, spaceId))));
	}

	private Mono<Void> initializeBroker(List<String> appBrokerProperties) {
		return this.cloudFoundryService.getOrCreateDefaultOrg()
			.map(OrganizationSummary::getId)
			.flatMap((orgId) -> this.cloudFoundryService.getOrCreateDefaultSpace()
				.map(SpaceSummary::getId)
				.flatMap((spaceId) -> cleanup(orgId, spaceId)
					.then(this.uaaService.createClient(brokerClientId(),
							CloudFoundryClientConfiguration.APP_BROKER_CLIENT_SECRET,
							CloudFoundryClientConfiguration.APP_BROKER_CLIENT_AUTHORITIES))
					.then(this.cloudFoundryService.associateAppBrokerClientWithOrgAndSpace(brokerClientId(), orgId,
							spaceId))
					.then(this.cloudFoundryService.pushBrokerApp(testBrokerAppName(), getTestBrokerAppPath(),
							brokerClientId(), appBrokerProperties))
					.then(this.cloudFoundryService.createServiceBroker(serviceBrokerName(), testBrokerAppName()))
					.then(this.cloudFoundryService.enableServiceBrokerAccess(appServiceName()))
					.then(this.cloudFoundryService.enableServiceBrokerAccess(backingServiceName()))));
	}

	private Mono<Void> updateBroker(List<String> appBrokerProperties) {
		return this.cloudFoundryService.updateBrokerApp(testBrokerAppName(), brokerClientId(), appBrokerProperties)
			.then(this.cloudFoundryService.updateServiceBroker(serviceBrokerName(), testBrokerAppName()));
	}

	private Mono<Void> cleanup(String orgId, String spaceId) {
		return this.cloudFoundryService.deleteServiceBroker(serviceBrokerName())
			.then(this.cloudFoundryService.deleteApp(testBrokerAppName()))
			.then(this.cloudFoundryService.removeAppBrokerClientFromOrgAndSpace(brokerClientId(), orgId, spaceId))
			.onErrorResume((e) -> Mono.empty());
	}

	protected void createServiceInstance(String serviceInstanceName) {
		createServiceInstance(serviceInstanceName, Collections.emptyMap());
	}

	protected void createServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		createServiceInstance(appServiceName(), PLAN_NAME, serviceInstanceName, parameters);
	}

	protected void createServiceInstance(String serviceName, String planName, String serviceInstanceName,
			Map<String, Object> parameters) {
		this.userCloudFoundryService.createServiceInstance(planName, serviceName, serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap((serviceInstance) -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Create service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	protected void createBackingServiceInstance(String serviceName, String planName, String serviceInstanceName,
			Map<String, Object> parameters) {
		this.cloudFoundryService.createBackingServiceInstance(planName, serviceName, serviceInstanceName, parameters)
			.then(this.cloudFoundryService.getServiceInstance(serviceInstanceName))
			.flatMap((serviceInstance) -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Create service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	protected void updateServiceInstance(String serviceInstanceName, Map<String, Object> parameters) {
		this.userCloudFoundryService.updateServiceInstance(serviceInstanceName, parameters)
			.then(getServiceInstanceMono(serviceInstanceName))
			.flatMap((serviceInstance) -> {
				assertThat(serviceInstance.getStatus())
					.withFailMessage("Update service instance failed:" + serviceInstance.getMessage())
					.isEqualTo("succeeded");
				return Mono.empty();
			})
			.block();
	}

	protected void deleteServiceInstance(String serviceInstanceName) {
		blockingSubscribe(this.userCloudFoundryService.deleteServiceInstance(serviceInstanceName));
	}

	protected List<String> listServiceInstances() {
		return this.cloudFoundryService.listServiceInstances()
			.map(ServiceInstanceSummary::getName)
			.collectList()
			.block();
	}

	protected ServiceInstance getBackingServiceInstance(String serviceInstanceName) {
		return this.cloudFoundryService.getServiceInstance(serviceInstanceName).block();
	}

	protected ServiceInstance getBackingServiceInstance(String serviceInstanceName, String space) {
		return this.cloudFoundryService.getServiceInstance(serviceInstanceName, space).block();
	}

	protected String getServiceInstanceGuid(String serviceInstanceName) {
		return getServiceInstanceMono(serviceInstanceName).map(ServiceInstance::getId).block();
	}

	Mono<ServiceInstance> getServiceInstanceMono(String serviceInstanceName) {
		return this.userCloudFoundryService.getServiceInstance(serviceInstanceName);
	}

	protected Optional<ApplicationSummary> getApplicationSummary(String appName) {
		return this.cloudFoundryService.getApplications()
			.flatMapMany(Flux::fromIterable)
			.filter((applicationSummary) -> appName.equals(applicationSummary.getName()))
			.next()
			.blockOptional();
	}

	protected Optional<ApplicationDetail> getApplicationDetail(String appName) {
		return this.cloudFoundryService.getApplication(appName)
			.filter((applicationSummary) -> appName.equals(applicationSummary.getName()))
			.blockOptional();
	}

	protected Optional<ApplicationSummary> getApplicationSummary(String appName, String space) {
		return this.cloudFoundryService.getApplication(appName, space).blockOptional();
	}

	private ApplicationEnvironments getApplicationEnvironment(String appName) {
		return this.cloudFoundryService.getApplicationEnvironment(appName).block();
	}

	private ApplicationEnvironments getApplicationEnvironment(String appName, String space) {
		return this.cloudFoundryService.getApplicationEnvironment(appName, space).block();
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
		return this.cloudFoundryService.getSpaces().block();
	}

	protected Optional<GetClientResponse> getUaaClient(String clientId) {
		return this.uaaService.getUaaClient(clientId).blockOptional();
	}

	protected void createDomain(String domain) {
		this.cloudFoundryService.createDomain(domain).block();
	}

	protected void deleteDomain(String domain) {
		this.cloudFoundryService.deleteDomain(domain).block();
	}

	private Path getTestBrokerAppPath() {
		return Paths.get(this.acceptanceTestProperties.getBrokerAppPath(), "");
	}

	private <T> void blockingSubscribe(Mono<? super T> publisher) {
		CountDownLatch latch = new CountDownLatch(1);
		publisher.subscribe(System.out::println, (t) -> {
			if (LOG.isDebugEnabled()) {
				LOG.debug("error subscribing to publisher", t);
			}
			latch.countDown();
		}, latch::countDown);
		try {
			latch.await();
		}
		catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected Mono<String> manageApps(String serviceInstanceName, String serviceName, String planName,
			String operation) {
		return this.userCloudFoundryService.getServiceInstance(serviceInstanceName)
			.map(ServiceInstance::getId)
			.flatMap((serviceInstanceId) -> this.cloudFoundryService.getApplicationRoute(testBrokerAppName())
				.flatMap((appRoute) -> this.webClient.get()
					.uri(URI.create(
							appRoute + "/" + operation + "/" + serviceName + "/" + planName + "/" + serviceInstanceId))
					.retrieve()
					.toEntity(String.class)
					.map(HttpEntity::getBody)));
	}

	private WebClient getSslIgnoringWebClient() {
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().secure((t) -> {
			try {
				t.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
			}
			catch (SSLException ex) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("problem ignoring SSL in WebClient", ex);
				}
			}
		}))).build();
	}

	protected Mono<List<ApplicationDetail>> getApplications(String app1, String app2) {
		return Flux.merge(this.cloudFoundryService.getApplication(app1), this.cloudFoundryService.getApplication(app2))
			.parallel()
			.runOn(Schedulers.parallel())
			.sequential()
			.collectList();
	}

	private void prepareCLI() {
		try {
			this.cfHome = Files.createTempDirectory("app-broker-acceptance-tests").toString();

			callCLICommand(List.of("cf", "login", "-a", this.cloudFoundryProperties.getApiHost(),
					"--skip-ssl-validation", "-u", this.cloudFoundryProperties.getUsername(), "-p",
					this.cloudFoundryProperties.getPassword(), "-o", "test-instances"))
				.block(Duration.ofSeconds(60));
			callCLICommand(List.of("cf", "install-plugin", "-f", "-r", "Cf-Community", "Service Instance Logging"))
				.block(Duration.ofSeconds(60));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected Mono<String> callCLICommand(List<String> command) {
		return Mono.fromCallable(() -> {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Executing command: {}", command);
			}
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.environment().put("CF_HOME", this.cfHome);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			return processOutput(process);
		}).subscribeOn(Schedulers.boundedElastic());
	}

	private static String processOutput(Process process) throws InterruptedException, ExecutionException {
		StringBuffer outputBuilder = new StringBuffer();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Void> future = appendLines(executor, process, outputBuilder);
		try {
			future.get(30, TimeUnit.SECONDS);
		}
		catch (TimeoutException ex) {
			LOG.info("Process reading timed out after 30 seconds");
		}
		finally {
			future.cancel(true);
			executor.shutdownNow();
			process.destroyForcibly();
		}
		return outputBuilder.toString();
	}

	private static Future<Void> appendLines(ExecutorService executor, Process process, StringBuffer outputBuilder) {
		return executor.submit(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = "";
				while (line != null) {
					line = reader.readLine();
					if (line != null) {
						outputBuilder.append(line).append('\n');
						if (LOG.isDebugEnabled()) {
							LOG.debug("Read line: {}", line);
						}
					}
				}
			}
			return null;
		});
	}

}
