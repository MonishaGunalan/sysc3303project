package sysc3303.project;

import java.net.InetAddress;
import java.util.Scanner;

import sysc3303.project.common.TftpRequestPacket;

/**
 * @author Korey Conway (100838924)
 * @author Monisha (100871444)
 * @author Arzaan (100826631)
 */
public class TftpServer {
	// Port on which to listen for requests (6900 for dev, 69 for submission)
	private static final int LISTEN_PORT = 69;

	// Folder where files are read/written
	private String publicFolder = System.getProperty("user.dir")
			+ "/server_files/";

	// Current number of threads (used to know when we have stopped)
	private int threadCount = 0;

	// Request listener thread. Need reference to stop receiving when stopping.
	private TftpRequestListener requestListener;

	/**
	 * Constructor
	 */
	private TftpServer() {
		requestListener = new TftpRequestListener(this, LISTEN_PORT);
		requestListener.start();
	}

	/**
	 * Main program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TftpServer server = new TftpServer();
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.print("Command: ");
			String command = scanner.nextLine().toLowerCase();

			// Continue if blank line was passed
			if (command.length() == 0) {
				continue;
			}

			if (command.equals("help")) {
				System.out.println("Available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the server (when current transfers finish)");
				System.out
						.println("    pwd: prints out the public directory for file transfers");
			} else if (command.equals("stop")) {
				System.out
						.println("Stopping server (when current transfers finish)");
				server.stop();
			} else if (command.equals("pwd")) {
				System.out.println("Current shared directory: "
						+ server.getPublicFolder());
			} else {
				System.out
						.println("Invalid command. These are the available commands:");
				System.out.println("    help: prints this help menu");
				System.out
						.println("    stop: stop the server (when current transfers finish)");
				System.out
						.println("    pwd: prints out the public directory for file transfers");
			}
		}
	}

	synchronized public void incrementThreadCount() {
		threadCount++;
	}

	synchronized public void decrementThreadCount() {
		threadCount--;
	}

	synchronized public int getThreadCount() {
		return threadCount;
	}

	public void stop() {
		requestListener.getSocket().close();
		System.out.println("Stopping... waiting for threads to finish");
		while (getThreadCount() > 0) {
			// Wait for threads to finish
			// TODO: future version could use wait/notify
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// Ignore errors
			}
		}
		System.out.println("Exiting");
		System.exit(0);
	}

	public String getPublicFolder() {
		return publicFolder;
	}

	public TftpServerFileTransfer newTransferThread(TftpRequestPacket packet,
			InetAddress address, int port) {
		return new TftpServerFileTransfer(this, packet, address, port);
	}

}