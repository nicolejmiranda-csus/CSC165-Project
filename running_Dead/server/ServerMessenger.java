package running_Dead.server;

import java.util.UUID;

/**
 * Small server callback used by ServerGameState to send messages without caring whether transport is UDP or TCP.
 * Connected to: Implemented by GameServerUDP/GameServerTCP and used by ServerGameState to send packets.
 */
interface ServerMessenger {
    void sendTo(UUID targetId, String message);
}
