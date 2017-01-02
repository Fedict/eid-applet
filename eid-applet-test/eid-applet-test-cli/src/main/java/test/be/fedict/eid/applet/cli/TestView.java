/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
 * Copyright (C) 2014 e-Contract.be BVBA.
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

package test.be.fedict.eid.applet.cli;

import java.awt.*;
import java.util.Locale;

import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;

public class TestView implements View {

	private final Messages messages = new Messages(Locale.getDefault());

	@Override
	public void addDetailMessage(String detailMessage) {
		System.out.println("detail: " + detailMessage);
	}

	@Override
	public Component getParentComponent() {
		return null;
	}

	@Override
	public boolean privacyQuestion(boolean includeAddress, boolean includePhoto, String identityDataUsage) {
		return false;
	}

	@Override
	public void setStatusMessage(Status status, Messages.MESSAGE_ID messageId) {
		String statusMessage = this.messages.getMessage(messageId);
		System.out.println("Status message: " + status + ": " + statusMessage);
		if (Status.ERROR == status) {
			throw new RuntimeException("status ERROR received");
		}
	}

	@Override
	public void setProgressIndeterminate() {
	}

	@Override
	public void resetProgress(int max) {
	}

	@Override
	public void increaseProgress() {
	}

	@Override
	public void confirmAuthenticationSignature(String message) {
	}

	@Override
	public int confirmSigning(String description, String digestAlgo) {
		return 0;
	}

	Messages getMessages() {
		return messages;
	}
}