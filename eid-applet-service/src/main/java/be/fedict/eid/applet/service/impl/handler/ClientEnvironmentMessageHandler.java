/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
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

package be.fedict.eid.applet.service.impl.handler;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.impl.AuthenticationChallenge;
import be.fedict.eid.applet.service.impl.ServiceLocator;
import be.fedict.eid.applet.service.spi.AuthenticationService;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityIntegrityService;
import be.fedict.eid.applet.service.spi.InsecureClientEnvironmentException;
import be.fedict.eid.applet.service.spi.SecureClientEnvironmentService;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.applet.shared.AdministrationMessage;
import be.fedict.eid.applet.shared.AuthenticationRequestMessage;
import be.fedict.eid.applet.shared.ClientEnvironmentMessage;
import be.fedict.eid.applet.shared.FilesDigestRequestMessage;
import be.fedict.eid.applet.shared.IdentificationRequestMessage;
import be.fedict.eid.applet.shared.InsecureClientMessage;
import be.fedict.eid.applet.shared.SignRequestMessage;

/**
 * Handler for client environment message.
 * 
 * @author Frank Cornelis
 * 
 */
@HandlesMessage(ClientEnvironmentMessage.class)
public class ClientEnvironmentMessageHandler implements
		MessageHandler<ClientEnvironmentMessage> {

	private static final Log LOG = LogFactory
			.getLog(ClientEnvironmentMessageHandler.class);

	private ServiceLocator<SecureClientEnvironmentService> secureClientEnvServiceLocator;

	private boolean includePhoto;

	private boolean includeAddress;

	private ServiceLocator<IdentityIntegrityService> identityIntegrityServiceLocator;

	private ServiceLocator<AuthenticationService> authenticationServiceLocator;

	private SecureRandom secureRandom;

	private boolean removeCard;

	private boolean changePin;

	private boolean unblockPin;

	private boolean includeHostname;

	private boolean includeInetAddress;

	private boolean logoff;

	private boolean preLogoff;

	private boolean includeCertificates;

	private boolean sessionIdChannelBinding;

	private boolean serverCertificateChannelBinding;

	private ServiceLocator<SignatureService> signatureServiceLocator;

	public Object handleMessage(ClientEnvironmentMessage message,
			Map<String, String> httpHeaders, HttpServletRequest request,
			HttpSession session) throws ServletException {
		SecureClientEnvironmentService secureClientEnvService = this.secureClientEnvServiceLocator
				.locateService();
		if (null == secureClientEnvService) {
			throw new ServletException(
					"no secure client env service configured");
		}
		String remoteAddress = request.getRemoteAddr();
		Integer sslKeySize = (Integer) request
				.getAttribute("javax.servlet.request.key_size");
		String userAgent = httpHeaders.get("user-agent");
		String sslCipherSuite = (String) request
				.getAttribute("javax.servlet.request.cipher_suite");
		try {
			secureClientEnvService.checkSecureClientEnvironment(
					message.javaVersion, message.javaVendor, message.osName,
					message.osArch, message.osVersion, userAgent,
					message.navigatorAppName, message.navigatorAppVersion,
					message.navigatorUserAgent, remoteAddress, sslKeySize,
					sslCipherSuite, message.readerList);
		} catch (InsecureClientEnvironmentException e) {
			return new InsecureClientMessage(e.isWarnOnly());
		}
		if (this.changePin || this.unblockPin) {
			AdministrationMessage administrationMessage = new AdministrationMessage(
					this.changePin, this.unblockPin, this.logoff,
					this.removeCard);
			return administrationMessage;
		}
		SignatureService signatureService = this.signatureServiceLocator
				.locateService();
		if (null != signatureService) {
			// TODO DRY refactor: is a copy-paste from HelloMessageHandler
			String filesDigestAlgo = signatureService.getFilesDigestAlgorithm();
			if (null != filesDigestAlgo) {
				LOG.debug("files digest algo: " + filesDigestAlgo);
				FilesDigestRequestMessage filesDigestRequestMessage = new FilesDigestRequestMessage();
				filesDigestRequestMessage.digestAlgo = filesDigestAlgo;
				return filesDigestRequestMessage;
			}

			DigestInfo digestInfo;
			try {
				digestInfo = signatureService.preSign(null, null);
			} catch (NoSuchAlgorithmException e) {
				throw new ServletException("no such algo: " + e.getMessage(), e);
			}

			// also save it in the session for later verification
			SignatureDataMessageHandler.setDigestValue(digestInfo.digestValue,
					session);

			SignRequestMessage signRequestMessage = new SignRequestMessage(
					digestInfo.digestValue, digestInfo.digestAlgo,
					digestInfo.description, this.logoff, this.removeCard);
			return signRequestMessage;
		}
		AuthenticationService authenticationService = this.authenticationServiceLocator
				.locateService();
		if (null != authenticationService) {
			byte[] challenge = AuthenticationChallenge
					.generateChallenge(session);
			AuthenticationRequestMessage authenticationRequestMessage = new AuthenticationRequestMessage(
					challenge, this.includeHostname, this.includeInetAddress,
					this.logoff, this.preLogoff, this.removeCard,
					this.sessionIdChannelBinding,
					this.serverCertificateChannelBinding);
			return authenticationRequestMessage;
		} else {
			IdentityIntegrityService identityIntegrityService = this.identityIntegrityServiceLocator
					.locateService();
			boolean includeIntegrityData = null != identityIntegrityService;
			IdentificationRequestMessage responseMessage = new IdentificationRequestMessage(
					this.includeAddress, this.includePhoto,
					includeIntegrityData, this.includeCertificates,
					this.removeCard);
			return responseMessage;
		}
	}

	public void init(ServletConfig config) throws ServletException {
		String includeAddress = config
				.getInitParameter(HelloMessageHandler.INCLUDE_ADDRESS_INIT_PARAM_NAME);
		if (null != includeAddress) {
			this.includeAddress = Boolean.parseBoolean(includeAddress);
		}
		String includePhoto = config
				.getInitParameter(HelloMessageHandler.INCLUDE_PHOTO_INIT_PARAM_NAME);
		if (null != includePhoto) {
			this.includePhoto = Boolean.parseBoolean(includePhoto);
		}
		this.secureClientEnvServiceLocator = new ServiceLocator<SecureClientEnvironmentService>(
				HelloMessageHandler.SECURE_CLIENT_ENV_SERVICE_INIT_PARAM_NAME,
				config);
		this.identityIntegrityServiceLocator = new ServiceLocator<IdentityIntegrityService>(
				HelloMessageHandler.IDENTITY_INTEGRITY_SERVICE_INIT_PARAM_NAME,
				config);
		this.authenticationServiceLocator = new ServiceLocator<AuthenticationService>(
				AuthenticationDataMessageHandler.AUTHN_SERVICE_INIT_PARAM_NAME,
				config);
		this.signatureServiceLocator = new ServiceLocator<SignatureService>(
				HelloMessageHandler.SIGNATURE_SERVICE_INIT_PARAM_NAME, config);

		this.secureRandom = new SecureRandom();
		this.secureRandom.setSeed(System.currentTimeMillis());

		String removeCard = config
				.getInitParameter(HelloMessageHandler.REMOVE_CARD_INIT_PARAM_NAME);
		if (null != removeCard) {
			this.removeCard = Boolean.parseBoolean(removeCard);
		}

		String includeCertificates = config
				.getInitParameter(HelloMessageHandler.INCLUDE_CERTS_INIT_PARAM_NAME);
		if (null != includeCertificates) {
			this.includeCertificates = Boolean
					.parseBoolean(includeCertificates);
		}

		String hostname = config
				.getInitParameter(HelloMessageHandler.HOSTNAME_INIT_PARAM_NAME);
		if (null != hostname) {
			this.includeHostname = true;
		}

		String inetAddress = config
				.getInitParameter(HelloMessageHandler.INET_ADDRESS_INIT_PARAM_NAME);
		if (null != inetAddress) {
			this.includeInetAddress = true;
		}

		String changePin = config
				.getInitParameter(HelloMessageHandler.CHANGE_PIN_INIT_PARAM_NAME);
		if (null != changePin) {
			this.changePin = Boolean.parseBoolean(changePin);
		}

		String unblockPin = config
				.getInitParameter(HelloMessageHandler.UNBLOCK_PIN_INIT_PARAM_NAME);
		if (null != unblockPin) {
			this.unblockPin = Boolean.parseBoolean(unblockPin);
		}

		String logoff = config
				.getInitParameter(HelloMessageHandler.LOGOFF_INIT_PARAM_NAME);
		if (null != logoff) {
			this.logoff = Boolean.parseBoolean(logoff);
		}

		String preLogoff = config
				.getInitParameter(HelloMessageHandler.PRE_LOGOFF_INIT_PARAM_NAME);
		if (null != preLogoff) {
			this.preLogoff = Boolean.parseBoolean(preLogoff);
		}

		String sessionIdChannelBinding = config
				.getInitParameter(HelloMessageHandler.SESSION_ID_CHANNEL_BINDING_INIT_PARAM_NAME);
		if (null != sessionIdChannelBinding) {
			this.sessionIdChannelBinding = Boolean
					.parseBoolean(sessionIdChannelBinding);
		}

		String channelBindingServerCertificate = config
				.getInitParameter(HelloMessageHandler.CHANNEL_BINDING_SERVER_CERTIFICATE);
		if (null != channelBindingServerCertificate) {
			this.serverCertificateChannelBinding = true;
		}
	}
}
