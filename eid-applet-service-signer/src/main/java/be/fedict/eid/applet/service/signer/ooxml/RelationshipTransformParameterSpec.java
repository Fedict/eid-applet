/*
 * eID Applet Project.
 * Copyright (C) 2009-2010 FedICT.
 * Copyright (C) 2009 Frank Cornelis.
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
 * Copyright (C) 2009 Frank Cornelis.
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

package be.fedict.eid.applet.service.signer.ooxml;

import java.util.LinkedList;
import java.util.List;

import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * Relationship Transform parameter specification class.
 * 
 * @author Frank Cornelis
 * 
 */
public class RelationshipTransformParameterSpec implements
		TransformParameterSpec {

	private final List<String> sourceIds;

	private final List<String> sourceTypes;

	/**
	 * Main constructor.
	 */
	public RelationshipTransformParameterSpec() {
		this.sourceIds = new LinkedList<String>();
		this.sourceTypes = new LinkedList<String>();
	}

	/**
	 * Adds a relationship reference for the given source identifier.
	 * 
	 * @param sourceId
	 */
	public void addRelationshipReference(String sourceId) {
		this.sourceIds.add(sourceId);
	}

	/**
	 * Adds a relationship group reference for the given source type.
	 * 
	 * @param sourceType
	 */
	public void addRelationshipGroupReference(String sourceType) {
		this.sourceTypes.add(sourceType);
	}

	List<String> getSourceIds() {
		return this.sourceIds;
	}

	List<String> getSourceTypes() {
		return this.sourceTypes;
	}
}
