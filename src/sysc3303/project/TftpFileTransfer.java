package sysc3303.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import sysc3303.project.packets.TftpAckPacket;
import sysc3303.project.packets.TftpDataPacket;
import sysc3303.project.packets.TftpErrorPacket;
import sysc3303.project.packets.TftpErrorPacket.ErrorType;
import sysc3303.project.packets.TftpPacket;
import sysc3303.project.packets.TftpRequestPacket;

class TftpFileTransfer implements Runnable {
	private static final int SOCKET_TIMEOUT = 10000; // 10 seconds
	private static final int MAX_TIMEOUTS = 5;

	private DatagramSocket socket;
	private File file;
	private int remotePort;
	private InetAddress remoteAddress;
	private Direction direction;
	private int previousSoTimeout = -1;
	private int currentBlock = 0;
	private int timeouts = 0; // count how many times we have timed out
	private Mode mode;

	enum Direction {
		SEND, RECEIVE
	}

	enum Mode {
		CLIENT, SERVER
	}

	public TftpFileTransfer(Mode mode) {
		if (null == mode) {
			throw new IllegalArgumentException(
					"Mode must be either ClIENT or SERVER");
		}
		this.mode = mode;
	}

	public TftpFileTransfer setSocket(DatagramSocket socket)
			throws SocketException {
		this.socket = socket;
		this.previousSoTimeout = socket.getSoTimeout();
		socket.setSoTimeout(SOCKET_TIMEOUT);
		return this;
	}

	public TftpFileTransfer setRemote(InetAddress remoteAddress, int remotePort)
			throws IllegalArgumentException {
		if (remoteAddress == null || remotePort <= 0) {
			throw new IllegalArgumentException();
		}
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		return this;
	}

	public TftpFileTransfer setFile(File file) throws IllegalArgumentException {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		this.file = file;
		return this;
	}

	public TftpFileTransfer setDirection(Direction direction)
			throws IllegalArgumentException {
		if (direction == null) {
			throw new IllegalArgumentException();
		}
		this.direction = direction;
		return this;
	}

	public void run() {
		// TODO validate remote address and port, and socket, and file, and
		// direction

		if (direction == Direction.RECEIVE) {
			this.receiveFile();
		} else {
			this.sendFile();
		}

		// Reset previous timeout
		if (previousSoTimeout >= 0) {
			try {
				socket.setSoTimeout(previousSoTimeout);
			} catch (SocketException e) {
				// Ignore errors
			}
		}
	}

	private boolean receiveFile() throws IOException {
		FileOutputStream stream = new FileOutputStream(file);

		// Set the starting block
		if (mode == Mode.CLIENT) {
			currentBlock = 1;
		} else {
			currentBlock = 0;
		}

		if (currentBlock == 0) {
			sendAck(currentBlock); // TODO make this server only
			currentBlock++;
		}

		// Receive a packet
		TftpPacket pk = receivePacket();
		if (pk == null) {
			return false;
		} else if (pk instanceof TftpAckPacket) {

		} else {
			// We have a data packet
		}

		return false;
	}

	private void sendAck(int blockNumber) throws IOException {
		try {
			TftpAckPacket pk = TftpPacket.createAckPacket(blockNumber);
			socket.send(pk.generateDatagram(remoteAddress, remotePort));
		} catch (IllegalArgumentException e) {
			System.out.println("Created a malformed TftpAckPacket.");
			e.printStackTrace();
		}
	}

	private TftpPacket receivePacket() throws IOException {
		try {
			while (true) { // Note: using while instead of recursing to reduce
							// memory usage (and avoid stack overflow errors in
							// the case where we would receive a lot of error
							// messages in a row)
				// Receive a packet
				DatagramPacket dp = TftpPacket.createDatagramForReceiving();
				socket.receive(dp);

				// Verify the TID
				if (remoteAddress != dp.getAddress()
						|| remotePort != dp.getPort()) {
					// Send an invalid TID error
					sendInvalidTidError(dp);

					// Restart receiving
					continue;
				}

				// Parse the packet
				TftpPacket pk = TftpPacket.createFromDatagram(dp);

				// Check if we got an error
				if (pk instanceof TftpErrorPacket) {
					if (((TftpErrorPacket) pk).shouldAbortTransfer()) {
						// Return null to cause an abort
						return null;
					} else {
						// No need to abort, just ignore and restart receiving
						continue;
					}
				} else if (pk instanceof TftpRequestPacket) {
					// We received a request packet on a data transfer
					sendIllegalOperationError("Seriously? This is a data transfer, not request listener. Check your destination port.");
				}

				// Return the packet
				return pk;
			}
		} catch (IllegalArgumentException e) {
			// We received an unparsable packet, send error packet
			sendIllegalOperationError(e.getMessage());
			return null;
		}
	}

	private boolean sendIllegalOperationError(String message) {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					ErrorType.ILLEGAL_OPERATION, message);
			socket.send(pk.generateDatagram(remoteAddress, remotePort));
			return true;
		} catch (IllegalArgumentException e) {
			System.out
					.println("We messed up in making the Illegal Operation packet");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.out
					.println("Could not send Illegal Operation error. Who cares, because we are aborting! ;-)");
			return false;
		}
	}

	private boolean sendInvalidTidError(DatagramPacket dpReceived)
			throws IOException {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					ErrorType.UNKOWN_TID,
					"Stop trying to hack with your fake TID");
			socket.send(pk.generateDatagram(dpReceived.getAddress(),
					dpReceived.getPort()));
			return true;
		} catch (IllegalArgumentException e) {
			// Created a malformed TID Packet
			System.out
					.println("Oops. We likely tried to create a malformed invalid TID packet.");
			e.printStackTrace();
			return false;
		}
	}

	private void sendFile() {
		// FileInputStream stream = new FileInputStream(file);
		// send next block

		// receive ack
	}

	// try {
	// int blockNumber = 1;
	// boolean isLastDataPacket = false;
	// InetAddress toAddress = this.toAddress;
	// TftpPacket pk;
	// int maxDataSize = TftpDataPacket.getMaxFileDataLength();
	//
	// FileInputStream fs = new FileInputStream(filePath);
	//
	// while (!isLastDataPacket) {
	// // Read file in 512 byte chunks
	// byte[] data = new byte[maxDataSize];
	// int bytesRead = fs.read(data);
	//
	// if (bytesRead == -1) {
	// // Special case when file size is multiple of 512 bytes
	// bytesRead = 0;
	// data = new byte[0];
	// }
	//
	// // Create the packet and get the flag to know if it is the
	// // last
	// TftpDataPacket dataPacket = TftpPacket.createDataPacket(
	// blockNumber, data, bytesRead);
	// isLastDataPacket = dataPacket.isLastDataPacket();
	// DatagramPacket dp = dataPacket.generateDatagram(toAddress,
	// toPort);
	//
	// // Send data packet
	// System.out.printf("Sending block %d of %s%n", blockNumber,
	// filename);
	// socket.send(dp);
	//
	// // Wait until we receive ack packet or error
	// dp = TftpPacket.createDatagramForReceiving();
	// do {
	// socket.receive(dp);
	// pk = TftpPacket.createFromDatagram(dp);
	//
	// // Check for error packet
	// if (dp.getPort() != toPort) {
	// // Check for correct TID (sender port)
	// TftpErrorPacket errorPacket = TftpPacket
	// .createErrorPacket(ErrorType.UNKOWN_TID,
	// "You used an unkown TID");
	// socket.send(errorPacket.generateDatagram(toAddress,
	// dp.getPort()));
	// continue;
	// } else if (pk instanceof TftpErrorPacket) {
	// TftpErrorPacket errorPk = (TftpErrorPacket) pk;
	// System.out
	// .printf("Received error of type '%s' with message '%s'%n",
	// errorPk.getErrorType().toString(),
	// errorPk.getErrorMessage());
	// if (errorPk.shouldAbortTransfer()) {
	// // Close file and abort transfer
	// fs.close();
	// System.out.printf("Aborting transfer of '%s'%n",
	// filename);
	// return;
	// }
	// }
	// } while (!(pk instanceof TftpAckPacket)
	// || ((TftpAckPacket) pk).getBlockNumber() != blockNumber);
	// System.out.printf("Received ack for block %d of %s%n",
	// blockNumber, filename);
	// blockNumber++;
	// }
	//
	// fs.close();
	// } catch (FileNotFoundException e) {
	// // Send file not found error packet
	// try {
	// socket.send(TftpPacket.createErrorPacket(
	// ErrorType.FILE_NOT_FOUND, "").generateDatagram(
	// toAddress, toPort));
	// } catch (IllegalArgumentException e1) {
	// e1.printStackTrace();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("File not found: " + filename);
	// return;
	// } catch (IOException e) {
	// // TODO check when IOException actually occurs and if we can
	// // send when socket is closed
	// // Send error packet (access violation)
	// try {
	// socket.send(TftpPacket.createErrorPacket(
	// ErrorType.ACCESS_VIOLATION, "").generateDatagram(
	// toAddress, toPort));
	// } catch (IllegalArgumentException e1) {
	// e1.printStackTrace();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("IOException with file: " + filename);
	// return;
	// } catch (IllegalArgumentException e) {
	// // We got an invalid packet
	// // Open new socket and send error packet response
	// DatagramSocket errorSocket = null;
	// try {
	// errorSocket = new DatagramSocket();
	// } catch (SocketException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("\nServer received invalid packet");
	// TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
	// ErrorType.ILLEGAL_OPERATION, e.getMessage());
	// DatagramPacket dp = errorPacket.generateDatagram(toAddress, toPort);
	// try {
	// errorSocket.send(dp);
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// }

	//
	// public void runReadRequest() {
	// try {
	// int blockNumber = 1;
	// boolean isLastDataPacket = false;
	// InetAddress toAddress = this.toAddress;
	// TftpPacket pk;
	// int maxDataSize = TftpDataPacket.getMaxFileDataLength();
	//
	// FileInputStream fs = new FileInputStream(filePath);
	//
	// while (!isLastDataPacket) {
	// // Read file in 512 byte chunks
	// byte[] data = new byte[maxDataSize];
	// int bytesRead = fs.read(data);
	//
	// if (bytesRead == -1) {
	// // Special case when file size is multiple of 512 bytes
	// bytesRead = 0;
	// data = new byte[0];
	// }
	//
	// // Create the packet and get the flag to know if it is the
	// // last
	// TftpDataPacket dataPacket = TftpPacket.createDataPacket(
	// blockNumber, data, bytesRead);
	// isLastDataPacket = dataPacket.isLastDataPacket();
	// DatagramPacket dp = dataPacket.generateDatagram(toAddress,
	// toPort);
	//
	// // Send data packet
	// System.out.printf("Sending block %d of %s%n", blockNumber,
	// filename);
	// socket.send(dp);
	//
	// // Wait until we receive ack packet or error
	// dp = TftpPacket.createDatagramForReceiving();
	// do {
	// socket.receive(dp);
	// pk = TftpPacket.createFromDatagram(dp);
	//
	// // Check for error packet
	// if (dp.getPort() != toPort) {
	// // Check for correct TID (sender port)
	// TftpErrorPacket errorPacket = TftpPacket
	// .createErrorPacket(ErrorType.UNKOWN_TID,
	// "You used an unkown TID");
	// socket.send(errorPacket.generateDatagram(toAddress,
	// dp.getPort()));
	// continue;
	// } else if (pk instanceof TftpErrorPacket) {
	// TftpErrorPacket errorPk = (TftpErrorPacket) pk;
	// System.out
	// .printf("Received error of type '%s' with message '%s'%n",
	// errorPk.getErrorType().toString(),
	// errorPk.getErrorMessage());
	// if (errorPk.shouldAbortTransfer()) {
	// // Close file and abort transfer
	// fs.close();
	// System.out.printf("Aborting transfer of '%s'%n",
	// filename);
	// return;
	// }
	// }
	// } while (!(pk instanceof TftpAckPacket)
	// || ((TftpAckPacket) pk).getBlockNumber() != blockNumber);
	// System.out.printf("Received ack for block %d of %s%n",
	// blockNumber, filename);
	// blockNumber++;
	// }
	//
	// fs.close();
	// } catch (FileNotFoundException e) {
	// // Send file not found error packet
	// try {
	// socket.send(TftpPacket.createErrorPacket(
	// ErrorType.FILE_NOT_FOUND, "").generateDatagram(
	// toAddress, toPort));
	// } catch (IllegalArgumentException e1) {
	// e1.printStackTrace();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("File not found: " + filename);
	// return;
	// } catch (IOException e) {
	// // TODO check when IOException actually occurs and if we can
	// // send when socket is closed
	// // Send error packet (access violation)
	// try {
	// socket.send(TftpPacket.createErrorPacket(
	// ErrorType.ACCESS_VIOLATION, "").generateDatagram(
	// toAddress, toPort));
	// } catch (IllegalArgumentException e1) {
	// e1.printStackTrace();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("IOException with file: " + filename);
	// return;
	// } catch (IllegalArgumentException e) {
	// // We got an invalid packet
	// // Open new socket and send error packet response
	// DatagramSocket errorSocket = null;
	// try {
	// errorSocket = new DatagramSocket();
	// } catch (SocketException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("\nServer received invalid packet");
	// TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
	// ErrorType.ILLEGAL_OPERATION, e.getMessage());
	// DatagramPacket dp = errorPacket.generateDatagram(toAddress, toPort);
	// try {
	// errorSocket.send(dp);
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// }
	// }
	//
	// public void runWriteRequest() {
	// try {
	// FileOutputStream fs;
	// fs = new FileOutputStream(filePath);
	// int blockNumber = 0;
	// boolean isLastDataPacket = false;
	// InetAddress toAddress = this.toAddress;
	// TftpPacket pk;
	// TftpDataPacket dataPk;
	// int maxDataSize = TftpDataPacket.getMaxFileDataLength();
	//
	// while (true) {
	// // Send ack packet
	// DatagramPacket dp = TftpPacket.createAckPacket(blockNumber)
	// .generateDatagram(toAddress, toPort);
	// socket.send(dp);
	// System.out.printf("Sent ack for block %d of %s%n", blockNumber,
	// filename);
	// if (isLastDataPacket) {
	// break;
	// }
	//
	// // Increment blockNumber
	// blockNumber++;
	//
	// // Receive data packet
	// do {
	// dp = TftpPacket.createDatagramForReceiving();
	// socket.receive(dp);
	// pk = TftpPacket.createFromDatagram(dp);
	//
	// // Check for error packet
	// if (dp.getPort() != toPort) {
	// // Check for correct TID (sender port)
	// TftpErrorPacket errorPacket = TftpPacket
	// .createErrorPacket(ErrorType.UNKOWN_TID,
	// "You're an idiot, and used an unkown TID");
	// socket.send(errorPacket.generateDatagram(toAddress,
	// dp.getPort()));
	// System.out
	// .println("******Ignoring invalid TID********");
	//
	// continue;
	// } else if (pk instanceof TftpErrorPacket) {
	// TftpErrorPacket errorPk = (TftpErrorPacket) pk;
	// System.out
	// .printf("Received error of type '%s' with message '%s'%n",
	// errorPk.getErrorType().toString(),
	// errorPk.getErrorMessage());
	// if (errorPk.shouldAbortTransfer()) {
	// // Close file and abort transfer
	// fs.close();
	//
	// // Delete the file
	// new File(filename).delete();
	//
	// System.out.printf("Aborting transfer of '%s'%n",
	// filename);
	// return;
	// }
	// }
	// } while (!(pk instanceof TftpDataPacket)
	// || ((TftpDataPacket) pk).getBlockNumber() != blockNumber);
	//
	// System.out.printf("Received block %d of %s%n", blockNumber,
	// filename);
	//
	// // Save into file
	// dataPk = (TftpDataPacket) pk;
	// fs.write(dataPk.getFileData());
	//
	// if (dataPk.getFileData().length != maxDataSize) {
	// isLastDataPacket = true;
	// }
	// }
	//
	// fs.close();
	// } catch (FileNotFoundException e) {
	// try {
	// socket.send(TftpPacket.createErrorPacket(
	// ErrorType.ACCESS_VIOLATION, "").generateDatagram(
	// toAddress, toPort));
	// } catch (IllegalArgumentException e1) {
	// e1.printStackTrace();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("Cannot write to: " + filename);
	// return;
	// } catch (IOException e) {
	// System.out.println("IOException with file: " + filename);
	// e.printStackTrace();
	// return;
	// } catch (IllegalArgumentException e) {
	// // We got an invalid packet
	// // Open new socket and send error packet response
	// DatagramSocket errorSocket = null;
	// try {
	// errorSocket = new DatagramSocket();
	// } catch (SocketException e1) {
	// e1.printStackTrace();
	// }
	// System.out.println("\nServer received invalid packet");
	// TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
	// ErrorType.ILLEGAL_OPERATION, e.getMessage());
	// DatagramPacket dp = errorPacket.generateDatagram(toAddress, toPort);
	// try {
	// errorSocket.send(dp);
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// }
	// }
}
