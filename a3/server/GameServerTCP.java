package a3.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerTCP extends GameConnectionServer<UUID> {
	public GameServerTCP(int localPort) throws IOException {
		super(localPort, ProtocolType.TCP);
	}

	private void logInboundPacket(String message, InetAddress senderIP, int senderPort) {
		if (senderIP != null)
			System.out.println("server received --> " + message + " from " + senderIP.getHostAddress() + ":" + senderPort);
		else
			System.out.println("server received --> " + message);
	}

	private void logDirectPacket(String message, UUID targetId) {
		System.out.println("server sending --> " + message + " to " + targetId.toString());
	}

	private void logForwardPacket(String message, UUID sourceId) {
		System.out.println("server forwarding --> " + message + " from " + sourceId.toString());
	}

	private boolean hasTokenCount(String[] messageTokens, int minCount, String message) {
		if (messageTokens.length < minCount) {
			System.out.println("server ignored malformed packet --> " + message);
			return false;
		}
		return true;
	}

	@Override
	public void acceptClient(IClientInfo ci, Object o) {
		if (!(o instanceof String)) {
			System.out.println("server ignored non-string packet");
			return;
		}

		String message = ((String) o).trim();
		if (message.isEmpty())
			return;

		logInboundPacket(message, null, -1);
		String[] messageTokens = message.split(",");
		if (!"join".equals(messageTokens[0]))
			return;

		if (!hasTokenCount(messageTokens, 2, message))
			return;

		try {
			UUID clientID = UUID.fromString(messageTokens[1]);
			addClient(ci, clientID);
			sendJoinedMessage(clientID, true);
		} catch (IllegalArgumentException e) {
			System.out.println("server ignored malformed packet --> " + message);
		}
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		if (!(o instanceof String)) {
			System.out.println("server ignored non-string packet");
			return;
		}

		String message = ((String) o).trim();
		if (message.isEmpty())
			return;

		logInboundPacket(message, senderIP, senderPort);
		String[] messageTokens = message.split(",");
		try {
			switch (messageTokens[0]) {
				case "bye":
					if (!hasTokenCount(messageTokens, 2, message))
						return;
					UUID byeClientId = UUID.fromString(messageTokens[1]);
					sendByeMessages(byeClientId);
					removeClient(byeClientId);
					return;

				case "create":
					if (!hasTokenCount(messageTokens, 7, message))
						return;
					UUID createClientId = UUID.fromString(messageTokens[1]);
					String createAvatarType = messageTokens[2];
					String[] createPos = { messageTokens[3], messageTokens[4], messageTokens[5] };
					String createYaw = messageTokens[6];
					sendCreateMessages(createClientId, createAvatarType, createPos, createYaw);
					sendWantsDetailsMessages(createClientId);
					return;

				case "dsfr":
					if (!hasTokenCount(messageTokens, 8, message))
						return;
					UUID detailsClientId = UUID.fromString(messageTokens[1]);
					UUID detailsRemoteId = UUID.fromString(messageTokens[2]);
					String detailsAvatarType = messageTokens[3];
					String[] detailsPos = { messageTokens[4], messageTokens[5], messageTokens[6] };
					String detailsYaw = messageTokens[7];
					sendDetailsForMessage(detailsClientId, detailsRemoteId, detailsAvatarType, detailsPos, detailsYaw);
					return;

				case "move":
					if (!hasTokenCount(messageTokens, 6, message))
						return;
					UUID moveClientId = UUID.fromString(messageTokens[1]);
					String[] movePos = { messageTokens[2], messageTokens[3], messageTokens[4] };
					String moveYaw = messageTokens[5];
					sendMoveMessages(moveClientId, movePos, moveYaw);
					return;

				case "build":
					if (!hasTokenCount(messageTokens, 8, message))
						return;
					UUID buildClientId = UUID.fromString(messageTokens[1]);
					String[] buildData = {
							messageTokens[2],
							messageTokens[3],
							messageTokens[4],
							messageTokens[5],
							messageTokens[6],
							messageTokens[7]
					};
					sendBuildMessages(buildClientId, buildData);
					return;

				case "removebuild":
					if (!hasTokenCount(messageTokens, 8, message))
						return;
					UUID removeBuildClientId = UUID.fromString(messageTokens[1]);
					String[] removeBuildData = {
							messageTokens[2],
							messageTokens[3],
							messageTokens[4],
							messageTokens[5],
							messageTokens[6],
							messageTokens[7]
					};
					sendRemoveBuildMessages(removeBuildClientId, removeBuildData);
					return;

				case "photo":
					if (!hasTokenCount(messageTokens, 3, message))
						return;
					UUID photoClientId = UUID.fromString(messageTokens[1]);
					sendPhotoMessages(photoClientId, messageTokens[2]);
					return;

				case "placephotos":
					if (!hasTokenCount(messageTokens, 2, message))
						return;
					UUID placePhotosClientId = UUID.fromString(messageTokens[1]);
					sendPlacePhotosMessages(placePhotosClientId);
					return;

				default:
					System.out.println("server ignored unknown packet --> " + message);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("server ignored malformed packet --> " + message);
		}
	}

	/**
	 * Informs the client who just requested to join the server if their if their
	 * request was able to be granted.
	 * <p>
	 * Message Format: (join,success) or (join,failure)
	 */
	public void sendJoinedMessage(UUID clientID, boolean success) {
		try {
			String message = new String("join,");
			if (success)
				message += "success";
			else
				message += "failure";
			logDirectPacket(message, clientID);
			sendPacket(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Informs a client that the avatar with the identifier remoteId has left the
	 * server.
	 * This message is meant to be sent to all client currently connected to the
	 * server
	 * when a client leaves the server.
	 * <p>
	 * Message Format: (bye,remoteId)
	 */
	public void sendByeMessages(UUID clientID) {
		try {
			String message = new String("bye," + clientID.toString());
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sends a CREATE message to all other clients when a new player joins.
	// This tells everyone to create a ghost avatar using the player's id,
	// chosen avatar type, and current position.
	// Format: create,clientID,avatarType,x,y,z,yaw
	public void sendCreateMessages(UUID clientID, String avatarType, String[] position, String yaw) {
		if (position == null || position.length < 3) {
			System.out.println("create message missing position data");
			return;
		}

		try {
			String message = "create," + clientID.toString()
					+ "," + avatarType
					+ "," + position[0]
					+ "," + position[1]
					+ "," + position[2]
					+ "," + yaw;

			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sends updated position info from one client to another specific client.
	// This is used when a new player joins and needs the current state of others.
	// Format: dsfr,clientID,avatarType,x,y,z,yaw
	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String avatarType, String[] position, String yaw) {
		try {
			String message = new String("dsfr," + clientID.toString());
			message += "," + avatarType;
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			logDirectPacket(message, remoteId);
			sendPacket(message, remoteId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Informs a local client that a remote client wants the local clients avatars
	 * information.
	 * This message is meant to be sent to all clients connected to the server when
	 * a new client
	 * joins the server.
	 * <p>
	 * Message Format: (wsds,remoteId)
	 */
	public void sendWantsDetailsMessages(UUID clientID) {
		try {
			String message = new String("wsds," + clientID.toString());
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Informs a client that a remote clients avatar has changed position. x, y, and
	 * z represent
	 * the new position of the remote avatar. This message is meant to be forwarded
	 * to all clients
	 * connected to the server when it receives a MOVE message from the remote
	 * client.
	 * <p>
	 * Message Format: (move,remoteId,x,y,z) where x, y, and z represent the
	 * position.
	 */
	public void sendMoveMessages(UUID clientID, String[] position, String yaw) {
		try {
			String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBuildMessages(UUID clientID, String[] buildData) {
		try {
			String message = "build," + clientID.toString()
					+ "," + buildData[0]
					+ "," + buildData[1]
					+ "," + buildData[2]
					+ "," + buildData[3]
					+ "," + buildData[4]
					+ "," + buildData[5];
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRemoveBuildMessages(UUID clientID, String[] buildData) {
		try {
			String message = "removebuild," + clientID.toString()
					+ "," + buildData[0]
					+ "," + buildData[1]
					+ "," + buildData[2]
					+ "," + buildData[3]
					+ "," + buildData[4]
					+ "," + buildData[5];
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPhotoMessages(UUID clientID, String pyramidIndex) {
		try {
			String message = "photo," + clientID.toString() + "," + pyramidIndex;
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPlacePhotosMessages(UUID clientID) {
		try {
			String message = "placephotos," + clientID.toString();
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
