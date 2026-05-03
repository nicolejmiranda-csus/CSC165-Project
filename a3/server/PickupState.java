package a3.server;

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
