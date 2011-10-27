/*
 * eID Applet Project.
 * Copyright (C) 2008-2010 FedICT.
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

package test.be.fedict.eid.applet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.sc.PcscEid;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSigGenericPKCS;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;

public class PdfSpikeTest {

	private static final Log LOG = LogFactory.getLog(PdfSpikeTest.class);

	@Test
	public void testSignPDF() throws Exception {
		// create a sample PDF file
		Document document = new Document();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, baos);

		document.open();

		Paragraph titleParagraph = new Paragraph("This is a test.");
		titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
		document.add(titleParagraph);

		document.newPage();
		Paragraph textParagraph = new Paragraph("Hello world.");
		document.add(textParagraph);

		document.close();

		File tmpFile = File.createTempFile("test-", ".pdf");
		LOG.debug("tmp file: " + tmpFile.getAbsolutePath());
		FileUtils.writeByteArrayToFile(tmpFile, baos.toByteArray());

		// eID
		PcscEid pcscEid = new PcscEid(new TestView(), new Messages(
				Locale.getDefault()));
		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEid.waitForEidPresent();
		}

		List<X509Certificate> signCertificateChain = pcscEid
				.getSignCertificateChain();
		Certificate[] certs = new Certificate[signCertificateChain.size()];
		for (int idx = 0; idx < certs.length; idx++) {
			certs[idx] = signCertificateChain.get(idx);
		}

		// open the pdf
		FileInputStream pdfInputStream = new FileInputStream(tmpFile);
		File signedTmpFile = File.createTempFile("test-signed-", ".pdf");
		PdfReader reader = new PdfReader(pdfInputStream);
		FileOutputStream pdfOutputStream = new FileOutputStream(signedTmpFile);
		PdfStamper stamper = PdfStamper.createSignature(reader,
				pdfOutputStream, '\0', null, true);

		// add extra page
		Rectangle pageSize = reader.getPageSize(1);
		int pageCount = reader.getNumberOfPages();
		int extraPageIndex = pageCount + 1;
		stamper.insertPage(extraPageIndex, pageSize);

		// calculate unique signature field name
		int signatureNameIndex = 1;
		String signatureName;
		AcroFields existingAcroFields = reader.getAcroFields();
		List<String> existingSignatureNames = existingAcroFields
				.getSignatureNames();
		do {
			signatureName = "Signature" + signatureNameIndex;
			signatureNameIndex++;
		} while (existingSignatureNames.contains(signatureName));
		LOG.debug("new unique signature name: " + signatureName);

		PdfSignatureAppearance signatureAppearance = stamper
				.getSignatureAppearance();
		signatureAppearance.setCrypto(null, certs, null,
				PdfSignatureAppearance.SELF_SIGNED);
		signatureAppearance
				.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);
		signatureAppearance.setReason("PDF Signature Test");
		signatureAppearance.setLocation("Belgium");
		signatureAppearance
				.setVisibleSignature(new Rectangle(54, 440, 234, 566),
						extraPageIndex, signatureName);
		signatureAppearance.setExternalDigest(new byte[128], new byte[20],
				"RSA");
		signatureAppearance.preClose();

		byte[] content = IOUtils.toByteArray(signatureAppearance
				.getRangeStream());
		byte[] hash = MessageDigest.getInstance("SHA-1").digest(content);
		byte[] signatureBytes = pcscEid.sign(hash, "SHA-1");
		pcscEid.close();

		PdfSigGenericPKCS sigStandard = signatureAppearance.getSigStandard();
		PdfPKCS7 signature = sigStandard.getSigner();
		signature.setExternalDigest(signatureBytes, hash, "RSA");
		PdfDictionary dictionary = new PdfDictionary();
		dictionary.put(PdfName.CONTENTS,
				new PdfString(signature.getEncodedPKCS1()).setHexWriting(true));
		signatureAppearance.close(dictionary);

		LOG.debug("signed tmp file: " + signedTmpFile.getAbsolutePath());

		// verify the signature
		reader = new PdfReader(new FileInputStream(signedTmpFile));
		AcroFields acroFields = reader.getAcroFields();
		ArrayList<String> signatureNames = acroFields.getSignatureNames();
		for (String signName : signatureNames) {
			LOG.debug("signature name: " + signName);
			LOG.debug("signature covers whole document: "
					+ acroFields.signatureCoversWholeDocument(signName));
			LOG.debug("document revision " + acroFields.getRevision(signName)
					+ " of " + acroFields.getTotalRevisions());
			PdfPKCS7 pkcs7 = acroFields.verifySignature(signName);
			Calendar signDate = pkcs7.getSignDate();
			LOG.debug("signing date: " + signDate.getTime());
			LOG.debug("Subject: "
					+ PdfPKCS7.getSubjectFields(pkcs7.getSigningCertificate()));
			LOG.debug("Document modified: " + !pkcs7.verify());
			Certificate[] verifyCerts = pkcs7.getCertificates();
			for (Certificate certificate : verifyCerts) {
				X509Certificate x509Certificate = (X509Certificate) certificate;
				LOG.debug("cert subject: "
						+ x509Certificate.getSubjectX500Principal());
			}
		}

		/*
		 * Reading the signature using Apache PDFBox.
		 */
		PDDocument pdDocument = PDDocument.load(signedTmpFile);
		COSDictionary trailer = pdDocument.getDocument().getTrailer();
		/*
		 * PDF Reference - third edition - Adobe Portable Document Format -
		 * Version 1.4 - 3.6.1 Document Catalog
		 */
		COSDictionary documentCatalog = (COSDictionary) trailer
				.getDictionaryObject(COSName.ROOT);

		/*
		 * 8.6.1 Interactive Form Dictionary
		 */
		COSDictionary acroForm = (COSDictionary) documentCatalog
				.getDictionaryObject(COSName.ACRO_FORM);

		COSArray fields = (COSArray) acroForm
				.getDictionaryObject(COSName.FIELDS);
		for (int fieldIdx = 0; fieldIdx < fields.size(); fieldIdx++) {
			COSDictionary field = (COSDictionary) fields.getObject(fieldIdx);
			String fieldType = field.getNameAsString("FT");
			if ("Sig".equals(fieldType)) {
				COSDictionary signatureDictionary = (COSDictionary) field
						.getDictionaryObject(COSName.V);
				/*
				 * TABLE 8.60 Entries in a signature dictionary
				 */
				COSString signatoryName = (COSString) signatureDictionary
						.getDictionaryObject(COSName.NAME);
				if (null != signatoryName) {
					LOG.debug("signatory name: " + signatoryName.getString());
				}
				COSString reason = (COSString) signatureDictionary
						.getDictionaryObject(COSName.REASON);
				if (null != reason) {
					LOG.debug("reason: " + reason.getString());
				}
				COSString location = (COSString) signatureDictionary
						.getDictionaryObject(COSName.LOCATION);
				if (null != location) {
					LOG.debug("location: " + location.getString());
				}
				Calendar signingTime = signatureDictionary.getDate(COSName.M);
				if (null != signingTime) {
					LOG.debug("signing time: " + signingTime.getTime());
				}
				String signatureHandler = signatureDictionary
						.getNameAsString(COSName.FILTER);
				LOG.debug("signature handler: " + signatureHandler);
			}
		}
	}
}
