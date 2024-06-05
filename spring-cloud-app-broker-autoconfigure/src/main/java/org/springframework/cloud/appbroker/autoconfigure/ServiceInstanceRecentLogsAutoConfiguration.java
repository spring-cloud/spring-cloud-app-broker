/*
 * Copyright 2016-2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.autoconfigure;

import org.cloudfoundry.logcache.v1.LogCacheClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.recent.ApplicationRecentLogsProvider;
import org.springframework.cloud.appbroker.logging.recent.RecentLogsProvider;
import org.springframework.cloud.appbroker.logging.recent.endpoint.RecentLogsController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ApplicationRecentLogsProvider.class)
@ConditionalOnBean(ApplicationIdsProvider.class)
public class ServiceInstanceRecentLogsAutoConfiguration {

	@Bean
	public RecentLogsProvider recentLogsProvider(
		LogCacheClient logCacheClient,
		ApplicationIdsProvider applicationIdsProvider) {
		return new ApplicationRecentLogsProvider(logCacheClient, applicationIdsProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public RecentLogsController recentLogsController(RecentLogsProvider recentLogsProvider) {
		return new RecentLogsController(recentLogsProvider);
	}

}
