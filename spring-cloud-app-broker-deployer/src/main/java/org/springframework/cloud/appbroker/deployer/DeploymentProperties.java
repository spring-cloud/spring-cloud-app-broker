/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.deployer;

import org.springframework.cloud.appbroker.deployer.util.ByteSizeUtils;

public class DeploymentProperties {
	/**
	 * The deployment property for the count (number of app instances).
	 * If not provided, a deployer should assume 1 instance.
	 */
	public static final String COUNT_PROPERTY_KEY = "count";

	/**
	 * The deployment property for the group to which an app belongs.
	 * If not provided, a deployer should assume no group.
	 */
	public static final String GROUP_PROPERTY_KEY = "group";

	/**
	 * The deployment property that indicates if each app instance should have an index value
	 * within a sequence from 0 to N-1, where N is the value of the {@value #COUNT_PROPERTY_KEY}
	 * property. If not provided, a deployer should assume app instance indexing is not necessary.
	 */
	public static final String INDEXED_PROPERTY_KEY = "indexed";

	/**
	 * The property to be set at each instance level to specify the sequence number
	 * amongst 0 to N-1, where N is the value of the {@value #COUNT_PROPERTY_KEY} property.
	 * Specified as CAPITAL_WITH_UNDERSCORES as this is typically passed as an environment
	 * variable, but when targeting a Spring app, other variations may apply.
	 *
	 * @see #INDEXED_PROPERTY_KEY
	 */
	public static final String INSTANCE_INDEX_PROPERTY_KEY = "INSTANCE_INDEX";

	/**
	 * The deployment property for the memory setting for the container that will run the app.
	 * The memory is specified in <a href="https://en.wikipedia.org/wiki/Mebibyte">Mebibytes</a>,
	 * by default, with optional case-insensitive trailing unit 'm' and 'g' being supported,
	 * for mebi- and giga- respectively.
	 * <p>
	 * 1 MiB = 2^20 bytes = 1024*1024 bytes vs. the decimal based 1MB = 10^6 bytes = 1000*1000 bytes,
	 * <p>
	 * Implementations are expected to translate this value to the target platform as faithfully as possible.
	 *
	 * @see ByteSizeUtils
	 */
	public static final String MEMORY_PROPERTY_KEY = "memory";

	/**
	 * The deployment property for the disk setting for the container that will run the app.
	 * The memory is specified in <a href="https://en.wikipedia.org/wiki/Mebibyte">Mebibytes</a>,
	 * by default, with optional case-insensitive trailing unit 'm' and 'g' being supported,
	 * for mebi- and giga- respectively.
	 * <p>
	 * 1 MiB = 2^20 bytes = 1024*1024 bytes vs. the decimal based 1MB = 10^6 bytes = 1000*1000 bytes,
	 * <p>
	 * Implementations are expected to translate this value to the target platform as faithfully as possible.
	 *
	 * @see ByteSizeUtils
	 */
	public static final String DISK_PROPERTY_KEY = "disk";

	/**
	 * The deployment property for the cpu setting for the container that will run the app.
	 * The cpu is specified as whole multiples or decimal fractions of virtual cores. Some platforms will not
	 * support setting cpu and will ignore this setting. Other platforms may require whole numbers and might
	 * round up. Exactly how this property affects the deployments will vary between implementations.
	 */
	public static final String CPU_PROPERTY_KEY = "cpu";
}
