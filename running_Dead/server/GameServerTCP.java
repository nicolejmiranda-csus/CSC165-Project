package running_Dead.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

/**
 * TCP version of the Running_Dead relay server.
 * It mirrors the UDP server behavior for classes/tests that choose TCP instead of UDP.
 * Connected to: Created by NetworkingServer for TCP mode; delegates authoritative state to ServerGameState.
 */
public class GameServerTCP extends GameConnectionServer<UUID> {
	private final ServerGameState gameState;

	public GameServerTCP(int localPort) throws IOException {
		super(localPort, ProtocolType.TCP);
		gameState = new ServerGameState(new ServerMessenger() {
			@Override
			public void sendTo(UUID targetId, String message) {
				try {
					logDirectPacket(message, targetId);
					sendPacket(message, targetId);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
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
			gameState.onJoin(clientID);
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
					gameState.onBye(byeClientId);
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
					String createName = messageTokens.length >= 8 ? messageTokens[7] : "Player";
					sendCreateMessages(createClientId, createAvatarType, createPos, createYaw, createName);
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
					String detailsName = messageTokens.length >= 9 ? messageTokens[8] : "Player";
					sendDetailsForMessage(detailsClientId, detailsRemoteId, detailsAvatarType, detailsPos, detailsYaw, detailsName);
					return;

				case "move":
					if (!hasTokenCount(messageTokens, 6, message))
						return;
					UUID moveClientId = UUID.fromString(messageTokens[1]);
					String[] movePos = { messageTokens[2], messageTokens[3], messageTokens[4] };
					String moveYaw = messageTokens[5];
					String equippedItem = messageTokens.length >= 7 ? messageTokens[6] : "0";
					String flashlightOn = messageTokens.length >= 8 ? messageTokens[7] : "0";
					String[] flashlightDir = messageTokens.length >= 11
							? new String[] { messageTokens[8], messageTokens[9], messageTokens[10] }
							: new String[] { "0", "0", "-1" };
					sendMoveMessages(moveClientId, movePos, moveYaw, equippedItem, flashlightOn, flashlightDir);
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
							messageTokens[7],
							messageTokens.length >= 9 ? messageTokens[8] : "0"
					};
					gameState.onBuild(buildClientId, buildData);
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
							messageTokens[7],
							messageTokens.length >= 9 ? messageTokens[8] : "0"
					};
					gameState.onRemoveBuild(removeBuildClientId, removeBuildData);
					sendRemoveBuildMessages(removeBuildClientId, removeBuildData);
					return;

				case "role":
					if (!hasTokenCount(messageTokens, 3, message))
						return;
					gameState.onRole(UUID.fromString(messageTokens[1]), Integer.parseInt(messageTokens[2]) != 0);
					return;

				case "tag":
					if (!hasTokenCount(messageTokens, 3, message))
						return;
					gameState.onTag(UUID.fromString(messageTokens[1]), UUID.fromString(messageTokens[2]));
					return;

				case "pickupcollect":
					if (!hasTokenCount(messageTokens, 3, message))
						return;
					gameState.onPickupCollect(UUID.fromString(messageTokens[1]), Integer.parseInt(messageTokens[2]));
					return;

				case "health":
					if (!hasTokenCount(messageTokens, 3, message))
						return;
					gameState.onHealth(UUID.fromString(messageTokens[1]), Integer.parseInt(messageTokens[2]));
					return;

				case "anim":
					if (!hasTokenCount(messageTokens, 3, message))
						return;
					gameState.onAnimation(UUID.fromString(messageTokens[1]), messageTokens[2]);
					return;

				case "sound":
					if (!hasTokenCount(messageTokens, 6, message))
						return;
					forwardGameplayMessage(message, UUID.fromString(messageTokens[1]));
					return;

				case "ability":
					if (!hasTokenCount(messageTokens, 4, message))
						return;
					UUID abilityClientId = UUID.fromString(messageTokens[1]);
					if (gameState.onAbility(abilityClientId, messageTokens[2], Integer.parseInt(messageTokens[3]) != 0))
						forwardGameplayMessage(message, abilityClientId);
					return;

				case "projectile":
					if (!hasTokenCount(messageTokens, 9, message))
						return;
					gameState.onProjectile(UUID.fromString(messageTokens[1]), Integer.parseInt(messageTokens[2]), message);
					return;

				case "slow":
					if (!hasTokenCount(messageTokens, 4, message))
						return;
					gameState.onSlow(UUID.fromString(messageTokens[1]), UUID.fromString(messageTokens[2]), Float.parseFloat(messageTokens[3]));
					return;

				case "blind":
					if (!hasTokenCount(messageTokens, 4, message))
						return;
					gameState.onBlind(UUID.fromString(messageTokens[1]), UUID.fromString(messageTokens[2]), Float.parseFloat(messageTokens[3]));
					return;

				case "smile":
					if (!hasTokenCount(messageTokens, 8, message))
						return;
					forwardQuietGameplayMessage(message, UUID.fromString(messageTokens[1]));
					return;

				case "smileblind":
					if (!hasTokenCount(messageTokens, 2, message))
						return;
					forwardGameplayMessage(message, UUID.fromString(messageTokens[1]));
					return;

				case "smiledamage":
					if (!hasTokenCount(messageTokens, 4, message))
						return;
					gameState.onSmilingManDamage(
							UUID.fromString(messageTokens[1]),
							UUID.fromString(messageTokens[2]),
							Integer.parseInt(messageTokens[3]));
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
	// Format: create,clientID,avatarType,x,y,z,yaw,playerName
	public void sendCreateMessages(UUID clientID, String avatarType, String[] position, String yaw, String playerName) {
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
					+ "," + yaw
					+ "," + playerName;

			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sends updated position info from one client to another specific client.
	// This is used when a new player joins and needs the current state of others.
	// Format: dsfr,clientID,avatarType,x,y,z,yaw,playerName
	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String avatarType, String[] position, String yaw, String playerName) {
		try {
			String message = new String("dsfr," + clientID.toString());
			message += "," + avatarType;
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			message += "," + playerName;
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
	 * Message Format: (move,remoteId,x,y,z,yaw,equippedItem,flashlightOn,flashDirX,flashDirY,flashDirZ)
	 */
	public void sendMoveMessages(UUID clientID, String[] position, String yaw, String equippedItem, String flashlightOn, String[] flashlightDir) {
		try {
			String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
			message += "," + equippedItem;
			message += "," + flashlightOn;
			message += "," + flashlightDir[0];
			message += "," + flashlightDir[1];
			message += "," + flashlightDir[2];
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBuildMessages(UUID clientID, String[] buildData) {
		try {
			String message = "build," + clientID.toString();
			for (String value : buildData) message += "," + value;
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRemoveBuildMessages(UUID clientID, String[] buildData) {
		try {
			String message = "removebuild," + clientID.toString();
			for (String value : buildData) message += "," + value;
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardGameplayMessage(String message, UUID clientID) {
		try {
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardQuietGameplayMessage(String message, UUID clientID) {
		try {
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
