package running_Dead.server;

/**
 * Server-side pickup state.
 * It is intentionally tiny because the server only needs type, active flag, and location to sync pickups.
 * Connected to: Owned by ServerGameState and serialized by GameServerUDP/GameServerTCP pickup messages.
 */
class PickupState {
    final int id;
    final int type;
    boolean active;
    float x;
    float z;
    long respawnEpochMs;

    PickupState(int id, int type) {
        this.id = id;
        this.type = type;
    }
}
