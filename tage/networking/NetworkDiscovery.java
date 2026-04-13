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
 */
public final class NetworkDiscovery {
	public static final String AUTO_DISCOVER_TOKEN = "AUTO";
	public static final int DEFAULT_DISCOVERY_PORT = 6011;
	public static final int DEFAULT_DISCOVERY_TIMEOUT_MS = 2500;

	private static final String DISCOVERY_REQUEST = "DISCOVER_CSC165_MYGAME_SERVER";
	private static final String DISCOVERY_RESPONSE_PREFIX = "CSC165_MYGAME_SERVER";

	private NetworkDiscovery() {
	}

	public static boolean usesAutoDiscovery(String serverAddress) {
		return serverAddress != null
				&& serverAddress.trim().compareToIgnoreCase(AUTO_DISCOVER_TOKEN) == 0;
	}

	public static boolean isDiscoveryRequest(String message) {
		return DISCOVERY_REQUEST.compareTo(message) == 0;
	}

	public static String buildDiscoveryResponse(int gamePort, ProtocolType protocolType) {
		return DISCOVERY_RESPONSE_PREFIX + "," + gamePort + "," + protocolType.name();
	}

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

	public static final class DiscoveredServer {
		private final InetAddress address;
		private final int gamePort;
		private final ProtocolType protocolType;

		public DiscoveredServer(InetAddress address, int gamePort, ProtocolType protocolType) {
			this.address = address;
			this.gamePort = gamePort;
			this.protocolType = protocolType;
		}

		public InetAddress getAddress() {
			return address;
		}

		public int getGamePort() {
			return gamePort;
		}

		public ProtocolType getProtocolType() {
			return protocolType;
		}
	}
}
