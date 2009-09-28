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

package test.be.fedict.eid.applet.model;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.spi.InsecureClientEnvironmentException;
import be.fedict.eid.applet.service.spi.SecureClientEnvironmentService;

@Stateless
@Local(SecureClientEnvironmentService.class)
@LocalBinding(jndiBinding = "test/eid/applet/model/InsecureClientEnvironmentBean")
public class InsecureClientEnvironmentBean implements
		SecureClientEnvironmentService {

	private static final Log LOG = LogFactory
			.getLog(InsecureClientEnvironmentBean.class);

	public void checkSecureClientEnvironment(String javaVersion,
			String javaVendor, String osName, String osArch, String osVersion,
			String userAgent, String navigatorAppName,
			String navigatorAppVersion, String navigatorUserAgent,
			String remoteAddress, Integer sslKeySize, String sslCipherSuite,
			List<String> readerList) throws InsecureClientEnvironmentException {
		LOG.debug("checking secure client environment");
		throw new InsecureClientEnvironmentException();
	}
}
