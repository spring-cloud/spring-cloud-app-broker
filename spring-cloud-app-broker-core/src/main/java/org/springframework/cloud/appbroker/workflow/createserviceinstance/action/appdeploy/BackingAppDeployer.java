package org.springframework.cloud.appbroker.workflow.createserviceinstance.action.appdeploy;

import java.util.function.BiConsumer;

import org.springframework.cloud.appbroker.workflow.createserviceinstance.CreateServiceRequestContext;


public interface BackingAppDeployer extends BiConsumer<BackingAppParameters, CreateServiceRequestContext> {

}
