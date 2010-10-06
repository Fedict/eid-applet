/*
 * eID Applet Project.
 * Copyright (C) 2010 FedICT.
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

package be.fedict.eid.applet.service.signer.facets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ocsp.ResponderID;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.RespID;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLIdentifierType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLRefType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLRefsType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertIDListType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertIDType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertificateValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CompleteCertificateRefsType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CompleteRevocationRefsType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.DigestAlgAndValueType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPIdentifierType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPRefType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPRefsType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ObjectFactory;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ResponderIDType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.RevocationValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.XAdESTimeStampType;
import be.fedict.eid.applet.service.signer.jaxb.xades141.ValidationDataType;
import be.fedict.eid.applet.service.signer.jaxb.xmldsig.CanonicalizationMethodType;
import be.fedict.eid.applet.service.signer.time.TimeStampService;

/**
 * XAdES-X-L v1.4.1 signature facet. This signature facet implementation will
 * upgrade a given XAdES-BES/EPES signature to XAdES-X-L.
 * 
 * We don't inherit from XAdESSignatureFacet as we also want to be able to use
 * this facet out of the context of a signature creation. This signature facet
 * assumes that the signature is already XAdES-BES/EPES compliant.
 * 
 * This implementation has been tested against an implementation that
 * participated multiple ETSI XAdES plugtests.
 * 
 * @author Frank Cornelis
 * @see XAdESSignatureFacet
 */
public class XAdESXLSignatureFacet implements SignatureFacet {

	private static final Log LOG = LogFactory
			.getLog(XAdESXLSignatureFacet.class);

	public static final String XADES_NAMESPACE = "http://uri.etsi.org/01903/v1.3.2#";

	public static final String XADES141_NAMESPACE = "http://uri.etsi.org/01903/v1.4.1#";

	private Element nsElement;

	private final ObjectFactory objectFactory;

	private final be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory xades141ObjectFactory;

	private final be.fedict.eid.applet.service.signer.jaxb.xmldsig.ObjectFactory xmldsigObjectFactory;

	private final TimeStampService timeStampService;

	private String c14nAlgoId;

	private final Marshaller marshaller;

	private final RevocationDataService revocationDataService;

	private final CertificateFactory certificateFactory;

	private final DatatypeFactory datatypeFactory;

	private final String digestAlgorithm;

	static {
		Init.init();
	}

	/**
	 * Convenience constructor.
	 * 
	 * @param timeStampService
	 *            the time-stamp service used for XAdES-T and XAdES-X.
	 * @param revocationDataService
	 *            the optional revocation data service used for XAdES-C and
	 *            XAdES-X-L. When <code>null</code> the signature will be
	 *            limited to XAdES-T only.
	 */
	public XAdESXLSignatureFacet(TimeStampService timeStampService,
			RevocationDataService revocationDataService) {
		this(timeStampService, revocationDataService, "SHA-1");
	}

	/**
	 * Main constructor.
	 * 
	 * @param timeStampService
	 *            the time-stamp service used for XAdES-T and XAdES-X.
	 * @param revocationDataService
	 *            the optional revocation data service used for XAdES-C and
	 *            XAdES-X-L. When <code>null</code> the signature will be
	 *            limited to XAdES-T only.
	 * @param digestAlgorithm
	 *            the digest algorithm to be used for construction of the
	 *            XAdES-X-L elements.
	 */
	public XAdESXLSignatureFacet(TimeStampService timeStampService,
			RevocationDataService revocationDataService, String digestAlgorithm) {
		this.objectFactory = new ObjectFactory();
		this.c14nAlgoId = CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS;
		this.digestAlgorithm = digestAlgorithm;
		this.timeStampService = timeStampService;
		this.revocationDataService = revocationDataService;
		this.xmldsigObjectFactory = new be.fedict.eid.applet.service.signer.jaxb.xmldsig.ObjectFactory();
		this.xades141ObjectFactory = new be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory();

		try {
			JAXBContext context = JAXBContext
					.newInstance(be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory.class);
			this.marshaller = context.createMarshaller();
			this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			this.marshaller.setProperty(
					"com.sun.xml.bind.namespacePrefixMapper",
					new XAdESNamespacePrefixMapper());
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}

		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException("X509 JCA error: " + e.getMessage(), e);
		}

		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("datatype config error: "
					+ e.getMessage(), e);
		}
	}

	public void setCanonicalizerAlgorithm(String c14nAlgoId) {
		this.c14nAlgoId = c14nAlgoId;
	}

	private Node findSingleNode(Node baseNode, String xpathExpression) {
		if (null == this.nsElement) {
			this.nsElement = createNamespaceElement(baseNode);
		}
		return findSingleNode(baseNode, xpathExpression, this.nsElement);
	}

	public static Node findSingleNode(Node baseNode, String xpathExpression,
			Element nsElement) {
		try {
			Node node = XPathAPI.selectSingleNode(baseNode, xpathExpression,
					nsElement);
			return node;
		} catch (TransformerException e) {
			throw new RuntimeException("XPath error: " + e.getMessage(), e);
		}
	}

	public static NodeList getNodes(Node baseNode, String xpathExpression,
			Element nsElement) {
		try {
			NodeList nodeList = XPathAPI.selectNodeList(baseNode,
					xpathExpression, nsElement);
			return nodeList;
		} catch (TransformerException e) {
			throw new RuntimeException("XPath error: " + e.getMessage(), e);
		}
	}

	public void postSign(Element signatureElement,
			List<X509Certificate> signingCertificateChain) {
		LOG.debug("XAdES-X-L post sign phase");

		// check for XAdES-BES
		Element qualifyingPropertiesElement = (Element) findSingleNode(
				signatureElement, "ds:Object/xades:QualifyingProperties");
		if (null == qualifyingPropertiesElement) {
			throw new IllegalArgumentException("no XAdES-BES extension present");
		}

		// create basic XML container structure
		Document document = signatureElement.getOwnerDocument();
		String xadesNamespacePrefix;
		if (null != qualifyingPropertiesElement.getPrefix()) {
			xadesNamespacePrefix = qualifyingPropertiesElement.getPrefix()
					+ ":";
		} else {
			xadesNamespacePrefix = "";
		}
		Element unsignedPropertiesElement = (Element) findSingleNode(
				qualifyingPropertiesElement, "xades:UnsignedProperties");
		if (null == unsignedPropertiesElement) {
			unsignedPropertiesElement = document.createElementNS(
					XADES_NAMESPACE, xadesNamespacePrefix
							+ "UnsignedProperties");
			qualifyingPropertiesElement.appendChild(unsignedPropertiesElement);
		}
		Element unsignedSignaturePropertiesElement = (Element) findSingleNode(
				unsignedPropertiesElement, "xades:UnsignedSignatureProperties");
		if (null == unsignedSignaturePropertiesElement) {
			unsignedSignaturePropertiesElement = document.createElementNS(
					XADES_NAMESPACE, xadesNamespacePrefix
							+ "UnsignedSignatureProperties");
			unsignedPropertiesElement
					.appendChild(unsignedSignaturePropertiesElement);
		}

		// create the XAdES-T time-stamp
		Node signatureValueNode = findSingleNode(signatureElement,
				"ds:SignatureValue");
		RevocationData tsaRevocationDataXadesT = new RevocationData();
		LOG.debug("creating XAdES-T time-stamp");
		XAdESTimeStampType signatureTimeStamp = createXAdESTimeStamp(
				Collections.singletonList(signatureValueNode),
				tsaRevocationDataXadesT, this.c14nAlgoId,
				this.timeStampService, this.objectFactory,
				this.xmldsigObjectFactory);

		// marshal the XAdES-T extension
		try {
			this.marshaller.marshal(this.objectFactory
					.createSignatureTimeStamp(signatureTimeStamp),
					unsignedSignaturePropertiesElement);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}

		// xadesv141::TimeStampValidationData
		if (tsaRevocationDataXadesT.hasRevocationDataEntries()) {
			ValidationDataType validationData = createValidationData(tsaRevocationDataXadesT);
			try {
				this.marshaller.marshal(this.xades141ObjectFactory
						.createTimeStampValidationData(validationData),
						unsignedSignaturePropertiesElement);
			} catch (JAXBException e) {
				throw new RuntimeException("JAXB error: " + e.getMessage(), e);
			}
		}

		if (null == this.revocationDataService) {
			/*
			 * Without revocation data service we cannot construct the XAdES-C
			 * extension.
			 */
			return;
		}

		// XAdES-C: complete certificate refs
		CompleteCertificateRefsType completeCertificateRefs = this.objectFactory
				.createCompleteCertificateRefsType();
		CertIDListType certIdList = this.objectFactory.createCertIDListType();
		completeCertificateRefs.setCertRefs(certIdList);
		List<CertIDType> certIds = certIdList.getCert();
		for (int certIdx = 1; certIdx < signingCertificateChain.size(); certIdx++) {
			/*
			 * We skip the signing certificate itself according to section
			 * 4.4.3.2 of the XAdES 1.4.1 specification.
			 */
			X509Certificate certificate = signingCertificateChain.get(certIdx);
			CertIDType certId = XAdESSignatureFacet.getCertID(certificate,
					this.objectFactory, this.xmldsigObjectFactory,
					this.digestAlgorithm);
			certIds.add(certId);
		}

		// XAdES-C: complete revocation refs
		CompleteRevocationRefsType completeRevocationRefs = this.objectFactory
				.createCompleteRevocationRefsType();
		RevocationData revocationData = this.revocationDataService
				.getRevocationData(signingCertificateChain);
		if (revocationData.hasCRLs()) {
			CRLRefsType crlRefs = this.objectFactory.createCRLRefsType();
			completeRevocationRefs.setCRLRefs(crlRefs);
			List<CRLRefType> crlRefList = crlRefs.getCRLRef();

			List<byte[]> crls = revocationData.getCRLs();
			for (byte[] encodedCrl : crls) {
				CRLRefType crlRef = this.objectFactory.createCRLRefType();
				crlRefList.add(crlRef);
				X509CRL crl;
				try {
					crl = (X509CRL) this.certificateFactory
							.generateCRL(new ByteArrayInputStream(encodedCrl));
				} catch (CRLException e) {
					throw new RuntimeException("CRL parse error: "
							+ e.getMessage(), e);
				}

				CRLIdentifierType crlIdentifier = this.objectFactory
						.createCRLIdentifierType();
				crlRef.setCRLIdentifier(crlIdentifier);
				crlIdentifier.setIssuer(crl.getIssuerX500Principal().getName(
						X500Principal.RFC1779));
				crlIdentifier.setIssueTime(this.datatypeFactory
						.newXMLGregorianCalendar(new DateTime(crl
								.getThisUpdate()).toGregorianCalendar()));
				crlIdentifier.setNumber(getCrlNumber(crl));

				DigestAlgAndValueType digestAlgAndValue = XAdESSignatureFacet
						.getDigestAlgAndValue(encodedCrl, this.objectFactory,
								this.xmldsigObjectFactory, this.digestAlgorithm);
				crlRef.setDigestAlgAndValue(digestAlgAndValue);
			}
		}
		if (revocationData.hasOCSPs()) {
			OCSPRefsType ocspRefs = this.objectFactory.createOCSPRefsType();
			completeRevocationRefs.setOCSPRefs(ocspRefs);
			List<OCSPRefType> ocspRefList = ocspRefs.getOCSPRef();
			List<byte[]> ocsps = revocationData.getOCSPs();
			for (byte[] ocsp : ocsps) {
				OCSPRefType ocspRef = this.objectFactory.createOCSPRefType();
				ocspRefList.add(ocspRef);

				DigestAlgAndValueType digestAlgAndValue = XAdESSignatureFacet
						.getDigestAlgAndValue(ocsp, this.objectFactory,
								this.xmldsigObjectFactory, this.digestAlgorithm);
				ocspRef.setDigestAlgAndValue(digestAlgAndValue);

				OCSPIdentifierType ocspIdentifier = this.objectFactory
						.createOCSPIdentifierType();
				ocspRef.setOCSPIdentifier(ocspIdentifier);
				OCSPResp ocspResp;
				try {
					ocspResp = new OCSPResp(ocsp);
				} catch (IOException e) {
					throw new RuntimeException("OCSP decoding error: "
							+ e.getMessage(), e);
				}
				Object ocspResponseObject;
				try {
					ocspResponseObject = ocspResp.getResponseObject();
				} catch (OCSPException e) {
					throw new RuntimeException("OCSP error: " + e.getMessage(),
							e);
				}
				BasicOCSPResp basicOcspResp = (BasicOCSPResp) ocspResponseObject;
				Date producedAt = basicOcspResp.getProducedAt();
				ocspIdentifier.setProducedAt(this.datatypeFactory
						.newXMLGregorianCalendar(new DateTime(producedAt)
								.toGregorianCalendar()));

				ResponderIDType responderId = this.objectFactory
						.createResponderIDType();
				ocspIdentifier.setResponderID(responderId);
				RespID respId = basicOcspResp.getResponderId();
				ResponderID ocspResponderId = respId.toASN1Object();
				DERTaggedObject derTaggedObject = (DERTaggedObject) ocspResponderId
						.toASN1Object();
				if (2 == derTaggedObject.getTagNo()) {
					ASN1OctetString keyHashOctetString = (ASN1OctetString) derTaggedObject
							.getObject();
					responderId.setByKey(keyHashOctetString.getOctets());
				} else {
					X509Name name = X509Name.getInstance(derTaggedObject
							.getObject());
					responderId.setByName(name.toString());
				}
			}
		}

		// marshal XAdES-C
		NodeList unsignedSignaturePropertiesNodeList = ((Element) qualifyingPropertiesElement)
				.getElementsByTagNameNS(XADES_NAMESPACE,
						"UnsignedSignatureProperties");
		Node unsignedSignaturePropertiesNode = unsignedSignaturePropertiesNodeList
				.item(0);
		try {
			this.marshaller.marshal(this.objectFactory
					.createCompleteCertificateRefs(completeCertificateRefs),
					unsignedSignaturePropertiesNode);
			this.marshaller.marshal(this.objectFactory
					.createCompleteRevocationRefs(completeRevocationRefs),
					unsignedSignaturePropertiesNode);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}

		// XAdES-X Type 1 timestamp
		List<Node> timeStampNodesXadesX1 = new LinkedList<Node>();
		timeStampNodesXadesX1.add(signatureValueNode);
		Node signatureTimeStampNode = findSingleNode(
				unsignedSignaturePropertiesNode, "xades:SignatureTimeStamp");
		timeStampNodesXadesX1.add(signatureTimeStampNode);
		Node completeCertificateRefsNode = findSingleNode(
				unsignedSignaturePropertiesNode,
				"xades:CompleteCertificateRefs");
		timeStampNodesXadesX1.add(completeCertificateRefsNode);
		Node completeRevocationRefsNode = findSingleNode(
				unsignedSignaturePropertiesNode, "xades:CompleteRevocationRefs");
		timeStampNodesXadesX1.add(completeRevocationRefsNode);

		RevocationData tsaRevocationDataXadesX1 = new RevocationData();
		LOG.debug("creating XAdES-X time-stamp");
		XAdESTimeStampType timeStampXadesX1 = createXAdESTimeStamp(
				timeStampNodesXadesX1, tsaRevocationDataXadesX1,
				this.c14nAlgoId, this.timeStampService, this.objectFactory,
				this.xmldsigObjectFactory);
		ValidationDataType timeStampXadesX1ValidationData;
		if (tsaRevocationDataXadesX1.hasRevocationDataEntries()) {
			timeStampXadesX1ValidationData = createValidationData(tsaRevocationDataXadesX1);
		} else {
			timeStampXadesX1ValidationData = null;
		}

		// marshal XAdES-X
		try {
			this.marshaller.marshal(this.objectFactory
					.createSigAndRefsTimeStamp(timeStampXadesX1),
					unsignedSignaturePropertiesNode);
			if (null != timeStampXadesX1ValidationData) {
				this.marshaller
						.marshal(
								this.xades141ObjectFactory
										.createTimeStampValidationData(timeStampXadesX1ValidationData),
								unsignedSignaturePropertiesNode);
			}
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}

		// XAdES-X-L
		CertificateValuesType certificateValues = this.objectFactory
				.createCertificateValuesType();
		List<Object> certificateValuesList = certificateValues
				.getEncapsulatedX509CertificateOrOtherCertificate();
		for (X509Certificate certificate : signingCertificateChain) {
			EncapsulatedPKIDataType encapsulatedPKIDataType = this.objectFactory
					.createEncapsulatedPKIDataType();
			try {
				encapsulatedPKIDataType.setValue(certificate.getEncoded());
			} catch (CertificateEncodingException e) {
				throw new RuntimeException("certificate encoding error: "
						+ e.getMessage(), e);
			}
			certificateValuesList.add(encapsulatedPKIDataType);
		}
		RevocationValuesType revocationValues = createRevocationValues(revocationData);

		// marshal XAdES-X-L
		try {
			this.marshaller.marshal(this.objectFactory
					.createCertificateValues(certificateValues),
					unsignedSignaturePropertiesNode);
			this.marshaller
					.marshal(this.objectFactory
							.createRevocationValues(revocationValues),
							unsignedSignaturePropertiesNode);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
	}

	public static byte[] getC14nValue(List<Node> nodeList, String c14nAlgoId) {
		byte[] c14nValue = null;
		try {
			for (Node node : nodeList) {
				/*
				 * Re-initialize the c14n else the namespaces will get cached
				 * and will be missing from the c14n resulting nodes.
				 */
				Canonicalizer c14n;
				try {
					c14n = Canonicalizer.getInstance(c14nAlgoId);
				} catch (InvalidCanonicalizerException e) {
					throw new RuntimeException("c14n algo error: "
							+ e.getMessage(), e);
				}
				c14nValue = ArrayUtils.addAll(c14nValue,
						c14n.canonicalizeSubtree(node));
			}
		} catch (CanonicalizationException e) {
			throw new RuntimeException("c14n error: " + e.getMessage(), e);
		}
		return c14nValue;
	}

	public static Element createNamespaceElement(Node documentNode) {
		Document document = documentNode.getOwnerDocument();
		Element nsElement = document.createElement("nsElement");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades",
				XADES_NAMESPACE);
		return nsElement;
	}

	public void preSign(XMLSignatureFactory signatureFactory,
			Document document, String signatureId,
			List<X509Certificate> signingCertificateChain,
			List<Reference> references, List<XMLObject> objects)
			throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		// nothing to do here
	}

	private BigInteger getCrlNumber(X509CRL crl) {
		byte[] crlNumberExtensionValue = crl
				.getExtensionValue(X509Extensions.CRLNumber.getId());
		if (null == crlNumberExtensionValue) {
			return null;
		}
		try {
			ASN1InputStream asn1InputStream = new ASN1InputStream(
					crlNumberExtensionValue);
			ASN1OctetString octetString = (ASN1OctetString) asn1InputStream
					.readObject();
			byte[] octets = octetString.getOctets();
			DERInteger integer = (DERInteger) new ASN1InputStream(octets)
					.readObject();
			BigInteger crlNumber = integer.getPositiveValue();
			return crlNumber;
		} catch (IOException e) {
			throw new RuntimeException("I/O error: " + e.getMessage(), e);
		}
	}

	public static XAdESTimeStampType createXAdESTimeStamp(
			List<Node> nodeList,
			RevocationData revocationData,
			String c14nAlgoId,
			TimeStampService timeStampService,
			ObjectFactory objectFactory,
			be.fedict.eid.applet.service.signer.jaxb.xmldsig.ObjectFactory xmldsigObjectFactory) {
		byte[] c14nSignatureValueElement = getC14nValue(nodeList, c14nAlgoId);

		return createXAdESTimeStamp(c14nSignatureValueElement, revocationData,
				c14nAlgoId, timeStampService, objectFactory,
				xmldsigObjectFactory);
	}

	public static XAdESTimeStampType createXAdESTimeStamp(
			byte[] data,
			RevocationData revocationData,
			String c14nAlgoId,
			TimeStampService timeStampService,
			ObjectFactory objectFactory,
			be.fedict.eid.applet.service.signer.jaxb.xmldsig.ObjectFactory xmldsigObjectFactory) {
		// create the time-stamp
		byte[] timeStampToken;
		try {
			timeStampToken = timeStampService.timeStamp(data, revocationData);
		} catch (Exception e) {
			throw new RuntimeException("error while creating a time-stamp: "
					+ e.getMessage(), e);
		}

		// create a XAdES time-stamp container
		XAdESTimeStampType xadesTimeStamp = objectFactory
				.createXAdESTimeStampType();
		CanonicalizationMethodType c14nMethod = xmldsigObjectFactory
				.createCanonicalizationMethodType();
		c14nMethod.setAlgorithm(c14nAlgoId);
		xadesTimeStamp.setCanonicalizationMethod(c14nMethod);
		xadesTimeStamp.setId("time-stamp-" + UUID.randomUUID().toString());

		// embed the time-stamp
		EncapsulatedPKIDataType encapsulatedTimeStamp = objectFactory
				.createEncapsulatedPKIDataType();
		encapsulatedTimeStamp.setValue(timeStampToken);
		encapsulatedTimeStamp.setId("time-stamp-token-"
				+ UUID.randomUUID().toString());
		List<Object> timeStampContent = xadesTimeStamp
				.getEncapsulatedTimeStampOrXMLTimeStamp();
		timeStampContent.add(encapsulatedTimeStamp);

		return xadesTimeStamp;
	}

	private ValidationDataType createValidationData(
			RevocationData revocationData) {
		ValidationDataType validationData = this.xades141ObjectFactory
				.createValidationDataType();
		RevocationValuesType revocationValues = createRevocationValues(revocationData);
		validationData.setRevocationValues(revocationValues);
		return validationData;
	}

	private RevocationValuesType createRevocationValues(
			RevocationData revocationData) {
		RevocationValuesType revocationValues = this.objectFactory
				.createRevocationValuesType();
		if (revocationData.hasCRLs()) {
			CRLValuesType crlValues = this.objectFactory.createCRLValuesType();
			revocationValues.setCRLValues(crlValues);
			List<EncapsulatedPKIDataType> encapsulatedCrlValues = crlValues
					.getEncapsulatedCRLValue();
			List<byte[]> crls = revocationData.getCRLs();
			for (byte[] crl : crls) {
				EncapsulatedPKIDataType encapsulatedCrlValue = this.objectFactory
						.createEncapsulatedPKIDataType();
				encapsulatedCrlValue.setValue(crl);
				encapsulatedCrlValues.add(encapsulatedCrlValue);
			}
		}
		if (revocationData.hasOCSPs()) {
			OCSPValuesType ocspValues = this.objectFactory
					.createOCSPValuesType();
			revocationValues.setOCSPValues(ocspValues);
			List<EncapsulatedPKIDataType> encapsulatedOcspValues = ocspValues
					.getEncapsulatedOCSPValue();
			List<byte[]> ocsps = revocationData.getOCSPs();
			for (byte[] ocsp : ocsps) {
				EncapsulatedPKIDataType encapsulatedOcspValue = this.objectFactory
						.createEncapsulatedPKIDataType();
				encapsulatedOcspValue.setValue(ocsp);
				encapsulatedOcspValues.add(encapsulatedOcspValue);
			}
		}
		return revocationValues;
	}
}
