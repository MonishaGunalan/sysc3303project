package sysc3303.tftp_project;

public class DataPacket implements Packet {
	static final protected int opCode = 3;
	static final protected int maxDataLength = 512;
	protected int blockNumber = 0;
	protected byte[] data = null;
	protected int dataLength = 0;
	
	/**
	 * Constructor
	 */
	DataPacket()
	{}
	
	/**
	 * Constructor
	 * @param blockNumber for the packet
	 */
	DataPacket(int blockNumber, byte[] data, int dataLength)
	{
		this.setBlockNumber(blockNumber);
		this.setData(data, dataLength);
	}

	/**
	 * @return the blockNumber
	 */
	public int getBlockNumber() {
		return blockNumber;
	}

	/**
	 * @param blockNumber the blockNumber to set
	 * @throws IllegalArgumentException
	 */
	public void setBlockNumber(int blockNumber) throws IllegalArgumentException {
		if (blockNumber < 0) {
			throw new IllegalArgumentException();
		}
		this.blockNumber = blockNumber;
	}

	/**
	 * Get the data
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 * @param length of the data
	 * @throws IllegalArgumentException
	 */
	public void setData(byte[] data, int length) throws IllegalArgumentException {
		if ((data == null && length != 0) || length < 0 || length > maxDataLength) {
			throw new IllegalArgumentException();
		}
		this.data = data;
		this.dataLength = length;
	}
	
	/**
	 * Get the length of the data
	 * @return length of the data
	 */
	public int getDataLength()
	{
		return this.dataLength;
	}

	/**
	 * Validate the packet
	 * @return return true when the packet is valid, false otherwise
	 * @see sysc3303.tftp_project.Packet#isValid()
	 */
	@Override
	public boolean isValid() {
		if ((data == null && dataLength != 0) || dataLength < 0 || dataLength > maxDataLength || blockNumber < 0) {
			return false;
		}
		return true;
	}

	/**
	 * Generate the packet data
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 * @see sysc3303.tftp_project.Packet#generatePacketData()
	 */
	@Override
	public byte[] generatePacketData() throws InvalidPacketException {
		if ((data == null && dataLength != 0) || dataLength < 0 || dataLength > maxDataLength || blockNumber < 0) {
			throw new IllegalArgumentException();
		}
		return null;
	}

	/**
	 * Convert the request into a visual packet string (for debugging/logging only)
	 * @return a string representation of the packet
	 * @throws InvalidPacketException;
	 * @see sysc3303.tftp_project.Packet#generatePacketString()
	 */
	@Override
	public String generatePacketString() throws InvalidPacketException {
		// TODO Auto-generated method stub
		return null;
	}

}
