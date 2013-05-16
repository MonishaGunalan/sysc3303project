package sysc3303.tftp_project;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * @author Korey Conway (100838924)
 * @author Monisha
 * @author Arzaan
 */
public abstract class Packet {
	protected Type type;

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

	static DataPacket CreateDataPacket(int blockNumber, byte[] data,
			int dataLength) {
		return new DataPacket(blockNumber, data, dataLength);
	}

	static void CreateErrorPacket() {
		// TODO
	}

	public Type getType() {
		return type;
	}

	/**
	 * @param data
	 * @param dataLength
	 * @return
	 * @throws InvalidPacketException
	 */
	static Packet CreateFromBytes(byte[] data, int dataLength)
			throws InvalidPacketException {

		if (data[0] != 0) {
			throw new InvalidPacketException();
		}

		switch (data[1]) {
		case 1:
			return RequestPacket.CreateFromBytes(data, dataLength);
		case 2:
			return RequestPacket.CreateFromBytes(data, dataLength);
		case 3:
			return AckPacket.CreateFromBytes(data, dataLength);
		case 4:
			return DataPacket.CreateFromBytes(data, dataLength);
		case 5:
			return DataPacket.CreateFromBytes(data, dataLength);
		default:
			throw new InvalidPacketException();
		}
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
	public String generateString() {
		byte[] packetBytes = this.generateData();
		StringBuilder packetString = new StringBuilder(4);
		for (byte b : packetBytes) {
			packetString.append((int) b);
		}
		return packetString.toString();
	}

}
