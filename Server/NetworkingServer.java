package Server;

import java.io.IOException;

public class NetworkingServer {
	private GameServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;

	public NetworkingServer(int serverPort, String protocol) {
		try {
			if (protocol.toUpperCase().compareTo("TCP") == 0) {
				thisTCPServer = new GameServerTCP(serverPort);
			} else {
				thisUDPServer = new GameServerUDP(serverPort);
			}
			printStartupStatus(serverPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printStartupStatus(int serverPort) {
		if (thisTCPServer != null) {
			System.out.println("TCP server listening on port " + serverPort);
			return;
		}

		if (thisUDPServer != null)
			System.out.println("UDP server listening on port " + serverPort);
	}

	public static void main(String[] args) {
		if (args.length > 1) {
			new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		} else {
			System.out.println("Usage: java NetworkingServer <port> <UDP|TCP>");
		}
	}
}
