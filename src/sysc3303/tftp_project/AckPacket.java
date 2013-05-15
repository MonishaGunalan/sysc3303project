package sysc3303.tftp_project;

/**
 * @author korey
 *
 */
public class AckPacket implements Packet {
	static final protected int opCode = 3;
	protected int blockNumber = 0;
	
	/**
	 * Constructor
	 */
	AckPacket()
	{}
	
	/**
	 * Constructor
	 * @param blockNumber for the packet
	 */
	AckPacket(int blockNumber) {
		this.setBlockNumber(blockNumber);
	}

	/**
	 * @return the blockNumber
	 * @throws IllegalArgumentException
	 */
	public int getBlockNumber() throws IllegalArgumentException {
		if (blockNumber < 0) {
			throw new IllegalArgumentException();
		}
		return blockNumber;
	}

	/**
	 * @param blockNumber the blockNumber to set
	 * @throws IllegalArgumentException
	 */
	public void setBlockNumber(int blockNumber) throws IllegalArgumentException {
		this.blockNumber = blockNumber;
	}

	/**
	 * Validate the packet
	 * @return return true when the packet is valid, false otherwise
	 * @see sysc3303.tftp_project.Packet#isValid()
	 */
	@Override
	public boolean isValid() {
		// Just need to make sure the blockNumber is non-negative
		return (blockNumber >= 0);
	}

	/**
	 * Generate the packet data
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 * @see sysc3303.tftp_project.Packet#generatePacketData()
	 */
	@Override
	public byte[] generatePacketData() throws InvalidPacketException {
		
		if (!this.isValid()) {
			throw new InvalidPacketException();
		}
		
		byte[] packetBytes = new byte[4];
		packetBytes[0] = 0;
		packetBytes[1] = (byte) opCode;
		packetBytes[2] = (byte) (blockNumber);
		packetBytes[3] = (byte) (blockNumber >> 8);
		return packetBytes;
	}

	/**
	 * Convert the request into a visual packet string (for debugging/logging only)
	 * @return a string representation of the packet
	 * @throws InvalidPacketException;
	 * @see sysc3303.tftp_project.Packet#generatePacketString()
	 */
	@Override
	public String generatePacketString() {
		byte [] packetBytes = this.generatePacketData();
		StringBuilder packetString = new StringBuilder(4);
		for (byte b : packetBytes) {
			packetString.append((int) b);
		}
		return packetString.toString();
	}
}
