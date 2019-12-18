package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.MethodSchema;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.Schemas;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.catalog.ServiceInstanceSchema;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceConfigurationYamlDumperTest extends SampleServicesBuilderBaseTest {

	@Test
	void dumpsBrokeredServicesToYamlStringAndDisk() throws IOException {
		//given
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				buildServiceDefinition("mysql", "10mb", "20mb"),
				buildServiceDefinition("noop", "default")))
			.build();

		BrokeredServices brokeredServices = BrokeredServices.builder()
			.service(buildBrokeredService("mysql", "10mb"))
			.service(buildBrokeredService("mysql", "20mb"))
			.service(buildBrokeredService("noop", "default"))
			.build();

		//when
		ServiceConfigurationYamlDumper serviceConfigurationYamlDumper = new ServiceConfigurationYamlDumper();
		String applicationYml = serviceConfigurationYamlDumper.dumpToYamlString(catalog, brokeredServices);

		//then
		String expectedYaml
			= "spring.cloud:\n" +
			"  appbroker.services:\n" +
			"  - apps: null\n" +
			"    planName: \"10mb\"\n" +
			"    serviceName: \"mysql\"\n" +
			"    services:\n" +
			"    - name: \"mysql\"\n" +
			"      parameters: {}\n" +
			"      parametersTransformers: []\n" +
			"      plan: \"10mb\"\n" +
			"      properties: {}\n" +
			"      rebindOnUpdate: false\n" +
			"      serviceInstanceName: \"mysql\"\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceInstance\"\n" +
			"  - apps: null\n" +
			"    planName: \"20mb\"\n" +
			"    serviceName: \"mysql\"\n" +
			"    services:\n" +
			"    - name: \"mysql\"\n" +
			"      parameters: {}\n" +
			"      parametersTransformers: []\n" +
			"      plan: \"20mb\"\n" +
			"      properties: {}\n" +
			"      rebindOnUpdate: false\n" +
			"      serviceInstanceName: \"mysql\"\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceInstance\"\n" +
			"  - apps: null\n" +
			"    planName: \"default\"\n" +
			"    serviceName: \"noop\"\n" +
			"    services:\n" +
			"    - name: \"noop\"\n" +
			"      parameters: {}\n" +
			"      parametersTransformers: []\n" +
			"      plan: \"default\"\n" +
			"      properties: {}\n" +
			"      rebindOnUpdate: false\n" +
			"      serviceInstanceName: \"noop\"\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceInstance\"\n" +
			"  openservicebroker.catalog:\n" +
			"    services:\n" +
			"    - bindable: false\n" +
			"      description: \"description mysql\"\n" +
			"      id: \"mysql-id\"\n" +
			"      name: \"mysql\"\n" +
			"      plans:\n" +
			"      - description: \"description 10mb\"\n" +
			"        free: true\n" +
			"        id: \"10mb-id\"\n" +
			"        name: \"10mb\"\n" +
			"      - description: \"description 20mb\"\n" +
			"        free: true\n" +
			"        id: \"20mb-id\"\n" +
			"        name: \"20mb\"\n" +
			"    - bindable: false\n" +
			"      description: \"description noop\"\n" +
			"      id: \"noop-id\"\n" +
			"      name: \"noop\"\n" +
			"      plans:\n" +
			"      - description: \"description default\"\n" +
			"        free: true\n" +
			"        id: \"default-id\"\n" +
			"        name: \"default\"\n";
		assertThat(applicationYml).isEqualTo(expectedYaml);

		//and when
		serviceConfigurationYamlDumper.dumpToYamlFile(catalog, brokeredServices);

		//then
		String readYamlFromFile = readFileFromDisk();
		assertThat(readYamlFromFile).isEqualTo(expectedYaml);
	}

	/**
	 * Spring cloud osb has a specific syntax to support catalog config schemas
	 * See https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/fe7cea3df1222d6acacdaec670852bf484d8aa60/spring-cloud-open-service-broker-autoconfigure/src/test/resources/catalog-full.yml#L76
	 */
	@Test
	void dumpsBrokeredServicesWithValidSchemaToYamlStringAndDisk() throws IOException {
		//given
		Catalog catalog = Catalog.builder()
			.serviceDefinitions(asList(
				ServiceDefinition.builder()
					.id("noop" + "-id")
					.name("noop")
					.description("description " + "noop")
					.plans(
						Stream.of(new String[] {"default"})
							.map(planName -> Plan.builder()
								.id(planName + "-id")
								.name(planName)
								.description("description " + planName)
								.schemas(Schemas.builder()
									.serviceInstanceSchema(ServiceInstanceSchema.builder()
										.createMethodSchema(MethodSchema.builder()
											.parameters("$schema", "http://json-schema.org/draft-04/schema#")
											.parameters("type", "object")
											.build())
										.updateMethodSchema(MethodSchema.builder()
											.parameters("$schema", "http://json-schema.org/draft-06/schema#")
											.parameters("type", "string")
											.build())
										.build())
									.build())
								.build())
							.toArray(Plan[]::new))
					.build()))
			.build();

		BrokeredServices brokeredServices = BrokeredServices.builder()
			.service(buildBrokeredService("noop", "default"))
			.build();

		//when
		ServiceConfigurationYamlDumper serviceConfigurationYamlDumper = new ServiceConfigurationYamlDumper();
		String applicationYml = serviceConfigurationYamlDumper.dumpToYamlString(catalog, brokeredServices);

		//then
		String expectedYaml
			= "spring.cloud:\n" +
			"  appbroker.services:\n" +
			"  - apps: null\n" +
			"    planName: \"default\"\n" +
			"    serviceName: \"noop\"\n" +
			"    services:\n" +
			"    - name: \"noop\"\n" +
			"      parameters: {}\n" +
			"      parametersTransformers: []\n" +
			"      plan: \"default\"\n" +
			"      properties: {}\n" +
			"      rebindOnUpdate: false\n" +
			"      serviceInstanceName: \"noop\"\n" +
			"    target:\n" +
			"      name: \"SpacePerServiceInstance\"\n" +
			"  openservicebroker.catalog:\n" +
			"    services:\n" +
			"    - bindable: false\n" +
			"      description: \"description noop\"\n" +
			"      id: \"noop-id\"\n" +
			"      name: \"noop\"\n" +
			"      plans:\n" +
			"      - description: \"description default\"\n" +
			"        free: true\n" +
			"        id: \"default-id\"\n" +
			"        name: \"default\"\n" +
			"        schemas:\n" +
			"          service_instance:\n" +
			"            create:\n" +
			"              parameters[$schema]: \"http://json-schema.org/draft-04/schema#\"\n" +
			"              parameters:\n" +
			"                type: \"object\"\n" +
			"            update:\n" +
			"              parameters[$schema]: \"http://json-schema.org/draft-06/schema#\"\n" +
			"              parameters:\n" +
			"                type: \"string\"\n";
		assertThat(applicationYml).isEqualTo(expectedYaml);

		//and when
		serviceConfigurationYamlDumper.dumpToYamlFile(catalog, brokeredServices);

		//then
		String readYamlFromFile = readFileFromDisk();
		assertThat(readYamlFromFile).doesNotContain("$schema:");
		assertThat(readYamlFromFile).isEqualTo(expectedYaml);
	}

	private String readFileFromDisk() throws IOException {
		return new String(
				Files.readAllBytes(
					FileSystems.getDefault().getPath(
						ServiceConfigurationYamlDumper.CATALOG_DUMP_PATH)));
	}

}
