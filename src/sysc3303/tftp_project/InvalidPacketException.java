package sysc3303.tftp_project;

/**
 * @author Korey Conway (100838924)
 * @author Monisha
 * @author Azraan
 */
public class InvalidPacketException extends RuntimeException {

	private static final long serialVersionUID = 2182716043011849584L;

	public InvalidPacketException() {
	}

	public InvalidPacketException(String arg0) {
		super(arg0);
	}

	public InvalidPacketException(Throwable arg0) {
		super(arg0);
	}

	public InvalidPacketException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
