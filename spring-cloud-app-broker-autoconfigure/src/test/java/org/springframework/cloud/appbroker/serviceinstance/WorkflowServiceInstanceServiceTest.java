package org.springframework.cloud.appbroker.serviceinstance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceServiceTest {

	@Mock
	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;
	private WorkflowServiceInstanceService workflowServiceInstanceService;

	@BeforeEach
	void setUp() {
		workflowServiceInstanceService = new WorkflowServiceInstanceService(provisionServiceInstanceWorkflow);
	}

	@Test
	void shouldProvisionServiceInstance() {
		// when we get a request to provision a service instance
		CreateServiceInstanceResponse createServiceInstanceResponse = workflowServiceInstanceService.createServiceInstance(null);

		// then we should delegate in the default workflow
		verify(provisionServiceInstanceWorkflow, times(1)).provision();

		// and then it should return a valid response with the last status
		assertThat(createServiceInstanceResponse).isNotNull();
	}
}