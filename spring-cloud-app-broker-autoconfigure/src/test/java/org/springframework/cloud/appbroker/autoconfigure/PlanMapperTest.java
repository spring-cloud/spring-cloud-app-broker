package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.serviceplans.Schema;
import org.cloudfoundry.client.v2.serviceplans.Schemas;
import org.cloudfoundry.client.v2.serviceplans.ServiceBindingSchema;
import org.cloudfoundry.client.v2.serviceplans.ServiceInstanceSchema;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.catalog.Plan;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class PlanMapperTest {

	private static final Logger logger = LoggerFactory.getLogger(PlanMapperTest.class);

	@Test
	void mapsPlanResourcesIntoPlan() throws JsonProcessingException {
		List<ServicePlanResource> servicePlans = asList(
		ServicePlanResource.builder()
			.entity(ServicePlanEntity.builder()
				.name("plan1")
				.active(true)
				.bindable(true)
				.description("description")
				.extra("{\"displayName\":\"Big Bunny\"}")
				.free(false)
				.build())
			.metadata(Metadata.builder()
				.id("plan-id")
				.build())
			.build(),
		ServicePlanResource.builder()
			.entity(ServicePlanEntity.builder()
				.name("plan2")
				.build())
			.build());


		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());

		List<Plan> plans = planMapper.toPlans(servicePlans);
		assertThat(plans).hasSize(2);
		Plan plan1 = plans.get(0);
		assertThat(plan1.getName()).isEqualTo("plan1");
		assertThat(plan1.getDescription()).isEqualTo("description");
		assertThat(plan1.isBindable()).isTrue();
		assertThat(plan1.isFree()).isFalse();
		assertThat(plan1.getId()).isEqualTo("plan-id");
		assertPlanSerializesWithoutPollutingWithNulls(plan1);
		Plan plan2 = plans.get(1);
		assertThat(plan2.getName()).isEqualTo("plan2");
		assertPlanSerializesWithoutPollutingWithNulls(plan2);
	}

	@Test
	void mapsPlanExtraToMetadata() {
		List<ServicePlanResource> servicePlans = Collections.singletonList(
			ServicePlanResource.builder()
				.entity(ServicePlanEntity.builder()
					.name("plan1")
					.active(true)
					.bindable(true)
					.description("description")
					.extra("{\"displayName\":\"Big Bunny\"}")
					.free(false)
					.build())
				.build());


		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());

		List<Plan> plans = planMapper.toPlans(servicePlans);
		assertThat(plans).hasSize(1);
		Plan plan = plans.get(0);
		assertThat(plan.getMetadata()).hasSize(1);
		assertThat(plan.getMetadata().get("displayName")).isEqualTo("Big Bunny");
	}

	@Test
	void doesNotAddNullInDeserializedEmptySchemas() {
		//given a Schemas object deserialized from a CF API response (which differs from what the builder produces)
	    String serializedSchemas = 
			"{\n" +
			" \"service_instance\": {\n" +
			"  \"create\": {\n" +
			"   \"parameters\": {}\n" +
			"  },\n" +
			"  \"update\": {\n" +
			"   \"parameters\": {}\n" +
			"  }\n" +
			" },\n" +
			" \"service_binding\": {\n" +
			"  \"create\": {\n" +
			"   \"parameters\": {}\n" +
			"  }\n" +
			" }\n" +
			"}\n";
		Schemas schemas = fromJson(serializedSchemas, Schemas.class);
		List<ServicePlanResource> servicePlans = Collections.singletonList(
			ServicePlanResource.builder()
				.entity(ServicePlanEntity.builder()
					.name("plan1")
					.extra("{}")
					.schemas(schemas)
					.build())
				.build());

		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());

		//when
		List<Plan> plans = planMapper.toPlans(servicePlans);

		//then
		assertPlanSerializesWithoutPollutingWithNulls(plans.get(0));
	}

	private <T> T fromJson(String json, Class<T> contentType) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readerFor(contentType).readValue(json);
		}
		catch (IOException e) {
			logger.error("Unable to parse json, caught: " + e, e);
			throw new IllegalStateException(e);
		}
	}


	@Test
	void doesNotAddNullsInEmptySchemasInMappedPlans() {
		//given
		List<ServicePlanResource> servicePlans = Collections.singletonList(
			ServicePlanResource.builder()
				.entity(ServicePlanEntity.builder()
					.name("plan1")
					.extra("{}")
					.schemas(Schemas.builder()
						.serviceBinding(ServiceBindingSchema.builder()
							.create(Schema.builder().build())
							.build())
						.serviceInstance(ServiceInstanceSchema.builder()
							.create(Schema.builder().build())
							.update(Schema.builder().build())
							.build())
						.build())
					.build())
				.build());


		PlanMapper planMapper = new PlanMapper(new PlanMapperProperties());

		//when
		List<Plan> plans = planMapper.toPlans(servicePlans);

		//then
		assertPlanSerializesWithoutPollutingWithNulls(plans.get(0));
	}

	@Test
	void PlanSerializesWithoutPollutingEmptySchemaObjects() {
		Plan plan = Plan.builder()
			.name("10mb")
			.build();
		assertPlanSerializesWithoutPollutingWithNulls(plan);
	}

	private void assertPlanSerializesWithoutPollutingWithNulls(Plan plan)  {
		if (plan == null) {
			return;
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			String serializedSchemas = mapper.writeValueAsString(plan.getSchemas());
			logger.info("serializedSchemas {}", serializedSchemas);
			assertThat(serializedSchemas).doesNotContain(":null");
			// Preventing CC message "Schema service_instance.create.parameters is not valid. Schema must have $schema key but was not present"
			// in schemas like:
			//               "create": { "parameters": {} }
			assertThat(serializedSchemas).doesNotContain("\"parameters\": {}");
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}