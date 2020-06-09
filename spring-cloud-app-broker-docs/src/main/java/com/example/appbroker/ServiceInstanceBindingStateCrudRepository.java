package com.example.appbroker;

import reactor.core.publisher.Mono;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface ServiceInstanceBindingStateCrudRepository extends ReactiveCrudRepository<ServiceInstanceBinding, Long> {

	@Query("select * from service_instance_binding " +
			"where service_instance_id = :service_instance_id " +
			"and binding_id = :binding_id")
	Mono<ServiceInstanceBinding> findByServiceInstanceIdAndBindingId(
			@Param("service_instance_id") String serviceInstanceId,
			@Param("binding_id") String bindingId);


	@Query("delete from service_instance_binding " +
			"where service_instance_id = :service_instance_id " +
			"and binding_id = :binding_id")
	Mono<Void> deleteByServiceInstanceIdAndBindingId(
			@Param("service_instance_id") String serviceInstanceId,
			@Param("binding_id") String bindingId);

}
