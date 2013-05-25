package sysc3303.project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

import sysc3303.project.packets.TftpAckPacket;
import sysc3303.project.packets.TftpDataPacket;
import sysc3303.project.packets.TftpPacket;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */
public class TftpErrorSimulator {

	private enum ErrorCommands {
		NORMAL("normal"), 
		ERROR_CHANGE_OPCODE("mode 0"), 
		ERROR_REMOVE_FILENAME_DELIMITER("mode 1"),
		ERROR_REMOVE_MODE_DELIMITER("mode 2"),
		ERROR_MODIFY_MODE("mode 3"),
		ERROR_APPEND_DATAPACKET("mode 4"),
		ERROR_SHRINK_PACKET("mode 5"),
		ERROR_APPEND_ACK("mode 6"),
		ERROR_REMOVE_FILENAME("mode 7"),
		ERROR_INVALID_TID("mode 8");
		

		/**
		 * @param text
		 */
		private ErrorCommands(final String text) {
			this.text = text;
		}

		private final String text;

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return text;
		}
	}


	protected InetAddress serverAddress;
	protected int serverRequestPort = 69;
	protected int clientRequestPort = 68;
	protected int threadCount = 0;
	protected boolean stopping = false;
	protected RequestReceiveThread requestReceive;

	private ErrorCommands errorCommand = ErrorCommands.NORMAL;

	/**
	 * Constructor
	 */
	public TftpErrorSimulator() {
		try {
			serverAddress = InetAddress.getLocalHost();
			requestReceive = new RequestReceiveThread();
			requestReceive.start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TftpErrorSimulator errorSimulator = new TftpErrorSimulator();
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.print("Command: ");
			String command = scanner.nextLine().toLowerCase();

			// Continue if blank line was passed
			if (command.length() == 0) {
				continue;
			}

			if (command.equals("help")) {
				System.out.println("Available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the error simulator (when current transfers finish)");
				System.out.println("mode 0 : change packet opcode ");
				System.out
						.println("mode 1: Remove the byte '0' after the file name");
				System.out
				.println("mode 2: Remove the byte '0' after the mode");
				System.out
				.println("mode 3: Modify the string mode");
				System.out
				.println("mode 4: Modify the data packet to be larger than 516 bytes");
				System.out
				.println("mode 5: Modify the packet size to be smaller than 4 bytes");
				System.out
				.println("mode 6: Modify the ack packet to be larger than 4 bytes");
				System.out
				.println("mode 7: Remove File name from the packet");
				System.out
				.println("mode 8: Change the port number - Invalid TID");
				
				System.out
						.println("stop : close the client (waits until current transmissions are done.");
			} else if (command.equals("stop")) {
				System.out
						.println("Stopping simulator (when current transfers finish)");
				errorSimulator.stop();
			}else if (command.equalsIgnoreCase(ErrorCommands.NORMAL.toString())) {
				errorSimulator.errorCommand = ErrorCommands.NORMAL;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_CHANGE_OPCODE.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_CHANGE_OPCODE;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_REMOVE_FILENAME_DELIMITER.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_REMOVE_FILENAME_DELIMITER;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_REMOVE_MODE_DELIMITER.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_REMOVE_MODE_DELIMITER;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_MODIFY_MODE.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_MODIFY_MODE;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_APPEND_DATAPACKET.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_APPEND_DATAPACKET;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_SHRINK_PACKET.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_SHRINK_PACKET;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_APPEND_ACK.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_APPEND_ACK;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_REMOVE_FILENAME.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_REMOVE_FILENAME;
			}else if (command.equalsIgnoreCase(ErrorCommands.ERROR_INVALID_TID.toString())) {
				errorSimulator.errorCommand = ErrorCommands.ERROR_INVALID_TID;				
			}else {
				System.out
						.println("Invalid command. These are the available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the error simulator (when current transfers finish)");
				System.out.println("mode 0 : change packet opcode ");
				System.out
						.println("mode 1: Remove the byte '0' after the file name");
				System.out
				.println("mode 2: Remove the byte '0' after the mode");
				System.out
				.println("mode 3: Modify the string mode");
				System.out
				.println("mode 4: Modify the data packet to be larger than 516 bytes");
				System.out
				.println("mode 5: Modify the packet size to be smaller than 4 bytes");
				System.out
				.println("mode 6: Modify the ack packet to be larger than 4 bytes");
				System.out
				.println("mode 7: Remove File name from the packet");
				System.out
				.println("mode 8: Change the port number - Invalid TID");
				
			}
		}
	
	}

	synchronized public void incrementThreadCount() {
		threadCount++;
	}

	synchronized public void decrementThreadCount() {
		threadCount--;
	}

	synchronized public int getThreadCount() {
		return threadCount;
	}

	public void stop() {
		requestReceive.getSocket().close();

		// wait for threads to finish
		while (getThreadCount() > 0) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// Ignore errors
			}
		}
		System.out.println("Error simulator closed.");
		System.exit(0);
	}

	private class RequestReceiveThread extends Thread {
		private DatagramSocket socket;

		public RequestReceiveThread() {
			try {
				socket = new DatagramSocket(clientRequestPort);
			} catch (SocketException e) {
				System.out.println("Count not bind to port: "
						+ clientRequestPort);
				System.exit(1);
			}
		}

		public void run() {
			try {
				incrementThreadCount();

				while (!socket.isClosed()) {
					DatagramPacket dp = TftpPacket.createDatagramForReceiving();
					socket.receive(dp);
					new ForwardThread(dp).start();
				}
			} catch (IOException e) {
				// Probably just closing the thread down
			}

			decrementThreadCount();
		}

		public DatagramSocket getSocket() {
			return socket;
		}
	}

	private class ForwardThread extends Thread {
		private DatagramSocket socket;
		private int timeoutMs = 10000; // 10 second receive timeout
		private DatagramPacket requestPacket;
		private InetAddress clientAddress;
		private int clientPort, serverPort;

		ForwardThread(DatagramPacket requestPacket) {
			this.requestPacket = requestPacket;
		}

		public void run() {
			try {
				incrementThreadCount();

				socket = new DatagramSocket();
				socket.setSoTimeout(timeoutMs);
				clientAddress = requestPacket.getAddress();
				clientPort = requestPacket.getPort();

				// Send request to server
				System.out.println("Sending request to server ");
				
				DatagramPacket dp = new DatagramPacket(requestPacket.getData(),
						requestPacket.getLength(), serverAddress,
						serverRequestPort);
				if(errorCommand == ErrorCommands.ERROR_CHANGE_OPCODE)
					socket.send(changeOpcode(dp));
				else if(errorCommand == ErrorCommands.ERROR_REMOVE_FILENAME_DELIMITER)
					socket.send(modifyFileNameTrailingByte(dp));
				else if(errorCommand == ErrorCommands.ERROR_REMOVE_FILENAME)
					socket.send(removeFileName(dp));
				else if(errorCommand == ErrorCommands.ERROR_REMOVE_MODE_DELIMITER)
					socket.send(removeModeTrailingByte(dp));
				else if(errorCommand == ErrorCommands.ERROR_MODIFY_MODE)
					socket.send(modifyMode(dp));
				else
					socket.send(dp);
				
				
					

				// Receive from server
				System.out.println("Receiving request from server");
				dp = TftpPacket.createDatagramForReceiving();
				socket.receive(dp);
				serverPort = dp.getPort();
				

				while (true) {
					// Forward to client
					System.out.println("Forwarding packet to client");
					dp = new DatagramPacket(dp.getData(), dp.getLength(),
							clientAddress, clientPort);
					if(errorCommand == ErrorCommands.ERROR_APPEND_DATAPACKET && TftpPacket.createFromDatagram(dp) instanceof TftpDataPacket
							&& ((TftpDataPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 3)
					{
						socket.send(appendData(dp));
					}else if(errorCommand == ErrorCommands.ERROR_SHRINK_PACKET && TftpPacket.createFromDatagram(dp) instanceof TftpAckPacket
							&& ((TftpAckPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 2){
						socket.send(shrinkData(dp));
					}else if(errorCommand == ErrorCommands.ERROR_APPEND_ACK && TftpPacket.createFromDatagram(dp) instanceof TftpAckPacket
							&& ((TftpAckPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 2){
						socket.send(appendAckPacket(dp));
					}else if(errorCommand == ErrorCommands.ERROR_INVALID_TID){
						if((TftpPacket.createFromDatagram(dp) instanceof TftpAckPacket && ((TftpAckPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 3) ||
								(TftpPacket.createFromDatagram(dp) instanceof TftpDataPacket && ((TftpDataPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 3))
						{
							DatagramSocket invalidSocket = new DatagramSocket();
							invalidSocket.setSoTimeout(timeoutMs);
							invalidSocket.send((dp));
						}
						socket.send(dp);
						
					}else{
						socket.send(dp);
					}

					// Wait for response from client
					System.out.println("Waiting to get packet from client");
					dp = TftpPacket.createDatagramForReceiving();
					socket.receive(dp);

					// Forward to server
					System.out.println("Forwarding packet to server");
					dp = new DatagramPacket(dp.getData(), dp.getLength(),
							serverAddress, serverPort);
					if(errorCommand == ErrorCommands.ERROR_APPEND_DATAPACKET && TftpPacket.createFromDatagram(dp) instanceof TftpDataPacket
							&& ((TftpDataPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 3)
					{
						socket.send(appendData(dp));
					}else if(errorCommand == ErrorCommands.ERROR_SHRINK_PACKET && TftpPacket.createFromDatagram(dp) instanceof TftpAckPacket
							&& ((TftpAckPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 2){
						socket.send(shrinkData(dp));
					}else if(errorCommand == ErrorCommands.ERROR_APPEND_ACK && TftpPacket.createFromDatagram(dp) instanceof TftpAckPacket
							&& ((TftpAckPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 2){
						socket.send(appendAckPacket(dp));
					}else if(errorCommand == ErrorCommands.ERROR_INVALID_TID){
						if((TftpPacket.createFromDatagram(dp) instanceof TftpAckPacket && ((TftpAckPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 3) ||
								(TftpPacket.createFromDatagram(dp) instanceof TftpDataPacket && ((TftpDataPacket)TftpPacket.createFromDatagram(dp)).getBlockNumber() == 3))
						{
							DatagramSocket invalidSocket = new DatagramSocket();
							invalidSocket.setSoTimeout(timeoutMs);
							invalidSocket.send(dp);
						}
						socket.send(dp);
						
					}else{
						socket.send(dp);
					}
						

					// Receive from server
					System.out.println("Waiting to get packet from server");
					dp = TftpPacket.createDatagramForReceiving();
					socket.receive(dp);
				}
			} catch (SocketTimeoutException e) {
				System.out
						.println("Socket timeout: closing thread. (Transfer may have simply finished)");
			} catch (IOException e) {
				System.out.println("Socket error: closing thread.");
			}

			decrementThreadCount();
		}
	}

	// Error - change the opcode to something other than 01,02, 03,04 or 05 
	// in the request or data packet
	
	private DatagramPacket changeOpcode(DatagramPacket packet) {
		byte[] data = packet.getData();
		data[0] = 1;
		data[1] = 8;
		return new DatagramPacket(data, data.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - Remove the trailing 0th byte after the mode in the request packet
	private DatagramPacket removeModeTrailingByte(DatagramPacket packet) {
		byte[] data = packet.getData();
		int i = 1;
		while (data[++i] != 0 && i < data.length);
		data[i+=6] = (byte)0xFF;
		return new DatagramPacket(data, data.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - Mod the trailing 0th byte after the file name in the request packet
	private DatagramPacket modifyFileNameTrailingByte(DatagramPacket packet) {
		byte[] data = packet.getData();
		// find the index of the 0th byte after filename
		int i = 1;
		while (data[++i] != 0 && i < data.length)
			;
		data[i] = 2;
		return new DatagramPacket(data, data.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - Change to invalid mode (other than NetAscii and Octet) in the
	// request packet
	private DatagramPacket modifyMode(DatagramPacket packet) {
		byte[] data = packet.getData();
		// find the index of the 0th byte after filename
		int i = 1;
		while (data[++i] != 0 && i < data.length);
		byte[] invalidMode = ("acctet").getBytes();
		for (int index = 0; index < invalidMode.length; index++)
			data[i + index] = invalidMode[index];
		return new DatagramPacket(data, data.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - Remove the file name in the request packet
	private DatagramPacket removeFileName(DatagramPacket packet) {
		byte[] data = packet.getData();
		// find the index of the 0th byte after filename
		int i = 1;
		while (data[++i] != 0 && i < data.length)
			;
		byte[] modData = new byte[data.length - (i - 2)];
		// copy the op code
		System.arraycopy(data, 0, modData, 0, 2);
		// copy the rest of the data ignoring filename
		System.arraycopy(data, i, modData, 2, modData.length - 2);
		return new DatagramPacket(modData, modData.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - change the data packet to be larger than 516 bytes (including
	// opcode and block number)
	private DatagramPacket appendData(DatagramPacket packet) {
		byte[] data = packet.getData();
		// Now append extra data
		for (int i = 516; i < data.length; i++)
			data[i] = (byte) 0xFF;
		return new DatagramPacket(data, data.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - change the data/ack packet to be smaller than 4 bytes
	private DatagramPacket shrinkData(DatagramPacket packet) {
		byte[] data = packet.getData();
		byte[] modData = new byte[2];
		modData[0] = data[0];
		modData[1] = data[1];
		return new DatagramPacket(modData, modData.length, packet.getAddress(),
				packet.getPort());
	}

	// Error - change the ack packet to be larger than 4 bytes
	private DatagramPacket appendAckPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		byte[] modData = new byte[data.length + 2];
		System.arraycopy(data, 0, modData, 0, data.length);
		modData[data.length] = data[0];
		modData[data.length + 1] = data[1];
		return new DatagramPacket(modData, modData.length, packet.getAddress(),
				packet.getPort());
	}
}