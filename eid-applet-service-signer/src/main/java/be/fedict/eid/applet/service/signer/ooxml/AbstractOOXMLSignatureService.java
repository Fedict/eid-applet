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

package be.fedict.eid.applet.service.signer.ooxml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dsig.CanonicalizationMethod;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.facets.KeyInfoSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.time.ConstantLocalClock;

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

	private final XAdESSignatureFacet xadesSignatureFacet;

	protected AbstractOOXMLSignatureService() {
		ConstantLocalClock clock = new ConstantLocalClock();
		addSignatureFacet(new OOXMLSignatureFacet(this, clock));
		addSignatureFacet(new KeyInfoSignatureFacet(true, false, false));

		this.xadesSignatureFacet = new XAdESSignatureFacet(clock);
		this.xadesSignatureFacet.setXadesNamespacePrefix("xd");
		this.xadesSignatureFacet.setIdSignedProperties("idSignedProperties");
		this.xadesSignatureFacet.setSignaturePolicyImplied(true);
		setSignatureId("idPackageSignature");
		// addSignatureFacet(xadesSignatureFacet);
		/*
		 * Activating the XAdES-BES still breaks the signature somehow.
		 */
	}

	/**
	 * Gives back the used XAdES signature facet.
	 * 
	 * @return
	 */
	protected XAdESSignatureFacet getXAdESSignatureFacet() {
		return this.xadesSignatureFacet;
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
	protected String getCanonicalizationMethod() {
		return CanonicalizationMethod.INCLUSIVE;
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
		boolean hasOriginSigsRels = false;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			LOG.debug("copy ZIP entry: " + zipEntry.getName());
			ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
			zipOutputStream.putNextEntry(newZipEntry);
			if ("[Content_Types].xml".equals(zipEntry.getName())) {
				Document contentTypesDocument = loadDocumentNoClose(zipInputStream);
				Element typesElement = contentTypesDocument
						.getDocumentElement();

				/*
				 * We need to add an Override element.
				 */
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

				Element nsElement = contentTypesDocument.createElement("ns");
				nsElement
						.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
								"http://schemas.openxmlformats.org/package/2006/content-types");
				NodeList nodeList = XPathAPI.selectNodeList(
						contentTypesDocument,
						"/tns:Types/tns:Default[@Extension='sigs']", nsElement);
				if (0 == nodeList.getLength()) {
					/*
					 * Add Default element for 'sigs' extension.
					 */
					Element defaultElement = contentTypesDocument
							.createElementNS(
									"http://schemas.openxmlformats.org/package/2006/content-types",
									"Default");
					defaultElement.setAttribute("Extension", "sigs");
					defaultElement
							.setAttribute("ContentType",
									"application/vnd.openxmlformats-package.digital-signature-origin");
					typesElement.appendChild(defaultElement);
				}

				writeDocumentNoClosing(contentTypesDocument, zipOutputStream,
						false);
			} else if ("_rels/.rels".equals(zipEntry.getName())) {
				Document relsDocument = loadDocumentNoClose(zipInputStream);

				Element nsElement = relsDocument.createElement("ns");
				nsElement
						.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
								"http://schemas.openxmlformats.org/package/2006/relationships");
				NodeList nodeList = XPathAPI
						.selectNodeList(
								relsDocument,
								"/tns:Relationships/tns:Relationship[@Target='_xmlsignatures/origin.sigs']",
								nsElement);
				if (0 == nodeList.getLength()) {
					Element relationshipElement = relsDocument
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

					relsDocument.getDocumentElement().appendChild(
							relationshipElement);
				}

				writeDocumentNoClosing(relsDocument, zipOutputStream, false);
			} else if ("_xmlsignatures/_rels/origin.sigs.rels".equals(zipEntry
					.getName())) {
				hasOriginSigsRels = true;
				Document originSignRelsDocument = loadDocumentNoClose(zipInputStream);

				Element relationshipElement = originSignRelsDocument
						.createElementNS(
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
				originSignRelsDocument.getDocumentElement().appendChild(
						relationshipElement);

				writeDocumentNoClosing(originSignRelsDocument, zipOutputStream,
						false);
			} else {
				IOUtils.copy(zipInputStream, zipOutputStream);
			}
		}

		if (false == hasOriginSigsRels) {
			/*
			 * Add signature relationships document.
			 */
			addOriginSigsRels(signatureZipEntryName, zipOutputStream);
			addOriginSigs(zipOutputStream);
		}

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
