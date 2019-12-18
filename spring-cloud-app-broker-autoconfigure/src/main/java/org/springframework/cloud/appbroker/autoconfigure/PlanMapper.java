package org.springframework.cloud.appbroker.autoconfigure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.serviceplans.Parameters;
import org.cloudfoundry.client.v2.serviceplans.Schema;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.catalog.MethodSchema;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.Schemas;
import org.springframework.cloud.servicebroker.model.catalog.ServiceBindingSchema;
import org.springframework.cloud.servicebroker.model.catalog.ServiceInstanceSchema;

public class PlanMapper extends BaseMapper {

	private static final Logger logger = LoggerFactory.getLogger(PlanMapper.class);

	private final PlanMapperProperties properties;

	public PlanMapper(PlanMapperProperties properties) {
		this.properties = properties;
	}

	public List<Plan> toPlans(List<ServicePlanResource> servicePlans) {
		return servicePlans.stream()
			.map(this::toPlan)
			.collect(Collectors.toList());
	}

	private Plan toPlan(ServicePlanResource resource) {
		Plan.PlanBuilder planBuilder = Plan.builder();

		Metadata metadata = resource.getMetadata();
		if (metadata != null) { //mostly during lazy unit tests
			planBuilder = planBuilder
				.id(metadata.getId());
		}
		ServicePlanEntity entity = ResourceUtils.getEntity(resource);
		planBuilder = planBuilder
			.name(entity.getName())
			.free(entity.getFree())
			.bindable(entity.getBindable())
			.description(entity.getDescription())
			.metadata(toServiceMetaData(entity.getExtra()));
		planBuilder = toSchemas(planBuilder, entity.getSchemas());

		Plan plan = planBuilder.build();

		logger.debug("plan entity {}", plan);
		return plan;
	}

	private Plan.PlanBuilder toSchemas(Plan.PlanBuilder planBuilder, org.cloudfoundry.client.v2.serviceplans.Schemas entitySchemas) {
		if (entitySchemas == null) {
			return planBuilder;
		}
		Schemas.SchemasBuilder builder = toBindingBuilder(Schemas.builder(), entitySchemas.getServiceBinding());
		builder = toInstanceBuilder(builder, entitySchemas.getServiceInstance());
		Schemas schemas = builder.build();
		planBuilder= planBuilder.schemas(schemas);
		return planBuilder;
	}

	private Schemas.SchemasBuilder toInstanceBuilder(Schemas.SchemasBuilder builder, org.cloudfoundry.client.v2.serviceplans.ServiceInstanceSchema entitySchemas) {
		if (entitySchemas == null) {
			return builder;
		}
		ServiceInstanceSchema.ServiceInstanceSchemaBuilder serviceInstanceSchemaBuilder =
			ServiceInstanceSchema.builder();
		serviceInstanceSchemaBuilder = toCreateServiceInstanceSchemaBuilder(serviceInstanceSchemaBuilder, entitySchemas.getCreate());
		serviceInstanceSchemaBuilder = toUpdateServiceInstanceSchemaBuilder(serviceInstanceSchemaBuilder, entitySchemas.getUpdate());
		builder = builder.serviceInstanceSchema(serviceInstanceSchemaBuilder.build());
		return builder;
	}

	private Schemas.SchemasBuilder toBindingBuilder(Schemas.SchemasBuilder builder, org.cloudfoundry.client.v2.serviceplans.ServiceBindingSchema entitySchemas) {
		if (entitySchemas == null) {
			return builder;
		}
		ServiceBindingSchema.ServiceBindingSchemaBuilder serviceBindingSchemaBuilder = ServiceBindingSchema.builder();
		serviceBindingSchemaBuilder = toCreateServiceBindingSchemaBuilder(serviceBindingSchemaBuilder, entitySchemas.getCreate());

		builder = builder.serviceBindingSchema(serviceBindingSchemaBuilder.build());
		return builder;
	}

	private ServiceBindingSchema.ServiceBindingSchemaBuilder toCreateServiceBindingSchemaBuilder(
		ServiceBindingSchema.ServiceBindingSchemaBuilder builder, Schema entitySchema) {
		if (entitySchema == null) {
			return builder;
		}
		MethodSchema.MethodSchemaBuilder methodSchemaBuilder = toParameters(entitySchema.getParameters());
		if (methodSchemaBuilder == null) {
			return builder;
		}
		return builder
			.createMethodSchema(methodSchemaBuilder
				.build());
	}

	private ServiceInstanceSchema.ServiceInstanceSchemaBuilder toUpdateServiceInstanceSchemaBuilder(
		ServiceInstanceSchema.ServiceInstanceSchemaBuilder builder, Schema entitySchema) {
		if (entitySchema == null) {
			return builder;
		}
		MethodSchema.MethodSchemaBuilder methodSchemaBuilder = toParameters(entitySchema.getParameters());
		if (methodSchemaBuilder == null) {
			return builder;
		}
		return builder
			.updateMethodSchema(methodSchemaBuilder
			.build());
	}

	private ServiceInstanceSchema.ServiceInstanceSchemaBuilder toCreateServiceInstanceSchemaBuilder(
		ServiceInstanceSchema.ServiceInstanceSchemaBuilder builder, Schema entitySchema) {
		if (entitySchema == null) {
			return builder;
		}
		MethodSchema.MethodSchemaBuilder methodSchemaBuilder = toParameters(entitySchema.getParameters());
		if (methodSchemaBuilder == null) {
			return builder;
		}
		return builder
			.createMethodSchema(methodSchemaBuilder
				.build());
	}

	/**
	 * maps a schema. Care to taken to avoid setting empty schema that CF rejects
	 * @return a Builder if a schema should be set, or null otherwise
	 */
	private MethodSchema.MethodSchemaBuilder toParameters(Parameters parameters) {
		if (parameters == null) {
			return null;
		}
		Map<String, Object> properties = parameters.getProperties();
		String jsonSchema = parameters.getJsonSchema();
		String type = parameters.getType();
		if (properties == null &&
			jsonSchema == null &&
			type == null) {
			return null;
		}
		MethodSchema.MethodSchemaBuilder builder = MethodSchema.builder();
		if (properties != null) {
			builder = builder.parameters("properties", properties);
		}
		if (jsonSchema != null) {
			builder = builder.parameters("$schema", jsonSchema);
		}
		if (type!= null) {
			builder = builder.parameters("type", type);
		}
		return builder;
	}

}
