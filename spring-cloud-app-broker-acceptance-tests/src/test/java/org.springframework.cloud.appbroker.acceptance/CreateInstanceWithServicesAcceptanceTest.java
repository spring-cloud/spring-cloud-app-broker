package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_APP_SERVICES = "broker-app-new-services";
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
		"spring.cloud.appbroker.services[0].services[0].plan=standard"
	})
	void shouldPushAppWithServicesBind() {
		// given that a service is available in the marketplace
		setupServiceBrokerForService("db-service");

		// when a service instance is created
		createServiceInstance();

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_APP_SERVICES);
		assertThat(backingApplication).isNotEmpty();

		// and the service bind to it
		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance(SI_1_NAME);
		assertThat(serviceInstance).isNotEmpty();
		assertThat(serviceInstance.get().getApplications()).contains(BROKER_APP_SERVICES);

		// when the service instance is deleted
		deleteServiceInstance();

		// service has no applications bound to it
		Optional<ServiceInstanceSummary> serviceInstanceAfterDeletion = getServiceInstance(SI_1_NAME);
		assertThat(serviceInstanceAfterDeletion).isEmpty();
	}

	@Override
	@AfterEach
	void tearDown() {
		super.tearDown();

		deleteServiceBrokerForService("db-service");
	}
}
