package Server;

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

	@Override
	public void acceptClient(IClientInfo ci, Object o) {
		String message = (String) o;
		String[] messageTokens = message.split(",");

		if (messageTokens.length > 0) { // JOIN -- Case where client just joined the server
										// Received Message Format: (join,localId)
			if (messageTokens[0].compareTo("join") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				addClient(ci, clientID);
				sendJoinedMessage(clientID, true);
			}
		}
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		String message = (String) o;
		String[] messageTokens = message.split(",");

		if (messageTokens.length > 0) { // Case where client just joined the server
										// Received Message Format: (join,localId)
			// Case where clients leaves the server
			// Received Message Format: (bye,localId)
			if (messageTokens[0].compareTo("bye") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
			}

			// Case where server receives a CREATE message
			// Received Message Format: (create,localId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String avatarType = messageTokens[2];
				String[] pos = { messageTokens[3], messageTokens[4], messageTokens[5] };
				String yaw = messageTokens[6];
				sendCreateMessages(clientID, avatarType, pos, yaw);
				sendWantsDetailsMessages(clientID);
			}

			// Case where server receives a DETAILS-FOR message
			// Received Message Format: (dsfr,localId,remoteId,x,y,z)
			if (messageTokens[0].compareTo("dsfr") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String avatarType = messageTokens[3];
				String[] pos = { messageTokens[4], messageTokens[5], messageTokens[6] };
				String yaw = messageTokens[7];
				sendDetailsForMessage(clientID, remoteID, avatarType, pos, yaw);
			}

			// Case where server receives a MOVE message
			// Received Message Format: (move,localId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = { messageTokens[2], messageTokens[3], messageTokens[4] };
				String yaw = messageTokens[5];
				sendMoveMessages(clientID, pos, yaw);
			}

			// Forward BUILD message
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
			// Forward REMOVEBUILD message
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
			// Forward PHOTO message
			if (messageTokens[0].compareTo("photo") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendPhotoMessages(clientID, messageTokens[2]);
			}
			// Forward PLACEPHOTOS message
			if (messageTokens[0].compareTo("placephotos") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendPlacePhotosMessages(clientID);
			}
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
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sends a CREATE message to all other clients when a new player joins.
	// This tells everyone to create a ghost avatar using the player's id,
	// chosen avatar type, and current position.
	// Format: create,clientID,avatarType,x,y,z
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

			System.out.println("forwarding create for " + clientID.toString());

			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Sends updated position info from one client to another specific client.
	// This is used when a new player joins and needs the current state of others.
	// Format: dsfr,clientID,x,y,z
	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String avatarType, String[] position, String yaw) {
		try {
			String message = new String("dsfr," + clientID.toString());
			message += "," + avatarType;
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + yaw;
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
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPhotoMessages(UUID clientID, String pyramidIndex) {
		try {
			String message = "photo," + clientID.toString() + "," + pyramidIndex;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPlacePhotosMessages(UUID clientID) {
		try {
			String message = "placephotos," + clientID.toString();
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
