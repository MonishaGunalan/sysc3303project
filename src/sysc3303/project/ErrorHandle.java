package sysc3303.project;

import java.io.*;
import java.net.*;

import sysc3303.project.packets.TftpPacket;

//import java.net. * ;
public class ErrorHandle {
	public static enum Request {
		READ, WRITE, ERROR
	};

    /*public static void accessViolation(String message, InetAddress address, int sendPort, DatagramSocket threadSocket) {

	}

    public static void illegalTFTPoperation(String message, InetAddress address, int sendPort, DatagramSocket threadSocket) {

	}

	public static void unknownPort(DatagramPacket packet) {

	}

	public static boolean isFileExit(String fileName) {
		return true;
	}

	public static String getFileName(byte[] request, int len) {
		return "test";
	}
*/
	public static String getMode(byte request[], int len) {
		String mode = null;
		int j, k;
		for (j = 2; j < len; j++) {
			if (request[j] == 0)
				break;
		}
		// search for next all 0 byte
		for (k = j + 1; j < len; k++) {
			if (request[k] == 0)
				break;
		}
		mode = new String(request, j + 1, k - j - 1);
		// System.out.println("mode is " + mode);
		return mode;

	}

/*	public static String checkRequest(byte[] request, int len) {

	}

	public static boolean checkACK(byte[] ACK, int blockNumber) {

	}

	public static boolean checkDATA(byte[] DATA) {

	}

	public static boolean checkBlockNumber(byte[] data, int blockNumber) {

	}

	public static boolean checkPort(DatagramPacket packet, InetAddress address, int port) {
		
	}

	public static boolean checkError(byte[] data) {
		
	}*/


}