package sysc3303.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import sysc3303.project.common.TftpAckPacket;
import sysc3303.project.common.TftpDataPacket;
import sysc3303.project.common.TftpErrorPacket;
import sysc3303.project.common.TftpPacket;
import sysc3303.project.common.TftpRequestPacket;
import sysc3303.project.common.TftpErrorPacket.ErrorType;

abstract class TftpTransfer implements Runnable {
	private static final int SOCKET_TIMEOUT = 10000; // 10 seconds
	private static final int MAX_TIMEOUTS = 5;

	private DatagramSocket socket;
	private File file;
	private int remotePort;
	private int requestPort = 69;
	private InetAddress remoteAddress;
	private int previousSoTimeout = -1;
	private int currentBlock = 0;
	private Scope scope;
	private TftpRequestPacket requestPacket;

	private class TftpAbortException extends Exception {
		private static final long serialVersionUID = -2879110457073551679L;

		TransferAbortException(String message) {
			super(message);
		}
	}

	enum Scope {
		CLIENT, SERVER
	}

	TftpTransfer(Scope scope) {
		if (null == scope) {
			throw new IllegalArgumentException(
					"Mode must be either ClIENT or SERVER");
		}
		this.scope = scope;
	}

	TftpTransfer setRequestPacket(TftpRequestPacket requestPacket) {
		this.requestPacket = requestPacket;
		return this;
	}

	TftpTransfer setSocket(DatagramSocket socket) throws SocketException {
		this.socket = socket;
		this.previousSoTimeout = socket.getSoTimeout();
		socket.setSoTimeout(SOCKET_TIMEOUT);
		return this;
	}

	TftpTransfer setRemote(InetAddress remoteAddress, int remotePort)
			throws IllegalArgumentException {
		if (remoteAddress == null || remotePort <= 0) {
			throw new IllegalArgumentException();
		}
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		return this;
	}

	TftpTransfer setFile(File file) throws IllegalArgumentException {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		this.file = file;
		return this;
	}

	public void run() {
		// TODO validate all attributes

		// check if we are sending or receiving
		boolean isReceiving = ((scope == Scope.CLIENT && requestPacket
				.isReadRequest()) || (scope == Scope.SERVER && !requestPacket
				.isReadRequest()));

		if (scope == Scope.SERVER && socket == null) {
			try {
				socket = new DatagramSocket();
			} catch (SocketException e) {
				System.out
						.println("Failed to create socket: " + e.getMessage());
				e.printStackTrace();
				return;
			}
		}

		// set/reset current block to 1
		currentBlock = 1;

		try {
			if (isReceiving) {
				this.receiveFile();
			} else {
				this.sendFile();
			}
		} catch (SocketTimeoutException e) {
			System.out
					.println("Remote end doesn't seem to want to talk to us anymore :(");
		} catch (IOException e) {
			System.out
					.println("Somekind of IOException happened. Here's a stack trace dump:");
			e.printStackTrace();
		} catch (TransferAbortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private boolean receiveFile() throws IOException, TransferAbortException {
		FileOutputStream stream = new FileOutputStream(file);
		TftpPacket pk;
		TftpDataPacket dataPk;
		int timeouts = 0;

		// Server sends ack 0 when receiving
		if (scope == Scope.SERVER) {
			sendAck(0);
		}

		while (true) {
			try {
				pk = receiveAckOrDataPacket();
				timeouts = 0;
			} catch (SocketTimeoutException e) {
				// Increment timeout
				timeouts++;

				// Resend ack unless we are the client waiting for the first
				// data packet from server
				if(currentBlock == 1 && scope == Scope.SERVER) {
					
				}

				continue;
			}

			if (pk instanceof TftpAckPacket) {
				// ignore ack packet
				continue;
			}

			dataPk = (TftpDataPacket) pk;
			if (dataPk.getBlockNumber() < currentBlock) {
				// duplicate data sent, send ack
				sendAck(dataPk.getBlockNumber());
				continue;
			} else if (dataPk.getBlockNumber() > currentBlock) {
				// ignore blocks that we aren't ready for
				continue;
			}

			// Write data to file
			stream.write(dataPk.getFileData());

			// Send ack
			sendAck(currentBlock);

			if (dataPk.isLastDataPacket()) {
				break;
			}

			currentBlock++;
		}

		// Close stream and return true for success!! :D
		stream.close();
		return true;
	}

	private boolean sendFile() throws IOException, TransferAbortException {
		FileInputStream stream = new FileInputStream(file);
		byte[] data = new byte[TftpDataPacket.MAX_FILE_DATA_LENGTH];
		int length;
		boolean isLastDataPacket = false;

		// Note: client must have already received ack 0

		try {
			do {
				// Read from the file
				length = stream.read(data);

				if (length == -1) {
					length = 0;
				}

				// Send the data packet
				isLastDataPacket = sendDataAndWaitForAck(data, length);
			} while (isLastDataPacket);

			stream.close();
			return true;
		} catch (IllegalArgumentException e) {
			stream.close();
			return false;
		}
	}

	private boolean sendDataAndWaitForAck(byte[] data, int length)
			throws IOException, TransferAbortException {
		// Send the data packet
		TftpDataPacket dataPk = TftpPacket.createDataPacket(currentBlock, data,
				length);
		DatagramPacket dp = dataPk.generateDatagram(remoteAddress, remotePort);

		socket.send(dp);
		waitForAck(dp);

		return !dataPk.isLastDataPacket();
	}

	private void waitForAck(DatagramPacket datagramToResend)
			throws IOException, TransferAbortException {
		TftpPacket pk;
		int timeouts = 0;

		// Wait to receive associated ack (resend data if need be)
		while (true) {
			// Get a packet but watch for number of times we timeout
			while (true) {
				try {
					pk = receiveAckOrDataPacket();
					timeouts = 0; // reset timeouts
					break;
				} catch (SocketTimeoutException e) {
					timeouts++;
					if (timeouts <= MAX_TIMEOUTS) {
						// Resend last data packet and try again
						socket.send(datagramToResend);
						continue;
					} else {
						throw e;
					}
				}
			}

			if (pk instanceof TftpAckPacket) {
				TftpAckPacket ackPk = (TftpAckPacket) pk;
				if (ackPk.getBlockNumber() == currentBlock) {
					return;
				}
			}
		}
	}

	// private TftpDataPacket receiveData() throws IOException,
	// TransferAbortException {
	// TftpPacket pk;
	// TftpDataPacket dataPk;
	// int timeouts = 0;
	//
	// // Wait to receive next data packet, resend ack if needed
	// while (true) {
	// // Get a packet but watch for number of times we timeout
	// while (true) {
	// try {
	// pk = receiveAckOrDataPacket();
	// timeouts = 0; // reset timeouts
	// break;
	// } catch (SocketTimeoutException e) {
	// timeouts++;
	// if (timeouts <= MAX_TIMEOUTS) {
	// // Resend last ack packet and try again
	// sendAck(currentBlock - 1);
	// continue;
	// } else {
	// throw e;
	// }
	// }
	// }
	//
	// // Check if it is valid or if an error occurred
	// if (pk instanceof TftpDataPacket) {
	// dataPk = (TftpDataPacket) pk;
	//
	// // Check if this is the one we are waiting for
	// if (dataPk.getBlockNumber() == currentBlock) {
	// return dataPk;
	// } else if (currentBlock > dataPk.getBlockNumber()) {
	// // Send ack even though it is a duplicate packet
	// sendAck(dataPk.getBlockNumber());
	// }
	//
	// // Note: we ignore data packets ahead of the block we want
	// }
	//
	// // Note: we ignore incoming ACK packets
	// }
	// }

	private void sendAck(int blockNumber) throws IOException,
			TransferAbortException {
		try {
			TftpAckPacket pk = TftpPacket.createAckPacket(blockNumber);
			socket.send(pk.generateDatagram(remoteAddress, remotePort));
		} catch (IllegalArgumentException e) {
			throw new TransferAbortException(
					"Created a malformed TftpAckPacket. " + e.getMessage());
		}
	}

	private TftpPacket receiveAckOrDataPacket() throws IOException,
			TransferAbortException {
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
					TftpErrorPacket errorPk = (TftpErrorPacket) pk;
					if (errorPk.shouldAbortTransfer()) {
						// Return null to cause an abort
						throw new TransferAbortException(
								"Received a fatal error packet with message: "
										+ errorPk.getErrorMessage());
					} else {
						// No need to abort, just ignore and restart receiving
						System.out
								.println("Received non-aborting error with message: "
										+ errorPk.getErrorMessage());
						continue;
					}
				} else if (pk instanceof TftpRequestPacket) {
					// We received a request packet on a data transfer
					sendIllegalOperationError("Seriously? This is a data transfer, not request listener. Check your destination port.");
					throw new TransferAbortException(
							"Illegal Operation Error: received request packet on data transfer port.");
				}

				// Return the packet
				return pk;
			}
		} catch (IllegalArgumentException e) {
			// We received an unparsable packet, send error packet
			sendIllegalOperationError(e.getMessage());
			throw new TransferAbortException("Received an unparsable packet: "
					+ e.getMessage());
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
