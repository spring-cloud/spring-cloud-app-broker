package org.springframework.cloud.appbroker.autoconfigure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseMapper {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected Map<String, Object> toServiceMetaData(String extraJson) {
		if (extraJson ==null) {
			return new HashMap<>();
		}
		logger.debug("extraJson {}", extraJson);
		//enforce check keys can't be mapped to other java primitives: Boolean, Integers
		//potentially customizing jackson deserialization
		// See https://www.baeldung.com/jackson-map
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
		Map<String, Object> metadata = fromJson(extraJson, typeRef);
		logger.debug("metadata {}", metadata);
		return metadata;
	}

	protected <T> T fromJson(String json, TypeReference<HashMap<String, Object>> contentType) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readerFor(contentType).readValue(json);
		}
		catch (IOException e) {
			logger.error("Unable to parse json, caught: " + e, e);
			throw new IllegalStateException(e);
		}
	}

}
