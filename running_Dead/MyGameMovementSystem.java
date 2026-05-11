package running_Dead;

import org.joml.Vector3f;

public class MyGameMovementSystem {
    private final MyGame game;
    private float frameMoveInput = 0.0f;
    private float frameStrafeInput = 0.0f;
    private float sprintNoiseTimer = 0.0f;

    public MyGameMovementSystem(MyGame game) {
        this.game = game;
    }

    public void setPadMove(float v) { game.state.padMove = v; }
    public void setPadStrafe(float v) { game.state.padStrafe = v; }
    public void triggerRun() { game.state.runHoldTime = 0.15f; }
    public float getCurrentMoveSpeed() {
        float speed;
        if (game.state.localPlayerZombie) speed = game.state.runHoldTime > 0.0f ? game.state.zombieRunSpeed : game.state.zombieWalkSpeed;
        else speed = game.state.runHoldTime > 0.0f ? game.state.humanRunSpeed : game.state.humanWalkSpeed;
        if (game.state.slowTimer > 0.0) speed *= game.state.slowedSpeedMultiplier;
        return speed;
    }
    public float getGroundHeight(float x, float z) {
        Vector3f loc = game.assets.avatar == null ? new Vector3f() : game.assets.avatar.getWorldLocation();
        return game.physicsSystem.getWalkableGroundHeight(x, z, loc.y);
    }

    public void processPlayerInput() {
        game.animationSystem.beginInputFrame();
        frameMoveInput = 0.0f;
        frameStrafeInput = 0.0f;
        game.state.padMove = 0.0f;
        game.state.padStrafe = 0.0f;
        game.state.im.update((float) game.state.elapsedTime);
        applyCombinedMovement((float) game.state.elapsedTime);
        updateJump((float) game.state.elapsedTime);
    }

    public void syncPlayerToTerrain() {
        Vector3f loc = game.assets.avatar.getWorldLocation();
        Vector3f clamped = game.physicsSystem.clampToWorldBounds(loc);
        float groundY = getGroundHeight(clamped.x(), clamped.z());
        if (game.state.onGround) {
            boolean changed = Math.abs(loc.x - clamped.x) > 0.001f
                    || Math.abs(loc.z - clamped.z) > 0.001f
                    || Math.abs(loc.y - groundY) > 0.001f;
            game.assets.avatar.setLocalLocation(new Vector3f(clamped.x(), groundY, clamped.z()));
            if (changed) game.networking.sendPlayerTransform();
        }
    }

    public void updateRunState(float dt) {
        game.state.runHoldTime -= dt;
        if (game.state.runHoldTime < 0.0f) game.state.runHoldTime = 0.0f;
    }

    public void doMove(float input, float time) {
        frameMoveInput += input;
    }

    public void doStrafe(float dir, float time) {
        frameStrafeInput += dir;
    }

    public void doYaw(float input, float time) {
        float ang = 1.6f * input * time;
        game.assets.avatar.globalYaw(ang);
        game.state.playerYaw += (float) Math.toDegrees(ang);
        game.networking.sendPlayerTransform();
    }

    public void doJump() {
        if (!game.state.onGround) return;
        game.state.onGround = false;
        game.state.yVel = 7.5f;
        game.networking.sendPlayerTransform();
    }

    private void applyCombinedMovement(float dt) {
        float f = clamp(frameMoveInput + game.state.padMove, -1.0f, 1.0f);
        float s = clamp(frameStrafeInput + game.state.padStrafe, -1.0f, 1.0f);
        float mag = (float) Math.sqrt((f * f) + (s * s));
        if (mag < 0.0001f) return;
        if (mag > 1.0f) { f /= mag; s /= mag; }

        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f fwd = game.avatarForward();
        fwd.y = 0f;
        if (fwd.lengthSquared() < 0.0001f) fwd.set(0f, 0f, -1f);
        fwd.normalize();
        Vector3f rt = new Vector3f(game.assets.avatar.getWorldRightVector()).normalize();
        Vector3f right = new Vector3f(rt.x, 0f, rt.z);
        if (right.lengthSquared() < 0.0001f) right.set(1f, 0f, 0f);
        right.normalize();

        Vector3f move = new Vector3f(fwd).mul(f).add(new Vector3f(right).mul(s));
        if (move.lengthSquared() < 0.0001f) return;
        move.normalize().mul(getCurrentMoveSpeed() * dt);

        Vector3f newPos = new Vector3f(pos).add(move);
        newPos.y = game.state.onGround ? getGroundHeight(newPos.x, newPos.z) : pos.y;
        if (game.physicsSystem.tryMoveLocalPlayer(newPos)) {
            game.animationSystem.recordMovement();
            reportSprintNoise(dt);
            game.networking.sendPlayerTransform();
        }
    }

    private void reportSprintNoise(float dt) {
        sprintNoiseTimer -= dt;
        if (sprintNoiseTimer > 0f) return;
        if (game.state.localPlayerZombie || game.state.runHoldTime <= 0.0f) return;
        game.smilingManSystem.reportNoise(game.assets.avatar.getWorldLocation(), GameConstants.NOISE_SPRINT_RADIUS);
        sprintNoiseTimer = 0.55f;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateJump(float dt) {
        if (game.state.onGround) return;
        game.state.yVel += -18.0f * dt;
        Vector3f pos = game.assets.avatar.getWorldLocation();
        float groundY = getGroundHeight(pos.x, pos.z);
        float newY = pos.y + game.state.yVel * dt;
        if (newY <= groundY) {
            newY = groundY;
            game.state.yVel = 0.0f;
            game.state.onGround = true;
        }
        game.physicsSystem.tryMoveLocalPlayer(new Vector3f(pos.x, newY, pos.z));
        game.networking.sendPlayerTransform();
    }
}
