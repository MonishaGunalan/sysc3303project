package sysc3303.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import sysc3303.project.common.TftpAbortException;
import sysc3303.project.common.TftpConnection;
import sysc3303.project.common.TftpDataPacket;
import sysc3303.project.common.TftpPacket;
import sysc3303.project.common.TftpRequestPacket;

public class TftpClient {
	private InetAddress remoteAddress;
	private int serverRequestPort = 6800;
	private String publicFolder = System.getProperty("user.dir")
			+ "/client_files/";
	private TftpConnection conn;

	public TftpClient() {
		try {
			remoteAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.out.println("Failed to get server address");
		}
	}

	public static void main(String[] args) {
		TftpClient c = new TftpClient();
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.print("Command: ");
			String cmdLine = scanner.nextLine().toLowerCase();
			String[] command = cmdLine.split("\\s+");

			// Continue if blank line was passed
			if (command.length == 0 || command[0].length() == 0) {
				continue;
			}

			if (command[0].equals("help")) {
				System.out.println("Available commands:");
				printCommandOptions();
			} else if (command[0].equals("stop")) {
				System.out.println("Stopping client");
				c.stop();
				return;
			} else if (command[0].equals("pwd")) {
				System.out.println("Current directory: " + c.getPublicFolder());
			} else if ((command[0].equals("read") || command[0].equals("get"))
					&& command.length > 1 && command[1].length() > 0) {
				c.getFile(command[1]);
			} else if ((command[0].equals("write") || command[0].equals("send"))
					&& command.length > 1 && command[1].length() > 0) {
				c.sendFile(command[1]);
			} else {
				System.out
						.println("Invalid command. These are the available commands:");
				printCommandOptions();
			}
		}
	}

	static private void printCommandOptions() {
		System.out.println("    help: prints this help menu");
		System.out.println("    stop: stop the client");
		System.out.println("    read filename: read file from server");
		System.out.println("    get filename: alias for read filename");
		System.out.println("    write filename: write file to server");
		System.out.println("    send filename: alias for write filename");
		System.out
				.println("    pwd: prints out the directory for file transfers");
	}

	public String getPublicFolder() {
		return publicFolder;
	}

	public void stop() {
		System.out.println("Client is shutting down... goodbye!");
	}

	public void connect(InetAddress remoteAddress, int serverRequestPort)
			throws SocketException {
		try {
			conn = new TftpConnection();
			conn.setRemoteAddress(remoteAddress);
			conn.setRequestPort(serverRequestPort);
			System.out.println("Connected to " + remoteAddress.toString() + ":"
					+ serverRequestPort);
			
		} catch (SocketException e) {
			System.out.println("Failed to connect to "
					+ remoteAddress.toString() + ":" + serverRequestPort);
			throw e;
		}
	}

	public void getFile(String filename) {
		String filePath = getPublicFolder() + filename;
		try {
			// Check write permissions
			File file = new File(filePath);
			if (file.exists() && !file.canWrite()) {
				System.out.println("Cannot overwrite file: " + filename);
				return;
			}

			// Connect to the server
			try {
				connect(remoteAddress, serverRequestPort);
			} catch (SocketException e) {
				return;
			}

			FileOutputStream fs = new FileOutputStream(filePath);

			TftpRequestPacket reqPk = TftpPacket.createReadRequest(filename,
					TftpRequestPacket.Mode.OCTET);
			conn.sendRequest(reqPk);

			TftpDataPacket pk;

			int blockNumber = 1;
			do {
				pk = conn.receiveData(blockNumber);
				try {
					fs.write(pk.getFileData());
					fs.getFD().sync();
				} catch (SyncFailedException e) {
					file.delete();
					fs.close();
					conn.sendDiscFull("Failed to sync with disc, likely is full");
					return;
				}
				conn.sendAck(blockNumber);
				blockNumber++;
			} while (!pk.isLastDataPacket());
			fs.close();
		} catch (TftpAbortException e) {
			new File(filePath).delete();
			System.out.println("Failed to get " + filename + ": "
					+ e.getMessage());
		} catch (IOException e) {
			new File(filePath).delete();
			System.out.println("IOException: failed to get " + filename + ": "
					+ e.getMessage());
		}
	}

	public void sendFile(String filename) {
		try {
			String filePath = getPublicFolder() + filename;

			// Check that file exists
			File file = new File(filePath);
			if (!file.exists()) {
				System.out.println("Cannot find file: " + filename);
				return;
			}

			// Check read permissions
			if (!file.canRead()) {
				System.out.println("Cannot read file: " + filename);
				return;
			}

			// Connect to the server
			try {
				connect(remoteAddress, serverRequestPort);
			} catch (SocketException e) {
				return;
			}

			// Open input stream
			FileInputStream fs = new FileInputStream(file);

			// Send request
			TftpRequestPacket reqPk = TftpPacket.createWriteRequest(filename,
					TftpRequestPacket.Mode.OCTET);
			conn.sendRequest(reqPk);

			int blockNumber = 0;
			byte[] data = new byte[512];
			int bytesRead = 0;

			do {
				conn.receiveAck(blockNumber);
				blockNumber++;

				bytesRead = fs.read(data);

				// Special case when file size is multiple of 512 bytes
				if (bytesRead == -1) {
					bytesRead = 0;
					data = new byte[0];
				}

				conn.sendData(blockNumber, data, bytesRead);
			} while (bytesRead == TftpDataPacket.MAX_FILE_DATA_LENGTH);

			// Wait for final ack
			conn.receiveAck(blockNumber);

			fs.close();
		} catch (TftpAbortException e) {
			System.out.println("Failed to send " + filename + ": "
					+ e.getMessage());
		} catch (IOException e) {
			System.out.println("IOException: failed to send " + filename + ": "
					+ e.getMessage());
		}
	}
}
