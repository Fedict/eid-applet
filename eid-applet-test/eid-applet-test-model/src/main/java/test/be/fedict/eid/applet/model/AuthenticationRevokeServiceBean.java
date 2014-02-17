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
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.spi.RevokedCertificateSecurityException;

@Stateless
@EJB(name = "java:global/test/AuthenticationRevokeServiceBean", beanInterface = AuthenticationRevokeService.class)
public class AuthenticationRevokeServiceBean implements
		AuthenticationRevokeService {

	private static final Log LOG = LogFactory
			.getLog(AuthenticationRevokeServiceBean.class);

	public void validateCertificateChain(List<X509Certificate> certificateChain)
			throws SecurityException {
		LOG.debug("validate certificate chain: " + certificateChain);
		throw new RevokedCertificateSecurityException();
	}
}
