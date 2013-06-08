package sysc3303.project.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TftpConnection {
	private DatagramSocket socket;
	private InetAddress remoteAddress;
	private int requestPort = 6800;
	private int remoteTid = -1;
	private DatagramPacket inDatagram = TftpPacket.createDatagramForReceiving();
	private DatagramPacket resendDatagram;
	private int maxResendAttempts = 4;
	private int timeoutTime = 2000;

	public TftpConnection() throws SocketException {
		this(new DatagramSocket());
	}

	public TftpConnection(int bindPort) throws SocketException {
		this(new DatagramSocket(bindPort));
	}

	public TftpConnection(DatagramSocket socket) throws SocketException {
		this.socket = socket;
		socket.setSoTimeout(timeoutTime);
		Log.d("connected on port " + socket.getLocalPort());
	}

	public void setRemoteAddress(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public void setRemoteTid(int remoteTid) {
		this.remoteTid = remoteTid;
		Log.d("setting remote tid to: " + remoteTid);
	}

	public void sendRequest(TftpRequestPacket packet) throws IOException {
		resendDatagram = packet.generateDatagram(remoteAddress, requestPort);
		socket.send(packet.generateDatagram(remoteAddress, requestPort));
	}

	public void setRequestPort(int requestPort) {
		this.requestPort = requestPort;
	}

	private void send(TftpPacket packet) throws IOException {
		send(packet, false);
	}

	private void send(TftpPacket packet, boolean cacheForResend)
			throws IOException {
		DatagramPacket dp = packet.generateDatagram(remoteAddress, remoteTid);

		if (cacheForResend) {
			resendDatagram = dp;
		}else{
			resendDatagram = null;
		}

		socket.send(dp);
	}

	public void sendAck(int blockNumber) throws TftpAbortException {
		try {
			send(TftpPacket.createAckPacket(blockNumber), false);
			Log.d("sent: ack #" + blockNumber);
		} catch (Exception e) {
			throw new TftpAbortException(e.getMessage());
		}
	}

	private void echoAck(int blockNumber) throws IOException {
		send(TftpPacket.createAckPacket(blockNumber));
		Log.d("sent: ack #" + blockNumber + " in response to duplicate data");
	}

	private void resendLastPacket() throws TftpAbortException {
		if (resendDatagram == null) {
			return;//throw new TftpAbortException("Cannot resend last packet");
		}

		try {
			socket.send(resendDatagram);
			Log.d("resent last non-error packet");
		} catch (IOException e) {
			throw new TftpAbortException(e.getMessage());
		}

	}

	private TftpPacket receive() throws IOException, TftpAbortException {
		while (true) {
			socket.receive(inDatagram);

			if (remoteTid > 0
					&& (inDatagram.getPort() != remoteTid || !(inDatagram
							.getAddress()).equals(remoteAddress))) {
				sendUnkownTidError(inDatagram.getAddress(),
						inDatagram.getPort());
				continue;
			}

			try {
				return TftpPacket.createFromDatagram(inDatagram);
			} catch (IllegalArgumentException e) {
				sendIllegalOperationError(e.getMessage());
			}
		}
	}

	private void sendIllegalOperationError(String message)
			throws TftpAbortException {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					TftpErrorPacket.ErrorType.ILLEGAL_OPERATION, message);
			send(pk);
			Log.d("sent: illegal operation error with message: " + message);
			throw new TftpAbortException(message);
		} catch (IOException e) {
			throw new TftpAbortException(message);
		}
	}

	private void sendUnkownTidError(InetAddress address, int port) {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					TftpErrorPacket.ErrorType.UNKOWN_TID, "Stop hacking foo!");
			socket.send(pk.generateDatagram(address, port));
			Log.d("sent: unknown tid error to "
					+ addressToString(address, port));
		} catch (Exception e) {
			Log.d("failed to send unkown tid error... oh well, life goes on");
		}
	}

	public void sendFileNotFound(String message) {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					TftpErrorPacket.ErrorType.FILE_NOT_FOUND, message);
			send(pk);
			Log.d("sent: file not found");
		} catch (IOException e) {
			// Ignore
		}
	}

	public void sendDiscFull(String message) {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					TftpErrorPacket.ErrorType.DISC_FULL_OR_ALLOCATION_EXCEEDED, message);
			send(pk);
			Log.d("sent: disc full or allocation exceeded");
		} catch (IOException e) {
			// Ignore
		}
	}

	public void sendAccessViolation(String message) {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					TftpErrorPacket.ErrorType.ACCESS_VIOLATION, message);
			send(pk);
			Log.d("sent: access violation");
		} catch (IOException e) {
			// Ignore
		}
	}

	public void sendFileAlreadyExists(String message) {
		try {
			TftpErrorPacket pk = TftpPacket.createErrorPacket(
					TftpErrorPacket.ErrorType.FILE_ALREADY_EXISTS, message);
			send(pk);
			Log.d("sent: file already exists");
		} catch (IOException e) {
			// Ignore
		}
	}

	public TftpDataPacket receiveData(int blockNumber)
			throws TftpAbortException {
		TftpDataPacket pk = (TftpDataPacket) receiveExpected(
				TftpPacket.Type.DATA, blockNumber);

		// Auto-set remoteTid, for convenience
		if (remoteTid <= 0 && blockNumber == 1) {
			setRemoteTid(inDatagram.getPort());
		}

		Log.d("received: data #" + blockNumber);

		return pk;
	}

	private String addressToString(InetAddress addr, int port) {
		return addr.toString() + ":" + port;
	}

	public TftpAckPacket receiveAck(int blockNumber) throws TftpAbortException {
		TftpAckPacket pk = (TftpAckPacket) receiveExpected(TftpPacket.Type.ACK,
				blockNumber);

		// Auto-set remoteTid, for convenience
		if (remoteTid <= 0 && blockNumber == 0) {
			setRemoteTid(inDatagram.getPort());
		}

		Log.d("received: ack #" + blockNumber);

		return pk;
	}

	private TftpPacket receiveExpected(TftpPacket.Type type, int blockNumber)
			throws TftpAbortException {

		int timeouts = 0;
		try {
			while (true) {
				try {
					TftpPacket pk = receive();

					if (pk.getType() == type) {
						if (pk.getType() == TftpPacket.Type.DATA) {
							TftpDataPacket dataPk = (TftpDataPacket) pk;
							if (dataPk.getBlockNumber() == blockNumber) {
								return dataPk;
							} else if (dataPk.getBlockNumber() < blockNumber) {
								// We received an old data packet, so send
								// corresponding ack
								echoAck(dataPk.getBlockNumber());
							}
						} else if (pk.getType() == TftpPacket.Type.ACK) {
							if (((TftpAckPacket) pk).getBlockNumber() == blockNumber) {
								return pk;
							}
						}
					} else if (pk instanceof TftpErrorPacket) {
						TftpErrorPacket errorPk = (TftpErrorPacket) pk;
						if (errorPk.shouldAbortTransfer()) {
							throw new TftpAbortException(
									errorPk.getErrorMessage());
						} else {
							Log.d("received error of type "
									+ errorPk.getType().toString()
									+ " with message: "
									+ errorPk.getErrorMessage());
						}
					} else if (pk instanceof TftpRequestPacket) {
						throw new TftpAbortException(
								"Received request packet within data transfer connection");
					}
				} catch (SocketTimeoutException e) {
					if (timeouts >= maxResendAttempts) {
						throw new TftpAbortException("Connection timed out");
					}

					timeouts++;
					resendLastPacket();
				}
			}
		} catch (Exception e) {
			throw new TftpAbortException(e.getMessage());
		}
	}

	public void sendData(int blockNumber, byte[] fileData, int fileDataLength)
			throws TftpAbortException {
		try {
			TftpDataPacket pk = TftpPacket.createDataPacket(blockNumber,
					fileData, fileDataLength);
			send(pk, true);
			Log.d("sent: data #" + blockNumber
					+ ((pk.isLastDataPacket()) ? " (last)" : ""));
		} catch (Exception e) {
			throw new TftpAbortException(e.getMessage());
		}
	}
}
