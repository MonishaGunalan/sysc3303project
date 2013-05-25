package sysc3303.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

import sysc3303.project.packets.TftpAckPacket;
import sysc3303.project.packets.TftpDataPacket;
import sysc3303.project.packets.TftpErrorPacket;
import sysc3303.project.packets.TftpErrorPacket.ErrorType;
import sysc3303.project.packets.TftpPacket;
import sysc3303.project.packets.TftpRequestPacket;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */
public class TftpServer {
	// Port on which to listen for requests (6900 for dev, 69 for submission)
	private static final int LISTEN_PORT = 69;

	// Folder where files are read/written
	private String publicFolder = System.getProperty("user.dir")
			+ "/server_files/";

	// Current number of threads (used to know when we have stopped)
	private int threadCount = 0;

	// Request listener thread. Need reference to stop receiving when stopping.
	private RequestListenerThread requestListener;

	/**
	 * Constructor
	 */
	private TftpServer() {
		requestListener = new RequestListenerThread(LISTEN_PORT);
		requestListener.start();
	}

	/**
	 * Main program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TftpServer server = new TftpServer();
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
			} else {
				System.out
						.println("Invalid command. These are the available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the server (when current transfers finish)");
				System.out
						.println("    pwd: prints out the public directory for file transfers");
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
		requestListener.getSocket().close();
		System.out.println("Stopping... waiting for threads to finish");
		while (getThreadCount() > 0) {
			// Wait for threads to finish
			// TODO: future version could use wait/notify
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// Ignore errors
			}
		}
		System.out.println("Exiting");
		System.exit(0);
	}

	public String getPublicFolder() {
		return publicFolder;
	}

	private class RequestListenerThread extends Thread {
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
					DatagramPacket dp = TftpPacket.createDatagramForReceiving();
					socket.receive(dp);
					try {
						TftpPacket packet = TftpPacket.createFromDatagram(dp);
						if (packet instanceof TftpRequestPacket) {
							TransferThread tt = new TransferThread(
									(TftpRequestPacket) packet,
									dp.getAddress(), dp.getPort());
							tt.start();
						} else {
							// We received a valid packet but not a request
							// Ignore error packets, otherwise send error
							if (!(packet instanceof TftpErrorPacket)) {
								// Protocol ambiguity: could send either
								// an illegal op or unkown TID
								// The following implementation opted for
								// sending an illegal op
								DatagramSocket errorSocket = new DatagramSocket();
								TftpErrorPacket errorPacket = TftpPacket
										.createErrorPacket(
												ErrorType.ILLEGAL_OPERATION,
												"Received the wrong kind of packet on request listener.");
								dp = errorPacket.generateDatagram(
										dp.getAddress(), dp.getPort());
								errorSocket.send(dp);
							}
						}
					} catch (IllegalArgumentException e) {
						// We got an invalid packet
						// Open new socket and send error packet response
						DatagramSocket errorSocket = new DatagramSocket();
						System.out.println("\nServer received invalid request packet");
						TftpErrorPacket errorPacket = TftpPacket
								.createErrorPacket(ErrorType.ILLEGAL_OPERATION,
										e.getMessage());
						dp = errorPacket.generateDatagram(dp.getAddress(),
								dp.getPort());
						errorSocket.send(dp);
					}
				}
			} catch (IOException e) {
				// Ignore, we are likely just stopping
			}

			socket.disconnect();
			System.out.println("RequestListenerThread has stopped.");
			decrementThreadCount();
		}
	}

	private class TransferThread extends Thread {
		private DatagramSocket socket;
		private String filename;
		private String filePath;
		private boolean isReadRequest;
		private int toPort;
		private InetAddress toAddress;

		public TransferThread(TftpRequestPacket packet, InetAddress toAddress,
				int toPort) {
			try {
				socket = new DatagramSocket();
				this.filename = packet.getFilename();
				this.filePath = publicFolder + filename;
				this.isReadRequest = packet.isReadRequest();
				this.toAddress = toAddress;
				this.toPort = toPort;
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
				int blockNumber = 1;
				boolean isLastDataPacket = false;
				InetAddress toAddress = this.toAddress;
				TftpPacket pk;
				int maxDataSize = TftpDataPacket.getMaxFileDataLength();

				FileInputStream fs = new FileInputStream(filePath);

				while (!isLastDataPacket) {
					// Read file in 512 byte chunks
					byte[] data = new byte[maxDataSize];
					int bytesRead = fs.read(data);

					if (bytesRead == -1) {
						// Special case when file size is multiple of 512 bytes
						bytesRead = 0;
						data = new byte[0];
					}

					// Create the packet and get the flag to know if it is the
					// last
					TftpDataPacket dataPacket = TftpPacket.createDataPacket(
							blockNumber, data, bytesRead);
					isLastDataPacket = dataPacket.isLastDataPacket();
					DatagramPacket dp = dataPacket.generateDatagram(toAddress,
							toPort);

					// Send data packet
					System.out.printf("Sending block %d of %s%n", blockNumber,
							filename);
					socket.send(dp);

					// Wait until we receive ack packet or error
					dp = TftpPacket.createDatagramForReceiving();
					do {
						socket.receive(dp);
						pk = TftpPacket.createFromDatagram(dp);

						// Check for error packet
						if (dp.getPort() != toPort) {
							// Check for correct TID (sender port)
							TftpErrorPacket errorPacket = TftpPacket
									.createErrorPacket(ErrorType.UNKOWN_TID,
											"You used an unkown TID");
							socket.send(errorPacket.generateDatagram(toAddress,
									dp.getPort()));
							continue;
						} else if (pk instanceof TftpErrorPacket) {
							TftpErrorPacket errorPk = (TftpErrorPacket) pk;
							System.out
									.printf("Received error of type '%s' with message '%s'%n",
											errorPk.getErrorType().toString(),
											errorPk.getErrorMessage());
							if (errorPk.shouldAbortTransfer()) {
								// Close file and abort transfer
								fs.close();
								System.out
										.printf("Aborting transfer of '%s'%n",
												filename);
								return;
							}
						}
					} while (!(pk instanceof TftpAckPacket)
							|| ((TftpAckPacket) pk).getBlockNumber() != blockNumber);
					System.out.printf("Received ack for block %d of %s%n",
							blockNumber, filename);
					blockNumber++;
				}

				fs.close();
			} catch (FileNotFoundException e) {
				// Send file not found error packet
				try {
					socket.send(TftpPacket.createErrorPacket(
							ErrorType.FILE_NOT_FOUND, "").generateDatagram(
							toAddress, toPort));
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.out.println("File not found: " + filename);
				return;
			} catch (IOException e) {
				// TODO check when IOException actually occurs and if we can
				// send when socket is closed
				// Send error packet (access violation)
				try {
					socket.send(TftpPacket.createErrorPacket(
							ErrorType.ACCESS_VIOLATION, "").generateDatagram(
							toAddress, toPort));
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.out.println("IOException with file: " + filename);
				return;
			} catch(IllegalArgumentException e){
				// We got an invalid packet
				// Open new socket and send error packet response
				DatagramSocket errorSocket = null;
				try {
					errorSocket = new DatagramSocket();
				} catch (SocketException e1) {
					e1.printStackTrace();
				}
				System.out.println("\nServer received invalid packet");
				TftpErrorPacket errorPacket = TftpPacket
						.createErrorPacket(ErrorType.ILLEGAL_OPERATION,
								e.getMessage());
				DatagramPacket dp = errorPacket.generateDatagram(toAddress,
						toPort);
				try {
					errorSocket.send(dp);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		public void runWriteRequest() {
			try {
				FileOutputStream fs;
				fs = new FileOutputStream(filePath);
				int blockNumber = 0;
				boolean isLastDataPacket = false;
				InetAddress toAddress = this.toAddress;
				TftpPacket pk;
				TftpDataPacket dataPk;
				int maxDataSize = TftpDataPacket.getMaxFileDataLength();

				while (true) {
					// Send ack packet
					DatagramPacket dp = TftpPacket.createAckPacket(blockNumber)
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
						dp = TftpPacket.createDatagramForReceiving();
						socket.receive(dp);
						pk = TftpPacket.createFromDatagram(dp);

						// Check for error packet
						if (dp.getPort() != toPort) {
							// Check for correct TID (sender port)
							TftpErrorPacket errorPacket = TftpPacket
									.createErrorPacket(ErrorType.UNKOWN_TID,
											"You're an idiot, and used an unkown TID");
							socket.send(errorPacket.generateDatagram(toAddress,
									dp.getPort()));
							System.out.println("******Ignoring invalid TID********");
							
							continue;
						} else if (pk instanceof TftpErrorPacket) {
							TftpErrorPacket errorPk = (TftpErrorPacket) pk;
							System.out
									.printf("Received error of type '%s' with message '%s'%n",
											errorPk.getErrorType().toString(),
											errorPk.getErrorMessage());
							if (errorPk.shouldAbortTransfer()) {
								// Close file and abort transfer
								fs.close();

								// Delete the file
								new File(filename).delete();

								System.out
										.printf("Aborting transfer of '%s'%n",
												filename);
								return;
							}
						}
					} while (!(pk instanceof TftpDataPacket)
							|| ((TftpDataPacket) pk).getBlockNumber() != blockNumber);

					System.out.printf("Received block %d of %s%n", blockNumber,
							filename);

					// Save into file
					dataPk = (TftpDataPacket) pk;
					fs.write(dataPk.getFileData());

					if (dataPk.getFileData().length != maxDataSize) {
						isLastDataPacket = true;
					}
				}

				fs.close();
			} catch (FileNotFoundException e) {
				try {
					socket.send(TftpPacket.createErrorPacket(
							ErrorType.ACCESS_VIOLATION, "").generateDatagram(
							toAddress, toPort));
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.out.println("Cannot write to: " + filename);
				return;
			} catch (IOException e) {
				System.out.println("IOException with file: " + filename);
				e.printStackTrace();
				return;
			}catch(IllegalArgumentException e){
				// We got an invalid packet
				// Open new socket and send error packet response
				DatagramSocket errorSocket = null;
				try {
					errorSocket = new DatagramSocket();
				} catch (SocketException e1) {
					e1.printStackTrace();
				}
				System.out.println("\nServer received invalid packet");
				TftpErrorPacket errorPacket = TftpPacket
						.createErrorPacket(ErrorType.ILLEGAL_OPERATION,
								e.getMessage());
				DatagramPacket dp = errorPacket.generateDatagram(toAddress,
						toPort);
				try {
					errorSocket.send(dp);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}