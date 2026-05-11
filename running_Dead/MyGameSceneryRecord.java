package running_Dead;

import org.joml.Vector3f;
import tage.GameObject;
import tage.physics.PhysicsObject;

class MyGameSceneryRecord {
    final GameObject object;
    final PhysicsObject body;
    final boolean tree;
    final Vector3f center;
    final float halfX;
    final float halfY;
    final float halfZ;
    final boolean walkableTop;
    float walkableTopY;

    MyGameSceneryRecord(GameObject object, PhysicsObject body, boolean tree, Vector3f center,
                  float halfX, float halfY, float halfZ, boolean walkableTop) {
        this.object = object;
        this.body = body;
        this.tree = tree;
        this.center = new Vector3f(center);
        this.halfX = halfX;
        this.halfY = halfY;
        this.halfZ = halfZ;
        this.walkableTop = walkableTop;
        this.walkableTopY = center.y + halfY;
    }
}
