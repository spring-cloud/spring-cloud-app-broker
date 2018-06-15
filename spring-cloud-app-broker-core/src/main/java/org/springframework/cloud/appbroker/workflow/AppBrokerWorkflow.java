package org.springframework.cloud.appbroker.workflow;

import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;

public interface AppBrokerWorkflow<REQ extends ServiceBrokerRequest, RES> {

	RES perform(REQ requestData);
}

