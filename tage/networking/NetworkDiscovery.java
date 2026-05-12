package tage.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import tage.networking.IGameConnection.ProtocolType;

/**
 * Small LAN discovery helper used to find a running game server without
 * manually typing an IPv4 address.
 *
 * @author Givin Yang
 * @author Nicole Joshua Espinoza
 */
public final class NetworkDiscovery {
	/** Server-address token that asks the client to search the local network for a server. */
	public static final String AUTO_DISCOVER_TOKEN = "AUTO";

	/** Default UDP port used by discovery request and response packets. */
	public static final int DEFAULT_DISCOVERY_PORT = 6011;

	/** Default client wait time, in milliseconds, for a discovery response. */
	public static final int DEFAULT_DISCOVERY_TIMEOUT_MS = 2500;

	private static final String DISCOVERY_REQUEST = "DISCOVER_CSC165_MYGAME_SERVER";
	private static final String DISCOVERY_RESPONSE_PREFIX = "CSC165_MYGAME_SERVER";

	private NetworkDiscovery() {
	}

	/** returns true when the provided server address is the auto-discovery token */
	public static boolean usesAutoDiscovery(String serverAddress) {
		return serverAddress != null
				&& serverAddress.trim().compareToIgnoreCase(AUTO_DISCOVER_TOKEN) == 0;
	}

	/** returns true when a UDP packet contains the expected discovery request text */
	public static boolean isDiscoveryRequest(String message) {
		return DISCOVERY_REQUEST.compareTo(message) == 0;
	}

	/** builds the discovery response payload containing the game server port and protocol */
	public static String buildDiscoveryResponse(int gamePort, ProtocolType protocolType) {
		return DISCOVERY_RESPONSE_PREFIX + "," + gamePort + "," + protocolType.name();
	}

	/**
	 * Broadcasts a discovery request and waits for the first valid response.
	 *
	 * @param discoveryPort UDP port used for discovery packets
	 * @param timeoutMs socket timeout in milliseconds
	 * @return the first discovered server response
	 * @throws IOException if the request cannot be sent or times out waiting for a response
	 */
	public static DiscoveredServer discoverServer(int discoveryPort, int timeoutMs) throws IOException {
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setBroadcast(true);
			socket.setSoTimeout(timeoutMs);

			byte[] requestBytes = DISCOVERY_REQUEST.getBytes(StandardCharsets.UTF_8);
			sendDiscoveryRequest(socket, requestBytes, InetAddress.getLoopbackAddress(), discoveryPort);

			try {
				sendDiscoveryRequest(socket, requestBytes, InetAddress.getByName("255.255.255.255"), discoveryPort);
			} catch (IOException ignored) {
			}

			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				if (!networkInterface.isUp() || networkInterface.isLoopback())
					continue;

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcastAddress = interfaceAddress.getBroadcast();
					if (broadcastAddress == null)
						continue;

					try {
						sendDiscoveryRequest(socket, requestBytes, broadcastAddress, discoveryPort);
					} catch (IOException ignored) {
					}
				}
			}

			return waitForDiscoveryResponse(socket);
		}
	}

	private static void sendDiscoveryRequest(DatagramSocket socket, byte[] requestBytes, InetAddress address,
			int discoveryPort) throws IOException {
		DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length, address, discoveryPort);
		socket.send(packet);
	}

	private static DiscoveredServer waitForDiscoveryResponse(DatagramSocket socket) throws IOException {
		while (true) {
			byte[] responseBuffer = new byte[256];
			DatagramPacket packet = new DatagramPacket(responseBuffer, responseBuffer.length);
			socket.receive(packet);

			String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
			DiscoveredServer discoveredServer = parseDiscoveryResponse(message, packet.getAddress());
			if (discoveredServer != null)
				return discoveredServer;
		}
	}

	/** parses a discovery response string into a DiscoveredServer, or returns null if invalid */
	public static DiscoveredServer parseDiscoveryResponse(String message, InetAddress responderAddress) {
		if (message == null)
			return null;

		String[] tokens = message.trim().split(",");
		if (tokens.length < 3)
			return null;
		if (tokens[0].compareTo(DISCOVERY_RESPONSE_PREFIX) != 0)
			return null;

		int gamePort;
		try {
			gamePort = Integer.parseInt(tokens[1]);
		} catch (NumberFormatException e) {
			return null;
		}

		ProtocolType protocolType;
		try {
			protocolType = ProtocolType.valueOf(tokens[2].trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}

		return new DiscoveredServer(responderAddress, gamePort, protocolType);
	}

	/**
	 * Immutable result returned by the LAN discovery helper.
	 */
	public static final class DiscoveredServer {
		private final InetAddress address;
		private final int gamePort;
		private final ProtocolType protocolType;

		/** creates a discovered server record from an address, game port, and protocol */
		public DiscoveredServer(InetAddress address, int gamePort, ProtocolType protocolType) {
			this.address = address;
			this.gamePort = gamePort;
			this.protocolType = protocolType;
		}

		/** returns the IP address that sent the discovery response */
		public InetAddress getAddress() {
			return address;
		}

		/** returns the game server port advertised by the discovery response */
		public int getGamePort() {
			return gamePort;
		}

		/** returns the protocol advertised by the discovery response */
		public ProtocolType getProtocolType() {
			return protocolType;
		}
	}
}
