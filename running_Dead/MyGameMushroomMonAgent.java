package running_Dead;

import java.util.UUID;
import org.joml.Vector3f;
import tage.GameObject;
import tage.shapes.AnimatedShape;

class MyGameMushroomMonAgent {
    final GameObject object;
    final AnimatedShape shape;
    final Vector3f position = new Vector3f(0f, -1000f, 0f);
    final Vector3f destination = new Vector3f();

    MyGameMushroomMonPhase phase = MyGameMushroomMonPhase.HIDDEN;
    UUID targetId;
    String activeAnimation = "";
    float yaw = 0f;
    float stateBroadcastTimer = 0f;
    float spawnTimer = 0f;
    float explodeTimer = 0f;
    float wanderTimer = 0f;
    boolean spawned = false;

    MyGameMushroomMonAgent(GameObject object, AnimatedShape shape) {
        this.object = object;
        this.shape = shape;
    }
}
