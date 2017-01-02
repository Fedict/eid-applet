package test.be.fedict.eid.applet.cli;

import javax.smartcardio.CardException;

import be.fedict.eid.applet.sc.PcscEid;

public class PcssEidSignCliTest {

	public static void main(String[] args) {
		new PcssEidSignCliTest().testSign();
		System.exit(0);
	}

	private PcssEidSignCliTest() {
	}

	private void testSign() {
		try {
			PcscEid pcscEid = createPcscEidAndWaitForCard();

			byte[] signature = pcscEid.sign("hello world".getBytes(), "SHA-256");
			System.out.println("Signature length: " + signature.length);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private PcscEid createPcscEidAndWaitForCard() throws CardException, InterruptedException {
		TestView testView = new TestView();
		PcscEid pcscEid = new PcscEid(testView, testView.getMessages());

		if (!pcscEid.isEidPresent()) {
			System.out.println("Insert eID card");
			pcscEid.waitForEidPresent();
		}

		return pcscEid;
	}

}
