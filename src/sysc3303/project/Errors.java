package sysc3303.project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import sysc3303.project.packets.TftpPacket;

public class Errors {

	private static DatagramSocket newSocket;
	private static DatagramPacket newPacket;
	private static DatagramPacket receivePacket;
	private static byte[] data;
	public static boolean errorSimulated = false;

	public static void ErrorSim(DatagramPacket sendPacket,
			DatagramSocket sendSocket) {
		// byte[] dataArray = sendPacket.getData();
		if (!errorSimulated) {
			if (identifyPacket(sendPacket)) {
				if (TftpErrorSimulator.duplicateSim) {
					duplicatePacketSim(sendPacket, sendSocket);
					errorSimulated = true;
				} else if (TftpErrorSimulator.deplayPacketSim) {
					System.out.println("Delay this Packet for "
							+ TftpErrorSimulator.delayTime);
					// delay
					try {
						Thread.sleep(TftpErrorSimulator.delayTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					errorSimulated = true;
				}

			}
		}
	}

	public static void duplicatePacketSim(DatagramPacket sendPacket,
			DatagramSocket socket) {
		System.out.println("Simulate duplicate Packet.");
		try {
			socket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);

		if (TftpErrorSimulator.packetType != 3) {
			try {
				socket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static boolean checkType(DatagramPacket sendPacket) {
		byte[] dataArray = sendPacket.getData();
		boolean packetType = false;
		if (TftpErrorSimulator.packetType == 1)
			packetType = (dataArray[0] == 0 && (dataArray[1] == 1 || dataArray[1] == 2));
		else if (TftpErrorSimulator.packetType == 2)
			packetType = (dataArray[0] == 0 && dataArray[1] == 3); // change
																	// data
		else if (TftpErrorSimulator.packetType == 3)
			packetType = (dataArray[0] == 0 && dataArray[1] == 4); // check ack
		else if (TftpErrorSimulator.packetType == 4)
			packetType = (dataArray[0] == 0 && dataArray[1] == 5); // check
																	// error
		return packetType;
	}

	public static boolean identifyPacket(DatagramPacket sendPacket) {
		boolean blockNumCheck = false;
		if (checkType(sendPacket)) {
			if (TftpErrorSimulator.packetType == 1)
				return true;
			if (TftpErrorSimulator.packetType == 4)
				return true;
			blockNumCheck = (TftpPacket.getBlockNumber(sendPacket.getData()) == TftpErrorSimulator.blockBumber);
			return blockNumCheck;
		}
		return (checkType(sendPacket) && blockNumCheck);
	}

	public static void alterPortSim(DatagramPacket sendPacket) {

		// create a new socket
		try {
			newSocket = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// create a new Packet
		// newPacket = new DatagramPacket(sendPacket.getData(),
		// sendPacket.getLength(), sendPacket.getAddress(), port);
		// send data to client
		try {
			newSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		data = new byte[516];
		receivePacket = new DatagramPacket(data, data.length);

		// Block and wait to receive error packet from client
		System.out.println("\nSimulator: Waiting for packet from client.");

		try {
			newSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("\n**ERROR packet was recieved**");
		System.out.println("Packet contain: ");
		Display.showContent(receivePacket.getData());
	}

	public static DatagramPacket packetSim(DatagramPacket sendPacket) {
		if (TftpErrorSimulator.packetSim) {
			if (identifyPacket(sendPacket)) {
				if (TftpErrorSimulator.portChange) {
					System.out
							.println("Simulate sending data from different port");
					alterPortSim(sendPacket);
				} else {
					System.out.println("Simulate packet");
					System.out.println("data before change:");
					Display.showContent(sendPacket.getData());
					if (TftpErrorSimulator.opCodeChange) {
						sendPacket = alterOpcode(sendPacket, TftpErrorSimulator.opcode);
					}
					if (TftpErrorSimulator.blknumChange) {
						sendPacket = alterBlockNumber(sendPacket,
								TftpErrorSimulator.blkNum);
					}
					if (TftpErrorSimulator.errorCodeChange) {
						sendPacket = alterErrorCode(sendPacket,
								TftpErrorSimulator.errorCode);
					}
					System.out.println("packet after change");
					Display.showContent(sendPacket.getData());
				}
			}
		}

		return sendPacket;
	}

	// pass in the packet and the a new file name, create a new packet and
	// insert the new file
	public static DatagramPacket alterFileName(DatagramPacket packet,
			String fileName) {
		DatagramPacket newpacket;
		byte[] opcode = new byte[] { 0, 0 };
		byte[] info = packet.getData();
		opcode[1] = info[1];
		opcode[0] = info[0];
		String modeName = ErrorHandle.getMode(info, packet.getLength());
		newpacket = TftpPacket.creatRequestPacket(opcode, fileName, modeName,
				packet.getAddress(), packet.getPort());
		// System.out.println("New Packet contain: ");
		// Display.showContent(newpacket.getData());
		return newpacket;
	}


	// pass in the packet and opcode, create a new packet and insert the new
	// opcode
	public static DatagramPacket alterOpcode(DatagramPacket packet,
			byte[] opcode) {

		DatagramPacket newpacket;
		byte[] data;
		data = packet.getData();

		data[0] = opcode[0];
		data[1] = opcode[1];

		newpacket = TftpPacket.creatPacket(data, packet.getAddress(),
				packet.getPort());

		return newpacket;
	}

	public static DatagramPacket alterBlockNumber(DatagramPacket packet,
			byte[] block) {

		DatagramPacket newpacket;
		byte[] data;
		data = packet.getData();

		data[2] = block[0];
		data[3] = block[1];

		newpacket = TftpPacket.creatPacket(data, packet.getAddress(),
				packet.getPort());

		return newpacket;
	}

	public static DatagramPacket alterErrorCode(DatagramPacket packet,
			byte[] block) {

		DatagramPacket newpacket;
		byte[] data;
		data = packet.getData();

		data[2] = block[0];
		data[3] = block[1];

		newpacket = TftpPacket.creatPacket(data, packet.getAddress(),
				packet.getPort());

		return newpacket;
	}

}