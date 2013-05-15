package sysc3303.tftp_project;

public interface Packet {

	/**
	 * Validate the packet
	 * @return return true when the packet is valid, false otherwise
	 */
	public abstract boolean isValid();

	/**
	 * Generate the packet data
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 */
	public abstract byte[] generatePacketData() throws InvalidPacketException;

	/**
	 * Convert the request into a visual packet string (for debugging/logging only)
	 * @return a string representation of the packet
	 * @throws InvalidPacketException;
	 */
	public abstract String generatePacketString() throws InvalidPacketException;

}

