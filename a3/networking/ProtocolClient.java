package a3.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.Vector3f;

import a3.GameConstants;
import a3.MyGame;
import tage.networking.IGameConnection.ProtocolType;
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

	private void sendLoggedPacket(String message) throws IOException {
		System.out.println("sending --> " + message);
		sendPacket(message);
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

			// Handle CREATE and DETAILS_FOR messages from the server.
			// Formats: (create,remoteId,avatarType,x,y,z,yaw)
			// and      (dsfr,remoteId,avatarType,x,y,z,yaw)
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

			// Handle MOVE message.
			// Format: (move,remoteId,x,y,z,yaw)
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
				int materialType = messageTokens.length >= 9 ? Integer.parseInt(messageTokens[8]) : GameConstants.BUILD_MATERIAL_WOOD;
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]),
						Float.parseFloat(messageTokens[7]));
				game.applyRemoteBuild(pieceType, modeType, roofDir, materialType, pos);
			}

			// Handle REMOVEBUILD message
			if (messageTokens[0].compareTo("removebuild") == 0) {
				if (messageTokens.length < 8)
					return;
				int pieceType = Integer.parseInt(messageTokens[2]);
				int modeType = Integer.parseInt(messageTokens[3]);
				int roofDir = Integer.parseInt(messageTokens[4]);
				int materialType = messageTokens.length >= 9 ? Integer.parseInt(messageTokens[8]) : GameConstants.BUILD_MATERIAL_WOOD;
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]),
						Float.parseFloat(messageTokens[7]));
				game.applyRemoteRemoveBuild(pieceType, modeType, roofDir, materialType, pos);
			}

			// Handle ROLE message
			// Format: (role,remoteId,0|1)
			if (messageTokens[0].compareTo("role") == 0) {
				if (messageTokens.length < 3)
					return;
				UUID remoteId = UUID.fromString(messageTokens[1]);
				boolean zombie = Integer.parseInt(messageTokens[2]) != 0;
				game.applyRemoteRole(remoteId, zombie);
			}

			// Handle TAG message
			// Format: (tag,sourceId,targetId)
			if (messageTokens[0].compareTo("tag") == 0) {
				if (messageTokens.length < 3)
					return;
				UUID sourceId = UUID.fromString(messageTokens[1]);
				UUID targetId = UUID.fromString(messageTokens[2]);
				game.applyRemoteTag(sourceId, targetId);
			}

			// Handle ESCAPE message
			// Format: (escape,playerId)
			if (messageTokens[0].compareTo("escape") == 0) {
				if (messageTokens.length < 2)
					return;
				UUID playerId = UUID.fromString(messageTokens[1]);
				game.applyHumanLastChanceEscape(playerId);
			}

			// Handle ABILITY message
			// Format: (ability,sourceId,abilityName,0|1)
			if (messageTokens[0].compareTo("ability") == 0) {
				if (messageTokens.length < 4)
					return;
				UUID sourceId = UUID.fromString(messageTokens[1]);
				String ability = messageTokens[2];
				boolean active = Integer.parseInt(messageTokens[3]) != 0;
				game.applyRemoteAbility(sourceId, ability, active);
			}

			// Handle PROJECTILE message
			// Format: (projectile,sourceId,type,x,y,z,vx,vy,vz)
			if (messageTokens[0].compareTo("projectile") == 0) {
				if (messageTokens.length < 9)
					return;
				UUID sourceId = UUID.fromString(messageTokens[1]);
				int projectileType = Integer.parseInt(messageTokens[2]);
				Vector3f pos = new Vector3f(
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]));
				Vector3f velocity = new Vector3f(
						Float.parseFloat(messageTokens[6]),
						Float.parseFloat(messageTokens[7]),
						Float.parseFloat(messageTokens[8]));
				game.applyRemoteProjectile(sourceId, projectileType, pos, velocity);
			}

			// Handle SLOW message
			// Format: (slow,sourceId,targetId,seconds)
			if (messageTokens[0].compareTo("slow") == 0) {
				if (messageTokens.length < 4)
					return;
				UUID sourceId = UUID.fromString(messageTokens[1]);
				UUID targetId = UUID.fromString(messageTokens[2]);
				float seconds = Float.parseFloat(messageTokens[3]);
				game.applyRemoteSlow(sourceId, targetId, seconds);
			}

			// Handle BLIND message
			// Format: (blind,sourceId,targetId,seconds)
			if (messageTokens[0].compareTo("blind") == 0) {
				if (messageTokens.length < 4)
					return;
				UUID sourceId = UUID.fromString(messageTokens[1]);
				UUID targetId = UUID.fromString(messageTokens[2]);
				float seconds = Float.parseFloat(messageTokens[3]);
				game.applyRemoteBlind(sourceId, targetId, seconds);
			}

			// Handle PICKUPSTATE message
			// Format: (pickupstate,id,type,0|1,x,z,respawnEpochMs)
			if (messageTokens[0].compareTo("pickupstate") == 0) {
				if (messageTokens.length < 7)
					return;
				int pickupId = Integer.parseInt(messageTokens[1]);
				int pickupType = Integer.parseInt(messageTokens[2]);
				boolean active = Integer.parseInt(messageTokens[3]) != 0;
				float x = Float.parseFloat(messageTokens[4]);
				float z = Float.parseFloat(messageTokens[5]);
				long respawnEpochMs = Long.parseLong(messageTokens[6]);
				game.applyServerPickupState(pickupId, pickupType, active, x, z, respawnEpochMs);
			}

			// Handle PICKUPSPAWN message
			// Format: (pickupspawn,id,type,x,z)
			if (messageTokens[0].compareTo("pickupspawn") == 0) {
				if (messageTokens.length < 5)
					return;
				int pickupId = Integer.parseInt(messageTokens[1]);
				int pickupType = Integer.parseInt(messageTokens[2]);
				float x = Float.parseFloat(messageTokens[3]);
				float z = Float.parseFloat(messageTokens[4]);
				game.applyServerPickupSpawn(pickupId, pickupType, x, z);
			}

			// Handle PICKUPHIDE message
			// Format: (pickuphide,id,respawnEpochMs)
			if (messageTokens[0].compareTo("pickuphide") == 0) {
				if (messageTokens.length < 3)
					return;
				int pickupId = Integer.parseInt(messageTokens[1]);
				long respawnEpochMs = Long.parseLong(messageTokens[2]);
				game.applyServerPickupHide(pickupId, respawnEpochMs);
			}

			// Handle PICKUPGRANT message
			// Format: (pickupgrant,id,type)
			if (messageTokens[0].compareTo("pickupgrant") == 0) {
				if (messageTokens.length < 3)
					return;
				int pickupId = Integer.parseInt(messageTokens[1]);
				int pickupType = Integer.parseInt(messageTokens[2]);
				game.applyServerPickupGrant(pickupId, pickupType);
			}

			// Handle ROUND message
			// Formats: round,waiting | round,countdown,endEpochMs |
			//          round,start,zombieId,endEpochMs | round,end,humans|zombies
			if (messageTokens[0].compareTo("round") == 0) {
				if (messageTokens.length < 2)
					return;
				String phase = messageTokens[1];
				if ("waiting".equals(phase)) {
					game.applyServerRoundWaiting();
				} else if ("countdown".equals(phase) && messageTokens.length >= 3) {
					game.applyServerRoundCountdown(Long.parseLong(messageTokens[2]));
				} else if ("start".equals(phase) && messageTokens.length >= 4) {
					UUID zombieId = UUID.fromString(messageTokens[2]);
					game.applyServerRoundStart(zombieId, Long.parseLong(messageTokens[3]));
				} else if ("end".equals(phase) && messageTokens.length >= 3) {
					game.applyServerRoundEnd(messageTokens[2]);
				}
			}

			// Handle HEALTH message
			// Format: (health,remoteId,value)
			if (messageTokens[0].compareTo("health") == 0) {
				if (messageTokens.length < 3)
					return;
				UUID remoteId = UUID.fromString(messageTokens[1]);
				int health = Integer.parseInt(messageTokens[2]);
				game.applyRemoteHealth(remoteId, health);
			}

			// Handle ANIM message
			// Format: (anim,remoteId,animationName)
			if (messageTokens[0].compareTo("anim") == 0) {
				if (messageTokens.length < 3)
					return;
				UUID remoteId = UUID.fromString(messageTokens[1]);
				game.applyRemoteAnimation(remoteId, messageTokens[2]);
			}

			// Handle SOUND message
			// Format: (sound,remoteId,soundType,x,y,z)
			if (messageTokens[0].compareTo("sound") == 0) {
				if (messageTokens.length < 6)
					return;
				UUID remoteId = UUID.fromString(messageTokens[1]);
				Vector3f location = new Vector3f(
						Float.parseFloat(messageTokens[3]),
						Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]));
				game.applyRemoteSound(remoteId, messageTokens[2], location);
			}

			// Handle SMILE NPC state message
			// Format: (smile,sourceId,index,0|1,x,y,z,yaw,animationName)
			// Old format accepted: (smile,sourceId,0|1,x,y,z,yaw,animationName)
			if (messageTokens[0].compareTo("smile") == 0) {
				if (messageTokens.length < 8)
					return;
				UUID sourceId = UUID.fromString(messageTokens[1]);
				int offset = messageTokens.length >= 9 ? 1 : 0;
				int index = offset == 1 ? Integer.parseInt(messageTokens[2]) : 0;
				boolean spawned = Integer.parseInt(messageTokens[2 + offset]) != 0;
				Vector3f position = new Vector3f(
						Float.parseFloat(messageTokens[3 + offset]),
						Float.parseFloat(messageTokens[4 + offset]),
						Float.parseFloat(messageTokens[5 + offset]));
				float yaw = Float.parseFloat(messageTokens[6 + offset]);
				game.applyRemoteSmilingManState(index, sourceId, spawned, position, yaw, messageTokens[7 + offset]);
			}

			// Handle SMILEBLIND message
			// Format: (smileblind,sourceId,index)
			if (messageTokens[0].compareTo("smileblind") == 0) {
				if (messageTokens.length < 2)
					return;
				int index = messageTokens.length >= 3 ? Integer.parseInt(messageTokens[2]) : 0;
				game.applyRemoteSmilingManBlind(index, UUID.fromString(messageTokens[1]));
			}
		}
	}

	// The initial message from the game client requesting to join the
	// server. localId is a unique identifier for the client. Recommend
	// a random UUID.
	// Message Format: (join,localId)

	public void sendJoinMessage() {
		try {
			sendLoggedPacket("join," + id.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server that the client is leaving the server.
	// Message Format: (bye,localId)

	public void sendByeMessage() {
		try {
			sendLoggedPacket("bye," + id.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server of the clients Avatars position. The server
	// takes this message and forwards it to all other clients registered
	// with the server.
	// Message Format: (create,localId,avatarType,x,y,z,yaw)

	public void sendCreateMessage(String avatarType, Vector3f position, float yaw) {
		try {
			String message = "create," + id.toString();
			message += "," + avatarType;
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;

			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server of the local avatar's position. The server then
	// forwards this message to the client with the ID value matching remoteId.
	// This message is generated in response to receiving a WANTS_DETAILS message
	// from the server.
	// Message Format: (dsfr,localId,remoteId,avatarType,x,y,z,yaw)

	public void sendDetailsForMessage(UUID remoteId, String avatarType, Vector3f position, float yaw) {
		try {
			String message = "dsfr," + id.toString() + "," + remoteId.toString() + "," + avatarType;
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;

			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has changed position.
	// Message Format: (move,localId,x,y,z,yaw)

	public void sendMoveMessage(Vector3f position, float yaw) {
		try {
			String message = "move," + id.toString();
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;

			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBuildMessage(int pieceType, int modeType, int roofDir, int materialType, Vector3f pos) {
		try {
			String message = "build," + id.toString()
					+ "," + pieceType + "," + modeType + "," + roofDir
					+ "," + pos.x() + "," + pos.y() + "," + pos.z()
					+ "," + materialType;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRemoveBuildMessage(int pieceType, int modeType, int roofDir, int materialType, Vector3f pos) {
		try {
			String message = "removebuild," + id.toString()
					+ "," + pieceType + "," + modeType + "," + roofDir
					+ "," + pos.x() + "," + pos.y() + "," + pos.z()
					+ "," + materialType;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRoleMessage(boolean zombie) {
		try {
			String message = "role," + id.toString() + "," + (zombie ? 1 : 0);
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendTagMessage(UUID targetId) {
		try {
			String message = "tag," + id.toString() + "," + targetId.toString();
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendAbilityMessage(String ability, boolean active) {
		try {
			String message = "ability," + id.toString() + "," + ability + "," + (active ? 1 : 0);
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendProjectileMessage(int projectileType, Vector3f pos, Vector3f velocity) {
		try {
			String message = "projectile," + id.toString() + "," + projectileType;
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			message += "," + velocity.x() + "," + velocity.y() + "," + velocity.z();
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendSlowMessage(UUID targetId, float seconds) {
		try {
			String message = "slow," + id.toString() + "," + targetId.toString() + "," + seconds;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendBlindMessage(UUID targetId, float seconds) {
		try {
			String message = "blind," + id.toString() + "," + targetId.toString() + "," + seconds;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPickupCollectMessage(int pickupId) {
		try {
			String message = "pickupcollect," + id.toString() + "," + pickupId;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHealthMessage(int health) {
		try {
			String message = "health," + id.toString() + "," + health;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendAnimationMessage(String animationName) {
		try {
			String message = "anim," + id.toString() + "," + animationName;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendSoundMessage(String soundType, Vector3f location) {
		try {
			String message = "sound," + id.toString() + "," + soundType;
			message += "," + location.x();
			message += "," + location.y();
			message += "," + location.z();
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendSmilingManStateMessage(boolean spawned, Vector3f position, float yaw, String animationName) {
		sendSmilingManStateMessage(0, spawned, position, yaw, animationName);
	}

	public void sendSmilingManStateMessage(int index, boolean spawned, Vector3f position, float yaw, String animationName) {
		try {
			String message = "smile," + id.toString() + "," + index + "," + (spawned ? 1 : 0);
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + yaw;
			message += "," + (animationName == null || animationName.isBlank() ? "none" : animationName);
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendSmilingManDamageMessage(UUID targetId, int amount) {
		if (targetId == null) return;
		try {
			String message = "smiledamage," + id.toString() + "," + targetId.toString() + "," + amount;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendSmilingManBlindMessage() {
		sendSmilingManBlindMessage(0);
	}

	public void sendSmilingManBlindMessage(int index) {
		try {
			String message = "smileblind," + id.toString() + "," + index;
			sendLoggedPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
