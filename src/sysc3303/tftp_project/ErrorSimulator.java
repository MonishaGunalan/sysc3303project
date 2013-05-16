package sysc3303.tftp_project;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
/**
 * 
 */

/**
 * @author korey
 *
 */
public class ErrorSimulator {
	protected int proxyPort = 68;
	protected int serverPort = 69;
	protected DatagramSocket receiveSocket, sendReceiveSocket;
	protected boolean isConnected;
	protected InetAddress serverAddress;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		new ErrorSimulator().run();
	}

	/**
	 * Constructor
	 */
	public ErrorSimulator()
	{}
	
	/**
	 * Create sockets and bind to needed ports
	 */
	public void connect()
	{
		try {
			System.out.println("Proxy connecting on " + InetAddress.getLocalHost().getHostAddress() + ":" + proxyPort);
			receiveSocket = new DatagramSocket(proxyPort, InetAddress.getLocalHost());
			sendReceiveSocket = new DatagramSocket();
			serverAddress = InetAddress.getLocalHost();
			isConnected = true;
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Turn the server on and wait for packets to be received
	 */
	public void run()
	{
		if ( !isConnected ) {
			connect();
		}
		
		while(true) forward();
	}
	
	/**
	 * Forward packets
	 */
	public void forward()
	{
		try {
			byte data[] = new byte[100];
			DatagramPacket dp = new DatagramPacket(data, data.length);
			InetAddress clientAddress;
			int clientPort;
			
			// Receive packet from client
			receiveSocket.receive(dp);
			clientAddress = dp.getAddress();
			clientPort = dp.getPort();
			
			System.out.println("Proxy forwarding request from " + clientAddress + ":" + clientPort);
			System.out.println("Request bytes: " + dp.getData());
			System.out.println("Request string: " + RequestPacket.CreateFromBytes(dp.getData(), dp.getLength()).generateString());
			System.out.println();
			
			// Forward packet to server
			dp = new DatagramPacket(dp.getData(), dp.getLength(), serverAddress, serverPort);
			sendReceiveSocket.send(dp);
			
			// Receive response from server
			data = new byte[100];
			dp = new DatagramPacket(data, data.length);
			sendReceiveSocket.receive(dp);

			System.out.println("Proxy received response from server");
			System.out.println("Response bytes: " + dp.getData());
			System.out.print("Response string: ");
			data = dp.getData();
			int dataLength = dp.getLength();
			for (int i = 0; i < dataLength; i++) {
				System.out.print(data[i]);
			}
			System.out.println();
			System.out.println();

			// Forward response to client
			dp = new DatagramPacket(dp.getData(), dp.getLength(), clientAddress, clientPort);
			sendReceiveSocket.send(dp);
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Disconnect sockets
	 */
	public void disconnect()
	{
		System.out.println("Proxy disconnecting");
		receiveSocket.close();
		sendReceiveSocket.close();
		isConnected = false;
	}

	/**
	 * Destructor, disconnects sockets
	 */
	public void finalize()
	{
		disconnect();
	}

}
