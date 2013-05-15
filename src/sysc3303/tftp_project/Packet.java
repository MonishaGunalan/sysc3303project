package sysc3303.tftp_project;

import java.net.DatagramPacket;
import java.net.InetAddress;

public abstract class Packet {
	protected Type type;
	protected boolean isValid = true;

	public enum Type {
		RRQ, WRQ, DATA, ACK, ERROR
	}

	static void CreateReadRequestPacket() {
		// TODO
	}

	static void CreateWriteRequestPacket() {
		// TODO
	}

	static void CreateAckPacket() {
		// TODO
	}

	static void CreateDataPacket() {
		// TODO
	}

	static void CreateErrorPacket() {
		// TODO
	}

	static Packet CreateFromBytes(byte[] data) throws InvalidPacketException {
		// TODO
		if (data[0] != 0) {
			throw new InvalidPacketException();
		}

		switch (data[1]) {
		case 1:
			break;
		case 2:
			break;
		case 3:
			break;
		case 4:
			break;
		case 5:
			break;
		default:
			throw new InvalidPacketException();
		}
		return null;
	}

	/**
	 * @param destinationAddress
	 * @param destinationPort
	 * @return a DatagramPacket ready to be sent through a socket
	 * @throws InvalidPacketException
	 */
	public DatagramPacket generateDatagram(InetAddress destinationAddress,
			int destinationPort) throws InvalidPacketException {
		byte data[] = this.generateData();
		return new DatagramPacket(data, data.length, destinationAddress,
				destinationPort);
	}

	/**
	 * Check if the packet is valid
	 * 
	 * @return return true when the packet is valid, false otherwise
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 */
	public abstract byte[] generateData() throws InvalidPacketException;

	/**
	 * Convert the request into a visual packet string (for debugging/logging
	 * only)
	 * 
	 * @return a string representation of the packet
	 * @throws InvalidPacketException
	 */
	public abstract String generateString() throws InvalidPacketException;

}
