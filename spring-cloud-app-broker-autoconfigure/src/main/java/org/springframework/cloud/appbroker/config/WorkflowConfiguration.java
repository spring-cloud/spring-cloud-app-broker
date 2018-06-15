package org.springframework.cloud.appbroker.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.appbroker.model.ServiceInstance;
import org.springframework.cloud.appbroker.pipeline.output.DeployedServices;
import org.springframework.cloud.appbroker.pipeline.output.GeneratedCredentials;
import org.springframework.cloud.appbroker.pipeline.output.PersistResponse;
import org.springframework.cloud.appbroker.pipeline.output.TransformedParameters;
import org.springframework.cloud.appbroker.pipeline.step.InMemoryPersistenceService;
import org.springframework.cloud.appbroker.pipeline.step.PersistenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public PersistenceService<TransformedParameters<?>, DeployedServices<?>, DeployedApp<?>, GeneratedCredentials<?>, PersistResponse<?>>
	createServicePersistenceStep(ConcurrentHashMap<String, ServiceInstance> inMemoryStore) {
		return new InMemoryPersistenceService(inMemoryStore);
	}

	@Bean
	ConcurrentHashMap<String, ServiceInstance> inMemoryStore() {
		return new ConcurrentHashMap<>();
	}
}
