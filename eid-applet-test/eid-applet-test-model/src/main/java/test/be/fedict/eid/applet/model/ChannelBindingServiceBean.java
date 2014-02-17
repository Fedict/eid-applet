/*
 * eID Applet Project.
 * Copyright (C) 2010 FedICT.
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

package test.be.fedict.eid.applet.model;

import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Stateless
@EJB(name = "java:global/test/ChannelBindingServiceBean", beanInterface = ChannelBindingService.class)
public class ChannelBindingServiceBean implements ChannelBindingService {

	private static final Log LOG = LogFactory
			.getLog(ChannelBindingServiceBean.class);

	public static final String SERVER_CERTIFICATE_SESSION_ATTRIBUTE = ChannelBindingServiceBean.class
			.getName() + ".serverCertificate";

	public X509Certificate getServerCertificate() {
		LOG.debug("getServerCertificate");
		HttpServletRequest httpServletRequest;
		try {
			httpServletRequest = (HttpServletRequest) PolicyContext
					.getContext("javax.servlet.http.HttpServletRequest");
		} catch (PolicyContextException e) {
			throw new RuntimeException("JACC error: " + e.getMessage());
		}
		HttpSession httpSession = httpServletRequest.getSession();
		X509Certificate serverCertificate = (X509Certificate) httpSession
				.getAttribute(SERVER_CERTIFICATE_SESSION_ATTRIBUTE);
		return serverCertificate;
	}

}
