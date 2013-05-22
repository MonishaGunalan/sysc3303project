package sysc3303.project.packets;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
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
