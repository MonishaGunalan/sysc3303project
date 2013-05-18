package sysc3303.tftp_project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */
public class ErrorSimulator {
	protected InetAddress serverAddress;
	protected int serverRequestPort = 69;
	protected int clientRequestPort = 68;
	protected int threadCount = 0;
	protected boolean stopping = false;
	protected RequestReceiveThread requestReceive;

	/**
	 * Constructor
	 */
	public ErrorSimulator() {
		try {
			serverAddress = InetAddress.getLocalHost();
			requestReceive = new RequestReceiveThread();
			requestReceive.start();
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
			String command = scanner.nextLine().toLowerCase();

			// Continue if blank line was passed
			if (command.length() == 0) {
				continue;
			}

			if (command.equals("help")) {
				System.out.println("Available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the error simulator (when current transfers finish)");
			} else if (command.equals("stop")) {
				System.out
						.println("Stopping simulator (when current transfers finish)");
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

	synchronized public void incrementThreadCount() {
		threadCount++;
	}

	synchronized public void decrementThreadCount() {
		threadCount--;
	}

	synchronized public int getThreadCount() {
		return threadCount;
	}

	public void stop() {
		requestReceive.getSocket().close();
		
		// wait for threads to finish
		while(getThreadCount() > 0) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// Ignore errors
			}
		}
		System.out.println("Error simulator closed.");
		System.exit(0);
	}

	protected class RequestReceiveThread extends Thread {
		protected DatagramSocket socket;

		public RequestReceiveThread() {
			try {
				socket = new DatagramSocket(clientRequestPort);
			} catch (SocketException e) {
				System.out.println("Count not bind to port: " + clientRequestPort);
				System.exit(1);
			}
		}

		public void run() {
			try {
				incrementThreadCount();

				while (!socket.isClosed()) {
					byte[] data = new byte[RequestPacket.maxPacketSize];
					DatagramPacket dp = new DatagramPacket(data, data.length);
					socket.receive(dp);
					new ForwardThread(dp).start();
				}
			} catch (IOException e) {
				// Probably just closing the thread down
			}

			decrementThreadCount();
		}

		public DatagramSocket getSocket() {
			return socket;
		}
	}

	protected class ForwardThread extends Thread {
		protected DatagramSocket socket;
		protected int timeoutMs = 10000; // 10 second receive timeout
		protected DatagramPacket requestPacket;
		protected InetAddress clientAddress;
		protected int clientPort, serverPort;

		ForwardThread(DatagramPacket requestPacket) {
			this.requestPacket = requestPacket;
		}

		public void run() {
			try {
				incrementThreadCount();

				socket = new DatagramSocket();
				socket.setSoTimeout(timeoutMs);
				clientAddress = requestPacket.getAddress();
				clientPort = requestPacket.getPort();

				// Send request to server
				System.out.println("Sending request to server");
				DatagramPacket dp = new DatagramPacket(requestPacket.getData(),
						requestPacket.getLength(), serverAddress,
						serverRequestPort);
				socket.send(dp);

				// Receive from server
				System.out.println("Receiving request from server");
				dp = Packet.createDatagramForReceiving();
				socket.receive(dp);
				serverPort = dp.getPort();

				while (true) {
					// Forward to client
					System.out.println("Forwarding packet to client");
					dp = new DatagramPacket(dp.getData(), dp.getLength(),
							clientAddress, clientPort);
					socket.send(dp);

					// Wait for response from client
					System.out.println("Waiting to get packet from client");
					dp = Packet.createDatagramForReceiving();
					socket.receive(dp);

					// Forward to server
					System.out.println("Forwarding packet to server");
					dp = new DatagramPacket(dp.getData(), dp.getLength(),
							serverAddress, serverPort);
					socket.send(dp);

					// Receive from server
					System.out.println("Waiting to get packet from server");
					dp = Packet.createDatagramForReceiving();
					socket.receive(dp);
				}
			} catch (SocketTimeoutException e) {
				System.out
						.println("Socket timeout: closing thread. (Transfer may have simply finished)");
			} catch (IOException e) {
				System.out.println("Socket error: closing thread.");
			}

			decrementThreadCount();
		}
	}
}
