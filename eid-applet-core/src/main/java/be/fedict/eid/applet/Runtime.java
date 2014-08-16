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

package be.fedict.eid.applet;

import java.net.URL;

/**
 * Interface for runtime component.
 * 
 * @author Frank Cornelis
 * 
 */
public interface Runtime {

	/**
	 * Navigates the web browser to the target page as configured on the eID
	 * Applet.
	 */
	void gotoTargetPage();

	/**
	 * Navigates the web browser to the cancel page, if configured via the eID
	 * Applet parameters.
	 * 
	 * @return <code>true</code> if cancel page was configured.
	 */
	boolean gotoCancelPage();

	/**
	 * Navigates the web browser to the authorization error page, if configured
	 * via the eID Applet parameters.
	 */
	void gotoAuthorizationErrorPage();

	URL getDocumentBase();

	/**
	 * Gives back the parameter value for the given parameter name. Will map to
	 * the applet runtime parameters as set within the web browser where this
	 * applet is being run.
	 * 
	 * @param name
	 *            the name of the parameter.
	 * @return the value of the parameter, or <code>null</code> if not
	 *         available.
	 */
	String getParameter(String name);

	Applet getApplet();
}
