package sysc3303.tftp_project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * @author Korey Conway (100838924)
 * @author Monisha
 * @author Arzaan
 */
public class ErrorSimulator {
	protected InetAddress serverAddress;
	protected boolean stopping = false;

	/**
	 * Constructor
	 */
	public ErrorSimulator() {
		try {
			serverAddress = InetAddress.getLocalHost();
			new RequestReceiveThread().start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ErrorSimulator errorSimulator = new ErrorSimulator();
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.print("Command: ");
			String command = scanner.nextLine();

			// Continue if blank line was passed
			if (command.length() == 0) {
				continue;
			}

			if (command.equals("help")) {
				System.out.println("Available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the server (when current transfers finish)");
			} else if (command.equals("stop")) {
				System.out
						.println("Stopping server (when current transfers finish)");
				errorSimulator.stop();
			} else {
				System.out
						.println("Invalid command. These are the available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the error simulator (when current transfers finish)");
			}
		}
	}

	public void stop() {
		stopping = true;
	}

	protected class RequestReceiveThread extends Thread {
		protected int listenPort = 6800;
		protected DatagramSocket socket;
		protected static final int maxPacketSize = 100;

		public RequestReceiveThread() {
			try {
				socket = new DatagramSocket(listenPort);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				while (!stopping) {
					byte[] data = new byte[maxPacketSize];
					DatagramPacket dp = new DatagramPacket(data, data.length);
					socket.receive(dp);
					new ForwardThread(dp).start();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected class ForwardThread extends Thread {
		protected DatagramSocket serverSocket, clientSocket;
		protected int timeoutMs = 10000;
		protected DatagramPacket requestPacket;
		protected int serverPort = 6900;

		ForwardThread(DatagramPacket requestPacket) {
			try {
				this.requestPacket = requestPacket;
				clientSocket = new DatagramSocket(requestPacket.getPort());
				clientSocket.setSoTimeout(timeoutMs);
				serverSocket = new DatagramSocket();
				serverSocket.setSoTimeout(timeoutMs);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void run() {
			// send to server
			DatagramPacket dp = new DatagramPacket(requestPacket.getData(), requestPacket.getLength(), serverAddress, serverPort);
			
			// start loop
			
			// wait for response from server
			
			// send response to client
			
			// wait for response from client
			
			// loop
		}
	}
}
