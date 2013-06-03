package sysc3303.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import sysc3303.project.common.Log;
import sysc3303.project.common.TftpAbortException;
import sysc3303.project.common.TftpConnection;
import sysc3303.project.common.TftpDataPacket;
import sysc3303.project.common.TftpRequestPacket;

class TftpServerFileTransfer extends Thread {
	private TftpConnection conn;
	private String filename;
	private String filePath;
	private boolean isReadRequest;
	private TftpServer server;

	public TftpServerFileTransfer(TftpServer server, TftpRequestPacket packet,
			InetAddress toAddress, int toPort) {
		try {
			this.server = server;
			conn = new TftpConnection();
			conn.setRemoteAddress(toAddress);
			conn.setRemoteTid(toPort);
			this.filename = packet.getFilename();
			this.filePath = server.getPublicFolder() + filename;
			this.isReadRequest = packet.isReadRequest();
		} catch (SocketException e) {
			System.out.println("Failed to open socket for transfer for "
					+ filename);
		}
	}

	@Override
	public void run() {
		server.incrementThreadCount();

		if (isReadRequest) {
			this.sendFileToClient();
		} else {
			this.receiveFileFromClient();
		}

		server.decrementThreadCount();
	}

	public void sendFileToClient() {
		int blockNumber = 1;

		FileInputStream fs;
		try {
			fs = new FileInputStream(filePath);
			int bytesRead;

			// Read file in 512 byte chunks
			byte[] data = new byte[TftpDataPacket.MAX_FILE_DATA_LENGTH];

			do {
				bytesRead = fs.read(data);

				// Special case when file size is multiple of 512 bytes
				if (bytesRead == -1) {
					bytesRead = 0;
					data = new byte[0];
				}

				// Send data, receive ack
				try {
					conn.sendData(blockNumber, data, bytesRead);
					conn.receiveAck(blockNumber);
				} catch (TftpAbortException e) {
					Log.d("Aborting transfer of " + filename + ": " + e.getMessage());
					fs.close();
					return;
				}

				blockNumber++;
			} while (bytesRead == TftpDataPacket.MAX_FILE_DATA_LENGTH);
			fs.close();
		} catch (FileNotFoundException e1) {
			Log.d("File not found: " + filename);
			return;
		} catch (IOException e) {
			Log.d("IOException: " + e.getMessage());
			return;
		}
	}

	public void receiveFileFromClient() {
		try {
			FileOutputStream fs = new FileOutputStream(filePath);
			int blockNumber = 0;
			TftpDataPacket dataPk;

			do {
				try {
					conn.sendAck(blockNumber);
					dataPk = conn.receiveData(++blockNumber);
					fs.write(dataPk.getFileData());
				} catch (TftpAbortException e) {
					Log.d("Aborting transfer of " + filename + ": " + e.getMessage());
					fs.close();
					new File(filename).delete();
					return;
				}
			} while (!dataPk.isLastDataPacket());

			// Send final ack packet
			try {
				conn.sendAck(blockNumber);
			} catch (Exception e) {
				// no worries, this ack was just a courtesy
			}

			fs.close();
		} catch (FileNotFoundException e) {
			new File(filePath).delete();
			System.out.println("Cannot write to: " + filename);
			return;
		} catch (IOException e) {
			new File(filePath).delete();
			System.out.println("IOException with file: " + filename);
			e.printStackTrace();
			return;
		}
	}
}
