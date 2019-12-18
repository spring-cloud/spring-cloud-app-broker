package org.springframework.cloud.appbroker.autoconfigure;

/**
 * Note: this is not yet a full Properties class with fields since DynamicCatalogService does not yet require config.
 * Instead, granular Properties classes are defined for services and plan mapper.
 */
public class DynamicCatalogConstants {

	public static final String PROPERTY_PREFIX = "osbcmdb.dynamic-catalog";
	public static final String OPT_IN_PROPERTY = PROPERTY_PREFIX + ".enabled";

}
