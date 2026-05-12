package running_Dead;

import org.joml.Vector3f;

/**
 * Short-lived sound/noise marker used by AI.
 * Sprinting, building, and rocks create these so enemies can react without hard-coding each source.
 * Connected to: Created by build/item/movement systems and consumed by Smiling Man and Mushroom Mon AI.
 */
class MyGameNoiseEvent {
    final Vector3f position;
    final float radius;
    float life;

    MyGameNoiseEvent(Vector3f position, float radius, float life) {
        this.position = new Vector3f(position);
        this.radius = radius;
        this.life = life;
    }
}
