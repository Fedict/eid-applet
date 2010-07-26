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

/*
 * Copyright (C) 2008-2009 FedICT.
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

package test.unit.be.fedict.eid.applet.service.signer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.xml.crypto.Data;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.jcp.xml.dsig.internal.dom.DOMReference;
import org.jcp.xml.dsig.internal.dom.DOMXMLSignature;
import org.joda.time.DateTime;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.EnvelopedSignatureFacet;
import be.fedict.eid.applet.service.signer.odf.ApacheNodeSetData;
import be.fedict.eid.applet.service.spi.DigestInfo;

public class AbstractXmlSignatureServiceTest {

	private static final Log LOG = LogFactory
			.getLog(AbstractXmlSignatureServiceTest.class);

	private static class XmlSignatureTestService extends
			AbstractXmlSignatureService {

		private Document envelopingDocument;

		private TemporaryTestDataStorage temporaryDataStorage;

		private String signatureDescription;

		private ByteArrayOutputStream signedDocumentOutputStream;

		private URIDereferencer uriDereferencer;

		public XmlSignatureTestService() {
			this(null);
		}

		public XmlSignatureTestService(SignatureFacet signatureFacet) {
			super();
			this.temporaryDataStorage = new TemporaryTestDataStorage();
			this.signedDocumentOutputStream = new ByteArrayOutputStream();
			if (null != signatureFacet) {
				addSignatureFacet(signatureFacet);
			}
		}

		public byte[] getSignedDocumentData() {
			return this.signedDocumentOutputStream.toByteArray();
		}

		public void setEnvelopingDocument(Document envelopingDocument) {
			this.envelopingDocument = envelopingDocument;
		}

		@Override
		protected Document getEnvelopingDocument() {
			return this.envelopingDocument;
		}

		@Override
		protected String getSignatureDescription() {
			return this.signatureDescription;
		}

		public void setSignatureDescription(String signatureDescription) {
			this.signatureDescription = signatureDescription;
		}

		@Override
		protected OutputStream getSignedDocumentOutputStream() {
			return this.signedDocumentOutputStream;
		}

		@Override
		protected TemporaryDataStorage getTemporaryDataStorage() {
			return this.temporaryDataStorage;
		}

		public String getFilesDigestAlgorithm() {
			return null;
		}

		@Override
		protected URIDereferencer getURIDereferencer() {
			return this.uriDereferencer;
		}

		public void setUriDereferencer(URIDereferencer uriDereferencer) {
			this.uriDereferencer = uriDereferencer;
		}
	}

	@Test
	public void testSignEnvelopingDocument() throws Exception {
		// setup
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "tns:root");
		rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
				"urn:test");
		document.appendChild(rootElement);
		Element dataElement = document.createElementNS("urn:test", "tns:data");
		dataElement.setAttributeNS(null, "Id", "id-1234");
		dataElement.setTextContent("data to be signed");
		rootElement.appendChild(dataElement);

		SignatureTestFacet signatureFacet = new SignatureTestFacet();
		signatureFacet.addReferenceUri("#id-1234");
		XmlSignatureTestService testedInstance = new XmlSignatureTestService(
				signatureFacet);
		testedInstance.setEnvelopingDocument(document);
		testedInstance.setSignatureDescription("test-signature-description");

		// operate
		DigestInfo digestInfo = testedInstance.preSign(null, null);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate certificate = PkiTestUtils.generateCertificate(keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null, keyPair
				.getPrivate(), true, 0, null, null, new KeyUsage(
				KeyUsage.nonRepudiation));

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, Collections
				.singletonList(certificate));

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);
		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		Node signatureNode = signatureNodeList.item(0);

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
	}

	public static class UriTestDereferencer implements URIDereferencer {

		private final Map<String, byte[]> resources;

		public UriTestDereferencer() {
			this.resources = new HashMap<String, byte[]>();
		}

		public void addResource(String uri, byte[] data) {
			this.resources.put(uri, data);
		}

		public Data dereference(URIReference uriReference,
				XMLCryptoContext xmlCryptoContext) throws URIReferenceException {
			String uri = uriReference.getURI();
			byte[] data = this.resources.get(uri);
			if (null == data) {
				return null;
			}
			return new OctetStreamData(new ByteArrayInputStream(data));
		}
	}

	@Test
	public void testSignExternalUri() throws Exception {
		// setup
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();

		SignatureTestFacet signatureFacet = new SignatureTestFacet();
		signatureFacet.addReferenceUri("external-uri");
		XmlSignatureTestService testedInstance = new XmlSignatureTestService(
				signatureFacet);
		testedInstance.setEnvelopingDocument(document);
		testedInstance.setSignatureDescription("test-signature-description");
		UriTestDereferencer uriDereferencer = new UriTestDereferencer();
		uriDereferencer.addResource("external-uri", "hello world".getBytes());
		testedInstance.setUriDereferencer(uriDereferencer);

		// operate
		DigestInfo digestInfo = testedInstance.preSign(null, null);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate certificate = PkiTestUtils.generateCertificate(keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null, keyPair
				.getPrivate(), true, 0, null, null, new KeyUsage(
				KeyUsage.nonRepudiation));

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, Collections
				.singletonList(certificate));

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);
		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		Node signatureNode = signatureNodeList.item(0);

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);
		domValidateContext.setURIDereferencer(uriDereferencer);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
	}

	@Test
	public void testSignEnvelopingDocumentWithExternalDigestInfo()
			throws Exception {
		// setup
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "tns:root");
		rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
				"urn:test");
		document.appendChild(rootElement);

		XmlSignatureTestService testedInstance = new XmlSignatureTestService();
		testedInstance.setEnvelopingDocument(document);
		testedInstance.setSignatureDescription("test-signature-description");

		byte[] refData = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		messageDigest.update(refData);
		byte[] digestValue = messageDigest.digest();
		DigestInfo refDigestInfo = new DigestInfo(digestValue, "SHA-1",
				"urn:test:ref");

		// operate
		DigestInfo digestInfo = testedInstance.preSign(Collections
				.singletonList(refDigestInfo), null);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate certificate = PkiTestUtils.generateCertificate(keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null, keyPair
				.getPrivate(), true, 0, null, null, new KeyUsage(
				KeyUsage.nonRepudiation));

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, Collections
				.singletonList(certificate));

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);
		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		Node signatureNode = signatureNodeList.item(0);

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);
		URIDereferencer dereferencer = new URITest2Dereferencer();
		domValidateContext.setURIDereferencer(dereferencer);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
	}

	@Test
	public void testSignExternalDigestInfo() throws Exception {
		// setup
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();

		XmlSignatureTestService testedInstance = new XmlSignatureTestService();
		testedInstance.setEnvelopingDocument(document);
		testedInstance.setSignatureDescription("test-signature-description");

		byte[] refData = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		messageDigest.update(refData);
		byte[] digestValue = messageDigest.digest();
		DigestInfo refDigestInfo = new DigestInfo(digestValue, "SHA-1",
				"urn:test:ref");

		// operate
		DigestInfo digestInfo = testedInstance.preSign(Collections
				.singletonList(refDigestInfo), null);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate certificate = PkiTestUtils.generateCertificate(keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null, keyPair
				.getPrivate(), true, 0, null, null, new KeyUsage(
				KeyUsage.nonRepudiation));

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, Collections
				.singletonList(certificate));

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);
		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		Node signatureNode = signatureNodeList.item(0);

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);
		URIDereferencer dereferencer = new URITest2Dereferencer();
		domValidateContext.setURIDereferencer(dereferencer);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
	}

	private static class URITest2Dereferencer implements URIDereferencer {

		private static final Log LOG = LogFactory
				.getLog(URITest2Dereferencer.class);

		public Data dereference(URIReference uriReference,
				XMLCryptoContext context) throws URIReferenceException {
			LOG.debug("dereference: " + uriReference.getURI());
			return new OctetStreamData(new ByteArrayInputStream("hello world"
					.getBytes()));
		}
	}

	@Test
	public void testJsr105Signature() throws Exception {
		KeyPair keyPair = PkiTestUtils.generateKeyPair();

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "tns:root");
		rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
				"urn:test");
		document.appendChild(rootElement);
		Element dataElement = document.createElementNS("urn:test", "tns:data");
		dataElement.setAttributeNS(null, "Id", "id-1234");
		dataElement.setTextContent("data to be signed");
		rootElement.appendChild(dataElement);

		XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance(
				"DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());

		XMLSignContext signContext = new DOMSignContext(keyPair.getPrivate(),
				document.getDocumentElement());
		signContext.putNamespacePrefix(
				javax.xml.crypto.dsig.XMLSignature.XMLNS, "ds");

		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA1, null);
		Reference reference = signatureFactory.newReference("#id-1234",
				digestMethod);
		DOMReference domReference = (DOMReference) reference;
		assertNull(domReference.getCalculatedDigestValue());
		assertNull(domReference.getDigestValue());

		SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
				SignatureMethod.RSA_SHA1, null);
		CanonicalizationMethod canonicalizationMethod = signatureFactory
				.newCanonicalizationMethod(
						CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null);
		SignedInfo signedInfo = signatureFactory.newSignedInfo(
				canonicalizationMethod, signatureMethod, Collections
						.singletonList(reference));

		javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory
				.newXMLSignature(signedInfo, null);

		DOMXMLSignature domXmlSignature = (DOMXMLSignature) xmlSignature;
		domXmlSignature.marshal(document.getDocumentElement(), "ds",
				(DOMCryptoContext) signContext);
		domReference.digest(signContext);
		// xmlSignature.sign(signContext);
		// LOG.debug("signed document: " + toString(document));

		Element nsElement = document.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(document,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());
	}

	@Test
	public void testJsr105SignatureExternalXML() throws Exception {
		KeyPair keyPair = PkiTestUtils.generateKeyPair();

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "tns:root");
		rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
				"urn:test");
		document.appendChild(rootElement);
		Element dataElement = document.createElementNS("urn:test", "tns:data");
		dataElement.setAttributeNS(null, "Id", "id-1234");
		dataElement.setTextContent("data to be signed");
		rootElement.appendChild(dataElement);

		XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance(
				"DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());

		XMLSignContext signContext = new DOMSignContext(keyPair.getPrivate(),
				document.getDocumentElement());
		signContext.setURIDereferencer(new MyURIDereferencer());
		signContext.putNamespacePrefix(
				javax.xml.crypto.dsig.XMLSignature.XMLNS, "ds");

		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA1, null);

		List<Transform> transforms = new LinkedList<Transform>();
		Transform transform = signatureFactory
				.newTransform(CanonicalizationMethod.INCLUSIVE,
						(TransformParameterSpec) null);
		transforms.add(transform);
		Reference reference = signatureFactory.newReference("/helloworld.xml",
				digestMethod, transforms, null, null);

		DOMReference domReference = (DOMReference) reference;
		assertNull(domReference.getCalculatedDigestValue());
		assertNull(domReference.getDigestValue());

		SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
				SignatureMethod.RSA_SHA1, null);
		CanonicalizationMethod canonicalizationMethod = signatureFactory
				.newCanonicalizationMethod(
						CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null);
		SignedInfo signedInfo = signatureFactory.newSignedInfo(
				canonicalizationMethod, signatureMethod, Collections
						.singletonList(reference));

		javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory
				.newXMLSignature(signedInfo, null);

		DOMXMLSignature domXmlSignature = (DOMXMLSignature) xmlSignature;
		domXmlSignature.marshal(document.getDocumentElement(), "ds",
				(DOMCryptoContext) signContext);
		domReference.digest(signContext);
		// xmlSignature.sign(signContext);
		// LOG.debug("signed document: " + toString(document));

		Element nsElement = document.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(document,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());
	}

	@Test
	public void testJsr105SignatureExternalXMLWithDTD() throws Exception {
		KeyPair keyPair = PkiTestUtils.generateKeyPair();

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "tns:root");
		rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
				"urn:test");
		document.appendChild(rootElement);
		Element dataElement = document.createElementNS("urn:test", "tns:data");
		dataElement.setAttributeNS(null, "Id", "id-1234");
		dataElement.setTextContent("data to be signed");
		rootElement.appendChild(dataElement);

		XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance(
				"DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());

		XMLSignContext signContext = new DOMSignContext(keyPair.getPrivate(),
				document.getDocumentElement());
		signContext.setURIDereferencer(new MyURIDereferencer());
		signContext.putNamespacePrefix(
				javax.xml.crypto.dsig.XMLSignature.XMLNS, "ds");

		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA1, null);

		List<Transform> transforms = new LinkedList<Transform>();
		Transform transform = signatureFactory
				.newTransform(CanonicalizationMethod.INCLUSIVE,
						(TransformParameterSpec) null);
		LOG.debug("transform type: " + transform.getClass().getName());
		transforms.add(transform);
		Reference reference = signatureFactory.newReference("/bookstore.xml",
				digestMethod, transforms, null, null);

		DOMReference domReference = (DOMReference) reference;
		assertNull(domReference.getCalculatedDigestValue());
		assertNull(domReference.getDigestValue());

		SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
				SignatureMethod.RSA_SHA1, null);
		CanonicalizationMethod canonicalizationMethod = signatureFactory
				.newCanonicalizationMethod(
						CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null);
		SignedInfo signedInfo = signatureFactory.newSignedInfo(
				canonicalizationMethod, signatureMethod, Collections
						.singletonList(reference));

		javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory
				.newXMLSignature(signedInfo, null);

		DOMXMLSignature domXmlSignature = (DOMXMLSignature) xmlSignature;
		domXmlSignature.marshal(document.getDocumentElement(), "ds",
				(DOMCryptoContext) signContext);
		domReference.digest(signContext);
		// xmlSignature.sign(signContext);
		// LOG.debug("signed document: " + toString(document));

		Element nsElement = document.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(document,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());
	}

	@Test
	public void testLoadXMLFileWithDTD() throws Exception {
		InputStream documentInputStream = AbstractXmlSignatureServiceTest.class
				.getResourceAsStream("/bookstore.xml");

		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		EntityResolver entityResolver = new MyEntityResolver();
		documentBuilder.setEntityResolver(entityResolver);
		Document document = documentBuilder.parse(inputSource);
		assertNotNull(document);
	}

	private static class MyEntityResolver implements EntityResolver {

		private static final Log LOG = LogFactory
				.getLog(MyEntityResolver.class);

		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			LOG.debug("resolve entity");
			LOG.debug("publicId: " + publicId);
			LOG.debug("systemId: " + systemId);
			if ("http://webserver/bookstore.dtd".equals(systemId)) {
				InputStream dtdInputStream = MyEntityResolver.class
						.getResourceAsStream("/bookstore.dtd");
				InputSource inputSource = new InputSource(dtdInputStream);
				return inputSource;
			}
			return null;
		}
	}

	@Test
	public void testSignEnvelopingDocumentWithDTD() throws Exception {
		// setup
		InputStream documentInputStream = AbstractXmlSignatureServiceTest.class
				.getResourceAsStream("/bookstore.xml");

		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		EntityResolver entityResolver = new MyEntityResolver();
		documentBuilder.setEntityResolver(entityResolver);
		Document document = documentBuilder.parse(inputSource);

		SignatureFacet signatureFacet = new EnvelopedSignatureFacet();
		XmlSignatureTestService testedInstance = new XmlSignatureTestService(
				signatureFacet);
		testedInstance.setEnvelopingDocument(document);
		testedInstance.setSignatureDescription("test-signature-description");

		// operate
		DigestInfo digestInfo = testedInstance.preSign(null, null);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate certificate = PkiTestUtils.generateCertificate(keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null, keyPair
				.getPrivate(), true, 0, null, null, new KeyUsage(
				KeyUsage.nonRepudiation));

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, Collections
				.singletonList(certificate));

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);
		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		Node signatureNode = signatureNodeList.item(0);

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
	}

	@Test
	public void testSignExternalXMLDocument() throws Exception {
		// setup
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElementNS("urn:test", "tns:root");
		rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
				"urn:test");
		document.appendChild(rootElement);

		SignatureTestFacet signatureFacet = new SignatureTestFacet();
		signatureFacet.addReferenceUri("/bookstore.xml");
		XmlSignatureTestService testedInstance = new XmlSignatureTestService(
				signatureFacet);

		testedInstance.setUriDereferencer(new MyURIDereferencer());
		testedInstance.setEnvelopingDocument(document);
		testedInstance.setSignatureDescription("test-signature-description");

		// operate
		DigestInfo digestInfo = testedInstance.preSign(null, null);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		KeyPair keyPair = PkiTestUtils.generateKeyPair();
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate certificate = PkiTestUtils.generateCertificate(keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null, keyPair
				.getPrivate(), true, 0, null, null, new KeyUsage(
				KeyUsage.nonRepudiation));

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, Collections
				.singletonList(certificate));

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);
		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		Node signatureNode = signatureNodeList.item(0);

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);

		/*
		 * Required to resolve the external XML document.
		 */
		domValidateContext.setURIDereferencer(new MyURIDereferencer());

		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
	}

	private static class MyURIDereferencer implements URIDereferencer {

		private static final Log LOG = LogFactory
				.getLog(MyURIDereferencer.class);

		public Data dereference(URIReference uriReference,
				XMLCryptoContext context) throws URIReferenceException {
			String uri = uriReference.getURI();
			LOG.debug("dereference URI: " + uri);
			LOG.debug("dereference Type: " + uriReference.getType());
			if ("/helloworld.xml".equals(uri)) {
				InputStream dataInputStream = MyURIDereferencer.class
						.getResourceAsStream("/helloworld.xml");
				return new OctetStreamData(dataInputStream, uri, null);
			}
			if ("/bookstore.xml".equals(uri)) {
				InputStream dataInputStream = MyURIDereferencer.class
						.getResourceAsStream("/bookstore.xml");
				InputSource inputSource = new InputSource(dataInputStream);
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
						.newInstance();
				documentBuilderFactory.setNamespaceAware(true);
				DocumentBuilder documentBuilder;
				try {
					documentBuilder = documentBuilderFactory
							.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					throw new URIReferenceException(e);
				}
				EntityResolver entityResolver = new MyEntityResolver();
				documentBuilder.setEntityResolver(entityResolver);
				Document document;
				try {
					document = documentBuilder.parse(inputSource);
				} catch (SAXException e) {
					throw new URIReferenceException(e);
				} catch (IOException e) {
					throw new URIReferenceException(e);
				}
				XMLSignatureInput xmlSignatureInput = new XMLSignatureInput(
						document);
				ApacheNodeSetData apacheNodeSetData = new ApacheNodeSetData(
						xmlSignatureInput);
				return apacheNodeSetData;
			}
			throw new URIReferenceException("cannot dereference: " + uri);
		}
	}

}
