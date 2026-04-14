package Server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {
	public GameServerUDP(int localPort) throws IOException {
		super(localPort, ProtocolType.UDP);
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

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		String message = (String) o;
		logInboundPacket(message, senderIP, senderPort);
		String[] messageTokens = message.split(",");

		if (messageTokens.length > 0) {

			// JOIN -- Received Message Format: (join,localId)
			if (messageTokens[0].compareTo("join") == 0) {
				try {
					IClientInfo ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);
					addClient(ci, clientID);
					System.out.println("Join request received from - " + clientID.toString());
					sendJoinedMessage(clientID, true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// BYE -- Received Message Format: (bye,localId)
			if (messageTokens[0].compareTo("bye") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("Exit request received from - " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
			}

			// CREATE -- Received Message Format: (create,localId,avatarType,x,y,z,yaw)
			if (messageTokens[0].compareTo("create") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String avatarType = messageTokens[2];
				String[] pos = { messageTokens[3], messageTokens[4], messageTokens[5] };
				String yaw = messageTokens[6];
				sendCreateMessages(clientID, avatarType, pos, yaw);
				sendWantsDetailsMessages(clientID);
			}

			// DETAILS-FOR -- Received Message Format:
			// (dsfr,localId,remoteId,avatarType,x,y,z,yaw)
			if (messageTokens[0].compareTo("dsfr") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String avatarType = messageTokens[3];
				String[] pos = { messageTokens[4], messageTokens[5], messageTokens[6] };
				String yaw = messageTokens[7];
				sendDetailsForMessage(clientID, remoteID, avatarType, pos, yaw);
			}

			// MOVE -- Received Message Format: (move,localId,x,y,z,yaw)
			if (messageTokens[0].compareTo("move") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = { messageTokens[2], messageTokens[3], messageTokens[4] };
				String yaw = messageTokens[5];
				sendMoveMessages(clientID, pos, yaw);
			}

			// BUILD -- Received Message Format: (build,localId,d1,d2,d3,d4,d5,d6)
			if (messageTokens[0].compareTo("build") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] buildData = {
						messageTokens[2],
						messageTokens[3],
						messageTokens[4],
						messageTokens[5],
						messageTokens[6],
						messageTokens[7]
				};
				sendBuildMessages(clientID, buildData);
			}

			// REMOVEBUILD -- Received Message Format:
			// (removebuild,localId,d1,d2,d3,d4,d5,d6)
			if (messageTokens[0].compareTo("removebuild") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] buildData = {
						messageTokens[2],
						messageTokens[3],
						messageTokens[4],
						messageTokens[5],
						messageTokens[6],
						messageTokens[7]
				};
				sendRemoveBuildMessages(clientID, buildData);
			}

			// PHOTO -- Received Message Format: (photo,localId,pyramidIndex)
			if (messageTokens[0].compareTo("photo") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String pyramidIndex = messageTokens[2];
				sendPhotoMessages(clientID, pyramidIndex);
			}

			// PLACEPHOTOS -- Received Message Format: (placephotos,localId)
			if (messageTokens[0].compareTo("placephotos") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendPlacePhotosMessages(clientID);
			}
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

	// Message Format: (dsfr,remoteId,avatarType,x,y,z,yaw)
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

	// Message Format: (photo,remoteId,pyramidIndex)
	public void sendPhotoMessages(UUID clientID, String pyramidIndex) {
		try {
			String message = "photo," + clientID.toString() + "," + pyramidIndex;
			logForwardPacket(message, clientID);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Message Format: (placephotos,remoteId)
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
