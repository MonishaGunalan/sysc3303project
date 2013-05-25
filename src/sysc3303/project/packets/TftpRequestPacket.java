package sysc3303.project.packets;

import java.io.ByteArrayOutputStream;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class TftpRequestPacket extends TftpPacket {
	private static final int MIN_LENGTH = 10; // assuming 1 character filename

	// Types of actions for the request
	public static enum Action {
		READ, WRITE
	}

	// Option for the mode of transfer
	public static enum Mode {
		ASCII, OCTET
	}

	private String filename = ""; // filename of the file to be transferred
	private Action action; // the action (read or write)
	private Mode mode; // transfer mode (ascii or octet)

	/**
	 * Constructor
	 * 
	 * @param filename
	 * @param action
	 * @param mode
	 * @throws IllegalArgumentException
	 */
	TftpRequestPacket(String filename, Action action, Mode mode)
			throws IllegalArgumentException {
		// Make sure our parameters are not null and filename isn't blank
		if (filename == null || filename.length() == 0|| action == null
				|| mode == null) {
			throw new IllegalArgumentException("Missing data in the request packet");
		}
		this.filename = filename;
		this.action = action;
		this.mode = mode;
		this.type = (action == Action.READ) ? Type.RRQ : Type.WRQ;
	}

	/**
	 * Get the filename for the request
	 * 
	 * @return
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Check if this is a read request.
	 * 
	 * @return true if read request, false if write request
	 */
	public boolean isReadRequest() {
		return (action == Action.READ);
	}

	/**
	 * Generate a RequestPacket from the given byte array
	 * 
	 * @param packetData
	 *            byte array data received over the network
	 * @param packetLength
	 *            length of the data from the packet received
	 * @throws IllegalArgumentException
	 * @return
	 */
	static TftpRequestPacket createFromBytes(byte[] packetData, int packetLength)
			throws IllegalArgumentException {
		Action action;
		String filename;
		Mode mode;

		// Make sure data is not null and is long enough
		if (packetData == null || packetData.length < packetLength || packetLength < MIN_LENGTH) {
			throw new IllegalArgumentException("Data is not long enough");
		}

		// Parse the op code
		if (packetData[0] != 0) {
			throw new IllegalArgumentException("Invalid OP code");
		} else if (packetData[1] == 1) {
			action = Action.READ;
		} else if (packetData[1] == 2) {
			action = Action.WRITE;
		} else {
			throw new IllegalArgumentException("Invalid OP code");
		}

		// Extract the filename
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder();
		while (packetData[++i] != 0 && i < packetLength) {
			filenameBuilder.append((char) packetData[i]);
		}
		filename = filenameBuilder.toString();

		// Must have 0 after filename
		if (packetData[i] != 0) {
			throw new IllegalArgumentException("Must have 0 after filename");
		}

		// Extract the transfer mode
		StringBuilder modeStrBuilder = new StringBuilder();
		while (packetData[++i] != 0 && i < packetLength) {
			modeStrBuilder.append((char) packetData[i]);
		}

		// Save the transfer mode
		String modeStr = modeStrBuilder.toString().toLowerCase();
		if (modeStr.equals("ascii")) {
			mode = Mode.ASCII;
		} else if (modeStr.equals("octet")) {
			mode = Mode.OCTET;
		} else {
			throw new IllegalArgumentException("Invalid transfer mode");
		}

		// Check for the terminating 0 and make sure there is no more data
		if (packetData[packetLength - 1] != 0) {
			throw new IllegalArgumentException("Trailing 0 not found");
		}

		// Create a RequestPacket
		return new TftpRequestPacket(filename, action, mode);
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @see sysc3303.project.packets.TftpPacket.Packet#generateData()
	 */
	@Override
	public byte[] generateData() {
		// Form the byte array
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		stream.write(0); // Always start with 0

		// Set the request action type byte
		if (action == Action.WRITE) {
			stream.write(2); // write request flag byte
		} else {
			stream.write(1); // read request flag byte
		}

		// Add filename and mode (along with terminating strings)
		byte[] tempByteArr = filename.getBytes();
		stream.write(tempByteArr, 0, tempByteArr.length);
		stream.write(0);

		tempByteArr = mode.toString().toLowerCase().getBytes();
		stream.write(tempByteArr, 0, tempByteArr.length);
		stream.write(0);

		// Convert to byte array and return
		return stream.toByteArray();
	}

	/**
	 * Convert the request into a visual packet string (for debugging/logging
	 * only)
	 * 
	 * @return a string representation of the packet
	 * @see sysc3303.project.packets.TftpPacket.Packet#toString()
	 */
	@Override
	public String toString() {
		StringBuilder packetStr = new StringBuilder();

		packetStr.append(0); // Always start with 0

		// Set the request action type byte
		if (action == Action.WRITE) {
			packetStr.append(2); // write request flag byte
		} else {
			packetStr.append(1); // read request flag byte
		}

		// Add the filename
		packetStr.append(filename);

		// Add filename and mode (along with terminating strings)
		packetStr.append(0);
		packetStr.append(mode.toString().toLowerCase());
		packetStr.append(0);

		// Convert to String and return
		return packetStr.toString();
	}
}
