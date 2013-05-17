package sysc3303.tftp_project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
		READ, WRITE, STOP, INVALID
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Client c = new Client();
		c.connect();

		boolean isDone = false;
		// Get the input from the user
		while (!isDone) {
			CMD cmd = c.getInput();
			switch (cmd) {
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
				break;
			}
			case INVALID: {
				break;
			}
			}
		}

	}


		public void writeReq() {
			boolean isWriteDone = false;
			int blockNumber = 0;
			byte[] data = null;
			int port = write(filename);
			try {
				data = toByteArray(new File(publicFolder,
						filename));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			while (!isWriteDone) {
				blockNumber++;
				// Generate data packets to be sent
				int packetSize = data.length
						- ((blockNumber -1) * DataPacket.maxDataLength);
				System.out.println(packetSize);
				if (packetSize > DataPacket.maxDataLength)
					packetSize = DataPacket.maxDataLength;
				else
					isWriteDone = true;
				byte[] blockData = new byte[packetSize];
				int offset = (blockNumber - 1) * DataPacket.maxDataLength;
				System.arraycopy(data, offset, blockData, 0, blockData.length);
				sendDataPacket(new DataPacket(blockNumber, blockData,
						blockData.length), port);
			}
		}
	


		public void readReq() {
			boolean isReadDone = false;
			int blockNumber = 1;
			byte[] data = null;
			int port = read(filename);
			FileOutputStream fs = null;
			try {
				fs  = new FileOutputStream(new File(
						publicFolder, filename));

				data = toByteArray(new File(publicFolder,
						filename));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			while (!isReadDone) {
				DatagramPacket dp = receiveDataPacket(port);
				data = dp.getData();
				DataPacket dataPacket = DataPacket.CreateFromBytes(data, data.length);
				try {
					fs.write(dataPacket.getData());
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(dataPacket.getDataLength() < DataPacket.maxDataLength){
					isReadDone = true;
					try {
						fs.flush();
						fs.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//Send Ack
				sendAckPacket(blockNumber++, dp.getPort());
			}
		}
	

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
					.println("stop : close the client (waits until current transmissions are done");
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

	public void sendAckPacket(int blockNumber, int port) {
		DatagramPacket dp;
		try {
			dp = Packet.CreateAckPacket(blockNumber)
					.generateDatagram(InetAddress.getLocalHost(), port);
			socket.send(dp);
		} catch (InvalidPacketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public DatagramPacket receiveDataPacket(int port) {
		DatagramPacket dp = null;
		try {
			byte data[] = new byte[DataPacket.maxDataLength + DataPacket.headerLength];
			dp = new DatagramPacket(data, data.length);
			socket.receive(dp);
			data = dp.getData();
			port = dp.getPort();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return dp;
	}

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

	private byte[] toByteArray(File file) throws FileNotFoundException,
			IOException {
		int length = (int) file.length();
		byte[] array = new byte[length];
		InputStream in = new FileInputStream(file);
		int offset = 0;
		while (offset < length) {
			offset += in.read(array, offset, (length - offset));
		}
		in.close();
		System.out.println("read size " + array.length);
		return array;
	}

	public void connect() {
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void disconnect() {
		socket.disconnect();
	}

	public int read(String filename) {
		return sendRequest(RequestPacket.CreateReadRequest(filename));
	}

	public int write(String filename) {
		return sendRequest(RequestPacket.CreateWriteRequest(filename));
	}

	// public void sendInvalidRequest(String filename) {
	// sendRequest(new RequestPacket(filename, RequestPacket.Action.INVALID));
	// }

	public int sendRequest(RequestPacket rq) {
		int port = -1;
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

			// Receive response
			port = receiveResponse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

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