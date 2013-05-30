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
	private int requestPort = 6900;
	private int remoteTid = -1;
	private DatagramPacket inDatagram = TftpPacket.createDatagramForReceiving();
	private DatagramPacket resendDatagram;
	private int maxResendAttempts = 5;
	private int timeoutTime = 10000;

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
		socket.send(resendDatagram);
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
	}

	public void echoAck(int blockNumber) throws IOException {
		send(TftpPacket.createAckPacket(blockNumber));
	}

	private void resendLastPacket() throws IOException {
		socket.send(resendDatagram);
	}

	private TftpPacket receive() throws IOException {
		socket.receive(inDatagram);
		return TftpPacket.createFromDatagram(inDatagram);
	}

	public TftpDataPacket receiveData(int blockNumber) throws IOException,
			TftpAbortException {
		TftpDataPacket pk = (TftpDataPacket) receiveExpected(
				TftpPacket.Type.DATA, blockNumber);

		// Auto-set remoteTid, for convenience
		if (remoteTid < 0 && blockNumber == 1) {
			remoteTid = inDatagram.getPort();
		}

		return pk;
	}

	public TftpAckPacket receiveAck(int blockNumber) throws IOException,
			TftpAbortException {
		TftpAckPacket pk = (TftpAckPacket) receiveExpected(TftpPacket.Type.ACK,
				blockNumber);

		// Auto-set remoteTid, for convenience
		if (remoteTid < 0 && blockNumber == 0) {
			remoteTid = inDatagram.getPort();
		}

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
							sendAck(dataPk.getBlockNumber());
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
	}
}
