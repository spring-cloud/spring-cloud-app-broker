package org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy;

import java.util.function.BiConsumer;

import org.springframework.cloud.appbroker.workflow.action.createserviceinstance.CreateServiceRequestContext;


public interface BackingAppDeployer extends BiConsumer<BackingAppParameters, CreateServiceRequestContext> {

}
