package a3.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.NetworkDiscovery;

/**
 * Replies to LAN discovery broadcasts so clients can find the game server
 * without manually typing an IPv4 address.
 */
public class ServerDiscoveryResponder implements AutoCloseable {
	private final DatagramSocket socket;
	private final int gamePort;
	private final ProtocolType protocolType;

	private volatile boolean running;
	private Thread discoveryThread;

	public ServerDiscoveryResponder(int discoveryPort, int gamePort, ProtocolType protocolType) throws SocketException {
		this.gamePort = gamePort;
		this.protocolType = protocolType;

		socket = new DatagramSocket(null);
		socket.setReuseAddress(true);
		socket.setBroadcast(true);
		socket.bind(new InetSocketAddress(discoveryPort));
	}

	public void start() {
		if (running)
			return;

		running = true;
		discoveryThread = new Thread(this::runDiscoveryLoop, "ServerDiscoveryResponder");
		discoveryThread.setDaemon(true);
		discoveryThread.start();
	}

	private void runDiscoveryLoop() {
		byte[] requestBuffer = new byte[256];

		while (running) {
			DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length);
			try {
				socket.receive(requestPacket);

				String message = new String(requestPacket.getData(), 0, requestPacket.getLength(), StandardCharsets.UTF_8)
						.trim();
				if (!NetworkDiscovery.isDiscoveryRequest(message))
					continue;

				String responseMessage = NetworkDiscovery.buildDiscoveryResponse(gamePort, protocolType);
				byte[] responseBytes = responseMessage.getBytes(StandardCharsets.UTF_8);
				DatagramPacket responsePacket = new DatagramPacket(
						responseBytes,
						responseBytes.length,
						requestPacket.getAddress(),
						requestPacket.getPort());

				socket.send(responsePacket);
				System.out.println("discovery --> replied to "
						+ requestPacket.getAddress().getHostAddress() + ":" + requestPacket.getPort()
						+ " with " + responseMessage);
			} catch (IOException e) {
				if (socket.isClosed())
					break;

				System.err.println("Exception generated while trying to respond to discovery requests.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() {
		running = false;
		socket.close();
	}
}
