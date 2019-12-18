package org.springframework.cloud.appbroker.integration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogConstants;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfiguration;
import org.springframework.cloud.appbroker.autoconfigure.ServiceDefinitionMapperProperties;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.integration.fixtures.CredHubStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.ExtendedCloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.appbroker.integration.fixtures.TargetPropertiesConfiguration;
import org.springframework.cloud.appbroker.integration.fixtures.TestBindingCredentialsProviderFixture;
import org.springframework.cloud.appbroker.integration.fixtures.UaaStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.WiremockServerFixture;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@ExtendWith(SpringExtension.class) //Junit 5 jupiter support
@ContextConfiguration(classes = {
	WiremockServerFixture.class,
	OpenServiceBrokerApiFixture.class,
	ExtendedCloudControllerStubFixture.class,
	UaaStubFixture.class,
	CredHubStubFixture.class,
	TestBindingCredentialsProviderFixture.class})
@TestPropertySource(properties = {
	"spring.cloud.appbroker.deployer.cloudfoundry.api-host=localhost",
	"spring.cloud.appbroker.deployer.cloudfoundry.api-port=8080",
	"spring.cloud.appbroker.deployer.cloudfoundry.username=admin",
	"spring.cloud.appbroker.deployer.cloudfoundry.password=adminpass",
	"spring.cloud.appbroker.deployer.cloudfoundry.default-org=test",
	"spring.cloud.appbroker.deployer.cloudfoundry.default-space=development",
	"spring.cloud.appbroker.deployer.cloudfoundry.secure=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //throw away wiremock across each test
class DynamicServiceAutoConfigurationComponentTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private WiremockServerFixture wiremockFixture;

	@Autowired
	private ExtendedCloudControllerStubFixture cloudControllerFixture;

	@Autowired
	private CloudControllerStubFixture cloudFoundryFixture;


	@Autowired
	private UaaStubFixture uaaFixture;

	@BeforeAll
	void setUp() {
		wiremockFixture.startWiremock();
	}

	@AfterAll
	void tearDown() {
		wiremockFixture.stopWiremock();
	}

	@BeforeEach
	void resetWiremock() {
		wiremockFixture.resetWiremock();

		uaaFixture.stubCommonUaaRequests();
		cloudFoundryFixture.stubCommonCloudControllerRequests();
	}

	@AfterEach
	void verifyStubs() {
		wiremockFixture.verifyAllRequiredStubsUsed();
	}

	private final ApplicationContextRunner contextRunner = setUpCommonApplicationContextRunner("");

	private ApplicationContextRunner setUpCommonApplicationContextRunner(String extraProperty) {
		return new ApplicationContextRunner()
			.withPropertyValues(
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.api-host=localhost",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.api-port=8080",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.username=admin",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.password=adminpass",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.default-org=test",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.default-space=development",
				"spring.cloud.appbroker.acceptancetest.cloudfoundry.secure=false",
				"spring.cloud.appbroker.acceptance-test.cloudfoundry.client-id=osb-cmdb-acceptance-test",
				"spring.cloud.appbroker.acceptance-test.cloudfoundry.client-secret=IPN4500Bgf0fQhZrA0CBpIovYzAyhln",
				DynamicCatalogConstants.OPT_IN_PROPERTY+"=true",
				extraProperty)
			.withConfiguration(AutoConfigurations.of(
				TargetPropertiesConfiguration.class,
				CloudFoundryClientConfiguration.class,
				DynamicCatalogServiceAutoConfiguration.class
			));
	}

	@Test
	void catalogCreatedWithDetailedValidMetadata() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services-detailed");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans-detailed");

		this.contextRunner
			.run(context -> {
				assertThat(context).hasSingleBean(BrokeredServices.class);
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).isNotEmpty();

				assertThat(context).hasSingleBean(Catalog.class);
				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).isNotEmpty();
				ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);
				assertThat(serviceDefinition.getName()).isEqualTo("db-service");
				assertThat(serviceDefinition.getDescription()).isEqualTo("My DB Service");
				assertThat(serviceDefinition.isBindable()).isTrue(); //Non default value
				assertThat(serviceDefinition.isPlanUpdateable()).isTrue(); //Non default value
				assertThat(serviceDefinition.isInstancesRetrievable()).isTrue(); //Non default value
				assertThat(serviceDefinition.getTags()).containsOnly("tag1", "tag2");
				
				Map<String, Object> metadata = serviceDefinition.getMetadata();
				assertThat(metadata)
					.isNotNull()
					.isNotEmpty()
					.containsOnly(
						entry("displayName", "displayName"),
						entry("longDescription", "longDescription")
						);
				assertThat(serviceDefinition.getPlans().size()).isEqualTo(1);
				Plan plan = serviceDefinition.getPlans().get(0);

				logger.info("schemas: {}", plan.getSchemas());
				assertThat(plan.getSchemas()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema().getParameters().size()).isEqualTo(3); //$schema, type, properties
				assertThat(plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema().getParameters().size()).isEqualTo(3); //$schema, type, properties
				//noinspection unchecked
				Map<String, Object> properties = (Map<String, Object>) plan.getSchemas().getServiceInstanceSchema().getCreateMethodSchema().getParameters().get("properties");
				assertThat(properties.get("baz")).isNotNull(); //$schema, type, properties
				assertThat(plan.getSchemas().getServiceInstanceSchema().getUpdateMethodSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceInstanceSchema().getUpdateMethodSchema().getParameters().size()).isEqualTo(3);
				assertThat(plan.getSchemas().getServiceBindingSchema().getCreateMethodSchema()).isNotNull();
				assertThat(plan.getSchemas().getServiceBindingSchema().getCreateMethodSchema().getParameters().size()).isEqualTo(3);

				assertThat(plan.getMetadata()).isNotEmpty();
				assertThat(plan.getMetadata().get("costs")).isNotNull();
			});
	}
	@Test
	void catalogCreatedWithNullMetadata() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services-null-extra");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans");
		assertCatalogCreatesWithoutError();
	}

	@Test
	void catalogCreatedWithSimpleMetadata() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans");
		assertCatalogCreatesWithoutError();
	}

	@Test
	void excludesBrokersMatchingRegexp() {
		//Given a marketplace with 2 service definition
		aStubbedMarketplaceWithTwoServiceOfferings();

		//And a broker exclusion regexp configrued
		String extraProperty = ServiceDefinitionMapperProperties.PROPERTY_PREFIX
			+ ServiceDefinitionMapperProperties.EXCLUDE_BROKER_PROPERTY_KEY
			+ "=service_broker_name2";
		ApplicationContextRunner customContextRunner = setUpCommonApplicationContextRunner(extraProperty);

		//when fetching marketplace
		customContextRunner
			.run(context -> {
				//then
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).hasSize(1);

				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).hasSize(1);
			});
	}

	private void aStubbedMarketplaceWithTwoServiceOfferings() {
		cloudControllerFixture.stubSpaceServiceWithResponse("list-space-services-multiple");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans-detailed", "SERVICE-ID");
		cloudControllerFixture.stubServicePlanWithResponse("list-service-plans-service2", "SERVICE-ID2");
	}

	@Test
	void doesNotExcludeBrokersNotMatchingRegexp() {
		//Given a marketplace with 2 service definition
		aStubbedMarketplaceWithTwoServiceOfferings();

		//And a broker exclusion regexp configrued
		String extraProperty = ServiceDefinitionMapperProperties.PROPERTY_PREFIX
			+ ServiceDefinitionMapperProperties.EXCLUDE_BROKER_PROPERTY_KEY
			+ "=non_matching_broker_name";
		ApplicationContextRunner customContextRunner = setUpCommonApplicationContextRunner(extraProperty);

		//when fetching marketplace
		customContextRunner
			.run(context -> {
				//then
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).hasSize(2);

				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).hasSize(2);
			});
	}

	private void assertCatalogCreatesWithoutError() {
		this.contextRunner
			.run(context -> {
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).isNotEmpty();

				Catalog catalog = context.getBean(Catalog.class);
				List<ServiceDefinition> serviceDefinitions = catalog.getServiceDefinitions();
				assertThat(serviceDefinitions).isNotEmpty();
				serviceDefinitions.stream()
					.map(ServiceDefinition::getPlans)
					.flatMap(Collection::stream)
					.forEach(this::assertPlanSerializesWithoutPollutingWithNulls);
			});
	}

	private void assertPlanSerializesWithoutPollutingWithNulls(Plan plan)  {
		try {
			ObjectMapper mapper = new ObjectMapper();
			String serializedPlan = mapper.writeValueAsString(plan);
			logger.info("serializedPlan {}", serializedPlan);
			assertThat(serializedPlan).doesNotContain(":null");
			assertThat(serializedPlan).doesNotContain("\"parameters\": {}");
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	@Ignore
		//Not yet implemented
	void catalogFetchingFailuresThrowsException() {
		//TODO: fail if the flux contains error events (was the case when Jackson was not configured to ignore
	}

}
