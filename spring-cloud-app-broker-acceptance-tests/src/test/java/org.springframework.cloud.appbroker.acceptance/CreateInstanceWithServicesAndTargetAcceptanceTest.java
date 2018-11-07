package org.springframework.cloud.appbroker.acceptance;

import java.util.List;
import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServicesAndTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_APP_SERVICES = "services-target";
	private static final String SI_1_NAME = "service-instance-1";
	private static final String SERVICE_1_NAME = "db-service";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_APP_SERVICES,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + SI_1_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + SI_1_NAME,
		"spring.cloud.appbroker.services[0].services[0].name=" + SERVICE_1_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=standard",
		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance"
	})
	void shouldPushAppWithServicesBind() {
		// given that a service is available in the marketplace
		setupServiceBrokerForService("db-service");

		// when a service instance is created with target
		createServiceInstance();
		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance();
		assertThat(serviceInstance).isNotEmpty();

		// then backing applications are deployed in a space named as the service instance id
		String space = serviceInstance.orElseThrow(RuntimeException::new).getId();

		Optional<ApplicationSummary> backingApplication =
			getApplicationSummaryByNameAndSpace(BROKER_APP_SERVICES, space);
		assertThat(backingApplication).isNotEmpty();

		// and the backing service bind to it
		Optional<ServiceInstanceSummary> backingServiceInstance = getServiceInstance(SI_1_NAME, space);
		assertThat(backingServiceInstance).isNotEmpty();
		assertThat(backingServiceInstance.get().getApplications()).contains(BROKER_APP_SERVICES);

		// when the service instance is deleted
		deleteServiceInstance();

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(space);
	}

	@Override
	@AfterEach
	void tearDown() {
		super.tearDown();

		deleteServiceBrokerForService("db-service");
	}
}
