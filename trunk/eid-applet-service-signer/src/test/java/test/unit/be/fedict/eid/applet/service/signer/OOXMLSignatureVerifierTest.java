/*
 * eID Applet Project.
 * Copyright (C) 2009 FedICT.
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

package test.unit.be.fedict.eid.applet.service.signer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.signature.PackageDigitalSignatureManager;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLSignatureVerifier;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLURIDereferencer;

public class OOXMLSignatureVerifierTest {

	private static final Log LOG = LogFactory
			.getLog(OOXMLSignatureVerifierTest.class);

	@Test
	public void testIsOOXMLDocument() throws Exception {
		// setup
		URL url = OOXMLSignatureVerifierTest.class
				.getResource("/hello-world-unsigned.docx");

		// operate
		boolean result = OOXMLSignatureVerifier.isOOXML(url);

		// verify
		assertTrue(result);
	}

	@Test
	public void testODFIsNotOOXML() throws Exception {
		// setup
		URL url = OOXMLSignatureVerifierTest.class
				.getResource("/hello-world.odt");

		// operate
		boolean result = OOXMLSignatureVerifier.isOOXML(url);

		// verify
		assertFalse(result);
	}

	@Test
	public void testPOI() throws Exception {
		// setup
		InputStream inputStream = OOXMLSignatureVerifierTest.class
				.getResourceAsStream("/hello-world-unsigned.docx");

		// operate
		boolean result = POIXMLDocument.hasOOXMLHeader(inputStream);

		// verify
		assertTrue(result);
	}

	@Test
	public void testOPC() throws Exception {
		// setup
		InputStream inputStream = OOXMLSignatureVerifierTest.class
				.getResourceAsStream("/hello-world-signed.docx");

		// operate
		OPCPackage opcPackage = OPCPackage.open(inputStream);

		ArrayList<PackagePart> parts = opcPackage.getParts();
		for (PackagePart part : parts) {
			LOG.debug("part name: " + part.getPartName().getName());
			LOG.debug("part content type: " + part.getContentType());
		}

		ArrayList<PackagePart> signatureParts = opcPackage
				.getPartsByContentType("application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml");
		assertFalse(signatureParts.isEmpty());

		PackagePart signaturePart = signatureParts.get(0);
		LOG.debug("signature part class type: "
				+ signaturePart.getClass().getName());

		PackageDigitalSignatureManager packageDigitalSignatureManager = new PackageDigitalSignatureManager();
		// yeah... POI implementation still missing
	}

	@Test
	public void testVerifySignature() throws Exception {

		java.util.logging.Logger logger = java.util.logging.Logger
				.getLogger("org.jcp.xml.dsig.internal.dom");
		logger.log(Level.FINE, "test");

		URL url = OOXMLSignatureVerifierTest.class
				.getResource("/hello-world-signed.docx");
		String signatureResourceName = getSignatureResourceName(url);
		LOG.debug("signature resource name: " + signatureResourceName);

		Security.addProvider(new OOXMLProvider());

		ZipInputStream zipInputStream = new ZipInputStream(url.openStream());
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			if (false == signatureResourceName.equals(zipEntry.getName())) {
				continue;
			}
			Document signatureDocument = loadDocument(zipInputStream);
			LOG.debug("signature loaded");
			NodeList signatureNodeList = signatureDocument
					.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			assertEquals(1, signatureNodeList.getLength());
			Node signatureNode = signatureNodeList.item(0);
			KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keySelector, signatureNode);
			domValidateContext.setProperty(
					"org.jcp.xml.dsig.validateManifests", Boolean.TRUE);

			OOXMLURIDereferencer dereferencer = new OOXMLURIDereferencer(url);
			domValidateContext.setURIDereferencer(dereferencer);

			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
					.getInstance();
			XMLSignature xmlSignature = xmlSignatureFactory
					.unmarshalXMLSignature(domValidateContext);
			boolean validity = xmlSignature.validate(domValidateContext);
			assertTrue(validity);
			List objects = xmlSignature.getObjects();
			for (Object object : objects) {
				LOG.debug("ds:Object class type: "
						+ object.getClass().getName());
			}
			break;
		}
	}

	private String getSignatureResourceName(URL url) throws IOException,
			ParserConfigurationException, SAXException, TransformerException {
		InputStream inputStream = url.openStream();
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			if (false == "[Content_Types].xml".equals(zipEntry.getName())) {
				continue;
			}
			Document contentTypesDocument = loadDocument(zipInputStream);
			Element nsElement = contentTypesDocument.createElement("ns");
			nsElement
					.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
							"http://schemas.openxmlformats.org/package/2006/content-types");
			NodeList nodeList = XPathAPI
					.selectNodeList(
							contentTypesDocument,
							"/tns:Types/tns:Override[@ContentType='application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml']/@PartName",
							nsElement);
			if (nodeList.getLength() == 0) {
				return null;
			}
			String partName = nodeList.item(0).getTextContent();
			LOG.debug("part name: " + partName);
			partName = partName.substring(1); // remove '/'
			return partName;
		}
		return null;
	}

	private Document loadDocument(InputStream documentInputStream)
			throws ParserConfigurationException, SAXException, IOException {
		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.parse(inputSource);
		return document;
	}
}