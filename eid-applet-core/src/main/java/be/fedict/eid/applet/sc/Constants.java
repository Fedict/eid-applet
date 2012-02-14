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

package be.fedict.eid.applet.sc;

/**
 * Constants shared between PKCS#11 and PC/SC. Needed to isolate these since
 * SunPKCS11 is not always available. Windows 64 bit has no sunpkcs11.jar yet
 * (JRE 1.6.0_20).
 * 
 * @author Frank Cornelis.
 * 
 */
public class Constants {

	private Constants() {
		super();
	}

	public static final byte[] SHA1_DIGEST_INFO_PREFIX = new byte[] { 0x30,
			0x1f, 0x30, 0x07, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x04,
			0x14 };

	public static final byte[] SHA224_DIGEST_INFO_PREFIX = new byte[] { 0x30,
			0x2b, 0x30, 0x0b, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65,
			0x03, 0x04, 0x02, 0x04, 0x04, 0x1c };

	public static final byte[] SHA256_DIGEST_INFO_PREFIX = new byte[] { 0x30,
			0x2f, 0x30, 0x0b, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65,
			0x03, 0x04, 0x02, 0x01, 0x04, 0x20 };

	public static final byte[] SHA384_DIGEST_INFO_PREFIX = new byte[] { 0x30,
			0x3f, 0x30, 0x0b, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65,
			0x03, 0x04, 0x02, 0x02, 0x04, 0x30 };

	public static final byte[] SHA512_DIGEST_INFO_PREFIX = new byte[] { 0x30,
			0x4f, 0x30, 0x0b, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65,
			0x03, 0x04, 0x02, 0x03, 0x04, 0x40 };

	public static final byte[] RIPEMD160_DIGEST_INFO_PREFIX = new byte[] {
			0x30, 0x1f, 0x30, 0x07, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x01,
			0x04, 0x14 };

	public static final byte[] RIPEMD128_DIGEST_INFO_PREFIX = new byte[] {
			0x30, 0x1b, 0x30, 0x07, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x02,
			0x04, 0x10 };

	public static final byte[] RIPEMD256_DIGEST_INFO_PREFIX = new byte[] {
			0x30, 0x2b, 0x30, 0x07, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x03,
			0x04, 0x20 };

	public static String PLAIN_TEXT_DIGEST_ALGO_OID = "2.16.56.1.2.1.3.1";

	/**
	 * Second 0xff (offset 14) is the size of the message.
	 * 
	 * First 0xff (offset 1) is the size of the message + 13
	 */
	public static final byte[] PLAIN_TEXT_DIGEST_INFO_PREFIX = new byte[] {
			0x30, (byte) 0xff, 0x30, 0x09, 0x06, 0x07, 0x60, 0x38, 0x01, 0x02,
			0x01, 0x03, 0x01, 0x04, (byte) 0xff };
}
