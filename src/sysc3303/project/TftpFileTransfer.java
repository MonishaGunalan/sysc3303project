package sysc3303.project;

import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

class TftpFileTransfer implements Runnable {
	private static final int SOCKET_TIMEOUT = 10000; // 10 seconds
	
	private DatagramSocket socket;
	private File file;
	private int remotePort;
	private InetAddress remoteAddress;
	private boolean receiving;
	private int previousSoTimeout;

	public TftpFileTransfer(File file, boolean receiving) {
		this.file = file;
		this.receiving = receiving;
	}
	
	public void setSocket(DatagramSocket socket) throws SocketException {
		this.socket = socket;
		this.previousSoTimeout = socket.getSoTimeout();
		socket.setSoTimeout(SOCKET_TIMEOUT);
	}
	
	public void setRemote(InetAddress remoteAddress, int remotePort) {
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
	}
	
	@Override
	public void run() {
		if (receiving) {
			this.receiveFile();
		} else {
			this.sendFile();
		}
	}
	
	private void receiveFile() {
		
	}
	
	private void sendFile() {
		// send next block
		
		// receive ack
	}
	

//	try {
//		int blockNumber = 1;
//		boolean isLastDataPacket = false;
//		InetAddress toAddress = this.toAddress;
//		TftpPacket pk;
//		int maxDataSize = TftpDataPacket.getMaxFileDataLength();
//
//		FileInputStream fs = new FileInputStream(filePath);
//
//		while (!isLastDataPacket) {
//			// Read file in 512 byte chunks
//			byte[] data = new byte[maxDataSize];
//			int bytesRead = fs.read(data);
//
//			if (bytesRead == -1) {
//				// Special case when file size is multiple of 512 bytes
//				bytesRead = 0;
//				data = new byte[0];
//			}
//
//			// Create the packet and get the flag to know if it is the
//			// last
//			TftpDataPacket dataPacket = TftpPacket.createDataPacket(
//					blockNumber, data, bytesRead);
//			isLastDataPacket = dataPacket.isLastDataPacket();
//			DatagramPacket dp = dataPacket.generateDatagram(toAddress,
//					toPort);
//
//			// Send data packet
//			System.out.printf("Sending block %d of %s%n", blockNumber,
//					filename);
//			socket.send(dp);
//
//			// Wait until we receive ack packet or error
//			dp = TftpPacket.createDatagramForReceiving();
//			do {
//				socket.receive(dp);
//				pk = TftpPacket.createFromDatagram(dp);
//
//				// Check for error packet
//				if (dp.getPort() != toPort) {
//					// Check for correct TID (sender port)
//					TftpErrorPacket errorPacket = TftpPacket
//							.createErrorPacket(ErrorType.UNKOWN_TID,
//									"You used an unkown TID");
//					socket.send(errorPacket.generateDatagram(toAddress,
//							dp.getPort()));
//					continue;
//				} else if (pk instanceof TftpErrorPacket) {
//					TftpErrorPacket errorPk = (TftpErrorPacket) pk;
//					System.out
//							.printf("Received error of type '%s' with message '%s'%n",
//									errorPk.getErrorType().toString(),
//									errorPk.getErrorMessage());
//					if (errorPk.shouldAbortTransfer()) {
//						// Close file and abort transfer
//						fs.close();
//						System.out.printf("Aborting transfer of '%s'%n",
//								filename);
//						return;
//					}
//				}
//			} while (!(pk instanceof TftpAckPacket)
//					|| ((TftpAckPacket) pk).getBlockNumber() != blockNumber);
//			System.out.printf("Received ack for block %d of %s%n",
//					blockNumber, filename);
//			blockNumber++;
//		}
//
//		fs.close();
//	} catch (FileNotFoundException e) {
//		// Send file not found error packet
//		try {
//			socket.send(TftpPacket.createErrorPacket(
//					ErrorType.FILE_NOT_FOUND, "").generateDatagram(
//					toAddress, toPort));
//		} catch (IllegalArgumentException e1) {
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		System.out.println("File not found: " + filename);
//		return;
//	} catch (IOException e) {
//		// TODO check when IOException actually occurs and if we can
//		// send when socket is closed
//		// Send error packet (access violation)
//		try {
//			socket.send(TftpPacket.createErrorPacket(
//					ErrorType.ACCESS_VIOLATION, "").generateDatagram(
//					toAddress, toPort));
//		} catch (IllegalArgumentException e1) {
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		System.out.println("IOException with file: " + filename);
//		return;
//	} catch (IllegalArgumentException e) {
//		// We got an invalid packet
//		// Open new socket and send error packet response
//		DatagramSocket errorSocket = null;
//		try {
//			errorSocket = new DatagramSocket();
//		} catch (SocketException e1) {
//			e1.printStackTrace();
//		}
//		System.out.println("\nServer received invalid packet");
//		TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
//				ErrorType.ILLEGAL_OPERATION, e.getMessage());
//		DatagramPacket dp = errorPacket.generateDatagram(toAddress, toPort);
//		try {
//			errorSocket.send(dp);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//	}

	
//
//	public void runReadRequest() {
//		try {
//			int blockNumber = 1;
//			boolean isLastDataPacket = false;
//			InetAddress toAddress = this.toAddress;
//			TftpPacket pk;
//			int maxDataSize = TftpDataPacket.getMaxFileDataLength();
//
//			FileInputStream fs = new FileInputStream(filePath);
//
//			while (!isLastDataPacket) {
//				// Read file in 512 byte chunks
//				byte[] data = new byte[maxDataSize];
//				int bytesRead = fs.read(data);
//
//				if (bytesRead == -1) {
//					// Special case when file size is multiple of 512 bytes
//					bytesRead = 0;
//					data = new byte[0];
//				}
//
//				// Create the packet and get the flag to know if it is the
//				// last
//				TftpDataPacket dataPacket = TftpPacket.createDataPacket(
//						blockNumber, data, bytesRead);
//				isLastDataPacket = dataPacket.isLastDataPacket();
//				DatagramPacket dp = dataPacket.generateDatagram(toAddress,
//						toPort);
//
//				// Send data packet
//				System.out.printf("Sending block %d of %s%n", blockNumber,
//						filename);
//				socket.send(dp);
//
//				// Wait until we receive ack packet or error
//				dp = TftpPacket.createDatagramForReceiving();
//				do {
//					socket.receive(dp);
//					pk = TftpPacket.createFromDatagram(dp);
//
//					// Check for error packet
//					if (dp.getPort() != toPort) {
//						// Check for correct TID (sender port)
//						TftpErrorPacket errorPacket = TftpPacket
//								.createErrorPacket(ErrorType.UNKOWN_TID,
//										"You used an unkown TID");
//						socket.send(errorPacket.generateDatagram(toAddress,
//								dp.getPort()));
//						continue;
//					} else if (pk instanceof TftpErrorPacket) {
//						TftpErrorPacket errorPk = (TftpErrorPacket) pk;
//						System.out
//								.printf("Received error of type '%s' with message '%s'%n",
//										errorPk.getErrorType().toString(),
//										errorPk.getErrorMessage());
//						if (errorPk.shouldAbortTransfer()) {
//							// Close file and abort transfer
//							fs.close();
//							System.out.printf("Aborting transfer of '%s'%n",
//									filename);
//							return;
//						}
//					}
//				} while (!(pk instanceof TftpAckPacket)
//						|| ((TftpAckPacket) pk).getBlockNumber() != blockNumber);
//				System.out.printf("Received ack for block %d of %s%n",
//						blockNumber, filename);
//				blockNumber++;
//			}
//
//			fs.close();
//		} catch (FileNotFoundException e) {
//			// Send file not found error packet
//			try {
//				socket.send(TftpPacket.createErrorPacket(
//						ErrorType.FILE_NOT_FOUND, "").generateDatagram(
//						toAddress, toPort));
//			} catch (IllegalArgumentException e1) {
//				e1.printStackTrace();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//			System.out.println("File not found: " + filename);
//			return;
//		} catch (IOException e) {
//			// TODO check when IOException actually occurs and if we can
//			// send when socket is closed
//			// Send error packet (access violation)
//			try {
//				socket.send(TftpPacket.createErrorPacket(
//						ErrorType.ACCESS_VIOLATION, "").generateDatagram(
//						toAddress, toPort));
//			} catch (IllegalArgumentException e1) {
//				e1.printStackTrace();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//			System.out.println("IOException with file: " + filename);
//			return;
//		} catch (IllegalArgumentException e) {
//			// We got an invalid packet
//			// Open new socket and send error packet response
//			DatagramSocket errorSocket = null;
//			try {
//				errorSocket = new DatagramSocket();
//			} catch (SocketException e1) {
//				e1.printStackTrace();
//			}
//			System.out.println("\nServer received invalid packet");
//			TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
//					ErrorType.ILLEGAL_OPERATION, e.getMessage());
//			DatagramPacket dp = errorPacket.generateDatagram(toAddress, toPort);
//			try {
//				errorSocket.send(dp);
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//		}
//	}
//
//	public void runWriteRequest() {
//		try {
//			FileOutputStream fs;
//			fs = new FileOutputStream(filePath);
//			int blockNumber = 0;
//			boolean isLastDataPacket = false;
//			InetAddress toAddress = this.toAddress;
//			TftpPacket pk;
//			TftpDataPacket dataPk;
//			int maxDataSize = TftpDataPacket.getMaxFileDataLength();
//
//			while (true) {
//				// Send ack packet
//				DatagramPacket dp = TftpPacket.createAckPacket(blockNumber)
//						.generateDatagram(toAddress, toPort);
//				socket.send(dp);
//				System.out.printf("Sent ack for block %d of %s%n", blockNumber,
//						filename);
//				if (isLastDataPacket) {
//					break;
//				}
//
//				// Increment blockNumber
//				blockNumber++;
//
//				// Receive data packet
//				do {
//					dp = TftpPacket.createDatagramForReceiving();
//					socket.receive(dp);
//					pk = TftpPacket.createFromDatagram(dp);
//
//					// Check for error packet
//					if (dp.getPort() != toPort) {
//						// Check for correct TID (sender port)
//						TftpErrorPacket errorPacket = TftpPacket
//								.createErrorPacket(ErrorType.UNKOWN_TID,
//										"You're an idiot, and used an unkown TID");
//						socket.send(errorPacket.generateDatagram(toAddress,
//								dp.getPort()));
//						System.out
//								.println("******Ignoring invalid TID********");
//
//						continue;
//					} else if (pk instanceof TftpErrorPacket) {
//						TftpErrorPacket errorPk = (TftpErrorPacket) pk;
//						System.out
//								.printf("Received error of type '%s' with message '%s'%n",
//										errorPk.getErrorType().toString(),
//										errorPk.getErrorMessage());
//						if (errorPk.shouldAbortTransfer()) {
//							// Close file and abort transfer
//							fs.close();
//
//							// Delete the file
//							new File(filename).delete();
//
//							System.out.printf("Aborting transfer of '%s'%n",
//									filename);
//							return;
//						}
//					}
//				} while (!(pk instanceof TftpDataPacket)
//						|| ((TftpDataPacket) pk).getBlockNumber() != blockNumber);
//
//				System.out.printf("Received block %d of %s%n", blockNumber,
//						filename);
//
//				// Save into file
//				dataPk = (TftpDataPacket) pk;
//				fs.write(dataPk.getFileData());
//
//				if (dataPk.getFileData().length != maxDataSize) {
//					isLastDataPacket = true;
//				}
//			}
//
//			fs.close();
//		} catch (FileNotFoundException e) {
//			try {
//				socket.send(TftpPacket.createErrorPacket(
//						ErrorType.ACCESS_VIOLATION, "").generateDatagram(
//						toAddress, toPort));
//			} catch (IllegalArgumentException e1) {
//				e1.printStackTrace();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//			System.out.println("Cannot write to: " + filename);
//			return;
//		} catch (IOException e) {
//			System.out.println("IOException with file: " + filename);
//			e.printStackTrace();
//			return;
//		} catch (IllegalArgumentException e) {
//			// We got an invalid packet
//			// Open new socket and send error packet response
//			DatagramSocket errorSocket = null;
//			try {
//				errorSocket = new DatagramSocket();
//			} catch (SocketException e1) {
//				e1.printStackTrace();
//			}
//			System.out.println("\nServer received invalid packet");
//			TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
//					ErrorType.ILLEGAL_OPERATION, e.getMessage());
//			DatagramPacket dp = errorPacket.generateDatagram(toAddress, toPort);
//			try {
//				errorSocket.send(dp);
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//		}
//	}
}
