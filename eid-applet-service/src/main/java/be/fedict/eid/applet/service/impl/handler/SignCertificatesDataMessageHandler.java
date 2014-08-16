/*
 * eID Applet Project.
 * Copyright (C) 2008-2013 FedICT.
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

package be.fedict.eid.applet.service.impl.handler;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.fedict.eid.applet.service.dto.DTOMapper;
import be.fedict.eid.applet.service.impl.RequestContext;
import be.fedict.eid.applet.service.impl.ServiceLocator;
import be.fedict.eid.applet.service.impl.tlv.TlvParser;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.AuditService;
import be.fedict.eid.applet.service.spi.AuthorizationException;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.IdentityIntegrityService;
import be.fedict.eid.applet.service.spi.IdentityRequest;
import be.fedict.eid.applet.service.spi.IdentityService;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.applet.shared.ErrorCode;
import be.fedict.eid.applet.shared.FinishedMessage;
import be.fedict.eid.applet.shared.SignCertificatesDataMessage;
import be.fedict.eid.applet.shared.SignRequestMessage;

/**
 * Sign Certificate Data Message Handler.
 * 
 * @author Frank Cornelis
 * 
 */
@HandlesMessage(SignCertificatesDataMessage.class)
public class SignCertificatesDataMessageHandler implements
		MessageHandler<SignCertificatesDataMessage> {

	private static final Log LOG = LogFactory
			.getLog(SignCertificatesDataMessageHandler.class);

	@InitParam(HelloMessageHandler.SIGNATURE_SERVICE_INIT_PARAM_NAME)
	private ServiceLocator<SignatureService> signatureServiceLocator;

	@InitParam(HelloMessageHandler.REMOVE_CARD_INIT_PARAM_NAME)
	private boolean removeCard;

	@InitParam(HelloMessageHandler.LOGOFF_INIT_PARAM_NAME)
	private boolean logoff;

	@InitParam(HelloMessageHandler.REQUIRE_SECURE_READER_INIT_PARAM_NAME)
	private boolean requireSecureReader;

	@InitParam(HelloMessageHandler.IDENTITY_INTEGRITY_SERVICE_INIT_PARAM_NAME)
	private ServiceLocator<IdentityIntegrityService> identityIntegrityServiceLocator;

	@InitParam(AuthenticationDataMessageHandler.AUDIT_SERVICE_INIT_PARAM_NAME)
	private ServiceLocator<AuditService> auditServiceLocator;

	@InitParam(HelloMessageHandler.IDENTITY_SERVICE_INIT_PARAM_NAME)
	private ServiceLocator<IdentityService> identityServiceLocator;

	public Object handleMessage(SignCertificatesDataMessage message,
			Map<String, String> httpHeaders, HttpServletRequest request,
			HttpSession session) throws ServletException {
		SignatureService signatureService = this.signatureServiceLocator
				.locateService();

		List<X509Certificate> signingCertificateChain = message.certificateChain;
		X509Certificate signingCertificate = signingCertificateChain.get(0);
		if (null == signingCertificate) {
			throw new ServletException("missing non-repudiation certificate");
		}
		LOG.debug("signing certificate: "
				+ signingCertificateChain.get(0).getSubjectX500Principal());

		RequestContext requestContext = new RequestContext(session);
		boolean includeIdentity = requestContext.includeIdentity();
		boolean includeAddress = requestContext.includeAddress();
		boolean includePhoto = requestContext.includePhoto();

		Identity identity = null;
		Address address = null;
		if (includeIdentity || includeAddress || includePhoto) {
			/*
			 * Pre-sign phase including identity data.
			 */
			if (includeIdentity) {
				if (null == message.identityData) {
					throw new ServletException("identity data missing");
				}
				identity = TlvParser
						.parse(message.identityData, Identity.class);
			}

			if (includeAddress) {
				if (null == message.addressData) {
					throw new ServletException("address data missing");
				}
				address = TlvParser.parse(message.addressData, Address.class);
			}

			if (includePhoto) {
				if (null == message.photoData) {
					throw new ServletException("photo data missing");
				}
				if (null != identity) {
					byte[] expectedPhotoDigest = identity.photoDigest;
					byte[] actualPhotoDigest;

					try {
						actualPhotoDigest = digestPhoto(
								getDigestAlgo(expectedPhotoDigest.length),
								message.photoData);
					} catch (NoSuchAlgorithmException e) {
						throw new ServletException(
								"photo signed with unsupported algorithm");
					}

					if (false == Arrays.equals(expectedPhotoDigest,
							actualPhotoDigest)) {
						throw new ServletException("photo digest incorrect");
					}
				}
			}

			IdentityIntegrityService identityIntegrityService = this.identityIntegrityServiceLocator
					.locateService();
			if (null != identityIntegrityService) {
				if (null == message.rrnCertificate) {
					throw new ServletException(
							"national registry certificate not included while requested");
				}
				PublicKey rrnPublicKey = message.rrnCertificate.getPublicKey();
				if (null != message.identityData) {
					if (null == message.identitySignatureData) {
						throw new ServletException(
								"missing identity data signature");
					}
					verifySignature(message.rrnCertificate.getSigAlgName(),
							message.identitySignatureData, rrnPublicKey,
							request, message.identityData);
					if (null != message.addressData) {
						if (null == message.addressSignatureData) {
							throw new ServletException(
									"missing address data signature");
						}
						byte[] addressFile = trimRight(message.addressData);
						verifySignature(message.rrnCertificate.getSigAlgName(),
								message.addressSignatureData, rrnPublicKey,
								request, addressFile,
								message.identitySignatureData);
					}
				}

				LOG.debug("checking national registration certificate: "
						+ message.rrnCertificate.getSubjectX500Principal());
				List<X509Certificate> rrnCertificateChain = new LinkedList<X509Certificate>();
				rrnCertificateChain.add(message.rrnCertificate);
				rrnCertificateChain.add(message.rootCertificate);
				identityIntegrityService
						.checkNationalRegistrationCertificate(rrnCertificateChain);
			}
		}

		DigestInfo digestInfo;
		DTOMapper dtoMapper = new DTOMapper();
		IdentityDTO identityDTO = dtoMapper.map(identity, IdentityDTO.class);
		AddressDTO addressDTO = dtoMapper.map(address, AddressDTO.class);
		try {
			digestInfo = signatureService.preSign(null,
					signingCertificateChain, identityDTO, addressDTO,
					message.photoData);
		} catch (NoSuchAlgorithmException e) {
			throw new ServletException("no such algo: " + e.getMessage(), e);
		} catch (AuthorizationException e) {
			return new FinishedMessage(ErrorCode.AUTHORIZATION);
		}

		// also save it in the session for later verification
		SignatureDataMessageHandler.setDigestValue(digestInfo.digestValue,
				digestInfo.digestAlgo, session);

		IdentityService identityService = this.identityServiceLocator
				.locateService();
		boolean removeCard;
		if (null != identityService) {
			IdentityRequest identityRequest = identityService
					.getIdentityRequest();
			removeCard = identityRequest.removeCard();
		} else {
			removeCard = this.removeCard;
		}

		SignRequestMessage signRequestMessage = new SignRequestMessage(
				digestInfo.digestValue, digestInfo.digestAlgo,
				digestInfo.description, this.logoff, removeCard,
				this.requireSecureReader);
		return signRequestMessage;
	}

	public void init(ServletConfig config) throws ServletException {
		// empty
	}

	private byte[] digestPhoto(String digestAlgoName, byte[] photoFile) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(digestAlgoName);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("digest error: " + e.getMessage(), e);
		}
		byte[] photoDigest = messageDigest.digest(photoFile);
		return photoDigest;
	}

	private void verifySignature(String signatureAlgoName,
			byte[] signatureData, PublicKey publicKey,
			HttpServletRequest request, byte[]... data) throws ServletException {
		Signature signature;
		try {
			signature = Signature.getInstance(signatureAlgoName);
		} catch (NoSuchAlgorithmException e) {
			throw new ServletException("algo error: " + e.getMessage(), e);
		}
		try {
			signature.initVerify(publicKey);
		} catch (InvalidKeyException e) {
			throw new ServletException("key error: " + e.getMessage(), e);
		}
		try {
			for (byte[] dataItem : data) {
				signature.update(dataItem);
			}
			boolean result = signature.verify(signatureData);
			if (false == result) {
				AuditService auditService = this.auditServiceLocator
						.locateService();
				if (null != auditService) {
					String remoteAddress = request.getRemoteAddr();
					auditService.identityIntegrityError(remoteAddress);
				}
				throw new ServletException("signature incorrect");
			}
		} catch (SignatureException e) {
			throw new ServletException("signature error: " + e.getMessage(), e);
		}
	}

	private byte[] trimRight(byte[] addressFile) {
		int idx;
		for (idx = 0; idx < addressFile.length; idx++) {
			if (0 == addressFile[idx]) {
				break;
			}
		}
		byte[] result = new byte[idx];
		System.arraycopy(addressFile, 0, result, 0, idx);
		return result;
	}

	private String getDigestAlgo(final int hashSize)
			throws NoSuchAlgorithmException {
		switch (hashSize) {
		case 20:
			return "SHA-1";
		case 28:
			return "SHA-224";
		case 32:
			return "SHA-256";
		case 48:
			return "SHA-384";
		case 64:
			return "SHA-512";
		}
		throw new NoSuchAlgorithmException(
				"Failed to find guess algorithm for hash size of " + hashSize
						+ " bytes");
	}
}
