package sysc3303.tftp_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DataPacket extends Packet {
	static final protected int opCode = 4;
	static final protected int maxDataLength = 512;
	protected int blockNumber = 0;
	protected byte[] data = null;
	protected int dataLength = 0;

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 * @param data
	 * @param dataLength
	 */
	DataPacket(int blockNumber, byte[] data, int dataLength) {
		if (blockNumber < 0 || data == null || data.length < 1
				|| dataLength < 1) {
			throw new IllegalArgumentException();
		}
		this.blockNumber = blockNumber;
		this.data = data;
		this.dataLength = dataLength;
		this.type = Type.DATA;
	}

	public static DataPacket CreateFromBytes(byte[] packetDate, int packetDataLength)
			throws InvalidPacketException {
		// TODO: catch index out of bounds exceptions
		if (packetDate[0] != 0 || packetDate[1] != 4) {
			throw new InvalidPacketException();
		}

		// Extract the data portion
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(packetDate, 4, packetDataLength - 4);

		int blockNumber = (packetDate[2] << 8) + packetDate[3];
		packetDate = stream.toByteArray();
		return new DataPacket(blockNumber, packetDate, packetDate.length);
	}

	/**
	 * Get the data
	 * 
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

	public int getBlockNumber() {
		return blockNumber;
	}

	public int getDataLength() {
		return dataLength;
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 * @see sysc3303.tftp_project.Packet#generatePacketData()
	 */
	@Override
	public byte[] generateData() throws InvalidPacketException {
		try {
			if ((data == null && dataLength != 0) || dataLength < 0
					|| dataLength > maxDataLength || blockNumber < 0) {
				throw new IllegalArgumentException();
			}
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			stream.write(0);
			stream.write(opCode);
			stream.write(blockNumber >> 8);
			stream.write(blockNumber);
			stream.write(data);
			return stream.toByteArray();
		} catch (IOException e) {
			throw new InvalidPacketException();
		}
	}

	/**
	 * Convert the request into a visual packet string (for debugging/logging
	 * only)
	 * 
	 * @return a string representation of the packet
	 * @throws InvalidPacketException
	 *             ;
	 * @see sysc3303.tftp_project.Packet#generatePacketString()
	 */
	@Override
	public String generateString() throws InvalidPacketException {
		// TODO print header as ints and data as hexadecimal code
		StringBuilder str = new StringBuilder();
		str.append(0);
		str.append((byte) opCode);
		str.append((byte) (blockNumber >> 8));
		str.append((byte) blockNumber);

		for (byte b : data) {
			str.append(String.format("%x", b));
		}

		return str.toString();
	}
}