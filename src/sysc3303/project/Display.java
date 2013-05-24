package sysc3303.project;

import java.net.*;

public class Display {

	public static void showContent(byte[] a) {
		System.out.print("Containing: ");
		// Form a String from the byte array.
		String message = new String(a);
		System.out.println(message);

		// Display content in hex
		message = byteArrayToHexString(a);
		System.out.println("Containing in hex: " + message);
	}

	public static void showPort(DatagramPacket dataPacket, DatagramSocket socket) {
		System.out.println("Packet Address: " + dataPacket.getAddress());
		System.out.println("Destination  port: " + dataPacket.getPort());
		// System.out.println("Length: " + dataPacket.getLength());
		System.out.println("packet sent or receive by this port "
				+ socket.getLocalPort());
	}

	public static String byteArrayToHexString(byte[] b) { // pass a byte, return
															// a string, when
															// you transfer the
															// packet, its
															// easier to display
															// in string form in
															// other words, bits
															// to ascii
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	public static void displayError(byte[] error) {
		System.out.println("Error type is");
		if (error[3] == 0)
			System.out.println("#0 Not defined, see error message (if any).");
		else if (error[3] == 1)
			System.out.println("#1 File not found.");
		else if (error[3] == 2)
			System.out.println("#2 Access violation.");
		else if (error[3] == 3)
			System.out.println("#3 Disk full or allocation exceeded.");
		else if (error[3] == 4)
			System.out.println("#4 Illegal TFTP operation.");
		else if (error[3] == 5)
			System.out.println("#5 Unknown transfer ID.");
		else if (error[3] == 6)
			System.out.println("#6 File already exists.");
		else if (error[3] == 7)
			System.out.println("#7 No such user. ");
		else
			System.out.println("get a wrong error message");
		System.out.println("Message contain:");
		String message = new String(error);
		System.out.println(message);
	}

}