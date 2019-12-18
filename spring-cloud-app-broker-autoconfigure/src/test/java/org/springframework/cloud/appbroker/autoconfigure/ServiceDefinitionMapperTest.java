package org.springframework.cloud.appbroker.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceDefinitionMapperTest {

	@Test
	void filtersOutServiceEntityMatchingExcludeRegexp() {
		assertEntityIsFiltered(".*exclude", "brokerNameTo_exclude", false);
	}

	@Test
	void filtersInServiceEntityNotMatchingExcludeRegexp() {
		assertEntityIsFiltered(".*exclude", "brokerNameTo_include", true);
	}

	@Test
	void filtersInAllServiceEntitiesWhenNoRegexpConfigured() {
		assertEntityIsFiltered(null, "a random brokerName", true);
	}

	private void assertEntityIsFiltered(String excludeBrokerNamesRegexp, String serviceBrokerName,
		boolean shouldMapEntity) {
		//given
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties = new ServiceDefinitionMapperProperties();
		serviceDefinitionMapperProperties.setExcludeBrokerNamesRegexp(excludeBrokerNamesRegexp);
		PlanMapper planMapper = mock(PlanMapper.class);
		ServiceDefinitionMapper serviceDefinitionMapper = new ServiceDefinitionMapper(
			planMapper, serviceDefinitionMapperProperties);

		//when
		ServiceEntity entity = ServiceEntity.builder()
			.serviceBrokerName(serviceBrokerName)
			.build();

		//then
		assertThat(serviceDefinitionMapper.shouldMapServiceEntity(entity)).isEqualTo(shouldMapEntity);
	}

	@Test
	void mapsToServiceDefinition() {
		//given
		ServiceResource resource = aServiceResource();
		List<ServicePlanResource> servicePlans = singletonList(aServicePlanResource());

		PlanMapper planMapper = mock(PlanMapper.class);
		List<Plan> expectedPlans = singletonList(anExpectedPlan());
		when(planMapper.toPlans(any())).thenReturn(expectedPlans);
		ServiceDefinitionMapper serviceDefinitionMapper = new ServiceDefinitionMapper(
			planMapper, new ServiceDefinitionMapperProperties());

		//when
		ServiceDefinition serviceDefinition = serviceDefinitionMapper.toServiceDefinition(resource, servicePlans);

		//then
		ServiceDefinition expectedServiceDefinition= anExpectedServiceDefinition(expectedPlans, "");
		assertThat(serviceDefinition).isEqualTo(expectedServiceDefinition);
	}

	@Test
	void addsSuffixToServiceNamesWhenAsked() {
		//given
		ServiceResource resource = aServiceResource();
		List<ServicePlanResource> servicePlans = singletonList(aServicePlanResource());

		PlanMapper planMapper = mock(PlanMapper.class);
		List<Plan> expectedPlans = singletonList(anExpectedPlan());
		when(planMapper.toPlans(any())).thenReturn(expectedPlans);
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties = new ServiceDefinitionMapperProperties();
		serviceDefinitionMapperProperties.setSuffix("-cmdb");
		ServiceDefinitionMapper serviceDefinitionMapper = new ServiceDefinitionMapper(
			planMapper, serviceDefinitionMapperProperties);

		//when
		ServiceDefinition serviceDefinition = serviceDefinitionMapper.toServiceDefinition(resource, servicePlans);

		//then
		ServiceDefinition expectedServiceDefinition= anExpectedServiceDefinition(expectedPlans, "-cmdb");
		assertThat(serviceDefinition).isEqualTo(expectedServiceDefinition);
	}



	private ServiceResource aServiceResource() {
		return ServiceResource.builder()
			.metadata(Metadata.builder()
				.id("service-id")
				.build())
			.entity(ServiceEntity.builder()
				.label("service-name")
				.description("service-description")
				.extra("{}")
				.serviceBrokerName("broker-name")
				.build())
			.build();
	}

	private ServiceDefinition anExpectedServiceDefinition(List<Plan> expectedPlans, String serviceNameSuffix) {
		return ServiceDefinition
			.builder()
			.id("service-id")
			.description("service-description")
			.name("service-name" + serviceNameSuffix)
			.bindable(false)
			.planUpdateable(false)
			.bindingsRetrievable(false)
			.instancesRetrievable(false)
			.tags(Collections.emptyList())
			.plans(expectedPlans)
			.metadata(Collections.emptyMap())
			.build();
	}

	private ServicePlanResource aServicePlanResource() {
		return ServicePlanResource.builder()
			.entity(ServicePlanEntity.builder()
				.name("plan-name")
				.description("plan-description")
				.build())
			.metadata(Metadata.builder()
				.id("plan-id")
				.build())
			.build();
	}

	private Plan anExpectedPlan() {
		return Plan.builder()
			.id("plan-id")
			.name("plan-name")
			.description("plan-description")
			.metadata(Collections.emptyMap())
			.free(false)
			.build();
	}

}