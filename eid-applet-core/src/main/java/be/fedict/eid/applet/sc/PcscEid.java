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

package be.fedict.eid.applet.sc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import be.fedict.eid.applet.Dialogs;
import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.View;

/**
 * Holds all function related to eID card access over PC/SC.
 * 
 * This class required the Java 6 runtime to operate.
 * 
 * @author fcorneli
 * 
 */
public class PcscEid extends Observable implements PcscEidSpi {

	private final static byte[] ATR_PATTERN = new byte[] { 0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10 };

	private final static byte[] ATR_MASK = new byte[] { (byte) 0xff,
			(byte) 0xff, 0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0 };

	public static final byte[] IDENTITY_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x31 };

	public static final byte[] IDENTITY_SIGN_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x32 };

	public static final byte[] ADDRESS_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x33 };

	public static final byte[] ADDRESS_SIGN_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x34 };

	public static final byte[] PHOTO_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x01, 0x40, 0x35 };

	public static final byte[] AUTHN_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x38 };

	public static final byte[] SIGN_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x39 };

	public static final byte[] CA_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x3A };

	public static final byte[] ROOT_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x3B };

	public static final byte[] RRN_CERT_FILE_ID = new byte[] { 0x3F, 0x00,
			(byte) 0xDF, 0x00, 0x50, 0x3C };

	public static final byte[] BELPIC_AID = new byte[] { (byte) 0xA0, 0x00,
			0x00, 0x01, 0x77, 0x50, 0x4B, 0x43, 0x53, 0x2D, 0x31, 0x35 };

	public static final byte[] APPLET_AID = new byte[] { (byte) 0xA0, 0x00,
			0x00, 0x00, 0x30, 0x29, 0x05, 0x70, 0x00, (byte) 0xAD, 0x13, 0x10,
			0x01, 0x01, (byte) 0xFF };

	private final View view;

	private final CardTerminals cardTerminals;

	private final Dialogs dialogs;

	public PcscEid(View view, Messages messages) {
		this.view = view;
		TerminalFactory factory = TerminalFactory.getDefault();
		this.cardTerminals = factory.terminals();
		this.dialogs = new Dialogs(this.view, messages);
	}

	public List<String> getReaderList() {
		List<String> readerList = new LinkedList<String>();
		TerminalFactory factory = TerminalFactory.getDefault();
		CardTerminals cardTerminals = factory.terminals();
		List<CardTerminal> cardTerminalList;
		try {
			cardTerminalList = cardTerminals.list();
		} catch (CardException e) {
			/*
			 * Windows can give use a sun.security.smartcardio.PCSCException
			 * SCARD_E_NO_READERS_AVAILABLE here in case no card readers are
			 * connected to the system.
			 */
			this.view.addDetailMessage("error on card terminals list: "
					+ e.getMessage());
			this.view.addDetailMessage("no card readers connected?");
			Throwable cause = e.getCause();
			if (null != cause) {
				this.view.addDetailMessage("cause: " + cause.getMessage());
				this.view.addDetailMessage("cause type: "
						+ cause.getClass().getName());
			}
			return readerList;
		}
		for (CardTerminal cardTerminal : cardTerminalList) {
			readerList.add(cardTerminal.getName());
		}
		return readerList;
	}

	public byte[] readFile(byte[] fileId) throws CardException, IOException {
		selectFile(fileId);
		byte[] data = readBinary();
		return data;
	}

	public void close() {
		try {
			this.card.endExclusive();
			this.card.disconnect(true);
		} catch (CardException e) {
			/*
			 * No need to propagate this further since we already have what we
			 * came for.
			 */
			this.view.addDetailMessage("error disconnecting card: "
					+ e.getMessage());
		}
	}

	private byte[] readBinary() throws CardException, IOException {
		int offset = 0;
		this.view.addDetailMessage("read binary");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data;
		do {
			CommandAPDU readBinaryApdu = new CommandAPDU(0x00, 0xB0,
					offset >> 8, offset & 0xFF, 0xFF);
			ResponseAPDU responseApdu = transmit(readBinaryApdu);
			if (0x9000 != responseApdu.getSW()) {
				throw new IOException("APDU response error: "
						+ responseApdu.getSW());
			}
			/*
			 * Notify our progress observers.
			 */
			setChanged();
			notifyObservers();

			/*
			 * Introduce some delay for old Belpic V1 eID cards.
			 */
			// try {
			// Thread.sleep(50);
			// } catch (InterruptedException e) {
			// throw new RuntimeException("sleep error: " + e.getMessage(), e);
			// }
			data = responseApdu.getData();
			baos.write(data);
			offset += data.length;
		} while (0xFF == data.length);
		return baos.toByteArray();
	}

	private Card card;

	public Card getCard() {
		return this.card;
	}

	private CardChannel cardChannel;

	private CardTerminal cardTerminal;

	public CardChannel getCardChannel() {
		return this.cardChannel;
	}

	public boolean isEidPresent() throws CardException {
		List<CardTerminal> cardTerminalList;
		try {
			cardTerminalList = this.cardTerminals.list();
		} catch (CardException e) {
			this.view.addDetailMessage("card terminals list error: "
					+ e.getMessage());
			this.view.addDetailMessage("no card readers connected?");
			Throwable cause = e.getCause();
			if (null != cause) {
				/*
				 * Windows can give us a sun.security.smartcardio.PCSCException
				 * SCARD_E_NO_READERS_AVAILABLE when no card readers are
				 * connected to the system.
				 */
				this.view.addDetailMessage("cause: " + cause.getMessage());
				this.view.addDetailMessage("cause type: "
						+ cause.getClass().getName());
				if ("SCARD_E_NO_READERS_AVAILABLE".equals(cause.getMessage())) {
					/*
					 * Windows platform.
					 */
					this.view.addDetailMessage("no reader available");
				}
			}
			return false;
		}
		for (CardTerminal cardTerminal : cardTerminalList) {
			this.view.addDetailMessage("Scanning card terminal: "
					+ cardTerminal.getName());
			if (cardTerminal.isCardPresent()) {
				Card card;
				try {
					/*
					 * eToken is not using T=0 apparently, hence the need for an
					 * explicit CardException catch
					 */
					card = cardTerminal.connect("T=0");
					/*
					 * The exclusive card lock in combination with reset at
					 * disconnect and some sleeps seems to fix the
					 * SCARD_E_SHARING_VIOLATION issue.
					 */
					card.beginExclusive();
				} catch (CardException e) {
					this.view.addDetailMessage("could not connect to card: "
							+ e.getMessage());
					continue;
				}
				ATR atr = card.getATR();
				if (matchesEidAtr(atr)) {
					this.view
							.addDetailMessage("eID card detected in card terminal : "
									+ cardTerminal.getName());
					this.cardTerminal = cardTerminal;
					this.card = card;
					this.cardChannel = card.getBasicChannel();
					return true;
				} else {
					byte[] atrBytes = atr.getBytes();
					StringBuffer atrStringBuffer = new StringBuffer();
					for (byte atrByte : atrBytes) {
						atrStringBuffer.append(Integer
								.toHexString(atrByte & 0xff));
					}
					this.view
							.addDetailMessage("not a supported eID card. ATR= "
									+ atrStringBuffer);
				}
				card.endExclusive(); // SCARD_E_SHARING_VIOLATION fix
				card.disconnect(true);
			}
		}
		return false;
	}

	private void selectFile(byte[] fileId) throws CardException,
			FileNotFoundException {
		this.view.addDetailMessage("selecting file");
		CommandAPDU selectFileApdu = new CommandAPDU(0x00, 0xA4, 0x08, 0x0C,
				fileId);
		ResponseAPDU responseApdu = transmit(selectFileApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new FileNotFoundException(
					"wrong status word after selecting file: "
							+ Integer.toHexString(responseApdu.getSW()));
		}
		try {
			// SCARD_E_SHARING_VIOLATION fix
			Thread.sleep(20);
		} catch (InterruptedException e) {
			throw new RuntimeException("sleep error: " + e.getMessage());
		}
	}

	private boolean matchesEidAtr(ATR atr) {
		byte[] atrBytes = atr.getBytes();
		if (atrBytes.length != ATR_PATTERN.length) {
			return false;
		}
		for (int idx = 0; idx < atrBytes.length; idx++) {
			atrBytes[idx] &= ATR_MASK[idx];
		}
		if (Arrays.equals(atrBytes, ATR_PATTERN)) {
			return true;
		}
		return false;
	}

	public void waitForEidPresent() throws CardException, InterruptedException {
		while (true) {
			try {
				this.cardTerminals.waitForChange();
			} catch (CardException e) {
				this.view.addDetailMessage("card error: " + e.getMessage());
				Throwable cause = e.getCause();
				if (null != cause) {
					if ("SCARD_E_NO_READERS_AVAILABLE".equals(cause
							.getMessage())) {
						/*
						 * sun.security.smartcardio.PCSCException
						 * 
						 * Windows platform.
						 */
						this.view.addDetailMessage("no readers available.");
					}
				}
				this.view.addDetailMessage("sleeping...");
				Thread.sleep(1000);
			} catch (IllegalStateException e) {
				this.view.addDetailMessage("no terminals at all. sleeping...");
				this.view
						.addDetailMessage("Maybe you should connect a smart card reader?");
				if (System.getProperty("os.name").startsWith("Linux")) {
					this.view
							.addDetailMessage("Maybe the pcscd service is not running?");
				}
				Thread.sleep(1000);
			}
			Thread.sleep(50); // SCARD_E_SHARING_VIOLATION fix
			if (isEidPresent()) {
				return;
			}
		}
	}

	public void removeCard() throws CardException {
		this.cardTerminal.waitForCardAbsent(0);
	}

	public List<X509Certificate> getAuthnCertificateChain()
			throws CardException, IOException, CertificateException {
		List<X509Certificate> authnCertificateChain = new LinkedList<X509Certificate>();
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");

		this.view.addDetailMessage("reading authn certificate...");
		byte[] authnCertFile = readFile(AUTHN_CERT_FILE_ID);
		X509Certificate authnCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(authnCertFile));
		authnCertificateChain.add(authnCert);

		this.view.addDetailMessage("reading Citizen CA certificate...");
		byte[] citizenCaCertFile = readFile(CA_CERT_FILE_ID);
		X509Certificate citizenCaCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(citizenCaCertFile));
		authnCertificateChain.add(citizenCaCert);

		this.view.addDetailMessage("reading Root CA certificate...");
		byte[] rootCaCertFile = readFile(ROOT_CERT_FILE_ID);
		X509Certificate rootCaCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rootCaCertFile));
		authnCertificateChain.add(rootCaCert);

		return authnCertificateChain;
	}

	private byte[] sign(byte[] digestValue, String digestAlgo, byte keyId)
			throws CardException, IOException {
		// select the key
		this.view.addDetailMessage("selecting key...");
		CommandAPDU setApdu = new CommandAPDU(0x00, 0x22, 0x41, 0xB6,
				new byte[] { 0x04, // length of following data
						(byte) 0x80, // algo ref
						0x01, // rsa pkcs#1
						(byte) 0x84, // tag for private key ref
						keyId });
		ResponseAPDU responseApdu = transmit(setApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new RuntimeException("SELECT error");
		}

		ByteArrayOutputStream digestInfo = new ByteArrayOutputStream();
		if ("SHA-1".equals(digestAlgo) || "SHA1".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.SHA1_DIGEST_INFO_PREFIX);
		} else if ("SHA-224".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.SHA224_DIGEST_INFO_PREFIX);
		} else if ("SHA-256".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.SHA256_DIGEST_INFO_PREFIX);
		} else if ("SHA-384".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.SHA384_DIGEST_INFO_PREFIX);
		} else if ("SHA-512".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.SHA512_DIGEST_INFO_PREFIX);
		} else if ("RIPEMD160".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.RIPEMD160_DIGEST_INFO_PREFIX);
		} else if ("RIPEMD128".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.RIPEMD128_DIGEST_INFO_PREFIX);
		} else if ("RIPEMD256".equals(digestAlgo)) {
			digestInfo.write(Pkcs11Eid.RIPEMD256_DIGEST_INFO_PREFIX);
		} else {
			throw new RuntimeException("digest also not supported: "
					+ digestAlgo);
		}
		digestInfo.write(digestValue);
		CommandAPDU computeDigitalSignatureApdu = new CommandAPDU(0x00, 0x2A,
				0x9E, 0x9A, digestInfo.toByteArray());

		this.view.addDetailMessage("computing digital signature...");
		responseApdu = transmit(computeDigitalSignatureApdu);
		if (0x9000 == responseApdu.getSW()) {
			/*
			 * OK, we could use the card PIN caching feature.
			 * 
			 * Notice that the card PIN caching also works when first doing an
			 * authentication after a non-repudiation signature.
			 */
			byte[] signatureValue = responseApdu.getData();
			return signatureValue;
		}
		if (0x6982 != responseApdu.getSW()) {
			this.view.addDetailMessage("SW: "
					+ Integer.toHexString(responseApdu.getSW()));
			throw new RuntimeException("compute digital signature error");
		}
		/*
		 * 0x6982 = Security status not satisfied, so we do a PIN verification
		 * before retrying.
		 */
		this.view.addDetailMessage("PIN verification required...");

		int retriesLeft = -1;
		do {
			char[] pin = this.dialogs.getPin(retriesLeft);
			if (4 != pin.length) {
				throw new RuntimeException("PIN length should be 4 digits");
			}
			byte[] verifyData = new byte[] { (byte) (0x20 | pin.length),
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
			for (int idx = 0; idx < pin.length; idx += 2) {
				char digit1 = pin[idx];
				char digit2 = pin[idx + 1];
				byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
				verifyData[idx / 2 + 1] = value;
			}
			Arrays.fill(pin, (char) 0); // minimize exposure

			this.view.addDetailMessage("verifying PIN...");
			CommandAPDU verifyApdu = new CommandAPDU(0x00, 0x20, 0x00, 0x01,
					verifyData);
			try {
				responseApdu = transmit(verifyApdu);
			} finally {
				Arrays.fill(verifyData, (byte) 0); // minimize exposure
			}
			if (0x9000 != responseApdu.getSW()) {
				this.view.addDetailMessage("VERIFY_PIN error");
				this.view.addDetailMessage("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					this.dialogs.showPinBlockedDialog();
					throw new RuntimeException("eID card blocked!");
				}
				if (0x63 != responseApdu.getSW1()) {
					this.view.addDetailMessage("PIN verification error.");
					throw new RuntimeException("PIN verification error.");
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				this.view.addDetailMessage("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());

		this.view.addDetailMessage("computing digital signature...");
		responseApdu = cardChannel.transmit(computeDigitalSignatureApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new RuntimeException("compute digital signature error");
		}

		byte[] signatureValue = responseApdu.getData();
		return signatureValue;
	}

	public byte[] signAuthn(byte[] toBeSigned) throws NoSuchAlgorithmException,
			CardException, IOException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		byte[] digest = messageDigest.digest(toBeSigned);
		byte keyId = (byte) 0x82; // authentication key

		byte[] signatureValue = sign(digest, "SHA-1", keyId);
		return signatureValue;
	}

	public void changePin() throws Exception {
		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			char[] oldPin = new char[4];
			char[] newPin = new char[4];
			this.dialogs.getPins(retriesLeft, oldPin, newPin);

			byte[] changePinData = new byte[] { 0x20 | 0x04, (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, 0x20 | 0x04, (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF };

			for (int idx = 0; idx < oldPin.length; idx += 2) {
				char digit1 = oldPin[idx];
				char digit2 = oldPin[idx + 1];
				byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
				changePinData[idx / 2 + 1] = value;
			}
			Arrays.fill(oldPin, (char) 0); // minimize exposure

			for (int idx = 0; idx < newPin.length; idx += 2) {
				char digit1 = newPin[idx];
				char digit2 = newPin[idx + 1];
				byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
				changePinData[(idx / 2 + 1) + 8] = value;
			}
			Arrays.fill(newPin, (char) 0); // minimize exposure

			CommandAPDU changePinApdu = new CommandAPDU(0x00, 0x24, // change
					// reference
					// data
					0x00, // user password change
					0x01, changePinData);
			try {
				responseApdu = transmit(changePinApdu);
			} finally {
				Arrays.fill(changePinData, (byte) 0);
			}
			if (0x9000 != responseApdu.getSW()) {
				this.view.addDetailMessage("CHANGE PIN error");
				this.view.addDetailMessage("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					this.dialogs.showPinBlockedDialog();
					throw new RuntimeException("eID card blocked!");
				}
				if (0x63 != responseApdu.getSW1()) {
					this.view
							.addDetailMessage("PIN change error. Card blocked?");
					throw new RuntimeException("PIN change error.");
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				this.view.addDetailMessage("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
		this.dialogs.showPinChanged();
	}

	public void unblockPin() throws Exception {
		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			char[] puk1 = new char[6];
			char[] puk2 = new char[6];
			this.dialogs.getPuks(retriesLeft, puk1, puk2);

			char[] fullPuk = new char[12];
			System.arraycopy(puk2, 0, fullPuk, 0, 6);
			Arrays.fill(puk2, (char) 0);
			System.arraycopy(puk1, 0, fullPuk, 6, 6);
			Arrays.fill(puk1, (char) 0);

			byte[] changePinData = new byte[] { 0x20 | (6 + 6), (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF };

			for (int idx = 0; idx < fullPuk.length; idx += 2) {
				char digit1 = fullPuk[idx];
				char digit2 = fullPuk[idx + 1];
				byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
				changePinData[idx / 2 + 1] = value;
			}
			Arrays.fill(fullPuk, (char) 0); // minimize exposure

			CommandAPDU changePinApdu = new CommandAPDU(0x00, 0x2C, 0x00, 0x01,
					changePinData);
			try {
				responseApdu = transmit(changePinApdu);
			} finally {
				Arrays.fill(changePinData, (byte) 0);
			}
			if (0x9000 != responseApdu.getSW()) {
				this.view.addDetailMessage("PIN unblock error");
				this.view.addDetailMessage("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					this.dialogs.showPinBlockedDialog();
					throw new RuntimeException("eID card blocked!");
				}
				if (0x63 != responseApdu.getSW1()) {
					this.view.addDetailMessage("PIN unblock error.");
					throw new RuntimeException("PIN unblock error.");
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				this.view.addDetailMessage("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
		this.dialogs.showPinUnblocked();
	}

	public List<X509Certificate> getSignCertificateChain()
			throws CardException, IOException, CertificateException {
		List<X509Certificate> signCertificateChain = new LinkedList<X509Certificate>();
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");

		this.view.addDetailMessage("reading sign certificate...");
		byte[] signCertFile = readFile(SIGN_CERT_FILE_ID);
		X509Certificate signCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(signCertFile));
		signCertificateChain.add(signCert);

		this.view.addDetailMessage("reading Citizen CA certificate...");
		byte[] citizenCaCertFile = readFile(CA_CERT_FILE_ID);
		X509Certificate citizenCaCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(citizenCaCertFile));
		signCertificateChain.add(citizenCaCert);

		this.view.addDetailMessage("reading Root CA certificate...");
		byte[] rootCaCertFile = readFile(ROOT_CERT_FILE_ID);
		X509Certificate rootCaCert = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(rootCaCertFile));
		signCertificateChain.add(rootCaCert);

		return signCertificateChain;
	}

	public byte[] sign(byte[] digestValue, String digestAlgo)
			throws NoSuchAlgorithmException, CardException, IOException {
		byte keyId = (byte) 0x83; // non-repudiation key
		byte[] signatureValue = sign(digestValue, digestAlgo, keyId);
		return signatureValue;
	}

	public void logoff() throws Exception {
		CommandAPDU logoffApdu = new CommandAPDU(0x80, 0xE6, 0x00, 0x00);
		this.view.addDetailMessage("logoff...");
		ResponseAPDU responseApdu = transmit(logoffApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new RuntimeException("logoff failed");
		}
	}

	public void logoff(String readerName) throws Exception {
		this.view
				.addDetailMessage("logoff from reader: \"" + readerName + "\"");
		TerminalFactory factory = TerminalFactory.getDefault();
		CardTerminals cardTerminals = factory.terminals();
		CardTerminal cardTerminal = cardTerminals.getTerminal(readerName);
		if (null == cardTerminal) {
			this.view.addDetailMessage("logoff: card reader not found: "
					+ readerName);
			List<String> readerList = getReaderList();
			this.view.addDetailMessage("reader list: " + readerList);
			throw new RuntimeException("card reader not found: " + readerName);
		}
		Card card = cardTerminal.connect("T=0");
		try {
			CardChannel cardChannel = card.getBasicChannel();
			CommandAPDU logoffApdu = new CommandAPDU(0x80, 0xE6, 0x00, 0x00);
			ResponseAPDU responseApdu = cardChannel.transmit(logoffApdu);
			this.view.addDetailMessage("logoff...");
			if (0x9000 != responseApdu.getSW()) {
				throw new RuntimeException("logoff failed");
			}
		} finally {
			card.disconnect(true);
		}
	}

	private ResponseAPDU transmit(CommandAPDU commandApdu) throws CardException {
		ResponseAPDU responseApdu = this.cardChannel.transmit(commandApdu);
		if (0x6c == responseApdu.getSW1()) {
			/*
			 * A minimum delay of 10 msec between the answer ‘6C xx’ and the
			 * next APDU is mandatory for eID v1.0 and v1.1 cards.
			 */
			this.view.addDetailMessage("sleeping...");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException("cannot sleep");
			}
			responseApdu = this.cardChannel.transmit(commandApdu);
		}
		return responseApdu;
	}

	public void selectBelpicJavaCardApplet() {
		CommandAPDU selectApplicationApdu = new CommandAPDU(0x00, 0xA4, 0x04,
				0x0C, BELPIC_AID);
		ResponseAPDU responseApdu;
		try {
			responseApdu = transmit(selectApplicationApdu);
		} catch (CardException e) {
			this.view.addDetailMessage("error selecting BELPIC");
			return;
		}
		if (0x9000 != responseApdu.getSW()) {
			this.view.addDetailMessage("could not select BELPIC");
			this.view.addDetailMessage("status word: "
					+ Integer.toHexString(responseApdu.getSW()));
			/*
			 * Try to select the Applet.
			 */
			selectApplicationApdu = new CommandAPDU(0x00, 0xA4, 0x04, 0x00,
					APPLET_AID);
			try {
				responseApdu = transmit(selectApplicationApdu);
			} catch (CardException e) {
				this.view.addDetailMessage("error selecting Applet");
				return;
			}
			if (0x9000 != responseApdu.getSW()) {
				this.view.addDetailMessage("could not select applet");
			} else {
				this.view.addDetailMessage("BELPIC JavaCard applet selected");
			}
		} else {
			this.view.addDetailMessage("BELPIC JavaCard applet selected");
		}
	}
}
