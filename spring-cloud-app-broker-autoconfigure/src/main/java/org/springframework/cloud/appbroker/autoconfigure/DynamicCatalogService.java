package org.springframework.cloud.appbroker.autoconfigure;

import java.util.List;

import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public interface DynamicCatalogService {

	List<ServiceDefinition> fetchServiceDefinitions();

}
