package sysc3303.project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import sysc3303.project.common.TftpErrorPacket;
import sysc3303.project.common.TftpErrorPacket.ErrorType;
import sysc3303.project.common.TftpPacket;
import sysc3303.project.common.TftpRequestPacket;

class TftpRequestListener extends Thread {
	protected DatagramSocket socket;
	private int boundPort;
	private TftpServer server;

	public TftpRequestListener(TftpServer server, int boundPort) {
		this.server = server;
		this.boundPort = boundPort;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	@Override
	public void run() {
		server.incrementThreadCount();
		try {
			socket = new DatagramSocket(boundPort);
		} catch (SocketException e) {
			System.out.printf("Failed to bind to port %d%n", boundPort);
			System.exit(1);
		}

		try {
			while (!socket.isClosed()) {
				DatagramPacket dp = TftpPacket.createDatagramForReceiving();
				socket.receive(dp);
				try {
					TftpPacket packet = TftpPacket.createFromDatagram(dp);
					if (packet instanceof TftpRequestPacket) {
						TftpServerFileTransfer tt = server.newTransferThread(
								(TftpRequestPacket) packet, dp.getAddress(),
								dp.getPort());
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
							dp = errorPacket.generateDatagram(dp.getAddress(),
									dp.getPort());
							errorSocket.send(dp);
							errorSocket.close();
						}
					}
				} catch (IllegalArgumentException e) {
					// We got an invalid packet
					// Open new socket and send error packet response
					DatagramSocket errorSocket = new DatagramSocket();
					System.out
							.println("\nServer received invalid request packet");
					TftpErrorPacket errorPacket = TftpPacket.createErrorPacket(
							ErrorType.ILLEGAL_OPERATION, e.getMessage());
					dp = errorPacket.generateDatagram(dp.getAddress(),
							dp.getPort());
					errorSocket.send(dp);
					errorSocket.close();
				}
			}
		} catch (IOException e) {
			// Ignore, we are likely just stopping
		}

		socket.disconnect();
		System.out.println("RequestListenerThread has stopped.");
		server.decrementThreadCount();
	}
}
