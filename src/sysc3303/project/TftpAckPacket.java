package sysc3303.project;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class TftpAckPacket extends TftpPacket {
	static final protected int opCode = 3;
	protected int blockNumber = 0;

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 *            for the packet
	 * @throws IllegalArgumentException
	 */
	public TftpAckPacket(int blockNumber) throws IllegalArgumentException {
		if (blockNumber < 0) {
			throw new IllegalArgumentException();
		}
		this.blockNumber = blockNumber;
		this.type = Type.ACK;
	}

	/**
	 * Generate the packet data
	 * 
	 * @param data
	 * @param dataLength
	 * @return the byte array of the packet
	 */
	public static TftpAckPacket CreateFromBytes(byte[] data, int dataLength)
			throws InvalidPacketException {
		try {
			if (data == null || data.length < 4 || data[0] != 0
					|| data[1] != opCode) {
				throw new InvalidPacketException();
			}

			int blockNumber = (data[2] << 8) + data[3];
			return new TftpAckPacket(blockNumber);
		} catch (Exception e) {
			throw new InvalidPacketException();
		}
	}

	/**
	 * Get the block number
	 * 
	 * @return the block number
	 */
	public int getBlockNumber() {
		return blockNumber;
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 * @see sysc3303.project.TftpPacket#generatePacketData()
	 */
	@Override
	public byte[] generateData() throws InvalidPacketException {
		byte[] data = new byte[4];
		data[0] = 0;
		data[1] = (byte) opCode;
		data[2] = (byte) (blockNumber >> 8);
		data[3] = (byte) (blockNumber);
		return data;
	}
}
