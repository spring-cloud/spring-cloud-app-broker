package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_APP_SERVICES = "broker-app-new-services";
	private static final String SI_1_NAME = "service-instance-created";
	private static final String SI_2_NAME = "service-instance-existing";
	private static final String SERVICE_NAME = "db-service";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_APP_SERVICES,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",

		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + SI_1_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + SI_1_NAME,
		"spring.cloud.appbroker.services[0].services[0].name=" + SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=standard",
		
		"spring.cloud.appbroker.services[0].apps[0].services[1].service-instance-name=" + SI_2_NAME
	})
	void shouldPushAppWithServicesBind() {
		// given that a service is available in the marketplace
		setupServiceBrokerForService(SERVICE_NAME);
		createServiceInstance("standard", SERVICE_NAME, SI_2_NAME, emptyMap());

		// when a service instance is created
		createServiceInstance();

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_APP_SERVICES);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		Optional<ServiceInstanceSummary> serviceInstance1 = getServiceInstance(SI_1_NAME);
		assertThat(serviceInstance1).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(BROKER_APP_SERVICES));

		Optional<ServiceInstanceSummary> serviceInstance2 = getServiceInstance(SI_2_NAME);
		assertThat(serviceInstance2).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(BROKER_APP_SERVICES));

		// when the service instance is deleted
		deleteServiceInstance();

		// service has no applications bound to it
		Optional<ServiceInstanceSummary> serviceInstance1AfterDeletion = getServiceInstance(SI_1_NAME);
		assertThat(serviceInstance1AfterDeletion).isEmpty();

		Optional<ServiceInstanceSummary> serviceInstance2AfterDeletion = getServiceInstance(SI_2_NAME);
		assertThat(serviceInstance2AfterDeletion).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).isEmpty());

		deleteServiceInstance(SI_2_NAME);
	}

	@Override
	@AfterEach
	void tearDown() {
		super.tearDown();

		deleteServiceBrokerForService(SERVICE_NAME);
	}
}
