/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;

public class ParameterMappingParametersTransformerFactory extends
	ParametersTransformerFactory<BackingService, ParameterMappingParametersTransformerFactory.Config> {

	public ParameterMappingParametersTransformerFactory() {
		super(Config.class);
	}

	@Override
	public ParametersTransformer<BackingService> create(Config config) {
		return (backingType, parameters) -> transform(backingType, parameters, config.getIncludes(),
			config.isIncludeAll());
	}

	private Mono<BackingService> transform(BackingService backingService,
		Map<String, Object> parameters,
		List<String> include,
		boolean includeAll) {
		if (parameters != null) {
			parameters.keySet().stream()
				.filter(o -> includeAll || include.contains(o))
				.forEach(key -> backingService.addParameter(key, parameters.get(key)));
		}

		return Mono.just(backingService);
	}

	@SuppressWarnings("WeakerAccess")
	public static class Config {

		private String include = "";

		private boolean includeAll;

		public List<String> getIncludes() {
			return Arrays.asList(include.split(","));
		}

		public void setInclude(String include) {
			this.include = include;
		}

		public void setIncludeAll(boolean includeAll) {
			this.includeAll = includeAll;
		}

		public boolean isIncludeAll() {
			return includeAll;
		}
	}

}
