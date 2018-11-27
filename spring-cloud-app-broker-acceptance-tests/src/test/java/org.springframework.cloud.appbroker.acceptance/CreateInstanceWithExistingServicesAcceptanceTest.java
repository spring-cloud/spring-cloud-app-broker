package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithExistingServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_APP_SERVICES = "broker-app-services";
	private static final String SI_1_NAME = "service-instance-1";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_APP_SERVICES,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + SI_1_NAME
	})
	void shouldPushAppWithServicesBind() {
		// given that a service instance of the specified service exists
		setupServiceBrokerForService("db-service");
		createServiceInstance("standard", "db-service", SI_1_NAME, emptyMap());

		// when a service instance is created
		createServiceInstance();

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_APP_SERVICES);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the service bind to it
		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance(SI_1_NAME);
		assertThat(serviceInstance).hasValueSatisfying(summary ->
			assertThat(summary.getApplications()).contains(BROKER_APP_SERVICES));

		// when the service instance is deleted
		deleteServiceInstance();

		// service has no applications bind to it
		Optional<ServiceInstanceSummary> serviceInstanceAfterDeletion = getServiceInstance(SI_1_NAME);
		assertThat(serviceInstanceAfterDeletion).hasValueSatisfying(summary ->
			assertThat(summary.getApplications()).isEmpty());
	}

	@Override
	@AfterEach
	void tearDown() {
		super.tearDown();

		deleteServiceInstance(SI_1_NAME);
		deleteServiceBrokerForService("db-service");
	}
}
