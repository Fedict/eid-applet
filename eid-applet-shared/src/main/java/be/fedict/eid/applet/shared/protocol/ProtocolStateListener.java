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

package be.fedict.eid.applet.shared.protocol;

/**
 * Interface for protocol state listeners.
 * 
 * @author Frank Cornelis
 * 
 */
public interface ProtocolStateListener {

	/**
	 * Fired when there is a protocol state transition.
	 * 
	 * @param newProtocolState
	 */
	void protocolStateTransition(ProtocolState newProtocolState);

	/**
	 * Called when a protocol run starts.
	 */
	void startProtocolRun();

	/**
	 * Called when a protocol run ends.
	 */
	void stopProtocolRun();
}
