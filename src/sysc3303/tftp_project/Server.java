package sysc3303.tftp_project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * @author korey
 * 
 */
public class Server {
	protected boolean stopping = false;

	protected class RequestListenerThread extends Thread {
		protected DatagramSocket socket;
		protected static final int defaultPort = 6900;
		protected static final int maxPacketSize = 100;

		public RequestListenerThread(InetAddress boundAddress, int boundPort) {
			try {
				socket = new DatagramSocket(boundPort, boundAddress);
			} catch (SocketException e) {
				System.out.printf("Failed to bind to %s:%i%n",
						boundAddress.getHostAddress(), boundPort);
				System.out.println("Terminating");
				System.exit(1);
			}
		}

		@Override
		public void run() {
			while (!stopping) {
				byte[] data = new byte[maxPacketSize];
				DatagramPacket dp = new DatagramPacket(data, data.length);
				try {
					socket.receive(dp);
					Packet packet = Packet.CreateFromBytes(dp.getData(),
							dp.getLength());
					if (packet instanceof RequestPacket) {

					} else {
						// we received an invalid packet, so ignore it
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Constructor
	 */
	public Server() {
	}

	/**
	 * Main program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Server server = new Server();
			server.start(InetAddress.getLocalHost(), defaultPort);
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
					server.stop();
				} else {
					System.out
							.println("Invalid command. These are the available commands:");
					System.out.println("    help: prints this help menu");
					System.out
							.println("    stop: stop the server (when current transfers finish)");
				}
			}

		} catch (UnknownHostException e) {
			System.out.println("Failed to connect");
		}
	}

	public void stop() {
		stopping = true;
	}

	/**
	 * Bind to the server port and IP address
	 */
	public void start(InetAddress address, int port) {
		try {
			receiveSocket = new DatagramSocket(port, address);
			System.out.printf("Bound on %s:%s%n", address.getHostAddress(),
					port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Receive a packet
	 */
	public void receive() {
		// try {
		// byte[] data = new byte[100];
		// DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		// receiveSocket.receive(receivePacket);
		// respond(receivePacket);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Respond to a received packet
	 * 
	 * @param receivedPacket
	 */
	protected void respond(DatagramPacket receivedPacket) {
		// try {
		// } catch (SocketException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Disconnect sockets
	 */
	public void disconnect() {
		receiveSocket.close();
		System.out.println("Disconnected");
	}

	/**
	 * Destructor, disconnects from sockets
	 */
	public void finalize() {
		disconnect();
	}
}
