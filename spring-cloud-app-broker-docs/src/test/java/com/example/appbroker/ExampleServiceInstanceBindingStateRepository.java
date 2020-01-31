package com.example.appbroker;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.state.ServiceInstanceBindingStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceState;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

class ExampleServiceInstanceBindingStateRepository implements ServiceInstanceBindingStateRepository {

	private final ServiceInstanceBindingStateCrudRepository serviceInstanceBindingStateCrudRepository;

	ExampleServiceInstanceBindingStateRepository(
			ServiceInstanceBindingStateCrudRepository serviceInstanceBindingStateCrudRepository) {
		this.serviceInstanceBindingStateCrudRepository = serviceInstanceBindingStateCrudRepository;
	}

	@Override
	public Mono<ServiceInstanceState> saveState(String serviceInstanceId, String bindingId, OperationState state,
			String description) {
		return serviceInstanceBindingStateCrudRepository
				.findByServiceInstanceIdAndBindingId(serviceInstanceId, bindingId)
				.switchIfEmpty(Mono.just(new ServiceInstanceBinding()))
				.flatMap(binding -> {
					binding.setServiceInstanceId(serviceInstanceId);
					binding.setBindingId(bindingId);
					binding.setOperationState(state);
					binding.setDescription(description);
					return Mono.just(binding);
				})
				.flatMap(serviceInstanceBindingStateCrudRepository::save)
				.map(ExampleServiceInstanceBindingStateRepository::toServiceInstanceState);
	}

	@Override
	public Mono<ServiceInstanceState> getState(String serviceInstanceId, String bindingId) {
		return serviceInstanceBindingStateCrudRepository
				.findByServiceInstanceIdAndBindingId(serviceInstanceId, bindingId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException(
						"Unknown binding: serviceInstanceId=" + serviceInstanceId + ", bindingId=" + bindingId)))
				.map(ExampleServiceInstanceBindingStateRepository::toServiceInstanceState);
	}

	@Override
	public Mono<ServiceInstanceState> removeState(String serviceInstanceId, String bindingId) {
		return getState(serviceInstanceId, bindingId)
				.doOnNext(serviceInstanceState -> serviceInstanceBindingStateCrudRepository
						.deleteByServiceInstanceIdAndBindingId(serviceInstanceId, bindingId));
	}

	private static ServiceInstanceState toServiceInstanceState(ServiceInstanceBinding binding) {
		return new ServiceInstanceState(binding.getOperationState(), binding.getDescription(), null);
	}

}
