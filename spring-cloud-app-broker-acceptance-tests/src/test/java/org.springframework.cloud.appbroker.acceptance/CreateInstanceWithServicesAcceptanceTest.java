package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-create-services";
	private static final String SI_NAME = "si-create-services";

	private static final String BACKING_SI_1_NAME = "backing-service-instance-created";
	private static final String BACKING_SI_2_NAME = "backing-service-instance-existing";
	private static final String BACKING_SERVICE_NAME = "backing-service";

	@BeforeEach
	void setUpServiceBrokerForService() {
		deployServiceBrokerForService(BACKING_SERVICE_NAME);
	}

	@AfterEach
	void tearDownServiceBrokerForService() {
		deleteServiceBrokerForService(BACKING_SERVICE_NAME);
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",

		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_1_NAME,
		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=standard",
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_1_NAME,

		"spring.cloud.appbroker.services[0].apps[0].services[1].service-instance-name=" + BACKING_SI_2_NAME
	})
	void deployAppsAndCreateServicesOnCreateService() {
		// given that a service is available in the marketplace
		createServiceInstance(BACKING_SERVICE_NAME, "standard", BACKING_SI_2_NAME, emptyMap());

		// when a service instance is created
		createServiceInstance(SI_NAME);

		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance(SI_NAME);
		assertThat(serviceInstance).hasValueSatisfying(value ->
			assertThat(value.getLastOperation()).contains("completed"));

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		Optional<ServiceInstanceSummary> serviceInstance1 = getServiceInstance(BACKING_SI_1_NAME);
		assertThat(serviceInstance1).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(APP_NAME));

		Optional<ServiceInstanceSummary> serviceInstance2 = getServiceInstance(BACKING_SI_2_NAME);
		assertThat(serviceInstance2).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(APP_NAME));

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// service has no applications bound to it
		Optional<ServiceInstanceSummary> serviceInstance1AfterDeletion = getServiceInstance(BACKING_SI_1_NAME);
		assertThat(serviceInstance1AfterDeletion).isEmpty();

		Optional<ServiceInstanceSummary> serviceInstance2AfterDeletion = getServiceInstance(BACKING_SI_2_NAME);
		assertThat(serviceInstance2AfterDeletion).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).isEmpty());

		deleteServiceInstance(BACKING_SI_2_NAME);
	}
}
