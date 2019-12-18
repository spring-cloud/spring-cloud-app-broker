/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryProperties;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogConstants;
import org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfiguration;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import static org.assertj.core.api.Assertions.assertThat;

// Expects the Cf client properties to be injected as system properties
// and the corresponding Cf marketplace to be non empty
class DynamicServiceAutoConfigurationAcceptanceTest {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			CloudFoundryProperties.class,
			CloudFoundryClientConfiguration.class,
			DynamicCatalogServiceAutoConfiguration.class
		))
		.withPropertyValues(DynamicCatalogConstants.OPT_IN_PROPERTY+"=true");

	@Test
	void contextLoadWithCatalogCreatedWhenPropertiesProvided() {
		configuredContext()
			.run(context -> {
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).isNotEmpty();
				Catalog catalog = context.getBean(Catalog.class);
				assertThat(catalog.getServiceDefinitions()).isNotEmpty();

				List<ServiceDefinition> serviceDefinitions = catalog.getServiceDefinitions();
				assertThat(serviceDefinitions).isNotEmpty();
				serviceDefinitions.stream()
					.map(ServiceDefinition::getPlans)
					.flatMap(Collection::stream)
					.forEach(this::assertPlanIsValid);

				assertThat(context).hasSingleBean(Catalog.class);
				assertThat(context).hasSingleBean(BrokeredServices.class);
			});
	}

	private void assertPlanIsValid(Plan plan)  {
		assertThat(plan.getId()).isNotNull(); // Plan id is required
		assertPlanSerializesWithoutInvalidSchemas(plan);
	}

	private void assertPlanSerializesWithoutInvalidSchemas(Plan plan) {
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


	@Test
	@Ignore
		//Not yet implemented
	void catalogFetchingFailuresThrowsException() {
		//TODO: fail if the flux contains error events (was the case when Jackson was not configured to ignore
	}


	private ApplicationContextRunner configuredContext() {
		return this.contextRunner;
	}



}
