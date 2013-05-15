package sysc3303.tftp_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RequestPacket implements Packet {
	public String filename = "";
	public Action action;
	public Mode mode;
	protected boolean isValid = true;
	public enum Action {READ, WRITE, INVALID}
	public enum Mode {ASCII, OCTET}
		
	public RequestPacket(String filename, Action action, Mode mode)
	{
		this.filename = filename;
		this.action = action;
		this.mode = mode;
	}
	
	public RequestPacket(byte[] packetData, int dataLength)
	{
		// Assume valid until we find an error
		isValid = true;
		
		// Get the action (read/write)
		if (packetData[0] == 0 && packetData[1] == 1) {
			action = RequestPacket.Action.READ;
		} else if (packetData[0] == 0 && packetData[1] == 2) {
			action = RequestPacket.Action.WRITE;
		} else {
			isValid = false;
		}
		
		// Extract the filename
		int i = 1;
		while (packetData[++i] != 0 && i < dataLength) {
			filename += (char)packetData[i]; // Note: might be more efficient with StringBuilder
		}
					
		// If this isn't 0 then we likely filled up the buffer (oops)
		if (packetData[i] != 0) {
			isValid = false;				
		}
		
		// Extract the transfer mode
		String modeStr = "";
		while (packetData[++i] != 0 && i < dataLength) {
			modeStr += (char)packetData[i]; // Note: might be more efficient with StringBuilder
		}
		
		modeStr = modeStr.toLowerCase();
		if ( modeStr.equals("ascii") ) {
			mode = Mode.ASCII; 
		} else if ( modeStr.equals("octet") ) {
			mode = Mode.OCTET; 
		}
		
		// Make sure there is just a 0 and no more data after
		while (i < dataLength) {
			if ( packetData[i++] != 0 ) {
				isValid = false;
				break;
			}
		}
	}

	/*
	 * Validate the request
	 */
	/* (non-Javadoc)
	 * @see sysc3303.project.Packet#isValid()
	 */
	@Override
	public boolean isValid()
	{
		if (!isValid) return false;
		if (filename == null || filename.isEmpty()) return false;
		if (mode == null) return false;
		if (action == null || action == Action.INVALID) return false;
		return true;
	}
	
	/*
	 * Generate the packet data
	 */
	/* (non-Javadoc)
	 * @see sysc3303.project.Packet#generatePacketData()
	 */
	@Override
	public byte[] generatePacketData()
	{
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
			if ( null != filename ) {
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
	
	/*
	 * Convert the request into a visual packet string (for debugging/logging only)
	 */
	/* (non-Javadoc)
	 * @see sysc3303.project.Packet#generatePacketString()
	 */
	@Override
	public String generatePacketString()
	{
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
