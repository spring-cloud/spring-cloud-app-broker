package org.springframework.cloud.appbroker.acceptance.services;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;

public class NoOpCreateServiceInstanceAppBindingWorkflow implements CreateServiceInstanceAppBindingWorkflow {

	public static final Map<String, Object> CREDENTIALS = Collections
		.singletonMap("noop-binding-key", "noop-binding-value");

	private static final Logger LOG = LoggerFactory.getLogger(NoOpCreateServiceInstanceAppBindingWorkflow.class);

	@Value("${spring.cloud.openservicebroker.catalog.services[1].id}")
	private String backingServiceId;

	@Override
	public Mono<Boolean> accept(CreateServiceInstanceBindingRequest request) {
		boolean isAcceptingRequest = request.getServiceDefinitionId().equals(backingServiceId);
		if (LOG.isInfoEnabled()) {
			LOG.info("Got request to accept service binding request: {} and returning {}=({} equals {})", request,
				isAcceptingRequest, request.getServiceDefinitionId(), backingServiceId);
		}
		return Mono.just(isAcceptingRequest);
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder> buildResponse(
		CreateServiceInstanceBindingRequest request,
		CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Got request to create service binding: " + request);
		}
		responseBuilder.credentials(CREDENTIALS);
		return Mono.just(responseBuilder);
	}

}
