package running_Dead.server;

import java.io.IOException;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.NetworkDiscovery;

public class NetworkingServer {
	private GameServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;
	private ServerDiscoveryResponder discoveryResponder;

	public NetworkingServer(int serverPort, String protocol) {
		ProtocolType protocolType = parseProtocolType(protocol);

		try {
			if (protocolType == ProtocolType.TCP) {
				thisTCPServer = new GameServerTCP(serverPort);
			} else {
				thisUDPServer = new GameServerUDP(serverPort);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		startDiscoveryResponder(serverPort, protocolType);
		printStartupStatus(serverPort, protocolType);
		registerShutdownHook();
	}

	private ProtocolType parseProtocolType(String protocol) {
		if (protocol != null && protocol.toUpperCase().compareTo("TCP") == 0)
			return ProtocolType.TCP;
		return ProtocolType.UDP;
	}

	private void startDiscoveryResponder(int serverPort, ProtocolType protocolType) {
		try {
			discoveryResponder = new ServerDiscoveryResponder(
					NetworkDiscovery.DEFAULT_DISCOVERY_PORT,
					serverPort,
					protocolType);
			discoveryResponder.start();
		} catch (IOException e) {
			System.out.println("Auto-discovery could not start on UDP port "
					+ NetworkDiscovery.DEFAULT_DISCOVERY_PORT + ".");
			e.printStackTrace();
		}
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (discoveryResponder != null)
				discoveryResponder.close();
		}));
	}

	private void printStartupStatus(int serverPort, ProtocolType protocolType) {
		System.out.println(protocolType.name() + " server listening on port " + serverPort);

		if (discoveryResponder != null) {
			System.out.println("Auto-discovery enabled on UDP port " + NetworkDiscovery.DEFAULT_DISCOVERY_PORT + ".");
			System.out.println("Clients can now launch with: java running_Dead.MyGame AUTO");
		}

		System.out.println("NetworkingServer relays packets only; it does not create a player avatar.");
		System.out.println("For 2 players, keep this server running and launch two separate running_Dead.MyGame clients.");
	}

	public static void main(String[] args) {
		if (args.length > 1) {
			new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		} else {
			System.out.println("Usage: java running_Dead.server.NetworkingServer <port> <UDP|TCP>");
		}
	}
}
