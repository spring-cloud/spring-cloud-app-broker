package org.springframework.cloud.appbroker.autoconfigure;

public class ServiceDefinitionMapperProperties {

	public static final String PROPERTY_PREFIX = DynamicCatalogConstants.PROPERTY_PREFIX + ".catalog.services";

	/**
	 * Adds a suffix to service names
	 */
	public static final String SUFFIX_PROPERTY_KEY=".suffix";

	/**
	 * Excludes some broker whose name matches the regexp.
	 * Default is missing property, i.e. null (i.e no exclusion)
	 */
	public static final String EXCLUDE_BROKER_PROPERTY_KEY=".excludeBrokerNamesRegexp";

	private String suffix ="";

	private String excludeBrokerNamesRegexp = null;

	public String getSuffix() { return suffix; }

	public void setSuffix(String suffix) { this.suffix = suffix; }

	public String getExcludeBrokerNamesRegexp() { return excludeBrokerNamesRegexp; }

	public void setExcludeBrokerNamesRegexp(String excludeBrokerNamesRegexp) {
		this.excludeBrokerNamesRegexp = excludeBrokerNamesRegexp;
	}

}
