package a3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.joml.Vector3f;

import tage.GameObject;
import tage.shapes.AnimatedShape;

class MyGameSmilingManAgent {
    final int index;
    final GameObject object;
    final AnimatedShape shape;
    final Vector3f position = new Vector3f(0f, -1000f, 0f);
    final Vector3f destination = new Vector3f();
    final HashMap<UUID, Float> hitCooldowns = new HashMap<>();
    final HashSet<UUID> targetCandidates = new HashSet<>();
    final HashSet<UUID> rememberedTargets = new HashSet<>();
    final Vector3f lastChaseProgressPosition = new Vector3f();

    MyGameSmilingManPhase phase = MyGameSmilingManPhase.HIDDEN;
    UUID targetId;
    String activeAnimation = "";
    float yaw = 0f;
    float idlePauseTimer = 0f;
    float idleYawDrift = 0f;
    float fakeIdleTimer = 0f;
    float wanderWalkTimer = 0f;
    float gatherTimer = 0f;
    float sequenceTimer = 0f;
    float stateBroadcastTimer = 0f;
    float buildHitTimer = 0f;
    float chaseStuckTimer = 0f;
    float blindTimer = 0f;
    float blindCooldownTimer = 0f;
    boolean spawned = false;

    MyGameSmilingManAgent(int index, GameObject object, AnimatedShape shape) {
        this.index = index;
        this.object = object;
        this.shape = shape;
    }
}
