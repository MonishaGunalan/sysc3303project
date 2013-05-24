package sysc3303.project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;

import sysc3303.project.packets.TftpPacket;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */
public class TftpErrorSimulator {
	private DatagramPacket sendPacket, receivePacket;

	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;

	public static boolean isContentShow = false;
	public static boolean isPortShow = false;

	public static boolean blknumChange = false, mesgChange = false,
			errorCodeChange = false, portChange = false;

	// , dataPacketSim = false, ackPacketSim = false
	public static boolean requestSim = false, packetSim = false;
	public static boolean opCodeChange = false, fileNameChange = false,
			modetypeChange = false;
	public static boolean duplicateSim = false, lostPacketSim = false,
			deplayPacketSim = false;
	public static String fileName;

	public static byte[] opcode = { 0, 0 }, blkNum = { 0, 0 }, errorCode = { 0,
			0 };

	public static int packetType = -1, blockBumber = -1, delayTime = 0;

	public static boolean errorSimulated = false;
	private boolean break1 = false, break2 = false;
	private int clientPort, serverPort;

	public TftpErrorSimulator() {
		try {
			// Construct a datagram socket and bind it to port 68
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets from clients.
			receiveSocket = new DatagramSocket(68);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void passOnTFTP() {
		byte[] data, sending;
		int j = 0;
		// Construct a DatagramPacket for receiving packets up
		// to 100 bytes long (the length of the byte array).
		for (;;) {
			try {
				sendReceiveSocket = new DatagramSocket();
				sendSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}

			data = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			// System.out.println(lostPacketSim);
			System.out
					.println("\nSimulator: Waiting for packet from client to port 68");
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Simulator: Packet received:");
			clientPort = receivePacket.getPort();
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			// Now pass it on to the server (to port 69)
			// 69 - the destination port number on the destination host.
			sendPacket = new DatagramPacket(receivePacket.getData(),
					receivePacket.getLength(), receivePacket.getAddress(), 69);
			if (lostPacketSim && Errors.identifyPacket(sendPacket)
					&& !errorSimulated) {
				System.out.println("We simulate Request Packet is missing");
				errorSimulated = true;
			} else {

				System.out.println("Simulator: sending Request.");
				// Send the datagram packet to the server via the send/receive
				// socket.
				Display.showContent(sendPacket.getData());
				if (requestSim) {
					System.out
							.println("We are simulating Request Packet change");
					if (fileNameChange)
						sendPacket = Errors.alterFileName(sendPacket, fileName);
					if (opCodeChange)
						sendPacket = Errors.alterOpcode(sendPacket, opcode);
					Display.showContent(sendPacket.getData());
				}
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				data = receivePacket.getData();
				if (lostPacketSim) {
					if (data[1] == 1)
						readingPacketLosingSim();
					else
						writtingPacketLosingSim();
				} else {
					transferSim();
				}

				System.out.println("Close socket");
				sendSocket.close();
				sendReceiveSocket.close();
				break1 = false;
				break2 = false;
			}

		} // end big for loop
	}

	public void readingPacketLosingSim() {
		System.out.println("launching readingPacketLosingSim");
		byte[] data;
		for (;;) { // loop forever
			// Construct a DatagramPacket for receiving packets up
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("\nSimulator: Waiting for packet from server.");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Simulator: Packet received:");
			serverPort = receivePacket.getPort();
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			// Process the received datagram.
			// Construct a datagram packet that is to be sent to a specified
			sendPacket = new DatagramPacket(data, receivePacket.getLength(),
					receivePacket.getAddress(), clientPort);

			Errors.ErrorSim(sendPacket, sendSocket);

			if (lostPacketSim && Errors.identifyPacket(sendPacket)
					&& !errorSimulated) {
				System.out
						.println("We simulate one DATA Packet is missing(Reading)");
				errorSimulated = true;
			} else {
				System.out.println("Simulator: Sending packet:");
				if (isContentShow)
					Display.showContent(receivePacket.getData());
				if (isPortShow)
					Display.showPort(receivePacket, sendReceiveSocket);

				try {
					sendSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// System.out.println("\nSimulator: packet sent using port " +
				// sendSocket.getLocalPort());
				System.out.println();

				// first break!
				// break1
				if (break1)
					break;
				// finding the last packet for break1!
				// System.out.println("Checking last one");
				if (receivePacket.getLength() < 516 && data[1] == 3) {
					System.out
							.println("find the last DATA packet for writting");
					break2 = true;
				}

				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);

				System.out
						.println("\nSimulator: Waiting for packet from client.");
				// Block until a datagram packet is received from receiveSocket.
				try {
					sendSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Simulator: Packet received:");
				if (isContentShow)
					Display.showContent(receivePacket.getData());
				if (isPortShow)
					Display.showPort(receivePacket, sendReceiveSocket);

				sendPacket = new DatagramPacket(data,
						receivePacket.getLength(), receivePacket.getAddress(),
						serverPort);

				if (lostPacketSim && Errors.identifyPacket(sendPacket)
						&& !errorSimulated) {
					System.out
							.println("We simulate one ACK Packet is missing(Reading)");
					errorSimulated = true;
				} else {
					System.out.println("Simulator: sending packet.");
					if (isContentShow)
						Display.showContent(receivePacket.getData());
					if (isPortShow)
						Display.showPort(receivePacket, sendReceiveSocket);

					Errors.ErrorSim(sendPacket, sendReceiveSocket);

					// Send the datagram packet to the server via the
					// send/receive
					// socket.
					try {
						sendReceiveSocket.send(sendPacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}

					// second break!
					// break1
					if (break2)
						break;
					// finding the last packet for break1!
					/*
					 * System.out.println("-----------------------------------");
					 * System.out.println("Checking last one");
					 * //System.out.println("length " +
					 * receivePacket.getLength());
					 * //System.out.println(data[1]);
					 * System.out.println("-------------------------------------"
					 * );
					 */
					if (receivePacket.getLength() < 516 && data[1] == 3) {
						System.out
								.println("find the last DATA packet for reading");
						break1 = true;
					}

					// We're finished with this socket, so close it.
					// sendSocket.close();
				}
			}
		} // end inside for loop
	}

	public void writtingPacketLosingSim() {
		System.out.println("launching writtingPacketLosingSim");

		byte[] data;
		for (;;) { // loop forever
			// Construct a DatagramPacket for receiving packets up
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("\nSimulator: Waiting for packet from server.");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Simulator: Packet received:");
			serverPort = receivePacket.getPort();
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			// Process the received datagram.
			// Construct a datagram packet that is to be sent to a specified
			sendPacket = new DatagramPacket(data, receivePacket.getLength(),
					receivePacket.getAddress(), clientPort);

			Errors.ErrorSim(sendPacket, sendSocket);

			if (lostPacketSim && Errors.identifyPacket(sendPacket)
					&& !errorSimulated) {
				System.out
						.println("We simulate one DATA Packet is missing(Reading)");
				errorSimulated = true;
			} else {
				System.out.println("Simulator: Sending packet:");
				if (isContentShow)
					Display.showContent(receivePacket.getData());
				if (isPortShow)
					Display.showPort(receivePacket, sendReceiveSocket);

				try {
					sendSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// System.out.println("\nSimulator: packet sent using port " +
				// sendSocket.getLocalPort());
				System.out.println();

				// first break!
				// break1
				if (break1)
					break;
				// finding the last packet for break1!
				// System.out.println("Checking last one");
				if (receivePacket.getLength() < 516 && data[1] == 3) {
					System.out
							.println("find the last DATA packet for writting");
					break2 = true;
				}

				data = new byte[516];
				receivePacket = new DatagramPacket(data, data.length);

				System.out
						.println("\nSimulator: Waiting for packet from client.");
				// Block until a datagram packet is received from receiveSocket.
				try {
					sendSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Simulator: Packet received:");
				if (isContentShow)
					Display.showContent(receivePacket.getData());
				if (isPortShow)
					Display.showPort(receivePacket, sendReceiveSocket);

				sendPacket = new DatagramPacket(data,
						receivePacket.getLength(), receivePacket.getAddress(),
						serverPort);

				if (lostPacketSim && Errors.identifyPacket(sendPacket)
						&& !errorSimulated) {
					System.out
							.println("We simulate one DATA Packet is missing(Reading)");
					errorSimulated = true;
					System.out
							.println("\nSimulator: Waiting for packet from client.");
					// Block until a datagram packet is received from
					// receiveSocket.
					try {
						sendSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(1);
					}
					System.out.println("Simulator: Packet received:");
				}

				System.out.println("Simulator: sending packet.");
				if (isContentShow)
					Display.showContent(receivePacket.getData());
				if (isPortShow)
					Display.showPort(receivePacket, sendReceiveSocket);

				Errors.ErrorSim(sendPacket, sendReceiveSocket);

				// Send the datagram packet to the server via the send/receive
				// socket.
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// second break!
				// break1
				if (break2)
					break;
				// finding the last packet for break1!
				/*
				 * System.out.println("-----------------------------------");
				 * System.out.println("Checking last one");
				 * //System.out.println("length " + receivePacket.getLength());
				 * //System.out.println(data[1]);
				 * System.out.println("-------------------------------------");
				 */
				if (receivePacket.getLength() < 516 && data[1] == 3) {
					System.out.println("find the last DATA packet for reading");
					break1 = true;
				}

				// We're finished with this socket, so close it.
				// sendSocket.close();
			}
		} // end inside for loop
	}

	public void transferSim() {
		System.out.println("launching TransferSim");
		byte[] data;
		for (;;) { // loop forever
			// Construct a DatagramPacket for receiving packets up
			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("\nSimulator: Waiting for packet from server.");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Simulator: Packet received:");
			serverPort = receivePacket.getPort();
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			// Process the received datagram.
			// Construct a datagram packet that is to be sent to a specified
			sendPacket = new DatagramPacket(data, receivePacket.getLength(),
					receivePacket.getAddress(), clientPort);

			System.out.println("Simulator: Sending packet:");
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			Errors.ErrorSim(sendPacket, sendSocket); // sim dulicate packet and
														// delay packet
			sendPacket = Errors.packetSim(sendPacket);
			/*
			 * if (packetSim && Errors.identifyPacket(sendPacket) &&
			 * !errorSimulated ) { System.out.println("Simulating "); if
			 * (fileNameChange == true) sendPacket =
			 * Errors.alterFileName(sendPacket, fileName); if (opCodeChange ==
			 * true) sendPacket = Errors.alterOpcode(sendPacket, opcode);
			 * errorSimulated = true; }
			 */

			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// System.out.println("\nSimulator: packet sent using port " +
			// sendSocket.getLocalPort());
			System.out.println();

			// first break!
			// break1
			if (break1)
				break; // finding the last packet for break1!
			// System.out.println("Checking last one");
			if (receivePacket.getLength() < 516 && data[1] == 3) {
				System.out.println("find the last DATA packet for writting");
				break2 = true;
			}

			data = new byte[516];
			receivePacket = new DatagramPacket(data, data.length);

			System.out.println("\nSimulator: Waiting for packet from client.");
			// Block until a datagram packet is received from receiveSocket.
			try {
				sendSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Simulator: Packet received:");
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			sendPacket = new DatagramPacket(data, receivePacket.getLength(),
					receivePacket.getAddress(), serverPort);

			System.out.println("Simulator: sending packet.");
			if (isContentShow)
				Display.showContent(receivePacket.getData());
			if (isPortShow)
				Display.showPort(receivePacket, sendReceiveSocket);

			Errors.ErrorSim(sendPacket, sendReceiveSocket); // sim dulicate
															// packet and delay
															// packet
			sendPacket = Errors.packetSim(sendPacket);
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// second break!
			// break1
			if (break2)
				break;
			if (receivePacket.getLength() < 516 && data[1] == 3) {
				System.out.println("find the last DATA packet for reading");
				break1 = true;
			}

			// We're finished with this socket, so close it.
			// sendSocket.close();
		} // end inside for loop
	}

	public static void main(String args[]) {
		TftpErrorSimulator s = new TftpErrorSimulator();

		int num = 0;

		int changeReq = 0;
		int choiceMode = 0;
		int change;
		int state = 1;
		String userChoice = null, changeData, command;

		Scanner user_input = new Scanner(System.in);
		boolean notReaday = true;

		System.out.println("TftpErrorSimulator");
		System.out.println("");
		System.out
				.println("Every time after you finish a file transfer you need restart TftpErrorSimulator");
		System.out.println("");
		System.out.println("Please type the number of what you like to do");
		System.out.println("0. Normal");
		System.out.println("1. Change request");
		System.out.println("2. Change Packet");
		System.out.println("3. Unknow Port Sim");
		System.out.println("4. duplicate Packet");
		System.out.println("5. losing a Packet");
		System.out.println("6. delay a Packet");
		choiceMode = user_input.nextInt();
		num++;

		switch (choiceMode) {
		case 0: {
		}
			break;
		case 1:
		// change request
		{
			requestSim = true;
			while (notReaday) {
				System.out.println("What you like to change type number?");
				System.out.println("1. OpCode");
				System.out.println("2. Filename");
				System.out.println("3. 0");
				System.out.println("4. Change mode");
				System.out.println("5. 0");
				System.out.println("");
				System.out.println("0.-- Run --");
				changeReq = user_input.nextInt();

				if (changeReq == 1) {
					System.out.println("Please type a new OpCode number?");
					change = user_input.nextInt();
					opcode[0] = (byte) (change / 10);
					opcode[1] = (byte) (change % 10);
					opCodeChange = true;

				} else if (changeReq == 2) {
					System.out.println("Please type a new file name?");
					fileName = user_input.next();
					fileNameChange = true;
				} else if (changeReq == 3) {
					System.out.println("Type a number");
					change = user_input.nextInt();
				} else if (changeReq == 4) {
					System.out.println("2. Type the mode you ");
					String modetype = user_input.next();
					modetypeChange = true;

				} else if (changeReq == 5) {
					System.out.println("Type a number");
					change = user_input.nextInt();
				} else if (changeReq == 0) {
					notReaday = false;

				} else {
					System.out.println("Invalid Entry");
				}
			} // end while
		}
			break;
		case 2:
		// change dataPacket and ACK
		{
			packetSim = true;
			// dataPacketSim = true;
			System.out.println("Simulate transfering fake Packet");
			System.out.println("Choose a Packet to change:");
			System.out.println("2. DATA Packet");
			System.out.println("3. ACK Packet");
			System.out.println("4. ERROR Packet");
			changeReq = user_input.nextInt();
			while (changeReq != 2 && changeReq != 3 && changeReq != 4) {
				System.out.println("please enter an valid number");
				changeReq = user_input.nextInt();
			}
			packetType = changeReq;
			System.out.println("Which Block you want to change(type a number)");
			blockBumber = user_input.nextInt();

			while (notReaday) {
				System.out.println("What you like to change?");
				System.out.println("1. OpCode");
				System.out.println("2. Data Block number");
				System.out.println("3. ErrType (for Error sim only)");
				System.out.println();
				System.out.println("0.-- Run --");
				changeReq = user_input.nextInt();

				if (changeReq == 1) {
					System.out
							.println("Please type a new OpCode number(ie:  02 by tying 2)");
					change = user_input.nextInt();
					opcode[0] = (byte) (change / 10);
					opcode[1] = (byte) (change % 10);
					opCodeChange = true;
				} else if (changeReq == 2) {
					System.out.println("Please type a new dataBlock number");
					change = user_input.nextInt();
					blkNum[0] = (byte) (change / 10);
					blkNum[1] = (byte) (change % 10);
					blknumChange = true;

				} else if (changeReq == 3) {
					System.out
							.println("Please type a new Error code(ie: 05 by tying 5)");
					change = user_input.nextInt();
					errorCode[0] = (byte) (change / 10);
					errorCode[1] = (byte) (change % 10);
					errorCodeChange = true;

				} else if (changeReq == 0) {
					notReaday = false;
				} else {
					System.out.println("Invalid Entry");
				}
			}
		}
			break;
		/*
		 * case 4: // change error { packetSim = true; while (notReaday) {
		 * System.out.println("What you like to change?");
		 * System.out.println("1. OpCode"); System.out.println("2. Error code");
		 * System.out.println("3. ErrMsg"); System.out.println("4. 0");
		 * System.out.println("6.-- Run --"); changeReq = user_input.nextInt();
		 * if (changeReq == 1) {
		 * System.out.println("Please type a new OpCode number?"); change =
		 * user_input.nextInt(); opcode[0] = (byte)(change / 10); opcode[1] =
		 * (byte)(change % 10); opCodeChange = true; } else if (changeReq == 2)
		 * { System.out.println("Please type a new Error code number?"); change
		 * = user_input.nextInt(); errorCode[0] = (byte)(change / 10);
		 * errorCode[1] = (byte)(change % 10); errorCodeChange = true; } else if
		 * (changeReq == 3) { System.out.println("Please type a new message?");
		 * String mesg = user_input.next(); mesgChange = true; } else if
		 * (changeReq == 6) { notReaday = false; } else {
		 * System.out.println("Invalid Entry"); } } } break;
		 */
		case 3:
		// change port
		{

			System.out.println("Simulate sending a Packet from different port");
			packetSim = true;
			portChange = true;
			System.out.println("Choose a Packet to change:");
			// System.out.println("1. REQUEST Packet");
			System.out.println("2. DATA Packet");
			System.out.println("3. ACK Packet");
			changeReq = user_input.nextInt();
			while (changeReq != 2 && changeReq != 3) {
				System.out.println("please enter an valid number");
				changeReq = user_input.nextInt();
			}

			packetType = changeReq;
			System.out
					.println("Which Block you want to do Unknown Port Simulating(type a number)");
			blockBumber = user_input.nextInt();
		}
			break;
		case 4: { // duplicate packet.
			duplicateSim = true;
			System.out.println("Simulate duplicate Packet");
			System.out.println("Choose a Packet to change:");
			System.out.println("1. REQUEST Packet");
			System.out.println("2. DATA Packet");
			System.out.println("3. ACK Packet");
			changeReq = user_input.nextInt();
			while (changeReq != 2 && changeReq != 3 && changeReq != 1) {
				System.out.println("please enter an valid number");
				changeReq = user_input.nextInt();
			}
			if (changeReq == 1) {
				System.out
						.println("Our project will accepted duplicate request, but it will only accepted duplicate read request, for duplicate, Server will return an error");
				System.out.println("So we are not simulating this situation");
			}
			packetType = changeReq;
			System.out
					.println("Which Block you want to duplicate(type a number)");
			blockBumber = user_input.nextInt();
		}
			break;
		case 5: { // lose a packet
			lostPacketSim = true;
			System.out.println("Simulate losing packet");
			System.out.println("Choose a Packet to change:");
			System.out.println("1. REQUEST Packet");
			System.out.println("2. DATA Packet");
			System.out.println("3. ACK Packet");
			changeReq = user_input.nextInt();
			while (changeReq != 2 && changeReq != 3 && changeReq != 1) {
				System.out.println("please enter an valid number");
				changeReq = user_input.nextInt();
			}
			packetType = changeReq;
			if (packetType != 1) {
				System.out
						.println("Which Block you want to lost(type a number)");
				blockBumber = user_input.nextInt();
			}

		}
			break;
		case 6: { // delay
			deplayPacketSim = true;
			System.out.println("Simulate delay packet");
			System.out.println("Choose a Packet to change:");
			System.out.println("1. REQUEST Packet");
			System.out.println("2. DATA Packet");
			System.out.println("3. ACK Packet");
			changeReq = user_input.nextInt();
			while (changeReq != 2 && changeReq != 3 && changeReq != 1) {
				System.out.println("please enter an valid number");
				changeReq = user_input.nextInt();
			}
			packetType = changeReq;
			if (packetType != 1) {
				System.out
						.println("Which Block you want to depaly(type a number)");
				blockBumber = user_input.nextInt();
			}
			System.out.println("How long you want delay:");
			System.out.println("Note: Maxium delay is 7999");
			System.out
					.println("delay big than the maxium will affect file transfer");
			delayTime = user_input.nextInt();

		}
			break;
		}
		System.out.println("TftpErrorSimulator start:");
		s.passOnTFTP();
	} // end run
	
	
}
