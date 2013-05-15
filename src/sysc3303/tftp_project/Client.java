package sysc3303.tftp_project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * @author Korey
 * 
 */
public class Client {
	protected DatagramSocket socket;
	protected int serverPort = 68;
	protected RequestPacket.Mode defaultTransferMode = RequestPacket.Mode.ASCII;

	private boolean isDone = false;
	private String filename;

	private enum CMD {
		READ, WRITE, STOP
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO create a thread for user input or client as a separate thread
		Client c = new Client();
		c.connect();

		// Get the input from the user
		// TODO make isDone a local variable
		while (!c.isDone) {
			CMD cmd = c.getInput();
			switch (cmd) {
				case READ: {
					c.read(c.filename);
					break;
				}
				case WRITE: {
					c.write(c.filename);
					break;
				}
				case STOP: {
					c.isDone = true;
					break;
				}
			}
		}

		// Test
		c.read("myfile.txt");
		c.write("myfile.txt");
		c.read("myfile.txt");
		c.sendInvalidRequest("myfile.txt");
		c.read("myfile.txt");
		c.write("myfile.txt");
		c.sendInvalidRequest("myfile.txt");
		c.write("myfile.txt");
		c.read("myfile.txt");
		c.write("myfile.txt");
	}

	public CMD getInput() {
		CMD cmd = null;
		Scanner in = new Scanner(System.in);
		System.out.println("Please enter the command: ");
		String inputCmd;
		while (in.hasNext()) {
			inputCmd = in.next();
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
				StringTokenizer st = new StringTokenizer(inputCmd, " ");
				if (st.countTokens() == 2) {
					inputCmd = (String) st.nextElement();
					filename = (String) st.nextElement();
					if (inputCmd.equalsIgnoreCase("read")) {
						cmd = CMD.READ;
					} else if (inputCmd.equalsIgnoreCase("write")) {
						cmd = CMD.WRITE;
					}
				} else {
					System.out.println("Invalid command, Please try again");
				}

			}// end if
		}// end while
		return cmd;
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

	public void read(String filename) {
		sendRequest(RequestPacket.CreateReadRequest(filename));
	}

	public void write(String filename) {
		sendRequest(RequestPacket.CreateWriteRequest(filename));
	}

	public void sendInvalidRequest(String filename) {
		sendRequest(new RequestPacket(filename, RequestPacket.Action.INVALID));
	}

	public void sendRequest(RequestPacket rq) {
		try {
			if (null == rq.mode) {
				rq.mode = defaultTransferMode;
			}

			// Convert the request into a byte array
			byte data[] = rq.generatePacketData();
			String dataStr = rq.generatePacketString();

			// Log to terminal
			System.out.println("Client sending (bytes): " + data);
			System.out.println("Client sending (string): " + dataStr);

			// Create the packet
			DatagramPacket dp = new DatagramPacket(data, data.length,
					InetAddress.getLocalHost(), serverPort);

			// Send the packet
			socket.send(dp);

			// Receive response
			receiveResponse();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void receiveResponse() {
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
			System.out.println();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finalize() {
		disconnect();
	}
}