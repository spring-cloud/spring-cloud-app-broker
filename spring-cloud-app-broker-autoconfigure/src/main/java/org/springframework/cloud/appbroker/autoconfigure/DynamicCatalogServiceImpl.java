package org.springframework.cloud.appbroker.autoconfigure;

import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.util.OperationsLogging;
import org.cloudfoundry.util.PaginationUtils;
import org.cloudfoundry.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

public class DynamicCatalogServiceImpl implements DynamicCatalogService {

	private static final Logger logger = LoggerFactory.getLogger(DynamicCatalogServiceImpl.class);

	protected CloudFoundryOperations operations;
	private Mono<CloudFoundryClient> cloudFoundryClient;
	private CloudFoundryTargetProperties targetProperties;
	private ServiceDefinitionMapper serviceDefinitionMapper;

	public DynamicCatalogServiceImpl(
		CloudFoundryDeploymentProperties defaultDeploymentProperties,
		CloudFoundryOperations operations,
		CloudFoundryClient cloudFoundryClient,
		CloudFoundryTargetProperties targetProperties,
		ServiceDefinitionMapper serviceDefinitionMapper) {

		this.serviceDefinitionMapper = serviceDefinitionMapper;
		this.operations = operations;
		this.cloudFoundryClient = Mono.just(cloudFoundryClient);
		this.targetProperties = targetProperties;
	}

	@Override
	public List<ServiceDefinition> fetchServiceDefinitions() {
		final Mono<String> finalSpaceId = getOperationsForOrgAndSpace().cast(DefaultCloudFoundryOperations.class)
			.flatMap(DefaultCloudFoundryOperations::getSpaceId);
		//Inspired from cf-java-client: org.cloudfoundry.operations.services.DefaultServices.listServiceOfferings
		return Mono.zip(this.cloudFoundryClient, finalSpaceId)
			.flatMapMany(function((cloudFoundryClient, spaceId) -> requestListServices(cloudFoundryClient, spaceId)
				.map(resource -> Tuples.of(cloudFoundryClient, resource))
			))
			.flatMap(function((cloudFoundryClient, resource) -> Mono.zip(
				Mono.just(resource),
				getServicePlans(cloudFoundryClient, ResourceUtils.getId(resource))
			)))
			.filter(t-> filterServiceEntity(t.getT1().getEntity()))
			.map(function(
				(resource1, servicePlans) -> serviceDefinitionMapper.toServiceDefinition(resource1, servicePlans)))
			.transform(OperationsLogging.log("List Service Definitions"))
			.checkpoint()
			.collectList().block();
	}

	private boolean filterServiceEntity(ServiceEntity entity) {
		return serviceDefinitionMapper.shouldMapServiceEntity(entity);
	}


	private static Mono<List<ServicePlanResource>> getServicePlans(CloudFoundryClient cloudFoundryClient, String serviceId) {
		return requestListServicePlans(cloudFoundryClient, serviceId)
			.collectList();
	}

	private static Flux<ServicePlanResource> requestListServicePlans(CloudFoundryClient cloudFoundryClient, String serviceId) {
		return PaginationUtils
			.requestClientV2Resources(page -> cloudFoundryClient.servicePlans()
				.list(ListServicePlansRequest.builder()
					.page(page)
					.serviceId(serviceId)
					.build()));
	}

	private static Flux<ServiceResource> requestListServices(CloudFoundryClient cloudFoundryClient, String spaceId) {
		return PaginationUtils
			.requestClientV2Resources(page -> cloudFoundryClient.spaces()
				.listServices(ListSpaceServicesRequest.builder()
					.page(page)
					.spaceId(spaceId)
					.build()));
	}


	private Mono<CloudFoundryOperations> getOperationsForOrgAndSpace() {
		return Mono.just(this.operations)
			.cast(DefaultCloudFoundryOperations.class)
			.map(cfOperations -> DefaultCloudFoundryOperations.builder()
				.from(cfOperations)
				.organization(targetProperties.getDefaultOrg())
				.space(targetProperties.getDefaultSpace())
				.build())
			.cast(CloudFoundryOperations.class)
			.doOnRequest(item -> logger
				.info("targetted operations to space {} and org {}", targetProperties.getDefaultOrg(),
					targetProperties.getDefaultSpace()));
	}


}
