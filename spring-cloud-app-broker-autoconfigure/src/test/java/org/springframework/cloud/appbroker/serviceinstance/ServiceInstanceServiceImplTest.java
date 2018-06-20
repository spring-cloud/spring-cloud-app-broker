package org.springframework.cloud.appbroker.serviceinstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceInstanceServiceImplTest {

	@Mock
	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;
	private ServiceInstanceServiceImpl serviceInstanceServiceImpl;

	@BeforeEach
	void setUp() {
		serviceInstanceServiceImpl = new ServiceInstanceServiceImpl(provisionServiceInstanceWorkflow);
	}

	@Test
	void shouldProvisionServiceInstance() {
		// when we get a request to provision a service instance
		CreateServiceInstanceResponse createServiceInstanceResponse = serviceInstanceServiceImpl.createServiceInstance(null);

		// then we should delegate in the default workflow
		verify(provisionServiceInstanceWorkflow, times(1)).provision(any());

		// and then it should return a valid response with the last status
		assertThat(createServiceInstanceResponse, is(notNullValue()));
	}
}