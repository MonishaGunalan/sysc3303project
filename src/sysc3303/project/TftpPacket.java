package sysc3303.project;

import java.net.DatagramPacket;
import java.net.InetAddress;

import sysc3303.project.TftpRequestPacket.Action;
import sysc3303.project.TftpRequestPacket.Mode;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

abstract class TftpPacket {
	static final int MAX_LENGTH = 516;

	enum Type {
		RRQ, WRQ, DATA, ACK, ERROR
	}

	Type type;

	/**
	 * Get the associated packet type (RRQ, WRQ, DATA, ACK, or ERROR)
	 * 
	 * @return the type of packet
	 */
	Type getType() {
		return type;
	}

	/**
	 * Factory method to create a read request
	 * 
	 * @param filename
	 *            filename of the file to be read
	 * @return a read RequestPacket
	 */
	static TftpRequestPacket createReadRequest(String filename, Mode mode) {
		return new TftpRequestPacket(filename, Action.READ, mode);
	}

	/**
	 * Factory method to create a write request
	 * 
	 * @param filename
	 *            filename of the file to be written
	 * @return a write RequestPacket
	 */
	static TftpRequestPacket createWriteRequest(String filename, Mode mode) {
		return new TftpRequestPacket(filename, Action.WRITE, mode);
	}

	/**
	 * Factory method to create an ack packet
	 * 
	 * @param blockNumber
	 *            block number of the ack
	 * @return an TftpAckPacket
	 */
	static TftpAckPacket createAckPacket(int blockNumber) {
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
	static TftpDataPacket createDataPacket(int blockNumber, byte[] data,
			int dataLength) {
		return new TftpDataPacket(blockNumber, data, dataLength);
	}

	static void CreateErrorPacket() {
		// TODO
	}

	/**
	 * Create a TFTP packet from the received datagram.
	 * 
	 * @param datagram
	 * @return
	 * @throws InvalidPacketException
	 */
	static TftpPacket createFromDatagram(DatagramPacket datagram)
			throws InvalidPacketException {
		return TftpPacket.createFromBytes(datagram.getData(),
				datagram.getLength());
	}

	/**
	 * Create a TFTP packet from the given data.
	 * 
	 * @param data
	 * @param dataLength
	 * @return
	 * @throws InvalidPacketException
	 */
	private static TftpPacket createFromBytes(byte[] data, int dataLength)
			throws InvalidPacketException {

		if (data[0] != 0) {
			throw new InvalidPacketException();
		}

		switch (data[1]) {
		case 1:
			return TftpRequestPacket.createFromBytes(data, dataLength);
		case 2:
			return TftpRequestPacket.createFromBytes(data, dataLength);
		case 3:
			return TftpAckPacket.createFromBytes(data, dataLength);
		case 4:
			return TftpDataPacket.createFromBytes(data, dataLength);
		case 5:
			return null; // TODO TftpErrorPacket.createFromBytes(data, dataLength);
		default:
			throw new InvalidPacketException();
		}
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
	DatagramPacket generateDatagram(InetAddress destinationAddress,
			int destinationPort) throws InvalidPacketException {
		byte data[] = this.generateData();
		return new DatagramPacket(data, data.length, destinationAddress,
				destinationPort);
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
	 * Generate the packet data. Must be implemented by extending classes.
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 */
	abstract byte[] generateData() throws InvalidPacketException;

	/**
	 * Convert the request into a visual packet string (for debugging/logging
	 * only)
	 * 
	 * @return a string representation of the packet
	 * @throws InvalidPacketException
	 */
	String generateString() {
		byte[] packetBytes = this.generateData();
		StringBuilder packetString = new StringBuilder(4);
		for (byte b : packetBytes) {
			packetString.append((int) b);
		}
		return packetString.toString();
	}

}
