package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import org.joml.Vector3f;

import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient {
	private final MyGame game;
	private final GhostManager ghostManager;
	private final UUID id;

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game)
			throws IOException {
		super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}

	public UUID getID() {
		return id;
	}

	@Override
	protected void processPacket(Object message) {
		if (message == null) {
			System.out.println("message received --> null");
			return;
		}

		if (!(message instanceof String)) {
			System.out.println("message received was not a string");
			return;
		}

		String strMessage = (String) message;

		if (strMessage.isBlank()) {
			System.out.println("message received was empty");
			return;
		}

		System.out.println("message received -->" + strMessage);

		String[] messageTokens = strMessage.split(",");

		if (messageTokens.length == 0)
			return;

		// Game specific protocol to handle the message
		if (messageTokens.length > 0) {
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if (messageTokens[0].compareTo("join") == 0) {
				if (messageTokens.length < 2) {
					System.out.println("bad join packet: " + strMessage);
					return;
				}
				if (messageTokens[1].compareTo("success") == 0) {
					System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getSelectedAvatarType(), game.getPlayerPosition(), game.getPlayerYaw());
				}
				if (messageTokens[1].compareTo("failure") == 0) {
					System.out.println("join failure confirmed");
					game.setIsConnected(false);
				}
			}

			// Handle BYE message
			// Format: (bye,remoteId)
			if (messageTokens[0].compareTo("bye") == 0) { // remove ghost avatar with id = remoteId
				if (messageTokens.length < 2) {
					System.out.println("bad bye packet: " + strMessage);
					return;
				}
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}

			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0)) {
				if (messageTokens.length < 7) {
					System.out.println("bad create/dsfr packet: " + strMessage);
					return;
				}

				UUID ghostID = UUID.fromString(messageTokens[1]);
				String avatarType = messageTokens[2];

				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]));

				float ghostYaw = Float.parseFloat(messageTokens[6]);

				try {
					ghostManager.createGhostAvatar(ghostID, avatarType, ghostPosition, ghostYaw);
				} catch (IOException e) {
					System.out.println("error creating ghost avatar");
				}
			}

			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0) {
				if (messageTokens.length < 2) {
					System.out.println("bad wsds packet: " + strMessage);
					return;
				}
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getSelectedAvatarType(), game.getPlayerPosition(),
						game.getPlayerYaw());
			}

			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0) {
				if (messageTokens.length < 6) {
					System.out.println("bad move packet: " + strMessage);
					return;
				}
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);

				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
						Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]));

				float ghostYaw = Float.parseFloat(messageTokens[5]);

				ghostManager.updateGhostAvatar(ghostID, ghostPosition, ghostYaw);
			}

			// Handle BUILD message
			if (messageTokens[0].compareTo("build") == 0) {
				if (messageTokens.length < 8)
					return;
				int pieceType = Integer.parseInt(messageTokens[2]);
				int modeType = Integer.parseInt(messageTokens[3]);
				int roofDir = Integer.parseInt(messageTokens[4]);
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]),
						Float.parseFloat(messageTokens[7]));
				game.applyRemoteBuild(pieceType, modeType, roofDir, pos);
			}

			// Handle REMOVEBUILD message
			if (messageTokens[0].compareTo("removebuild") == 0) {
				if (messageTokens.length < 8)
					return;
				int pieceType = Integer.parseInt(messageTokens[2]);
				int modeType = Integer.parseInt(messageTokens[3]);
				int roofDir = Integer.parseInt(messageTokens[4]);
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]),
						Float.parseFloat(messageTokens[7]));
				game.applyRemoteRemoveBuild(pieceType, modeType, roofDir, pos);
			}

			// Handle PHOTO message
			if (messageTokens[0].compareTo("photo") == 0) {
				if (messageTokens.length < 3)
					return;
				int pyramidIndex = Integer.parseInt(messageTokens[2]);
				game.applyRemotePhoto(pyramidIndex);
			}

			// Handle PLACEPHOTOS message
			if (messageTokens[0].compareTo("placephotos") == 0) {
				game.applyRemotePlacePhotos();
			}
		}
	}

	// The initial message from the game client requesting to join the
	// server. localId is a unique identifier for the client. Recommend
	// a random UUID.
	// Message Format: (join,localId)

	public void sendJoinMessage() {
		try {
			sendPacket("join," + id.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server that the client is leaving the server.
	// Message Format: (bye,localId)

	public void sendByeMessage() {
		try {
			sendPacket("bye," + id.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server of the clients Avatars position. The server
	// takes this message and forwards it to all other clients registered
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the
	// position

	public void sendCreateMessage(String avatarType, Vector3f position, float yaw) {
		try {
			String message = "create," + id.toString();
			message += "," + avatarType;
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server of the local avatar's position. The server then
	// forwards this message to the client with the ID value matching remoteId.
	// This message is generated in response to receiving a WANTS_DETAILS message
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the
	// position.

	public void sendDetailsForMessage(UUID remoteId, String avatarType, Vector3f position, float yaw) {
		try {
			String message = "dsfr," + id.toString() + "," + remoteId.toString() + "," + avatarType;
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has changed position.
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the
	// position.

	public void sendMoveMessage(Vector3f position, float yaw) {
		try {
			String message = "move," + id.toString();
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;

			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBuildMessage(int pieceType, int modeType, int roofDir, Vector3f pos) {
		try {
			String message = "build," + id.toString()
					+ "," + pieceType + "," + modeType + "," + roofDir
					+ "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRemoveBuildMessage(int pieceType, int modeType, int roofDir, Vector3f pos) {
		try {
			String message = "removebuild," + id.toString()
					+ "," + pieceType + "," + modeType + "," + roofDir
					+ "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPhotoMessage(int pyramidIndex) {
		try {
			String message = "photo," + id.toString() + "," + pyramidIndex;
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPlacePhotosMessage() {
		try {
			String message = "placephotos," + id.toString();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
