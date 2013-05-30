package sysc3303.project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import sysc3303.project.common.TftpAbortException;
import sysc3303.project.common.TftpAckPacket;
import sysc3303.project.common.TftpConnection;
import sysc3303.project.common.TftpDataPacket;
import sysc3303.project.common.TftpPacket;
import sysc3303.project.common.TftpRequestPacket;

public class Client {
	private InetAddress remoteAddress;
	private int serverRequestPort = 6800;
	private String publicFolder = System.getProperty("user.dir")
			+ "/client_files/";

	public Client() {
		try {
			remoteAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Client c = new Client();
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
				System.out.println("    help: prints this help menu");
				System.out.println("    stop: stop the client");
				System.out
						.println("    pwd: prints out the directory for file transfers");
			} else if (command[0].equals("stop")) {
				System.out.println("Stopping client");
				c.stop();
				return;
			} else if (command[0].equals("pwd")) {
				System.out.println("Current directory: " + c.getPublicFolder());
			} else if (command[0].equals("write") && command.length > 1
					&& command[1].length() > 0) {
				try {
					c.sendFile(command[1]);
				} catch (TftpAbortException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (command[0].equals("read") && command.length > 1
					&& command[1].length() > 0) {
				try {
					c.getFile(command[1]);
				} catch (TftpAbortException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out
						.println("Invalid command. These are the available commands:");
				System.out.println("    help: prints this help menu");
				System.out.println("    stop: stop the client");
				System.out
						.println("    pwd: prints out the directory for file transfers");
			}
		}
	}

	public String getPublicFolder() {
		return publicFolder;
	}

	public void stop() {
		System.out.println("Client is shutting down... goodbye!");
	}

	public void getFile(String filename) throws TftpAbortException {
		try {
			TftpConnection con = new TftpConnection();
			con.setRemoteAddress(remoteAddress);
			con.setRequestPort(serverRequestPort);

			TftpRequestPacket reqPk = TftpPacket.createReadRequest(filename,
					TftpRequestPacket.Mode.OCTET);
			con.sendRequest(reqPk);

			TftpDataPacket pk;

			int blockNumber = 1;
			do {
				pk = con.receiveData(blockNumber);
				con.sendAck(blockNumber);
				blockNumber++;
			} while (!pk.isLastDataPacket());
		} catch (IOException e) {
			throw new TftpAbortException("IOException: " + e.getMessage());
		}
	}

	public void sendFile(String filename) throws TftpAbortException {
		try {
			TftpConnection con = new TftpConnection();
			con.setRemoteAddress(remoteAddress);
			con.setRequestPort(serverRequestPort);

			TftpRequestPacket reqPk = TftpPacket.createWriteRequest(filename,
					TftpRequestPacket.Mode.OCTET);
			con.sendRequest(reqPk);

			int blockNumber = 0;
			byte[] data = new byte[512];

			do {
				con.receiveAck(blockNumber);
				blockNumber++;
				con.sendData(blockNumber, data, data.length);
			} while (data.length == TftpDataPacket.MAX_FILE_DATA_LENGTH);
		} catch (IOException e) {
			throw new TftpAbortException("IOException: " + e.getMessage());
		}

	}
}
