/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.sample.transformers;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.apache.logging.log4j.util.Strings;

public class URLLocalhostStubResponseTransformer extends ResponseTransformer {

	@Override
	public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
		String responseBody = updateUrlsToLocalhost(response);
		return Response.Builder.like(response)
			.but()
			.body(responseBody)
			.build();
	}

	private static String updateUrlsToLocalhost(Response response) {
		String responseBody = response.getBodyAsString();
		if (Strings.isNotEmpty(responseBody)) {
			responseBody = responseBody.replaceAll("\"https://login.cf.*\"", "\"http://localhost\"");
			responseBody = responseBody.replaceAll("\"https://uaa.cf.*\"", "\"http://localhost\"");
			responseBody = responseBody.replaceAll("\"https://api.cf.*\"", "\"http://localhost\"");
			responseBody = responseBody.replaceAll("\"name\": \"apps.*\"", "\"name\": \"localhost\"");
			responseBody = responseBody.replaceAll("\"ssh.cf.*\"", "\"localhost\"");
			responseBody = responseBody.replaceAll("\"https://credhub.cf.*\"", "\"http://localhost\"");
			responseBody = responseBody.replaceAll("\"wss://doppler.cf.*\"", "\"wss://localhost\"");
		}
		return responseBody;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}
}
