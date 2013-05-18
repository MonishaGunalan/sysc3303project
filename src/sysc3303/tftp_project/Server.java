package sysc3303.tftp_project;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

/**
 * @author Korey Conway (100838924)
 * @author Monisha
 * @author Arzaan
 */
public class Server {
	protected static final int listenPort = 6900;
	protected String publicFolder = System.getProperty("user.dir")
			+ "/server_files/";
	protected int threadCount = 0;
	protected RequestListenerThread requestListener;

	/**
	 * Constructor
	 */
	public Server() {
		requestListener = new RequestListenerThread(listenPort);
		requestListener.start();
	}

	/**
	 * Main program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server server = new Server();
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
				System.out
						.println("    pwd: prints out the public directory for file transfers");
			} else if (command.equals("stop")) {
				System.out
						.println("Stopping server (when current transfers finish)");
				server.stop();
			} else if (command.equals("pwd")) {
				System.out.println("Current shared directory: "
						+ server.getPublicFolder());
			} else if (command.equals("test")) {
				System.out.println(server.getThreadCount());
			} else {
				System.out
						.println("Invalid command. These are the available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the server (when current transfers finish)");
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
		try {
			requestListener.getSocket().close();
			System.out.println("Stopping... waiting for threads to finish");
			while (getThreadCount() > 0) {
				// Wait for threads to finish
				// TODO: future version could use wait/notify
				Thread.sleep(20);
			}
			System.out.println("Exiting");
			System.exit(0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getPublicFolder() {
		return publicFolder;
	}

	protected class RequestListenerThread extends Thread {
		protected DatagramSocket socket;

		public RequestListenerThread(int boundPort) {
			try {
				socket = new DatagramSocket(boundPort);
			} catch (SocketException e) {
				System.out.printf("Failed to bind to port %d%n", boundPort);
				System.exit(1);
			}
		}

		public DatagramSocket getSocket() {
			return socket;
		}

		@Override
		public void run() {
			try {
				incrementThreadCount();

				while (!socket.isClosed()) {
					byte[] data = new byte[RequestPacket.maxPacketSize];
					DatagramPacket dp = new DatagramPacket(data, data.length);
					socket.receive(dp);
					Packet packet = Packet.CreateFromBytes(dp.getData(),
							dp.getLength());
					if (packet instanceof RequestPacket) {
						TransferThread tt = new TransferThread(
								(RequestPacket) packet, dp.getAddress(),
								dp.getPort());
						tt.start();
					} else {
						// we received an invalid packet, so ignore it
					}
				}
			} catch (IOException e) {
				// Ignore
			}

			socket.disconnect();
			System.out.println("RequestListenerThread has stopped.");
			decrementThreadCount();
		}

	}

	protected class TransferThread extends Thread {
		protected DatagramSocket socket;
		protected String filename;
		protected String filePath;
		protected boolean isReadRequest;
		protected int toPort;
		protected InetAddress toAddress;
		protected static final int maxDataSize = 512;

		public TransferThread(RequestPacket packet, InetAddress toAddress,
				int toPort) {
			try {
				socket = new DatagramSocket();
				this.filename = packet.getFilename();
				this.filePath = publicFolder + filename;
				this.isReadRequest = packet.isReadRequest();
				this.toAddress = toAddress;
				this.toPort = toPort;

				// this.start();
			} catch (SocketException e) {
				System.out.println("Failed to open socket for transfer for "
						+ filename);
			}
		}

		@Override
		public void run() {
			incrementThreadCount();

			if (isReadRequest) {
				this.runReadRequest();
			} else {
				this.runWriteRequest();
			}

			decrementThreadCount();
		}

		public void runReadRequest() {
			try {
				FileInputStream fs = new FileInputStream(filePath);
				byte[] data = new byte[maxDataSize];
				int blockNumber = 1;
				boolean isLastDataPacket = false;
				InetAddress toAddress = this.toAddress;
				Packet pk;

				while (!isLastDataPacket) {
					// Read file in 512 byte chunks
					int bytesRead = fs.read(data);
					isLastDataPacket = (bytesRead < maxDataSize);

					if (bytesRead == -1) {
						// Special case when file size is multiple of 512 bytes
						bytesRead = 0;
						data = new byte[0];
					}

					// Send data packet
					System.out.printf("Sending block %d of %s%n", blockNumber,
							filename);
					DatagramPacket dp = Packet.CreateDataPacket(blockNumber,
							data, bytesRead)
							.generateDatagram(toAddress, toPort);
					socket.send(dp);

					// Wait until we receive correct ack packet
					data = new byte[Packet.maxLength];
					dp = new DatagramPacket(data, data.length);
					do {
						socket.receive(dp);
						pk = Packet.CreateFromBytes(data, dp.getLength());
					} while (pk.getType() != Packet.Type.ACK
							|| ((AckPacket) pk).getBlockNumber() != blockNumber);
					toAddress = dp.getAddress(); // This updates in case the
													// client is using dynamic
													// IP (not likely needed,
													// but good extra feature)
					System.out.printf("Received ack for block %d of %s%n",
							blockNumber, filename);
					blockNumber++;
				}
				fs.close();
			} catch (FileNotFoundException e) {
				System.out.println("File not found: " + filename);
				return;
			} catch (IOException e) {
				System.out.println("IOException with file: " + filename);
				e.printStackTrace();
				return;
			}
		}

		public void runWriteRequest() {
			try {
				FileOutputStream fs;
				fs = new FileOutputStream(filePath);
				int blockNumber = 0;
				boolean isLastDataPacket = false;
				InetAddress toAddress = this.toAddress;
				Packet pk;
				DataPacket dataPk;

				while (true) {
					// Send ack packet
					DatagramPacket dp = Packet.CreateAckPacket(blockNumber)
							.generateDatagram(toAddress, toPort);
					socket.send(dp);
					System.out.printf("Sent ack for block %d of %s%n",
							blockNumber, filename);
					if (isLastDataPacket) {
						break;
					}

					// Increment blockNumber
					blockNumber++;

					// Receive data packet
					do {
						dp = new DatagramPacket(new byte[Packet.maxLength],
								Packet.maxLength);
						socket.receive(dp);
						pk = Packet.CreateFromBytes(dp.getData(),
								dp.getLength());
						System.out.println(((DataPacket) pk).getBlockNumber());
					} while (pk.getType() != Packet.Type.DATA
							|| ((DataPacket) pk).getBlockNumber() != blockNumber);

					System.out.printf("Received block %d of %s%n", blockNumber,
							filename);

					// Save into file
					dataPk = (DataPacket) pk;
					fs.write(dataPk.getData());

					if (dataPk.getDataLength() != maxDataSize) {
						isLastDataPacket = true;
					}
				}

				fs.close();
			} catch (FileNotFoundException e) {
				System.out.println("Cannot write to: " + filename);
				return;
			} catch (IOException e) {
				System.out.println("IOException with file: " + filename);
				e.printStackTrace();
				return;
			}
		}
	}
}