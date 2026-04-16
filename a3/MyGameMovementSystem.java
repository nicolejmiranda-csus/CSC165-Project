package a3;

import org.joml.Vector3f;

public class MyGameMovementSystem {
    private final MyGame game;

    public MyGameMovementSystem(MyGame game) {
        this.game = game;
    }

    public void setPadMove(float v) { game.state.padMove = v; }
    public void setPadStrafe(float v) { game.state.padStrafe = v; }
    public void triggerRun() { game.state.runHoldTime = 0.15f; }
    public float getCurrentMoveSpeed() { return game.state.runHoldTime > 0.0f ? game.state.runSpeed : game.state.walkSpeed; }
    public float getGroundHeight(float x, float z) { return game.assets.terrain.getHeight(x, z); }

    public void processPlayerInput() {
        game.state.padMove = 0.0f;
        game.state.padStrafe = 0.0f;
        game.state.im.update((float) game.state.elapsedTime);
        applyPadMovement((float) game.state.elapsedTime);
        updateJump((float) game.state.elapsedTime);
    }

    public void syncPlayerToTerrain() {
        Vector3f loc = game.assets.avatar.getWorldLocation();
        float groundY = getGroundHeight(loc.x(), loc.z());
        if (game.state.onGround) {
            boolean yChanged = Math.abs(loc.y - groundY) > 0.001f;
            game.assets.avatar.setLocalLocation(new Vector3f(loc.x(), groundY, loc.z()));
            if (yChanged) game.networking.sendPlayerTransform();
        }
    }

    public void updateRunState(float dt) {
        game.state.runHoldTime -= dt;
        if (game.state.runHoldTime < 0.0f) game.state.runHoldTime = 0.0f;
    }

    public void doMove(float input, float time) {
        if (game.state.lost || game.state.won) return;
        float dist = getCurrentMoveSpeed() * input * time;
        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f fwd = game.photoSystem.avatarForward();
        Vector3f newPos = new Vector3f(pos).add(new Vector3f(fwd).mul(dist));
        newPos.y = game.state.onGround ? getGroundHeight(newPos.x, newPos.z) : pos.y;
        game.assets.avatar.setLocalLocation(newPos);
        game.networking.sendPlayerTransform();
    }

    public void doStrafe(float dir, float time) {
        if (game.state.lost || game.state.won) return;
        float dist = getCurrentMoveSpeed() * dir * time;
        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f rt = new Vector3f(game.assets.avatar.getWorldRightVector()).normalize();
        Vector3f right = new Vector3f(rt.x, 0f, rt.z);
        if (right.lengthSquared() < 0.0001f) right.set(1f, 0f, 0f);
        right.normalize();
        Vector3f newPos = new Vector3f(pos).add(right.mul(dist));
        newPos.y = game.state.onGround ? getGroundHeight(newPos.x, newPos.z) : pos.y;
        game.assets.avatar.setLocalLocation(newPos);
        game.networking.sendPlayerTransform();
    }

    public void doYaw(float input, float time) {
        if (game.state.lost || game.state.won) return;
        float ang = 1.6f * input * time;
        game.assets.avatar.globalYaw(ang);
        game.state.playerYaw += (float) Math.toDegrees(ang);
        game.networking.sendPlayerTransform();
    }

    public void doJump() {
        if (game.state.lost || game.state.won || !game.state.onGround) return;
        game.state.onGround = false;
        game.state.yVel = 7.5f;
        game.networking.sendPlayerTransform();
    }

    private void applyPadMovement(float dt) {
        float f = game.state.padMove;
        float s = game.state.padStrafe;
        float mag = (float) Math.sqrt((f * f) + (s * s));
        if (mag < 0.0001f) return;
        if (mag > 1.0f) { f /= mag; s /= mag; }
        doMove(f, dt);
        doStrafe(s, dt);
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
        game.assets.avatar.setLocalLocation(new Vector3f(pos.x, newY, pos.z));
        game.networking.sendPlayerTransform();
    }
}
