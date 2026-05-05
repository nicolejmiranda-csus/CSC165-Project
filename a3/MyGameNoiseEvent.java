package a3;

import org.joml.Vector3f;

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
