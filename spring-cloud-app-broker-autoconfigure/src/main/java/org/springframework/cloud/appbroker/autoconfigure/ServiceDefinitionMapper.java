package org.springframework.cloud.appbroker.autoconfigure;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class ServiceDefinitionMapper extends BaseMapper {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final PlanMapper planMapper;
	private ServiceDefinitionMapperProperties serviceDefinitionMapperProperties;

	public ServiceDefinitionMapper(PlanMapper planMapper,
		ServiceDefinitionMapperProperties serviceDefinitionMapperProperties) {
		this.planMapper = planMapper;
		this.serviceDefinitionMapperProperties = serviceDefinitionMapperProperties;
	}

	/**
	 * Decides whether the specified entity should be mapped
	 * @param entity
	 * @return true if the entity should be preserved and mapped, false if the entity should be skipped
	 */
	public boolean shouldMapServiceEntity(ServiceEntity entity) {
		String excludeBrokerNamesRegexp = serviceDefinitionMapperProperties.getExcludeBrokerNamesRegexp();
		if (excludeBrokerNamesRegexp == null) {
			return true;
		}
		String serviceBrokerName = entity.getServiceBrokerName();
		if (serviceBrokerName == null) {
			logger.error("Unexpected missing broker name in service {}, filtering it out", entity);
			return false;
		}
		boolean shouldMapEntity = !Pattern.matches(excludeBrokerNamesRegexp, serviceBrokerName);
		if (!shouldMapEntity) {
			logger.info("Filtering out service {} as it is excluded by configured rexgexp {}", entity,
				excludeBrokerNamesRegexp);
		}
		return shouldMapEntity;
	}

	public ServiceDefinition toServiceDefinition(ServiceResource resource,
		List<ServicePlanResource> servicePlans) {

		ServiceEntity entity = resource.getEntity();
		return ServiceDefinition.builder()
			.id(resource.getMetadata().getId())
			.name(entity.getLabel() + serviceDefinitionMapperProperties.getSuffix())
			.description(entity.getDescription())
			.tags(safeList(entity.getTags()))
			.bindable(safeBoolean(entity.getBindable()))
			.planUpdateable(safeBoolean(entity.getPlanUpdateable()))
			.bindingsRetrievable(safeBoolean(entity.getBindingsRetrievable()))
			.instancesRetrievable(safeBoolean(entity.getInstancesRetrievable()))
			.plans(planMapper.toPlans(safeList(servicePlans)))
			.metadata(toServiceMetaData(entity.getExtra()))
			.build();
	}

	private <R> List<R> safeList(List<R> list) {
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}

	private Boolean safeBoolean(Boolean field) {
		if (field == null) {
			return Boolean.FALSE;
		}
		return field;
	}

}
