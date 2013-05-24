package sysc3303.project.packets;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class TftpAckPacket extends TftpPacket {
	private static final int OP_CODE = 3; // The TFTP op code
	private static final int MIN_BLOCK_NUMBER = 0; // minimum block number
	private static final int MAX_BLOCK_NUMBER = 0XFF; // maximum block number
	private static final int PACKET_LENGTH = 4; // exact length packet should be
	private int blockNumber = 0; // The ack's block number

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 *            for the packet
	 * @throws IllegalArgumentException
	 */
	TftpAckPacket(int blockNumber) throws IllegalArgumentException {
		// Verify block number makes sense
		if (blockNumber < MIN_BLOCK_NUMBER || blockNumber > MAX_BLOCK_NUMBER) {
			throw new IllegalArgumentException();
		}
		this.blockNumber = blockNumber;
		this.type = Type.ACK;
	}

	/**
	 * Generate the packet data
	 * 
	 * @param packetData
	 * @param packetLength
	 * @throws IllegalArgumentException
	 * @return the byte array of the packet
	 */
	static TftpAckPacket createFromBytes(byte[] packetData, int packetLength)
			throws IllegalArgumentException {
		// Make sure data is not null and is long enough
		if (packetData == null || packetData.length < PACKET_LENGTH || packetLength != PACKET_LENGTH) {
			throw new IllegalArgumentException();
		}
		
		// Make sure we have the right op code
		if (packetData[0] != 0 || packetData[1] != OP_CODE) {
			throw new IllegalArgumentException();
		}

		int blockNumber = (packetData[2] << 8) + packetData[3];
		return new TftpAckPacket(blockNumber);
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
	 * @see sysc3303.project.packets.TftpPacket#generateData()
	 */
	@Override
	public byte[] generateData() {
		byte[] data = new byte[4];
		data[0] = 0;
		data[1] = (byte) OP_CODE;
		data[2] = (byte) (blockNumber >> 8);
		data[3] = (byte) (blockNumber);
		return data;
	}
}
