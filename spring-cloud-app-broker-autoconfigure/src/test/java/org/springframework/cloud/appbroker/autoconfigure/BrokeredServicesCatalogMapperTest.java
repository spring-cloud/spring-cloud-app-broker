package org.springframework.cloud.appbroker.autoconfigure;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class BrokeredServicesCatalogMapperTest extends SampleServicesBuilderBaseTest {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	void mapsCatalogToBrokeredServices() {
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				buildServiceDefinition("mysql", "10mb", "20mb"),
				buildServiceDefinition("noop", "default")))
			.build();

		BrokeredServices brokeredServices = new BrokeredServicesCatalogMapper(new ServiceDefinitionMapperProperties()).toBrokeredServices(catalog);
		logger.info("brokered services: {}", brokeredServices);
		BrokeredServices expectedBrokeredServices = BrokeredServices.builder()
			.service(buildBrokeredService("mysql", "10mb"))
			.service(buildBrokeredService("mysql", "20mb"))
			.service(buildBrokeredService("noop", "default"))
			.build();
		assertThat(brokeredServices).isEqualTo(expectedBrokeredServices);
	}

	@Test
	void mapsSuffixedCatalogToUnsuffixedBrokeredServices() {
		//given
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				buildServiceDefinition("mysql-suffixed", "10mb")))
			.build();
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties = new ServiceDefinitionMapperProperties();
		serviceDefinitionMapperProperties.setSuffix("-suffixed");

		//when
		BrokeredServices brokeredServices = new BrokeredServicesCatalogMapper(serviceDefinitionMapperProperties).toBrokeredServices(catalog);

		//then
		logger.info("brokered services: {}", brokeredServices);
		BrokeredServices expectedBrokeredServices = BrokeredServices.builder()
			.service(BrokeredService.builder()
				.serviceName("mysql-suffixed")
				.planName("10mb")
				.services(BackingServices.builder()
					.backingService(buildBackingService("mysql", "10mb"))
					.build())
				.target(TargetSpec.builder()
					.name("SpacePerServiceInstance")
					.build())
				.build())
			.build();
		assertThat(brokeredServices).isEqualTo(expectedBrokeredServices);
	}

}
