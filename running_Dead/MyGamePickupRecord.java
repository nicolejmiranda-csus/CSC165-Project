package running_Dead;

import org.joml.Vector3f;
import tage.GameObject;
import tage.physics.PhysicsObject;

/**
 * One pickup in the world and its network-visible state.
 * The server sends active/type/location data so every client hides the same collected pickup.
 * Connected to: Created by MyGamePhysicsSystem and read by MyGameItemSystem and ProtocolClient pickup sync.
 */
class MyGamePickupRecord {
    final int id;
    final int type;
    final MyGameBodyKind kind;
    final GameObject object;
    final Vector3f location = new Vector3f();
    final String label;
    PhysicsObject body;
    boolean active = false;
    boolean claimPending = false;
    double respawnTimer = 0.0;
    float bobPhase = 0.0f;
    float bobTime = 0.0f;

    MyGamePickupRecord(int id, int type, MyGameBodyKind kind, GameObject object, String label) {
        this.id = id;
        this.type = type;
        this.kind = kind;
        this.object = object;
        this.label = label;
    }
}
