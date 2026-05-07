package a3;

import java.util.Random;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import a3.networking.GhostAvatar;

public class MyGameMushroomMonSystem {
    private static final String ANIM_WALK = "WALK";
    private static final float WANDER_SPEED = 1.05f;
    private static final float CHASE_SPEED  = 1.15f;
    private static final float TURN_RATE    = 2.0f;
    private static final float STATE_BROADCAST_INTERVAL = 0.18f;
    private static final float SPAWN_DELAY   = 30.0f;
    private static final float EXPLODE_LINGER = 1.5f;

    private final MyGame game;
    private final Random random = new Random();
    private MyGameMushroomMonAgent agent;

    private boolean roundWasActive = false;
    private boolean wasNetworkOwner = false;

    public MyGameMushroomMonSystem(MyGame game) {
        this.game = game;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void update(float dt) {
        ensureAgent();
        if (dt <= 0f || agent == null || !assetsUsable()) {
            hideAgent();
            return;
        }

        boolean roundActive = game.state.zombieRoundActive && !game.isMatchOver();
        if (roundActive && !roundWasActive) resetRoundState();
        if (!roundActive) {
            if (roundWasActive || agent.spawned) resetRoundState();
            roundWasActive = false;
            updateAnimationIfVisible();
            return;
        }
        roundWasActive = true;

        boolean networkOwner = isNetworkOwner();
        if (!networkOwner) {
            wasNetworkOwner = false;
            updateAnimationIfVisible();
            return;
        }

        if (!wasNetworkOwner && agent.phase == MyGameMushroomMonPhase.REMOTE_CONTROLLED) {
            agent.phase = agent.spawned ? MyGameMushroomMonPhase.WANDER : MyGameMushroomMonPhase.HIDDEN;
        }
        wasNetworkOwner = true;

        updateAI(dt);
        applyObjectTransform();
        broadcastStateIfNeeded(dt);
        updateAnimationIfVisible();
    }

    public void applyRemoteState(UUID sourceId, boolean remoteSpawned, Vector3f remotePosition, float remoteYaw, String animationName) {
        ensureAgent();
        if (agent == null) return;
        if (sourceId != null && sourceId.equals(game.getLocalPlayerId())) return;
        if (isNetworkOwner()) return;
        if (!assetsUsable()) { hideAgent(); return; }

        if (!remoteSpawned) {
            hideAgent();
            agent.phase = MyGameMushroomMonPhase.REMOTE_CONTROLLED;
            return;
        }

        agent.spawned = true;
        agent.phase   = MyGameMushroomMonPhase.REMOTE_CONTROLLED;
        agent.position.set(remotePosition);
        agent.yaw = remoteYaw;
        showAgent();
        playAnimation(animationName == null || animationName.isBlank() ? ANIM_WALK : animationName);
        applyObjectTransform();
    }

    public void applyExplosion(UUID targetId) {
        if (targetId == null) return;
        if (targetId.equals(game.getLocalPlayerId())) {
            game.physicsSystem.applyRemoteSlow(null, game.getLocalPlayerId(), 3.0f);
        }
    }

    // -------------------------------------------------------------------------
    // AI state machine
    // -------------------------------------------------------------------------

    private void updateAI(float dt) {
        switch (agent.phase) {
            case HIDDEN    -> updateHidden(dt);
            case WANDER    -> updateWander(dt);
            case CHASE     -> updateChase(dt);
            case EXPLODED  -> updateExploded(dt);
            default        -> {}
        }
    }

    private void updateHidden(float dt) {
        agent.spawnTimer -= dt;
        if (agent.spawnTimer > 0f) return;
        spawnAgent();
    }

    private void updateWander(float dt) {
        UUID nearestHuman = nearestHumanInRange(GameConstants.MUSHROOM_MON_DETECTION_RADIUS);
        if (nearestHuman != null) {
            agent.targetId = nearestHuman;
            agent.phase = MyGameMushroomMonPhase.CHASE;
            return;
        }

        agent.wanderTimer -= dt;
        boolean arrived = agent.position.distance(agent.destination) < 1.0f;
        if (agent.wanderTimer <= 0f || arrived) {
            chooseWanderDestination();
            agent.wanderTimer = 5.0f + random.nextFloat() * 5.0f;
        }
        moveToward(agent.destination, WANDER_SPEED, dt);
        playAnimation(ANIM_WALK);
    }

    private void updateChase(float dt) {
        Vector3f targetPos = humanPosition(agent.targetId);
        if (targetPos == null) {
            agent.targetId = null;
            agent.phase = MyGameMushroomMonPhase.WANDER;
            return;
        }

        float dist = agent.position.distance(targetPos);
        if (dist <= GameConstants.MUSHROOM_MON_EXPLOSION_RADIUS) {
            triggerExplosion();
            return;
        }

        moveToward(targetPos, CHASE_SPEED, dt);
        playAnimation(ANIM_WALK);
    }

    private void updateExploded(float dt) {
        agent.explodeTimer -= dt;
        if (agent.explodeTimer <= 0f) hideAgent();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void triggerExplosion() {
        agent.phase = MyGameMushroomMonPhase.EXPLODED;
        agent.explodeTimer = EXPLODE_LINGER;
        if (game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendMushroomMonExplosionMessage(agent.targetId);
        }
        applyExplosion(agent.targetId);
        forceBroadcastState();
    }

    private void moveToward(Vector3f target, float speed, float dt) {
        Vector3f dir = new Vector3f(target).sub(agent.position);
        dir.y = 0f;
        float len = dir.length();
        if (len < 0.01f) return;

        float targetYaw = (float) Math.atan2(dir.x, dir.z);
        float diff = targetYaw - agent.yaw;
        while (diff > Math.PI)  diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        float maxTurn = TURN_RATE * dt;
        agent.yaw += Math.max(-maxTurn, Math.min(diff, maxTurn));

        Vector3f forward = new Vector3f((float) Math.sin(agent.yaw), 0f, (float) Math.cos(agent.yaw));
        float alignment = Math.max(0.25f, forward.dot(dir.normalize()));
        agent.position.add(forward.mul(speed * dt * alignment));
        agent.position.y = game.physicsSystem.terrainHeightAt(agent.position.x, agent.position.z);
    }

    private void chooseWanderDestination() {
        for (int i = 0; i < 20; i++) {
            float x = (random.nextFloat() - 0.5f) * 60f;
            float z = (random.nextFloat() - 0.5f) * 60f;
            float y = game.physicsSystem.terrainHeightAt(x, z);
            agent.destination.set(x, y, z);
            if (agent.position.distance(agent.destination) > 3.0f) return;
        }
        agent.destination.set(0f, game.physicsSystem.terrainHeightAt(0f, 0f), 0f);
    }

    private void spawnAgent() {
        chooseWanderDestination();
        agent.position.set(agent.destination);
        agent.spawned = true;
        agent.phase = MyGameMushroomMonPhase.WANDER;
        showAgent();
        forceBroadcastState();
    }

    private UUID nearestHumanInRange(float radius) {
        UUID best = null;
        float bestDist = radius;

        if (game.isLocalHumanTarget()) {
            Vector3f pos = game.getPlayerPosition();
            if (pos != null) {
                float d = agent.position.distance(pos);
                if (d < bestDist) { bestDist = d; best = game.getLocalPlayerId(); }
            }
        }

        if (game.state.gm != null) {
            for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
                UUID id = ghost.getID();
                if (!isRemoteHumanTarget(id)) continue;
                float d = agent.position.distance(ghost.getWorldLocation());
                if (d < bestDist) { bestDist = d; best = id; }
            }
        }
        return best;
    }

    private Vector3f humanPosition(UUID id) {
        if (id == null) return null;
        if (id.equals(game.getLocalPlayerId())) {
            return game.isLocalHumanTarget() ? game.getPlayerPosition() : null;
        }
        if (game.state.gm == null) return null;
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            if (id.equals(ghost.getID())) return ghost.getWorldLocation();
        }
        return null;
    }

    private boolean isRemoteHumanTarget(UUID id) {
        if (id == null) return false;
        if (game.state.remoteZombieStates.getOrDefault(id, false)) return false;
        return game.state.remoteHealthStates.getOrDefault(id, game.state.maxHealth) > 0;
    }

    private boolean isNetworkOwner() {
        if (!game.state.isMultiplayer || !game.state.isClientConnected) return true;
        UUID localId = game.getLocalPlayerId();
        UUID owner = game.isLocalHumanTarget() ? localId : null;
        if (game.state.gm != null) {
            for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
                UUID id = ghost.getID();
                if (!isRemoteHumanTarget(id)) continue;
                if (owner == null || id.toString().compareTo(owner.toString()) < 0) owner = id;
            }
        }
        if (owner == null) {
            owner = localId;
            if (game.state.gm != null) {
                for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
                    UUID id = ghost.getID();
                    if (id.toString().compareTo(owner.toString()) < 0) owner = id;
                }
            }
        }
        return localId.equals(owner);
    }

    private void broadcastStateIfNeeded(float dt) {
        if (game.state.protClient == null || !game.state.isClientConnected) return;
        agent.stateBroadcastTimer -= dt;
        if (agent.stateBroadcastTimer > 0f) return;
        forceBroadcastState();
    }

    private void forceBroadcastState() {
        agent.stateBroadcastTimer = STATE_BROADCAST_INTERVAL;
        if (game.state.protClient == null || !game.state.isClientConnected) return;
        game.state.protClient.sendMushroomMonStateMessage(agent.spawned, agent.position, agent.yaw, agent.activeAnimation);
    }

    private void showAgent() {
        if (agent.object != null) agent.object.getRenderStates().enableRendering();
    }

    private void hideAgent() {
        if (agent == null) return;
        agent.spawned = false;
        agent.targetId = null;
        agent.phase = MyGameMushroomMonPhase.HIDDEN;
        agent.spawnTimer = SPAWN_DELAY;
        agent.position.set(0f, -1000f, 0f);
        agent.activeAnimation = "";
        if (agent.object != null) {
            agent.object.setLocalLocation(new Vector3f(agent.position));
            agent.object.getRenderStates().disableRendering();
        }
        if (agent.shape != null) agent.shape.stopAnimation();
    }

    private void resetRoundState() {
        hideAgent();
        wasNetworkOwner = false;
        if (agent != null) {
            agent.spawnTimer = SPAWN_DELAY;
            agent.explodeTimer = 0f;
            agent.wanderTimer = 0f;
            agent.stateBroadcastTimer = 0f;
        }
    }

    private void applyObjectTransform() {
        if (agent == null || agent.object == null) return;
        agent.position.y = game.physicsSystem.terrainHeightAt(agent.position.x, agent.position.z);
        agent.object.setLocalLocation(new Vector3f(agent.position));
        agent.object.setLocalRotation(new Matrix4f().rotationY(agent.yaw));
    }

    private void playAnimation(String name) {
        if (agent.shape == null || name.equals(agent.activeAnimation)) return;
        if (agent.shape.getAnimation(name) == null) return;
        agent.activeAnimation = name;
        agent.shape.playAnimation(name, 0.5f, tage.shapes.AnimatedShape.EndType.LOOP, 0);
    }

    private void updateAnimationIfVisible() {
        if (agent != null && agent.spawned && agent.shape != null) agent.shape.updateAnimation();
    }

    private void ensureAgent() {
        if (agent != null) return;
        if (game.assets.mushMon == null || game.assets.mushMonAnimatedS == null) return;
        agent = new MyGameMushroomMonAgent(game.assets.mushMon, game.assets.mushMonAnimatedS);
        agent.spawnTimer = SPAWN_DELAY;
    }

    private boolean assetsUsable() {
        return agent != null && agent.object != null && agent.shape != null;
    }
}
