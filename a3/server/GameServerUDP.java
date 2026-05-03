package a3.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {
	private final ServerGameState gameState;

	public GameServerUDP(int localPort) throws IOException {
		super(localPort, ProtocolType.UDP);
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
				case "join":
					if (!hasTokenCount(messageTokens, 2, message))
						return;
					IClientInfo ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID joinClientId = UUID.fromString(messageTokens[1]);
					addClient(ci, joinClientId);
					System.out.println("Join request received from - " + joinClientId.toString());
					sendJoinedMessage(joinClientId, true);
					gameState.onJoin(joinClientId);
					return;

				case "bye":
					if (!hasTokenCount(messageTokens, 2, message))
						return;
					UUID byeClientId = UUID.fromString(messageTokens[1]);
					System.out.println("Exit request received from - " + byeClientId.toString());
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
							messageTokens[7]
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

				default:
					System.out.println("server ignored unknown packet --> " + message);
			}
		} catch (IllegalArgumentException e) {
			System.out.println("server ignored malformed packet --> " + message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Message Format: (join,success) or (join,failure)
	public void sendJoinedMessage(UUID clientID, boolean success) {
		try {
			String message = "join,";
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

	// Message Format: (bye,remoteId)
	public void sendByeMessages(UUID clientID) {
		try {
			String message = "bye," + clientID.toString();
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Message Format: (create,remoteId,avatarType,x,y,z,yaw)
	public void sendCreateMessages(UUID clientID, String avatarType, String[] position, String yaw) {
		try {
			String message = "create," + clientID.toString();
			message += "," + avatarType;
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

	// Message Format: (dsfr,clientId,avatarType,x,y,z,yaw)
	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String avatarType, String[] position, String yaw) {
		try {
			String message = "dsfr," + clientID.toString();
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

	// Message Format: (wsds,remoteId)
	public void sendWantsDetailsMessages(UUID clientID) {
		try {
			String message = "wsds," + clientID.toString();
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Message Format: (move,remoteId,x,y,z,yaw)
	public void sendMoveMessages(UUID clientID, String[] position, String yaw) {
		try {
			String message = "move," + clientID.toString();
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

	// Message Format: (build,remoteId,d1,d2,d3,d4,d5,d6)
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

	// Message Format: (removebuild,remoteId,d1,d2,d3,d4,d5,d6)
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

	private void forwardGameplayMessage(String message, UUID clientID) {
		try {
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
