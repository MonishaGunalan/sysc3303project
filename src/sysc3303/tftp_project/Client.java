package sysc3303.tftp_project;

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

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class Client {
	protected DatagramSocket socket;
	protected int serverPort = 6900;
	protected RequestPacket.Mode defaultTransferMode = RequestPacket.Mode.ASCII;
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
		Client c = new Client();
		c.connect();

		boolean isDone = false;
		// Perform functions based on the input command from the console
		while (!isDone) {
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
				isDone = true;
				c.finalize();
				break;
			}
			case INVALID: {
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
		byte[] data = null;
		write(filename);
		int port = receiveResponse();
		try {
			data = toByteArray(new File(publicFolder, filename));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (!isWriteDone) {

			// Generate data packets to be sent
			int packetSize = data.length
					- ((blockNumber) * DataPacket.maxDataLength);
			System.out.println(packetSize);
			if (packetSize > DataPacket.maxDataLength)
				packetSize = DataPacket.maxDataLength;
			else
				isWriteDone = true;
			byte[] blockData = new byte[packetSize];
			int offset = (blockNumber) * DataPacket.maxDataLength;
			System.arraycopy(data, offset, blockData, 0, blockData.length);
			blockNumber++;

			sendDataPacket(new DataPacket(blockNumber, blockData,
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
		byte[] data = null;
		read(filename);
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(new File(publicFolder, filename));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		while (!isReadDone) {
			DatagramPacket dp = receiveDataPacket();
			data = dp.getData();
			DataPacket dataPacket = DataPacket.CreateFromBytes(data,
					data.length);
			
			try {
				fs.write(dataPacket.getData());
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (dataPacket.getDataLength() < DataPacket.maxDataLength) {
				isReadDone = true;
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Send Ack 
			sendAckPacket(blockNumber++, dp.getPort());
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
		DatagramPacket dp;
		try {
			dp = Packet.CreateAckPacket(blockNumber).generateDatagram(
					InetAddress.getLocalHost(), port);
			socket.send(dp);
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
		DatagramPacket dp = null;
		try {
			byte data[] = new byte[DataPacket.maxLength];
			dp = new DatagramPacket(data, data.length);
			socket.receive(dp);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return dp;
	}

	/*
	 * Send the DataPacket to the server 
	 * Wait for ack packet from the server and receive it.
	 * 
	 * @param Data Packet
	 * @param destination port
	 * @return port# associated with Ack packet from the source
	 */
	public int sendDataPacket(DataPacket packet, int port) {

		// Log to terminal
		System.out.println("Client sending block#: " + packet.getBlockNumber());
		try {
			// Create the packet
			DatagramPacket dp = packet.generateDatagram(
					InetAddress.getLocalHost(), port);

			// Send the packet
			socket.send(dp);

			// Receive response
			port = receiveResponse();
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
	private byte[] toByteArray(File file) throws IOException {
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
	 * Disconnect the socket
	 */
	public void disconnect() {
		socket.disconnect();
	}
	
	/*
	 * Create Read request packet and send the request to the server
	 */
	public void read(String filename) {
		sendRequest(RequestPacket.CreateReadRequest(filename));
	}
	
	/*
	 * Create Write request packet and send the request to the server
	 */
	public void write(String filename) {
		sendRequest(RequestPacket.CreateWriteRequest(filename));
	}

	/*
	 * Send the Request to the server via the error simulator
	 */
	public void sendRequest(RequestPacket rq) {
		try {
			if (null == rq.mode) {
				rq.mode = defaultTransferMode;
			}

			// Convert the request into a byte array
			byte data[] = rq.generateData();
			String dataStr = rq.generateString();

			// Log to terminal
			System.out.println("Client sending (bytes): " + data);
			System.out.println("Client sending (string): " + dataStr);

			// Create the packet
			DatagramPacket dp = new DatagramPacket(data, data.length,
					InetAddress.getLocalHost(), serverPort);

			// Send the packet
			socket.send(dp);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Receive the Ack packet
	 */
	protected int receiveResponse() {
		int port = -1;
		try {
			byte data[] = new byte[100];
			DatagramPacket dp = new DatagramPacket(data, data.length);
			socket.receive(dp);
			data = dp.getData();
			int dataLength = dp.getLength();

			// Log receipt
			System.out.println("Client received (bytes): " + dp.getData());
			System.out.print("Client received (string): ");
			for (int i = 0; i < dataLength; i++) {
				System.out.print(data[i]);
			}
			port = dp.getPort();
			AckPacket ack = AckPacket.CreateFromBytes(data, dataLength);
			System.out.println("\nClient: Received ack block:"
					+ ack.getBlockNumber());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

	public void finalize() {
		disconnect();
	}
}