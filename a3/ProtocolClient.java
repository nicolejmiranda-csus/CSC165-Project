package a3;

import tage.networking.client.GameConnectionClient;
import tage.networking.IGameConnection.ProtocolType;
import org.joml.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Handles all client-side multiplayer network communication.
 *
 * Responsibilities:
 *  - Maintains a unique UUID for this client (randomly generated at startup).
 *  - Sends outbound messages to the server (join, create, move, bye, dsfr).
 *  - Receives and dispatches inbound messages from the server to update the game state.
 *
 * The framework calls processPackets() every frame (from MyGame.processNetworking),
 * which drains the receive queue and invokes processPacket() for each message.
 */
public class ProtocolClient extends GameConnectionClient {
    private MyGame game;               // reference to the game for reading player state and updating ghost avatars
    private UUID id;                   // this client's unique identifier — generated once at startup and never changes
    private GhostManager ghostManager; // convenience reference to the game's GhostManager

    /**
     * Connects to the server and initialises the client.
     * @param remAddr server IP address
     * @param remPort server port number
     * @param pType   protocol type (UDP in this project)
     * @param g       the running game instance
     */
    public ProtocolClient(InetAddress remAddr, int remPort,
                          ProtocolType pType, MyGame g) throws IOException {
        super(remAddr, remPort, pType);          // open the socket to the server
        this.game = g;
        this.id = UUID.randomUUID();             // generate a unique ID for this client session
        ghostManager = game.getGhostManager();   // cache the ghost manager for quick access in processPacket
    }

    /**
     * Called by the framework for each inbound packet.
     * Parses the comma-delimited message and handles the appropriate command.
     * @param msg the raw message object (cast to String)
     */
    @Override
    protected void processPacket(Object msg) {
        String strMessage = (String) msg;            // cast to String
        String[] msgTokens = strMessage.split(",");  // split on commas — first token is the command
        if (msgTokens.length == 0) return;           // ignore malformed/empty packets

        // --- join,success  or  join,failure ---
        // Server's response to our sendJoinMessage().
        if (msgTokens[0].compareTo("join") == 0) {
            if (msgTokens[1].compareTo("success") == 0) {
                game.setIsConnected(true);                                  // mark the game as connected
                sendCreateMessage(game.getPlayerPosition(), game.getAvatarName()); // announce ourselves to all other clients
            }
            if (msgTokens[1].compareTo("failure") == 0) {
                game.setIsConnected(false); // server rejected us; stay in a disconnected state
            }
        }

        // --- bye,remoteId ---
        // Another client has disconnected; remove their ghost from our scene.
        if (msgTokens[0].compareTo("bye") == 0) {
            UUID ghostID = UUID.fromString(msgTokens[1]); // UUID of the player who left
            ghostManager.removeGhostAvatar(ghostID);       // detach and discard their ghost object
        }

        // --- create,remoteId,x,y,z,avatarName ---
        // A new player has joined and the server is forwarding their spawn details to us.
        // Also handles "dsfr" (details-for) which uses the same payload format:
        // an existing client responding to a "wsds" request with their current state.
        if (msgTokens[0].compareTo("create") == 0 || msgTokens[0].compareTo("dsfr") == 0) {
            UUID ghostID = UUID.fromString(msgTokens[1]); // UUID of the remote player
            // Parse the three position components from tokens 2-4
            Vector3f ghostPosition = new Vector3f(
                Float.parseFloat(msgTokens[2]),  // x
                Float.parseFloat(msgTokens[3]),  // y
                Float.parseFloat(msgTokens[4])); // z
            String avatarName = (msgTokens.length > 5) ? msgTokens[5] : "boy"; // avatar name, default "boy"
            try {
                ghostManager.createGhost(ghostID, ghostPosition, avatarName); // spawn the ghost in our scene
            } catch (IOException e) {
                System.out.println("error creating ghost avatar");
            }
        }

        // --- wsds,newClientId ---
        // Server is asking us to send our current position/avatar to a newly joined client
        // so that client can spawn a ghost for us.
        if (msgTokens[0].compareTo("wsds") == 0) {
            UUID targetID = UUID.fromString(msgTokens[1]); // the new client who needs our details
            // Send a "dsfr" packet addressed to that client through the server
            sendDetailsForMessage(targetID, game.getPlayerPosition(), game.getAvatarName());
        }

        // --- move,remoteId,x,y,z ---
        // Another player moved; update their ghost avatar's position in our scene.
        if (msgTokens[0].compareTo("move") == 0) {
            UUID ghostID = UUID.fromString(msgTokens[1]); // UUID of the player who moved
            Vector3f ghostPosition = new Vector3f(
                Float.parseFloat(msgTokens[2]),  // x
                Float.parseFloat(msgTokens[3]),  // y
                Float.parseFloat(msgTokens[4])); // z
            ghostManager.updateGhostAvatar(ghostID, ghostPosition); // reposition the ghost in our scene
        }
    }

    /**
     * Sends a join request to the server with this client's UUID.
     * The server will respond with "join,success" or "join,failure".
     * format: join,localId
     */
    public void sendJoinMessage() {
        try {
            sendPacket(new String("join," + id.toString())); // include our UUID so the server can register us
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Announces this player's avatar to all other connected clients.
     * Called immediately after the server confirms a successful join.
     * @param pos        current world-space position
     * @param avatarName "player" or "boy"
     * format: create,localId,x,y,z,avatarName
     */
    public void sendCreateMessage(Vector3f pos, String avatarName) {
        try {
            String message = "create," + id.toString();                 // command + our UUID
            message += "," + pos.x() + "," + pos.y() + "," + pos.z(); // append position
            message += "," + avatarName;                                // append avatar type
            sendPacket(message);
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Notifies the server (and through it, all other clients) that this client is leaving.
     * Should be called before the application exits.
     * format: bye,localId
     */
    public void sendByeMessage() {
        try {
            sendPacket(new String("bye," + id.toString())); // include our UUID so others know who left
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Sends our current position and avatar details to a specific client (through the server).
     * Called in response to a "wsds" packet — the new client needs our info to create a ghost for us.
     * @param targetId   UUID of the new client who should receive our details
     * @param pos        our current world-space position
     * @param avatarName our avatar type
     * format: dsfr,myId,targetId,x,y,z,avatarName
     */
    public void sendDetailsForMessage(UUID targetId, Vector3f pos, String avatarName) {
        try {
            String message = "dsfr," + id.toString() + "," + targetId.toString(); // command + sender UUID + target UUID
            message += "," + pos.x() + "," + pos.y() + "," + pos.z();            // append our position
            message += "," + avatarName;                                           // append our avatar type
            sendPacket(message);
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Broadcasts our new world-space position to all other clients via the server.
     * Called by MoveAction, StrafeAction, and YawAction after every movement step.
     * @param pos our updated world-space position
     * format: move,localId,x,y,z
     */
    public void sendMoveMessage(Vector3f pos) {
        try {
            String message = "move," + id.toString();                    // command + our UUID
            message += "," + pos.x() + "," + pos.y() + "," + pos.z(); // append new position
            sendPacket(message);
        } catch (IOException e) { e.printStackTrace(); }
    }
}