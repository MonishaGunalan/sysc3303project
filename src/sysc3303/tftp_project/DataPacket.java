package sysc3303.tftp_project;

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
		if ( blockNumber < 0 || data == null || data.length < 1 || dataLength < 1 ) {
			throw new IllegalArgumentException();
		}
		this.blockNumber = blockNumber;
		this.data = data;
		this.dataLength = dataLength;
		this.type = Type.DATA;
	}

	public static DataPacket CreateFromBytes(byte[] data, int dataLength) {
		return null;
	}

	/**
	 * Get the data
	 * 
	 * @return the data
	 */
	public byte[] getData() {
		return data;
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
		if ((data == null && dataLength != 0) || dataLength < 0
				|| dataLength > maxDataLength || blockNumber < 0) {
			throw new IllegalArgumentException();
		}
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}
}
