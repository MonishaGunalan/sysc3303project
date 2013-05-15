package sysc3303.tftp_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RequestPacket extends Packet {
	protected String filename = "";
	protected Action action;
	protected Mode mode;
	protected Mode defaultMode = Mode.OCTET;
	
	public enum Action {
		READ, WRITE
	}

	public enum Mode {
		ASCII, OCTET
	}

	/**
	 * @param action
	 * @param filename
	 * @param mode
	 * @throws IllegalArgumentException
	 */
	public RequestPacket(Action action, String filename) throws IllegalArgumentException {
		if ( action == null || filename == null || filename.length() == 0 || mode == null ) {
			throw new IllegalArgumentException();
		}
		this.filename = filename;
		this.action = action;
		this.mode = defaultMode;
		this.type = (action == Action.READ) ? Type.RRQ : Type.WRQ;
	}
	
	public static RequestPacket CreateReadRequest(String filename) {
		return new RequestPacket(Action.READ, filename);
	}

	public static RequestPacket CreateWriteRequest(String filename) {
		return new RequestPacket(Action.WRITE, filename);
	}

	public static RequestPacket CreateFromBytes(byte[] data, int dataLength) throws InvalidPacketException {
		// Assume valid until we find an error
		boolean isValid = true;

		// Get the action (read/write)
		if (data[0] == 0 && data[1] == 1) {
			action = RequestPacket.Action.READ;
		} else if (data[0] == 0 && data[1] == 2) {
			action = RequestPacket.Action.WRITE;
		} else {
			isValid = false;
		}

		// Extract the filename
		int i = 1;
		while (data[++i] != 0 && i < dataLength) {
			filename += (char) data[i]; // Note: might be more efficient
												// with StringBuilder
		}

		// If this isn't 0 then we likely filled up the buffer (oops)
		if (data[i] != 0) {
			isValid = false;
		}

		// Extract the transfer mode
		String modeStr = "";
		while (data[++i] != 0 && i < dataLength) {
			modeStr += (char) data[i]; // Note: might be more efficient
												// with StringBuilder
		}

		modeStr = modeStr.toLowerCase();
		if (modeStr.equals("ascii")) {
			mode = Mode.ASCII;
		} else if (modeStr.equals("octet")) {
			mode = Mode.OCTET;
		}

		// Make sure there is just a 0 and no more data after
		while (i < dataLength) {
			if (data[i++] != 0) {
				isValid = false;
				break;
			}
		}
	}

	/**
	 * Validate the packet
	 * 
	 * @return return true when the packet is valid, false otherwise
	 * @see sysc3303.project.Packet#isValid()
	 */
	@Override
	public boolean isValid() {
		if (!isValid)
			return false;
		if (filename == null || filename.isEmpty())
			return false;
		if (mode == null)
			return false;
		if (action == null || action == Action.INVALID)
			return false;
		return true;
	}

	/**
	 * Generate the packet data
	 * 
	 * @return the byte array of the packet
	 * @throws InvalidPacketException
	 * @see sysc3303.project.Packet#generatePacketData()
	 */
	@Override
	public byte[] generateData() throws InvalidPacketException {
		try {
			// Form the byte array
			ByteArrayOutputStream stream = new ByteArrayOutputStream();

			// Always start with 0
			stream.write(0);

			// Set the request action type byte
			switch (action) {
			case READ:
				stream.write(1); // read request flag byte
				break;
			case WRITE:
				stream.write(2); // write request flag byte
				break;
			case INVALID:
				stream.write(3); // undefined request
				break;
			default:
				throw new InvalidPacketException();
			}

			// Add the filename
			if (null != filename) {
				stream.write(filename.getBytes());
			}

			// Add 0, as per the required format
			stream.write(0);

			// Set the mode
			stream.write(mode.toString().getBytes());

			// Finish with 0
			stream.write(0);

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
	 * @throws InvalidPacketException
	 * @see sysc3303.project.Packet#generatePacketString()
	 */
	@Override
	public String generateString() throws InvalidPacketException {
		try {
			if (!isValid()) {
				return "invalid packet";
			}

			StringBuilder packetStr = new StringBuilder();

			// Always start with 0
			packetStr.append(0);

			// Set the request action type byte
			switch (action) {
			case READ:
				packetStr.append(1); // read request flag byte
				break;
			case WRITE:
				packetStr.append(2); // write request flag byte
				break;
			case INVALID:
				packetStr.append(3); // undefined request
				break;
			default:
				break;
			}

			// Add the filename
			if (null != filename) {
				packetStr.append(filename);
			}

			// Add 0, as per the required format
			packetStr.append(0);

			// Set the mode
			packetStr.append(mode.toString());

			// Finish with 0
			packetStr.append(0);

			return packetStr.toString();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return "";
	}
}
