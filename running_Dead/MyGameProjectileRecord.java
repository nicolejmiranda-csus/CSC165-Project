package running_Dead;

import java.util.UUID;

import tage.GameObject;
import tage.physics.PhysicsObject;

class MyGameProjectileRecord {
    final UUID ownerId;
    final int type;
    final GameObject object;
    final PhysicsObject body;
    final boolean localProjectile;
    float ttl;

    MyGameProjectileRecord(UUID ownerId, int type, GameObject object, PhysicsObject body, boolean localProjectile, float ttl) {
        this.ownerId = ownerId;
        this.type = type;
        this.object = object;
        this.body = body;
        this.localProjectile = localProjectile;
        this.ttl = ttl;
    }
}
