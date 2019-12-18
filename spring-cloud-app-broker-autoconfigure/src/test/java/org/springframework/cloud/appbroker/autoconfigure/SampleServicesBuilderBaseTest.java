package org.springframework.cloud.appbroker.autoconfigure;

import java.util.stream.Stream;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class SampleServicesBuilderBaseTest {

	protected BrokeredService buildBrokeredService(String serviceName, String planName) {
		return
			BrokeredService.builder()
				.serviceName(serviceName)
				.planName(planName)
				.services(BackingServices.builder()
					.backingService(buildBackingService(serviceName, planName))
					.build())
				.target(TargetSpec.builder()
					.name("SpacePerServiceInstance")
					.build())
				.build();
	}

	protected ServiceDefinition buildServiceDefinition(String serviceName, String... planNames) {
		return ServiceDefinition.builder()
			.id(serviceName + "-id")
			.name(serviceName)
			.description("description " +  serviceName)
			.plans(
				buildPlan(planNames))
			.build();
	}

	protected BackingService buildBackingService(String serviceName, String planName) {
		return BackingService.builder()
			.name(serviceName)
			.plan(planName)
			.serviceInstanceName(serviceName)
			.build();
	}

	protected Plan[] buildPlan(String[] planNames) {
		return Stream.of(planNames)
			.map(planName-> Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.description("description " +  planName)
				.build())
			.toArray(Plan[]::new);
	}

}
