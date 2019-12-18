package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@AutoConfigureBefore(AppBrokerAutoConfiguration.class)
@ConditionalOnProperty(value= DynamicCatalogConstants.OPT_IN_PROPERTY)
@EnableConfigurationProperties
public class DynamicCatalogServiceAutoConfiguration {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private BrokeredServices brokeredServices;
	private Catalog catalog;

	@Bean
	@ConfigurationProperties(prefix = PlanMapperProperties.PROPERTY_PREFIX, ignoreUnknownFields = false)
	public PlanMapperProperties planMapperProperties() {
		return new PlanMapperProperties();
	}

	@Bean
	@ConfigurationProperties(prefix = ServiceDefinitionMapperProperties.PROPERTY_PREFIX, ignoreUnknownFields = false)
	public ServiceDefinitionMapperProperties serviceDefinitionMapperProperties() {
		return new ServiceDefinitionMapperProperties();
	}

	@Bean
	public ServiceDefinitionMapper serviceDefinitionMapper(
		PlanMapper planMapper,
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties) {
		return new ServiceDefinitionMapper(planMapper, serviceDefinitionMapperProperties);
	}

	@Bean
	public PlanMapper planMapper(PlanMapperProperties planMapperProperties) {
		return new PlanMapper(planMapperProperties);
	}

	@Bean
	public DynamicCatalogService dynamicCatalogService(
		CloudFoundryDeploymentProperties defaultDeploymentProperties,
		CloudFoundryOperations operations,
		CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		ServiceDefinitionMapper serviceDefinitionMapper) {

		logger.info("Will be fetching catalog from org {} and space {}",
			targetProperties.getDefaultOrg(), targetProperties.getDefaultSpace());

		return new DynamicCatalogServiceImpl(
			defaultDeploymentProperties,
			operations,
			cloudFoundryClient,
			targetProperties,
			serviceDefinitionMapper);
	}

	@Bean
	public BrokeredServicesCatalogMapper brokeredServicesCatalogMapper(
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties) {
		return new BrokeredServicesCatalogMapper(serviceDefinitionMapperProperties);
	}

	@Bean
	public Catalog catalog(DynamicCatalogService dynamicCatalogService,
		BrokeredServicesCatalogMapper brokeredServicesCatalogMapper) {
		brokeredServices = brokeredServices(dynamicCatalogService,
			brokeredServicesCatalogMapper);
		return catalog;
	}

	@Bean
	public BrokeredServices brokeredServices(DynamicCatalogService dynamicCatalogService,
		BrokeredServicesCatalogMapper brokeredServicesCatalogMapper) {
		initializeCatalog(dynamicCatalogService, brokeredServicesCatalogMapper);
		return brokeredServices;
	}

	private void initializeCatalog(DynamicCatalogService dynamicCatalogService,
		BrokeredServicesCatalogMapper brokeredServicesCatalogMapper) {
		if (catalog == null || brokeredServices == null) {
			List<ServiceDefinition> serviceDefinitions = dynamicCatalogService.fetchServiceDefinitions();
			Assert.notEmpty(serviceDefinitions, "Unexpected empty marketplace dynamically fetched");

			this.catalog = Catalog.builder().serviceDefinitions(serviceDefinitions).build();
			Assert.notEmpty(catalog.getServiceDefinitions(),
				"Unexpected empty mapped catalog, check configured filters");

			brokeredServices = brokeredServicesCatalogMapper.toBrokeredServices(this.catalog);
			Assert.notEmpty(catalog.getServiceDefinitions(),
				"Unexpected empty list of brokered services, check configured filters");

			logger.info("Mapped catalog is: {}", catalog);
			logger.info("Mapped brokered services are: {}", brokeredServices);

			dumpCatalogToDisk();
		}
	}

	private void dumpCatalogToDisk() {
		try {
			ServiceConfigurationYamlDumper serviceConfigurationYamlDumper = new ServiceConfigurationYamlDumper();
			serviceConfigurationYamlDumper.dumpToYamlFile(catalog, brokeredServices);
			if (logger.isDebugEnabled()) {
				String yamlDebug = serviceConfigurationYamlDumper.dumpToYamlString(catalog, brokeredServices);
				logger.debug("Mapped catalog yml is {}", yamlDebug);
			}
		}
		catch (IOException e) {
			//Don't fail application start
			logger.error("Unable to dump dynamic catalog to disk, caught: " + e, e);
		}
	}

}
