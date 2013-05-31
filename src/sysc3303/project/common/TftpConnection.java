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
	private int requestPort = 68;
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
	}

	public void setRemoteAddress(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public void setRemoteTid(int remoteTid) {
		this.remoteTid = remoteTid;
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
		}

		socket.send(dp);
	}

	public void sendAck(int blockNumber) throws IOException {
		send(TftpPacket.createAckPacket(blockNumber), true);
		Log.d("sent: ack #" + blockNumber);
	}

	private void echoAck(int blockNumber) throws IOException {
		send(TftpPacket.createAckPacket(blockNumber));
		Log.d("sent: ack #" + blockNumber + " in response to duplicate data");
	}

	private void resendLastPacket() throws TftpAbortException {
		if (resendDatagram == null) {
			throw new TftpAbortException("Cannot resend last packet");
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

			if ((remoteTid > 0 && inDatagram.getPort() != remoteTid)
					|| remoteAddress != inDatagram.getAddress()) {
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
			Log.d("sent: illegal operation error");
			throw new TftpAbortException(message);
		} catch (IOException e) {
			throw new TftpAbortException(message);
		}
	}

	private void sendUnkownTidError(InetAddress address, int port)
			throws IOException {
		TftpErrorPacket pk = TftpPacket.createErrorPacket(
				TftpErrorPacket.ErrorType.UNKOWN_TID, "Stop hacking foo!");
		socket.send(pk.generateDatagram(address, port));
		Log.d("sent: unknown tid error");
	}

	public TftpDataPacket receiveData(int blockNumber) throws IOException,
			TftpAbortException {
		TftpDataPacket pk = (TftpDataPacket) receiveExpected(
				TftpPacket.Type.DATA, blockNumber);

		// Auto-set remoteTid, for convenience
		if (remoteTid <= 0 && blockNumber == 1) {
			remoteTid = inDatagram.getPort();
			Log.d("setting remote tid: " + remoteTid);
		}

		Log.d("received: data #" + blockNumber);

		return pk;
	}

	public TftpAckPacket receiveAck(int blockNumber) throws IOException,
			TftpAbortException {
		TftpAckPacket pk = (TftpAckPacket) receiveExpected(TftpPacket.Type.ACK,
				blockNumber);

		// Auto-set remoteTid, for convenience
		if (remoteTid <= 0 && blockNumber == 0) {
			remoteTid = inDatagram.getPort();
			Log.d("setting remote tid: " + remoteTid);
		}

		Log.d("received: ack #" + blockNumber);

		return pk;
	}

	private TftpPacket receiveExpected(TftpPacket.Type type, int blockNumber)
			throws IOException, TftpAbortException {

		int timeouts = 0;
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
						throw new TftpAbortException(errorPk.getErrorMessage());
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
	}

	public void sendData(int blockNumber, byte[] fileData, int fileDataLength)
			throws IOException {
		TftpDataPacket pk = TftpPacket.createDataPacket(blockNumber, fileData,
				fileDataLength);
		send(pk, true);
		Log.d("sent: data #" + blockNumber);
	}
}
