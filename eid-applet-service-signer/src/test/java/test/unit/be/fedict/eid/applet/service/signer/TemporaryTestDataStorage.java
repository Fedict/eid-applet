/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

/*
 * Copyright (C) 2008-2009 FedICT.
 * This file is part of the eID Applet Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.unit.be.fedict.eid.applet.service.signer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import be.fedict.eid.applet.service.signer.TemporaryDataStorage;

class TemporaryTestDataStorage implements TemporaryDataStorage {

	private ByteArrayOutputStream outputStream;

	private Map<String, Serializable> attributes;

	public TemporaryTestDataStorage() {
		this.outputStream = new ByteArrayOutputStream();
		this.attributes = new HashMap<String, Serializable>();
	}

	public InputStream getTempInputStream() {
		byte[] data = this.outputStream.toByteArray();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
		return inputStream;
	}

	public OutputStream getTempOutputStream() {
		return this.outputStream;
	}

	public Serializable getAttribute(String attributeName) {
		return this.attributes.get(attributeName);
	}

	public void setAttribute(String attributeName, Serializable attributeValue) {
		this.attributes.put(attributeName, attributeValue);
	}
}