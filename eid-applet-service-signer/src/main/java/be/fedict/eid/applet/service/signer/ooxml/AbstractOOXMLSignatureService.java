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

package be.fedict.eid.applet.service.signer.ooxml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.jcp.xml.dsig.internal.dom.DOMKeyInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.NoCloseOutputStream;

/**
 * Signature Service implementation for Office OpenXML document format XML
 * signatures.
 * 
 * @author Frank Cornelis
 * 
 */
public abstract class AbstractOOXMLSignatureService extends
		AbstractXmlSignatureService {

	static final Log LOG = LogFactory
			.getLog(AbstractOOXMLSignatureService.class);

	protected AbstractOOXMLSignatureService() {
		addSignatureAspect(new OOXMLSignatureAspect(this));
	}

	@Override
	protected String getSignatureDescription() {
		return "Office OpenXML Document";
	}

	public String getFilesDigestAlgorithm() {
		return null;
	}

	@Override
	protected final URIDereferencer getURIDereferencer() {
		URL ooxmlUrl = getOfficeOpenXMLDocumentURL();
		return new OOXMLURIDereferencer(ooxmlUrl);
	}

	@Override
	protected void postSign(Element signatureElement,
			List<X509Certificate> signingCertificateChain) {
		// TODO: implement as SignatureAspect
		LOG.debug("postSign: adding ds:KeyInfo");
		/*
		 * Make sure we insert right after the ds:SignatureValue element.
		 */
		Node nextSibling;
		NodeList objectNodeList = signatureElement.getElementsByTagNameNS(
				"http://www.w3.org/2000/09/xmldsig#", "Object");
		if (0 == objectNodeList.getLength()) {
			nextSibling = null;
		} else {
			nextSibling = objectNodeList.item(0);
		}
		/*
		 * Add a ds:KeyInfo entry.
		 */
		KeyInfoFactory keyInfoFactory = KeyInfoFactory.getInstance();
		List<Object> x509DataObjects = new LinkedList<Object>();

		X509Certificate signingCertificate = signingCertificateChain.get(0);
		KeyValue keyValue;
		try {
			keyValue = keyInfoFactory.newKeyValue(signingCertificate
					.getPublicKey());
		} catch (KeyException e) {
			throw new RuntimeException("key exception: " + e.getMessage(), e);
		}

		for (X509Certificate certificate : signingCertificateChain) {
			x509DataObjects.add(certificate);
		}
		X509Data x509Data = keyInfoFactory.newX509Data(x509DataObjects);
		List<Object> keyInfoContent = new LinkedList<Object>();
		keyInfoContent.add(keyValue);
		keyInfoContent.add(x509Data);
		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoContent);
		DOMKeyInfo domKeyInfo = (DOMKeyInfo) keyInfo;
		Key key = new Key() {
			private static final long serialVersionUID = 1L;

			public String getAlgorithm() {
				return null;
			}

			public byte[] getEncoded() {
				return null;
			}

			public String getFormat() {
				return null;
			}
		};
		XMLSignContext xmlSignContext = new DOMSignContext(key,
				signatureElement);
		DOMCryptoContext domCryptoContext = (DOMCryptoContext) xmlSignContext;
		String dsPrefix = null;
		// String dsPrefix = "ds";
		try {
			domKeyInfo.marshal(signatureElement, nextSibling, dsPrefix,
					domCryptoContext);
		} catch (MarshalException e) {
			throw new RuntimeException("marshall error: " + e.getMessage(), e);
		}
	}

	private class OOXMLSignedDocumentOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() throws IOException {
			LOG.debug("close OOXML signed document output stream");
			super.close();
			try {
				outputSignedOfficeOpenXMLDocument(this.toByteArray());
			} catch (Exception e) {
				throw new IOException("generic error: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * The output stream to which to write the signed Office OpenXML file.
	 * 
	 * @return
	 */
	abstract protected OutputStream getSignedOfficeOpenXMLDocumentOutputStream();

	/**
	 * Gives back the URL of the OOXML to be signed.
	 * 
	 * @return
	 */
	abstract protected URL getOfficeOpenXMLDocumentURL();

	private void outputSignedOfficeOpenXMLDocument(byte[] signatureData)
			throws IOException, ParserConfigurationException, SAXException,
			TransformerException {
		LOG.debug("output signed Office OpenXML document");
		OutputStream signedOOXMLOutputStream = getSignedOfficeOpenXMLDocumentOutputStream();
		if (null == signedOOXMLOutputStream) {
			throw new NullPointerException("signedOOXMLOutputStream is null");
		}

		String signatureZipEntryName = "_xmlsignatures/sig-"
				+ UUID.randomUUID().toString() + ".xml";
		LOG.debug("signature ZIP entry name: " + signatureZipEntryName);
		/*
		 * Copy the original OOXML content to the signed OOXML package. During
		 * copying some files need to changed.
		 */
		ZipOutputStream zipOutputStream = copyOOXMLContent(
				signatureZipEntryName, signedOOXMLOutputStream);

		/*
		 * Add the OOXML XML signature file to the OOXML package.
		 */
		ZipEntry zipEntry = new ZipEntry(signatureZipEntryName);
		zipOutputStream.putNextEntry(zipEntry);
		IOUtils.write(signatureData, zipOutputStream);
		zipOutputStream.close();
	}

	private List<String> getSignatureResourceNames(URL url) throws IOException,
			ParserConfigurationException, SAXException, TransformerException {
		List<String> signatureResourceNames = getResourceNames(url,
				"application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml");
		return signatureResourceNames;
	}

	private List<String> getResourceNames(URL url, String contentType)
			throws IOException, ParserConfigurationException, SAXException,
			TransformerException {
		List<String> signatureResourceNames = new LinkedList<String>();
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
			NodeList nodeList = XPathAPI.selectNodeList(contentTypesDocument,
					"/tns:Types/tns:Override[@ContentType='" + contentType
							+ "']/@PartName", nsElement);
			for (int nodeIdx = 0; nodeIdx < nodeList.getLength(); nodeIdx++) {
				String partName = nodeList.item(nodeIdx).getTextContent();
				LOG.debug("part name: " + partName);
				partName = partName.substring(1); // remove '/'
				signatureResourceNames.add(partName);
			}
			break;
		}
		return signatureResourceNames;
	}

	private ZipOutputStream copyOOXMLContent(String signatureZipEntryName,
			OutputStream signedOOXMLOutputStream) throws IOException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException {
		ZipOutputStream zipOutputStream = new ZipOutputStream(
				signedOOXMLOutputStream);
		ZipInputStream zipInputStream = new ZipInputStream(this
				.getOfficeOpenXMLDocumentURL().openStream());
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			LOG.debug("copy ZIP entry: " + zipEntry.getName());
			ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
			zipOutputStream.putNextEntry(newZipEntry);
			if ("[Content_Types].xml".equals(zipEntry.getName())) {
				/*
				 * We need to add an Override element.
				 */
				Document contentTypesDocument = loadDocumentNoClose(zipInputStream);
				Element typesElement = contentTypesDocument
						.getDocumentElement();
				Element overrideElement = contentTypesDocument
						.createElementNS(
								"http://schemas.openxmlformats.org/package/2006/content-types",
								"Override");
				overrideElement.setAttribute("PartName", "/"
						+ signatureZipEntryName);
				overrideElement
						.setAttribute("ContentType",
								"application/vnd.openxmlformats-package.digital-signature-xmlsignature+xml");
				typesElement.appendChild(overrideElement);

				NoCloseOutputStream outputStream = new NoCloseOutputStream(
						zipOutputStream);
				writeDocumentNoClosing(contentTypesDocument, outputStream,
						false);
			} else if ("_xmlsignatures/_rels/origin.sigs.rels".equals(zipEntry
					.getName())) {
				throw new RuntimeException("implement me");
			} else if ("_rels/.rels".equals(zipEntry.getName())) {
				/*
				 * Add a Relationship element for _xmlsignatures/origins.sigs
				 */
				Document relationshipsDocument = loadDocumentNoClose(zipInputStream);
				Element relationshipElement = relationshipsDocument
						.createElementNS(
								"http://schemas.openxmlformats.org/package/2006/relationships",
								"Relationship");
				relationshipElement.setAttribute("Id", "rel-id-"
						+ UUID.randomUUID().toString());
				relationshipElement
						.setAttribute(
								"Type",
								"http://schemas.openxmlformats.org/package/2006/relationships/digital-signature/origin");
				relationshipElement.setAttribute("Target",
						"_xmlsignatures/origin.sigs");
				relationshipsDocument.getDocumentElement().appendChild(
						relationshipElement);

				NoCloseOutputStream outputStream = new NoCloseOutputStream(
						zipOutputStream);
				writeDocumentNoClosing(relationshipsDocument, outputStream,
						false);
			} else {
				IOUtils.copy(zipInputStream, zipOutputStream);
			}
		}
		/*
		 * Add signature relationships document.
		 */
		addOriginSigsRels(signatureZipEntryName, zipOutputStream);

		addOriginSigs(zipOutputStream);

		/*
		 * Return.
		 */
		zipInputStream.close();
		return zipOutputStream;
	}

	private void addOriginSigs(ZipOutputStream zipOutputStream)
			throws IOException {
		zipOutputStream
				.putNextEntry(new ZipEntry("_xmlsignatures/origin.sigs"));
	}

	private void addOriginSigsRels(String signatureZipEntryName,
			ZipOutputStream zipOutputStream)
			throws ParserConfigurationException, IOException,
			TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document originSignRelsDocument = documentBuilder.newDocument();

		Element relationshipsElement = originSignRelsDocument.createElementNS(
				"http://schemas.openxmlformats.org/package/2006/relationships",
				"Relationships");
		relationshipsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns",
				"http://schemas.openxmlformats.org/package/2006/relationships");
		originSignRelsDocument.appendChild(relationshipsElement);

		Element relationshipElement = originSignRelsDocument.createElementNS(
				"http://schemas.openxmlformats.org/package/2006/relationships",
				"Relationship");
		String relationshipId = "rel-" + UUID.randomUUID().toString();
		relationshipElement.setAttribute("Id", relationshipId);
		relationshipElement
				.setAttribute(
						"Type",
						"http://schemas.openxmlformats.org/package/2006/relationships/digital-signature/signature");
		String target = FilenameUtils.getName(signatureZipEntryName);
		LOG.debug("target: " + target);
		relationshipElement.setAttribute("Target", target);
		relationshipsElement.appendChild(relationshipElement);

		zipOutputStream.putNextEntry(new ZipEntry(
				"_xmlsignatures/_rels/origin.sigs.rels"));
		writeDocumentNoClosing(originSignRelsDocument, zipOutputStream, false);
	}

	@Override
	protected OutputStream getSignedDocumentOutputStream() {
		LOG.debug("get signed document output stream");
		/*
		 * Create each time a new object; we want an empty output stream to
		 * start with.
		 */
		OutputStream signedDocumentOutputStream = new OOXMLSignedDocumentOutputStream();
		return signedDocumentOutputStream;
	}
}
