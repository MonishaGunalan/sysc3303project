package sysc3303.project.packets;


/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class TftpErrorPacket extends TftpPacket {
	static final protected int opCode = 5;
	protected int blockNumber = 0;

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 *            for the packet
	 * @throws IllegalArgumentException
	 */
	TftpErrorPacket(int blockNumber) throws IllegalArgumentException {
		if (blockNumber < 0) {
			throw new IllegalArgumentException();
		}
		this.blockNumber = blockNumber;
		this.type = Type.ERROR;
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 * @see sysc3303.project.packets.TftpPacket#generatePacketData()
	 */
	@Override
	public byte[] generateData() throws InvalidPacketException {
		byte[] packetBytes = new byte[4];
		packetBytes[0] = 0;
		packetBytes[1] = (byte) opCode;
		// TODO
		return packetBytes;
	}
}
