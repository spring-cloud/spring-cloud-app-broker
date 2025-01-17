/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.cloud.appbroker.logging.recent.endpoint;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

class MultipartEncoder implements Closeable {

	private final byte[] bytesCRLF = { '\r', '\n' };

	private final byte[] bytesSEP = { '-', '-' };

	private final byte[] boundary;

	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	MultipartEncoder(String boundary) {
		this.boundary = boundary.getBytes();
	}

	void append(byte[] part) {
		try {
			this.out.write(this.bytesCRLF);
			this.out.write(this.bytesSEP);
			this.out.write(this.boundary);
			this.out.write(this.bytesCRLF);
			this.out.write(this.bytesCRLF);
			this.out.write(part);
		}
		catch (IOException ex) {
			throw new EncodingException(ex);
		}
	}

	byte[] terminateAndGetBytes() {
		try {
			this.out.write(this.bytesCRLF);
			this.out.write(this.bytesSEP);
			this.out.write(this.boundary);
			this.out.write(this.bytesSEP);
			this.out.write(this.bytesCRLF);
			final byte[] bytes = this.out.toByteArray();
			this.out.close();
			return bytes;
		}
		catch (IOException ex) {
			throw new EncodingException(ex);
		}
	}

	@Override
	public void close() throws IOException {
		this.out.close();
	}

}
