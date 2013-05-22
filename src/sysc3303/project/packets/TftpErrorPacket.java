package sysc3303.project.packets;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class TftpErrorPacket extends TftpPacket {
	static final protected int opCode = 5;
	protected int blockNumber = 0;

	public enum ErrorType {
		NOT_DEFINED(0), FILE_NOT_FOUND(1), ACCESS_VIOLATION(2), DISC_FULL_OR_ALLOCATION_EXCEEDED(
				3), ILLEGAL_OPERATION(4), UNKOWN_TID(5), FILE_ALREADY_EXISTS(6), NO_SUCH_USER(
				7);
		private int code;

		ErrorType(int code) {
			this.code = code;
		}
		
		int getCode() {
			return code;
		}
	}

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 *            for the packet
	 * @throws IllegalArgumentException
	 */
	TftpErrorPacket(ErrorType errorType, String errorMessage) throws IllegalArgumentException {
		if (errorType == null) {
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
