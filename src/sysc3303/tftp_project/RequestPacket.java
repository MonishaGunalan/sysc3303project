package sysc3303.tftp_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */

public class RequestPacket extends Packet {
	protected String filename = ""; // filename of the file to be transferred
	protected Action action; // the action (read or write)
	protected Mode mode; // transfer mode (ascii or octet)
	public static int maxPacketSize = 100;

	// Types of actions for the request
	public enum Action {
		READ, WRITE
	}

	// Option for the mode of transfer
	public enum Mode {
		ASCII, OCTET
	}

	/**
	 * Constructor
	 * 
	 * @param action
	 * @param filename
	 * @param mode
	 * @throws IllegalArgumentException
	 */
	public RequestPacket(Action action, String filename, Mode mode)
			throws IllegalArgumentException {
		if (action == null || filename == null || filename.length() == 0
				|| mode == null) {
			throw new IllegalArgumentException();
		}
		this.filename = filename;
		this.action = action;
		this.mode = mode;
		this.type = (action == Action.READ) ? Type.RRQ : Type.WRQ;
	}

	public String getFilename() {
		return filename;
	}

	public boolean isReadRequest() {
		return (action == Action.READ);
	}

	/**
	 * Generate a RequestPacket from the given byte array
	 * 
	 * @param data
	 *            byte array data received over the network
	 * @param dataLength
	 *            length of the data from the packet received
	 * @return
	 * @throws InvalidPacketException
	 */
	public static RequestPacket CreateFromBytes(byte[] data, int dataLength)
			throws InvalidPacketException {
		// Assume valid until we find an error
		Action action;
		String filename;
		Mode mode;

		if (data[0] != 0) {
			throw new InvalidPacketException();
		}

		// Get the action (read/write)
		if (data[1] == 1) {
			action = Action.READ;
		} else if (data[1] == 2) {
			action = Action.WRITE;
		} else {
			throw new InvalidPacketException();
		}

		// Extract the filename
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder();
		while (data[++i] != 0 && i < dataLength) {
			filenameBuilder.append((char) data[i]);
		}
		filename = filenameBuilder.toString();

		if (data[i] != 0) {
			// byte array must have been filled
			throw new InvalidPacketException();
		}

		// Extract the transfer mode
		StringBuilder modeStrBuilder = new StringBuilder();
		while (data[++i] != 0 && i < dataLength) {
			modeStrBuilder.append((char) data[i]);
		}

		// Save the transfer mode
		String modeStr = modeStrBuilder.toString().toLowerCase();
		if (modeStr.equals("ascii")) {
			mode = Mode.ASCII;
		} else if (modeStr.equals("octet")) {
			mode = Mode.OCTET;
		} else {
			throw new InvalidPacketException();
		}

		// Check for the terminating 0 and make sure there is no more data
		if (data[i] != 0 || i != (dataLength - 1)) {
			// TODO verify that end of data detection actually works
			throw new InvalidPacketException();
		}

		// Create a RequestPacket
		return new RequestPacket(action, filename, mode);
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @see sysc3303.project.Packet#generatePacketData()
	 */
	@Override
	public byte[] generateData() {
		try {
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
			stream.write(filename.getBytes());
			stream.write(0);
			stream.write(mode.toString().toLowerCase().getBytes());
			stream.write(0);

			// Convert to byte array and return
			return stream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Convert the request into a visual packet string (for debugging/logging
	 * only)
	 * 
	 * @return a string representation of the packet
	 * @see sysc3303.project.Packet#generatePacketString()
	 */
	@Override
	public String generateString() {
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
