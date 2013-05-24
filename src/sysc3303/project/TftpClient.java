package sysc3303.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.StringTokenizer;

import sysc3303.project.packets.InvalidPacketException;
import sysc3303.project.packets.TftpAckPacket;
import sysc3303.project.packets.TftpDataPacket;
import sysc3303.project.packets.TftpPacket;
import sysc3303.project.packets.TftpRequestPacket;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class TftpClient {
	protected DatagramSocket socket;
	protected int serverPort = 68;
	protected TftpRequestPacket.Mode defaultTransferMode = TftpRequestPacket.Mode.ASCII;
	protected String publicFolder = System.getProperty("user.dir")
			+ "/client_files/";

	private String filename;
	private enum CMD {
		READ, WRITE, STOP, INVALID, HELP
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TftpClient c = new TftpClient();
		c.connect();

		
		// Perform functions based on the input command from the console
		while (true) {
			CMD cmd = c.getInput();
			switch (cmd) {
			case HELP:{
				break;
			}
			case READ: {
				c.readReq();
				break;
			}
			case WRITE: {
				c.writeReq();
				break;
			}
			case STOP: {
				c.disconnect();
				System.out.println("Exiting Program");
				System.exit(0);
				break;
			}
			case INVALID: {
				System.out.println("Entered an Invalid command");
				System.out.println("List of  Valid Terminal Commands:");
				System.out
						.println("read <filename> : send read request to server. @param <filename>: full path of the file name.");
				System.out
						.println("write <filename> : send write request to server. @param <filename>: full path of the file name.");
				System.out
						.println("stop : close the client (waits until current transmissions are done.");
				break;
			}
			}
		}

	}

	/*
	 * Create a Write Request Packet and the send the request packet
	 * Generate bytes of data from the file into data packets and send the data packet
	 * Receive the ack packet after every data packet sent.
	 */
	public void writeReq() {
		boolean isWriteDone = false;
		int blockNumber = 0;
		byte[] fileData = null;
		write(filename);
		int port = receiveAckPacket();
		try {
			fileData = readDataFromFile(new File(publicFolder, filename));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (!isWriteDone) {
			// Generate data packets to be sent
			int packetSize = fileData.length
					- ((blockNumber) * TftpDataPacket.getMaxDataLength());
			System.out.println(packetSize);
			if (packetSize >TftpDataPacket.getMaxDataLength())
				packetSize = TftpDataPacket.getMaxDataLength();
			else
				isWriteDone = true;
			byte[] blockData = new byte[packetSize];
			int offset = (blockNumber) * TftpDataPacket.getMaxDataLength();
			System.arraycopy(fileData, offset, blockData, 0, blockData.length);
			blockNumber++;
			
			sendDataPacket(TftpPacket.createDataPacket(blockNumber, blockData,
					blockData.length), port);
		}
	}

	/*
	 * Create a Read Request Packet and the send the request packet
	 * Receive data packets from the server, extract the data.
	 * Write the extracted data into the file.
	 * Send ack packet to the server after every data packet received.
	 */
	public void readReq() {
		boolean isReadDone = false;
		int blockNumber = 1;
		read(filename);
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(new File(publicFolder, filename));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		while (!isReadDone) {
			DatagramPacket datagramPacket = receiveDataPacket();
			TftpDataPacket dataPacket =(TftpDataPacket) TftpPacket.createFromDatagram(datagramPacket);
			try {
				fs.write(dataPacket.getFileData());
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Length of data packet: " +dataPacket.getFileData().length);
			if (dataPacket.getFileData().length < TftpDataPacket.getMaxDataLength()) {
				isReadDone = true;
				System.out.println("Last Data packet");
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Send Ack 
			System.out.println("Sending Ack from client, block#" + blockNumber);
			sendAckPacket(blockNumber++, datagramPacket.getPort());
		}
	}

	/*
	 * Get the input command and the file name from the user
	 * 
	 * @return Input Command
	 */
	public CMD getInput() {
		CMD cmd = null;
		Scanner in = new Scanner(System.in);
		System.out.println();
		System.out.println("Please enter the command: ");
		
		String inputCmd;
		inputCmd = in.nextLine();
		if (inputCmd.equalsIgnoreCase("help")) {
			System.out.println("List of Terminal Commands");
			System.out
					.println("read <filename> : send read request to server. @param <filename>: full path of the file name.");
			System.out
					.println("write <filename> : send write request to server. @param <filename>: full path of the file name.");
			System.out
					.println("stop : close the client (waits until current transmissions are done.");
			
			cmd = CMD.HELP;
		} else if (inputCmd.equalsIgnoreCase("stop")) {
			cmd = CMD.STOP;
		} else {
			StringTokenizer st = new StringTokenizer(inputCmd);
			if (st.countTokens() == 2) {
				inputCmd = (String) st.nextToken();
				filename = (String) st.nextToken();
				if (inputCmd.equalsIgnoreCase("read")) {
					cmd = CMD.READ;
				} else if (inputCmd.equalsIgnoreCase("write")) {
					cmd = CMD.WRITE;
				}
			} else {
				System.out.println(st.countTokens());
				cmd = CMD.INVALID;
				System.out.println("Invalid command, Please try again");
			}

		}// end if
		return cmd;
	}

	/*
	 * Create Ack Packet and send 
	 * 
	 * @param blockNumber
	 * @param destination port
	 */
	public void sendAckPacket(int blockNumber, int port) {
		DatagramPacket datagramPacket;
		try {
			datagramPacket = TftpPacket.createAckPacket(blockNumber).generateDatagram(
					InetAddress.getLocalHost(), port);
			socket.send(datagramPacket);
		} catch (InvalidPacketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * Receive the data packet 
	 * 
	 * @return: Data Packet
	 */
	public DatagramPacket receiveDataPacket() {
		DatagramPacket datagramPacket = null;
		try {
			datagramPacket = TftpPacket.createDatagramForReceiving();
			socket.receive(datagramPacket);
			TftpDataPacket dataPacket =(TftpDataPacket) TftpPacket.createFromDatagram(datagramPacket);
			System.out.println("Received data packet from server of block " + dataPacket.getBlockNumber());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return datagramPacket;
	}

	/*
	 * Send the DataPacket to the server 
	 * Wait for ack packet from the server and receive it.
	 * 
	 * @param Data Packet
	 * @param destination port
	 * @return port# associated with Ack packet from the source
	 */
	public int sendDataPacket(TftpDataPacket packet, int port) {

		// Log to terminal
		System.out.println("Client sending block#: " + packet.getBlockNumber());
		try {
			// Create the packet
			DatagramPacket datagramPacket = packet.generateDatagram(
					InetAddress.getLocalHost(), port);

			// Send the packet
			socket.send(datagramPacket);

			// Receive response
			port = receiveAckPacket();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return port;
	}

	
	/*
	 * Read the data from the file and store in in a byte Array
	 * @param File
	 * @return byte array
	 * 
	 */
	private byte[] readDataFromFile(File file) throws IOException {
		RandomAccessFile f = new RandomAccessFile(file.getAbsolutePath(), "r");
		byte[] b = new byte[(int) f.length()];
		f.read(b);
		f.close();
		return b;
	}

	/*
	 * Create a new datagram socket 
	 */
	public void connect() {
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	

	/*
	 * Create Read request packet and send the request to the server
	 */
	public void read(String filename) {
		sendRequest(TftpPacket.createReadRequest(filename,defaultTransferMode));
	}
	
	/*
	 * Create Write request packet and send the request to the server
	 */
	public void write(String filename) {
		sendRequest(TftpPacket.createWriteRequest(filename, defaultTransferMode));
	}

	/*
	 * Send the Request to the server via the error simulator
	 */
	public void sendRequest(TftpRequestPacket rq) {
		try {

			// Convert the request into a byte array
			byte data[] = rq.generateData();
			String dataStr = rq.toString();

			// Log to terminal
			System.out.println("Client sending (bytes): " + data);
			System.out.println("Client sending (string): " + dataStr);

			// Create the packet
			DatagramPacket datagramPacket = rq.generateDatagram(InetAddress.getLocalHost(), serverPort);

			// Send the packet
			socket.send(datagramPacket);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Receive the Ack packet
	 */
	protected int receiveAckPacket() {
		int port = -1;
		try {
			DatagramPacket datagramPacket = TftpPacket.createDatagramForReceiving();
			socket.receive(datagramPacket);
			byte[] packetData = datagramPacket.getData();
			int packetLength = datagramPacket.getLength();

			// Log receipt
			System.out.println("Client received (bytes): " + packetData);
			System.out.print("Client received (string): ");
			for (int i = 0; i < packetLength; i++) {
				System.out.print(packetData[i]);
			}
			port = datagramPacket.getPort();
			TftpAckPacket ack = (TftpAckPacket)TftpPacket.createFromDatagram(datagramPacket);
			System.out.println("\nClient: Received ack block:"
					+ ack.getBlockNumber());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}
	
	/*
	 * Disconnect the socket
	 */
	public void disconnect() {
		socket.disconnect();
	}
	

}