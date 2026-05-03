package a3;

import org.joml.Vector3f;
import tage.GameObject;
import tage.physics.PhysicsObject;

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
