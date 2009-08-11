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

package test.be.fedict.eid.applet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;
import be.fedict.eid.applet.sc.PcscEid;
import be.fedict.eid.applet.sc.PcscEidSpi;
import be.fedict.eid.applet.sc.Task;
import be.fedict.eid.applet.sc.TaskRunner;

/**
 * Integration tests for PC/SC eID component.
 * 
 * @author fcorneli
 * 
 */
public class PcscTest {

	private static final Log LOG = LogFactory.getLog(PcscTest.class);

	public static class TestView implements View {

		@Override
		public void addDetailMessage(String detailMessage) {
			LOG.debug("detail: " + detailMessage);
		}

		@Override
		public Component getParentComponent() {
			return null;
		}

		@Override
		public boolean privacyQuestion(boolean includeAddress,
				boolean includePhoto) {
			return false;
		}

		@Override
		public void setStatusMessage(Status status, String statusMessage) {
			LOG.debug("status: [" + status + "]: " + statusMessage);
		}

		@Override
		public void progressIndication(int max, int current) {
		}
	}

	private Messages messages;

	@Before
	public void setUp() {
		this.messages = new Messages(Locale.getDefault());
	}

	@Test
	public void pcscAuthnSignature() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}
		byte[] challenge = "hello world".getBytes();
		byte[] signatureValue = pcscEidSpi.signAuthn(challenge);
		List<X509Certificate> authnCertChain = pcscEidSpi
				.getAuthnCertificateChain();
		pcscEidSpi.close();

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initVerify(authnCertChain.get(0).getPublicKey());
		signature.update(challenge);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void logoff() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}

		pcscEidSpi.logoff();

		pcscEidSpi.close();
	}

	@Test
	public void pcscChangePin() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}

		pcscEidSpi.changePin();

		pcscEidSpi.close();
	}

	@Test
	public void pcscUnblockPin() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}

		pcscEidSpi.unblockPin();

		pcscEidSpi.close();
	}

	@Test
	public void photo() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}

		byte[] photo = pcscEidSpi.readFile(PcscEid.PHOTO_FILE_ID);
		LOG.debug("image size: " + photo.length);
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(photo));
		assertNotNull(image);
		LOG.debug("width: " + image.getWidth());
		LOG.debug("height: " + image.getHeight());

		pcscEidSpi.close();
	}

	@Test
	public void testReadAddress() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}

		pcscEidSpi.readFile(PcscEid.IDENTITY_FILE_ID);
		pcscEidSpi.readFile(PcscEid.ADDRESS_FILE_ID);

		pcscEidSpi.close();
	}

	private void selectCardManager(CardChannel cardChannel) {
		CommandAPDU selectApplicationApdu = new CommandAPDU(0x00, 0xA4, 0x04,
				0x00);
		ResponseAPDU responseApdu;
		try {
			responseApdu = cardChannel.transmit(selectApplicationApdu);
		} catch (CardException e) {
			LOG.debug("error selecting application");
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			LOG.debug("array error");
			return;
		}
		if (0x9000 != responseApdu.getSW()) {
			LOG.debug("could not select application");
		} else {
			LOG.debug("application selected");
		}
	}

	@Test
	public void testSelectBelpic() throws Exception {
		final PcscEid pcscEid = new PcscEid(new TestView(), this.messages);
		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEid.waitForEidPresent();
		}

		try {
			pcscEid.selectBelpicJavaCardApplet();
		} finally {
			pcscEid.close();
		}
	}

	@Test
	public void testCardManager() throws Exception {
		final PcscEid pcscEid = new PcscEid(new TestView(), this.messages);
		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEid.waitForEidPresent();
		}
		View view = new TestView();

		CardChannel cardChannel = pcscEid.getCardChannel();
		selectCardManager(cardChannel);
		// card manager active

		TaskRunner taskRunner = new TaskRunner(pcscEid, view);
		try {
			byte[] data = taskRunner.run(new Task<byte[]>() {
				public byte[] run() throws Exception {
					return pcscEid.readFile(PcscEid.IDENTITY_FILE_ID);
				}
			});
			assertNotNull(data);
		} finally {
			pcscEid.close();
		}
	}

	@Test
	public void displayCitizenCertificates() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}

		byte[] authnCertFile = pcscEidSpi.readFile(PcscEid.AUTHN_CERT_FILE_ID);
		byte[] signCertFile = pcscEidSpi.readFile(PcscEid.SIGN_CERT_FILE_ID);

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		X509Certificate authnCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(authnCertFile));
		X509Certificate signCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(signCertFile));

		LOG.debug("authentication certificate: " + authnCert);
		LOG.debug("signature certificate: " + signCert);
		LOG.debug("authn cert size: " + authnCertFile.length);
		LOG.debug("sign cert size: " + signCertFile.length);

		pcscEidSpi.close();
	}

	@Test
	public void testReadIdentityFile() throws Exception {
		PcscEidSpi pcscEidSpi = new PcscEid(new TestView(), this.messages);
		if (false == pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEidSpi.waitForEidPresent();
		}
		byte[] identityFile;
		try {
			identityFile = pcscEidSpi.readFile(PcscEid.IDENTITY_FILE_ID);
		} finally {
			pcscEidSpi.close();
		}
		File tmpIdentityFile = File.createTempFile("identity-", ".tlv");
		LOG.debug("tmp identity file: " + tmpIdentityFile.getAbsolutePath());
		FileUtils.writeByteArrayToFile(tmpIdentityFile, identityFile);
	}

	@Test
	public void testCcid() throws Exception {
		PcscEid pcscEid = new PcscEid(new TestView(), this.messages);
		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card");
			pcscEid.waitForEidPresent();
		}

		Card card = pcscEid.getCard();
		// GET FEATURE LIST
		byte[] features = card.transmitControlCommand(0x42000D48, new byte[0]);
		if (0 == features.length) {
			LOG.debug("no CCID reader");
			return;
		}
		LOG.debug("feature list: " + new String(Hex.encodeHex(features)));
		LOG.debug("feature verify pin direct: "
				+ hasFeature(FEATURE_VERIFY_PIN_DIRECT_TAG, features));
		Integer verifyPinControl = findFeature(FEATURE_VERIFY_PIN_DIRECT_TAG,
				features);
		LOG.debug("VERIFY PIN control: 0x"
				+ Integer.toHexString(verifyPinControl));

		CardChannel cardChannel = pcscEid.getCardChannel();
		CommandAPDU setApdu = new CommandAPDU(0x00, 0x22, 0x41, 0xB6,
				new byte[] { 0x04, // length of following data
						(byte) 0x80, // algo ref
						0x01, // rsa pkcs#1
						(byte) 0x84, // tag for private key ref
						(byte) 0x82 });
		ResponseAPDU responseApdu = cardChannel.transmit(setApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new RuntimeException("SELECT error");
		}

		ByteArrayOutputStream verifyCommand = new ByteArrayOutputStream();
		verifyCommand.write(30); // bTimeOut
		verifyCommand.write(30); // bTimeOut2
		verifyCommand.write(0x89); // bmFormatString: BCD PIN - SPR532 only,
		// else 0x01
		verifyCommand.write(0x47); // bmPINBlockString
		verifyCommand.write(0x04); // bmPINLengthFormat
		verifyCommand.write(new byte[] { 0x0C, 0x04 }); // wPINMaxExtraDigit
		verifyCommand.write(0x02); // bEntryValidationCondition
		verifyCommand.write(0x01); // bNumberMessage
		verifyCommand.write(new byte[] { 0x13, 0x08 }); // wLangId
		verifyCommand.write(0x00); // bMsgIndex
		verifyCommand.write(new byte[] { 0x00, 0x00, 0x00 }); // bTeoPrologue
		byte[] verifyApdu = new byte[] { 0x00, 0x20, 0x00, 0x01, 0x08, 0x20,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		verifyCommand.write(verifyApdu.length & 0xff); // ulDataLength[0]
		verifyCommand.write(0x00); // ulDataLength[1]
		verifyCommand.write(0x00); // ulDataLength[2]
		verifyCommand.write(0x00); // ulDataLength[3]
		verifyCommand.write(verifyApdu); // abData

		byte[] result = card.transmitControlCommand(verifyPinControl,
				verifyCommand.toByteArray());
		responseApdu = new ResponseAPDU(result);
		LOG.debug("status work: " + Integer.toHexString(responseApdu.getSW()));
		if (0x9000 == responseApdu.getSW()) {
			LOG.debug("status OK");
		} else if (0x6401 == responseApdu.getSW()) {
			LOG.debug("canceled by user");
		} else if (0x6400 == responseApdu.getSW()) {
			LOG.debug("timeout");
		}
	}

	public static final byte FEATURE_VERIFY_PIN_DIRECT_TAG = 0x06;

	private boolean hasFeature(byte featureTag, byte[] features) {
		int idx = 0;
		while (idx < features.length) {
			byte tag = features[idx];
			if (featureTag == tag) {
				return true;
			}
			idx += 1 + 1 + 4;
		}
		return false;
	}

	private Integer findFeature(byte featureTag, byte[] features) {
		int idx = 0;
		while (idx < features.length) {
			byte tag = features[idx];
			idx++;
			idx++;
			if (featureTag == tag) {
				int feature = 0;
				for (int count = 0; count < 3; count++) {
					feature |= features[idx] & 0xff;
					idx++;
					feature <<= 8;
				}
				feature |= features[idx] & 0xff;
				return feature;
			}
			idx += 4;
		}
		return null;
	}

	@Test
	public void testListReaders() throws Exception {
		PcscEid pcscEid = new PcscEid(new TestView(), this.messages);
		LOG.debug("reader list: " + pcscEid.getReaderList());
	}
}
