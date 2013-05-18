package sysc3303.tftp_project;

import java.net.DatagramPacket;
import java.net.InetAddress;

import sysc3303.tftp_project.RequestPacket.Action;
import sysc3303.tftp_project.RequestPacket.Mode;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public abstract class Packet {
	protected Type type;
	public static final int maxLength = 516;

	public enum Type {
		RRQ, WRQ, DATA, ACK, ERROR
	}

	/**
	 * Factory method to create a read request
	 * 
	 * @param filename
	 *            filename of the file to be read
	 * @return a read RequestPacket
	 */
	public static RequestPacket CreateReadRequest(String filename) {
		return new RequestPacket(Action.READ, filename, Mode.OCTET);
	}

	/**
	 * Factory method to create a write request
	 * 
	 * @param filename
	 *            filename of the file to be written
	 * @return a write RequestPacket
	 */
	public static RequestPacket CreateWriteRequest(String filename) {
		return new RequestPacket(Action.WRITE, filename, Mode.OCTET);
	}

	static AckPacket CreateAckPacket(int blockNumber) {
		return new AckPacket(blockNumber);
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
	
	public static DatagramPacket createDatagramForReceiving()
	{
		return new DatagramPacket(new byte[maxLength], maxLength);
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
