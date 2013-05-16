package sysc3303.tftp_project;

/**
 * @author Korey Conway (100838924)
 * @author Monisha
 * @author Arzaan
 */
public class ErrorPacket extends Packet {
	static final protected int opCode = 5;
	protected int blockNumber = 0;

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 *            for the packet
	 * @throws IllegalArgumentException
	 */
	ErrorPacket(int blockNumber) throws IllegalArgumentException {
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
	 * @see sysc3303.tftp_project.Packet#generatePacketData()
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
