package org.springframework.cloud.appbroker.autoconfigure;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceInstance;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class BrokeredServicesCatalogMapper {

	private final ServiceDefinitionMapperProperties serviceDefinitionMapperProperties;

	public BrokeredServicesCatalogMapper(ServiceDefinitionMapperProperties serviceDefinitionMapperProperties) {
		this.serviceDefinitionMapperProperties = serviceDefinitionMapperProperties;
	}

	public BrokeredServices toBrokeredServices(Catalog catalog) {
		List<BrokeredServices> services = catalog.getServiceDefinitions().stream()
			.map(this::toBrokeredServices)
			.collect(Collectors.toList());

		BrokeredServices.BrokeredServicesBuilder builder = BrokeredServices.builder();
		services.stream()
			.sequential()
			.forEach(builder::services);
		return builder.build();
	}

	private BrokeredServices toBrokeredServices(ServiceDefinition serviceDefinition) {
		BrokeredServices.BrokeredServicesBuilder builder = BrokeredServices.builder();
		serviceDefinition.getPlans().stream().sequential()
			.forEach(p -> builder.service(toBrokeredService(serviceDefinition.getName(), p.getName())));
		return builder.build();
	}

	private BrokeredService toBrokeredService(String serviceName, String planName) {
		String originalBackingServiceName = removePreprocessingIfAny(serviceName);
		return
			BrokeredService.builder()
				.serviceName(serviceName)
				.planName(planName)
				.services(BackingServices.builder()
					.backingService(toBackingService(originalBackingServiceName, planName))
					.build())
				.target(TargetSpec.builder()
					.name(SpacePerServiceInstance.class.getSimpleName())
					.build())
				.build();
	}

	private String removePreprocessingIfAny(String serviceName) {
		String suffix = this.serviceDefinitionMapperProperties.getSuffix();
		if (suffix == null) {
			return serviceName;
		}
		//noinspection UnnecessaryLocalVariable
		String serviceNameWithoutSuffix = serviceName.substring(0, serviceName.length() - suffix.length());
		return serviceNameWithoutSuffix;
	}

	private BackingService toBackingService(String serviceName, String planName) {
		return BackingService.builder()
			.name(serviceName)
			.plan(planName)
			.serviceInstanceName(serviceName)
			.build();
	}



}
