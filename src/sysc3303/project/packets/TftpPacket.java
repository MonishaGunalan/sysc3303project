package sysc3303.project.packets;

import java.net.DatagramPacket;
import java.net.InetAddress;

import sysc3303.project.packets.TftpRequestPacket.Action;
import sysc3303.project.packets.TftpRequestPacket.Mode;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

abstract class TftpPacket {
	static final int MAX_LENGTH = 516;

	public enum Type {
		RRQ, WRQ, DATA, ACK, ERROR
	}

	Type type;

	/**
	 * Get the associated packet type (RRQ, WRQ, DATA, ACK, or ERROR)
	 * 
	 * @return the type of packet
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Factory method to create a read request
	 * 
	 * @param filename
	 *            filename of the file to be read
	 * @return a read RequestPacket
	 */
	public static TftpRequestPacket createReadRequest(String filename, Mode mode) {
		return new TftpRequestPacket(filename, Action.READ, mode);
	}

	/**
	 * Factory method to create a write request
	 * 
	 * @param filename
	 *            filename of the file to be written
	 * @return a write RequestPacket
	 */
	public static TftpRequestPacket createWriteRequest(String filename,
			Mode mode) {
		return new TftpRequestPacket(filename, Action.WRITE, mode);
	}

	/**
	 * Factory method to create an ack packet
	 * 
	 * @param blockNumber
	 *            block number of the ack
	 * @return an TftpAckPacket
	 */
	public static TftpAckPacket createAckPacket(int blockNumber) {
		return new TftpAckPacket(blockNumber);
	}

	/**
	 * Factory method to create a data packet
	 * 
	 * @param blockNumber
	 *            block number of the data
	 * @param data
	 *            data byte array
	 * @param dataLength
	 *            length of the data that is valid in data
	 * @return a TftpDataPacket
	 */
	public static TftpDataPacket createDataPacket(int blockNumber, byte[] data,
			int dataLength) {
		return new TftpDataPacket(blockNumber, data, dataLength);
	}

	public static void CreateErrorPacket() {
		// TODO
	}

	/**
	 * Create a TFTP packet from the received datagram.
	 * 
	 * @param datagram
	 * @return
	 * @throws InvalidPacketException
	 */
	public static TftpPacket createFromDatagram(DatagramPacket datagram)
			throws InvalidPacketException {
		return TftpPacket.createFromBytes(datagram.getData(),
				datagram.getLength());
	}

	/**
	 * Create a TFTP packet from the given data.
	 * 
	 * @param packetData
	 * @param packetLength
	 * @return
	 * @throws InvalidPacketException
	 */
	private static TftpPacket createFromBytes(byte[] packetData, int packetLength)
			throws InvalidPacketException {

		if (packetData[0] != 0) {
			throw new InvalidPacketException();
		}

		switch (packetData[1]) {
		case 1:
			return TftpRequestPacket.createFromBytes(packetData, packetLength);
		case 2:
			return TftpRequestPacket.createFromBytes(packetData, packetLength);
		case 3:
			return TftpAckPacket.createFromBytes(packetData, packetLength);
		case 4:
			return TftpDataPacket.createFromBytes(packetData, packetLength);
		case 5:
			return null; // TODO TftpErrorPacket.createFromBytes(data,
							// dataLength);
		default:
			throw new InvalidPacketException();
		}
	}

	/**
	 * Create a datagram packet used for receiving. It will automatically assign
	 * the maximum length of a possible TFTP packet for the data array. This is
	 * a convenience method.
	 * 
	 * @return a DatagramPacket for receiving
	 */
	static DatagramPacket createDatagramForReceiving() {
		return new DatagramPacket(new byte[MAX_LENGTH], MAX_LENGTH);
	}

	/**
	 * Generate a UDP datagram packet from the current data. This is a
	 * convenience method.
	 * 
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
	 * Generate the packet data used for the DatagramPacket. Must be implemented
	 * by extending classes.
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 */
	public abstract byte[] generateData();

	/**
	 * Convert the request into a visual packet string (for debugging/logging
	 * only)
	 * 
	 * @return a string representation of the packet
	 * @throws InvalidPacketException
	 */
	public String toString() {
		byte[] packetBytes = this.generateData();
		StringBuilder packetString = new StringBuilder(4);
		for (byte b : packetBytes) {
			packetString.append(String.format("%x", b));
		}
		return packetString.toString();
	}

}
