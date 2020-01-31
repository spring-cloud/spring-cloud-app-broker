package com.example.appbroker;

import reactor.core.publisher.Mono;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface ServiceInstanceStateCrudRepository extends ReactiveCrudRepository<ServiceInstance, Long> {

	@Query("select * from service_instance where service_instance_id = :service_instance_id")
	Mono<ServiceInstance> findByServiceInstanceId(@Param("service_instance_id") String serviceInstanceId);

	@Query("delete from service_instance where service_instance_id = :service_instance_id")
	Mono<Void> deleteByServiceInstanceId(@Param("service_instance_id") String serviceInstanceId);

}
