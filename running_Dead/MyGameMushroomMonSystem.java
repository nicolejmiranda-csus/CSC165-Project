package running_Dead;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import running_Dead.networking.GhostAvatar;

/**
 * Runs Mushroom Mon spawning, chasing, and explosion damage.
 * The enemies are hidden until activated so the scene can contain several threats without visible clutter.
 * Connected to: Owned by MyGame; built by MyGameWorldBuilder and updated by MyGameUpdater.
 */
public class MyGameMushroomMonSystem {
    public static final int TOTAL_COUNT = 6; // 1 random-spawn + 5 static-spawn

    private static final String ANIM_WALK = "WALK";
    private static final float NPC_RADIUS    = 0.72f;
    private static final float WANDER_SPEED  = 1.05f;
    private static final float CHASE_SPEED   = 1.15f;
    private static final float TURN_RATE     = 2.0f;
    private static final float STATE_BROADCAST_INTERVAL = 0.18f;
    private static final float SPAWN_DELAY   = 30.0f;
    private static final float EXPLODE_LINGER = 1.5f;
    private static final float STATIC_SPAWN_EDGE = GameConstants.WORLD_EDGE_LIMIT - NPC_RADIUS;

    // Static home positions for agents 1-5 (X, Z); Y is computed from terrain at runtime.
    private static final float[][] STATIC_SPAWN_XZ = {
        {  STATIC_SPAWN_EDGE,    0f },  // East
        { -STATIC_SPAWN_EDGE,    0f },  // West
        {   0f,  STATIC_SPAWN_EDGE },  // North
        {   0f, -STATIC_SPAWN_EDGE },  // South
        {  57f,   57f },  // Northeast
    };

    private final MyGame game;
    private final Random random = new Random();
    private final MyGameMushroomMonAgent[] agents = new MyGameMushroomMonAgent[TOTAL_COUNT];
    // homePositions[0] == null → random spawn;
    private final Vector3f[] homePositions = new Vector3f[TOTAL_COUNT];

    private boolean roundWasActive = false;
    private boolean wasNetworkOwner = false;

    public MyGameMushroomMonSystem(MyGame game) {
        this.game = game;
        for (int i = 0; i < STATIC_SPAWN_XZ.length; i++) {
            homePositions[i + 1] = new Vector3f(STATIC_SPAWN_XZ[i][0], 0f, STATIC_SPAWN_XZ[i][1]);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void update(float dt) {
        ensureAgents();
        if (dt <= 0f || !assetsUsable()) {
            for (int i = 0; i < TOTAL_COUNT; i++) hideAgent(i);
            return;
        }

        boolean roundActive = game.state.zombieRoundActive && !game.isMatchOver();
        if (roundActive && !roundWasActive) resetRoundState();
        if (!roundActive) {
            if (roundWasActive) resetRoundState();
            roundWasActive = false;
            for (int i = 0; i < TOTAL_COUNT; i++) updateAnimationIfVisible(i);
            return;
        }
        roundWasActive = true;

        boolean networkOwner = isNetworkOwner();
        if (!networkOwner) {
            wasNetworkOwner = false;
            for (int i = 0; i < TOTAL_COUNT; i++) updateAnimationIfVisible(i);
            return;
        }

        if (!wasNetworkOwner) {
            for (int i = 0; i < TOTAL_COUNT; i++) {
                if (agents[i] != null && agents[i].phase == MyGameMushroomMonPhase.REMOTE_CONTROLLED) {
                    agents[i].phase = agents[i].spawned ? MyGameMushroomMonPhase.WANDER : MyGameMushroomMonPhase.HIDDEN;
                }
            }
        }
        wasNetworkOwner = true;

        List<GhostAvatar> ghosts = game.state.gm != null
            ? game.getGhostManager().getGhostAvatarsSnapshot()
            : Collections.emptyList();

        for (int i = 0; i < TOTAL_COUNT; i++) {
            if (agents[i] == null) continue;
            updateAI(i, dt, ghosts);
            applyObjectTransform(i);
            broadcastStateIfNeeded(i, dt);
            updateAnimationIfVisible(i);
        }
        updateMushroomMonAudio();
    }

    public void applyRemoteState(UUID sourceId, boolean remoteSpawned, Vector3f remotePosition, float remoteYaw, String animationName) {
        ensureAgents();
        MyGameMushroomMonAgent a = agents[0];
        if (a == null) return;
        if (sourceId != null && sourceId.equals(game.getLocalPlayerId())) return;
        if (isNetworkOwner()) return;
        if (!assetsUsable()) { hideAgent(0); return; }

        if (!remoteSpawned) {
            hideAgent(0);
            a.phase = MyGameMushroomMonPhase.REMOTE_CONTROLLED;
            return;
        }

        a.spawned = true;
        a.phase   = MyGameMushroomMonPhase.REMOTE_CONTROLLED;
        a.position.set(remotePosition);
        a.yaw = remoteYaw;
        if (a.object != null) a.object.getRenderStates().enableRendering();
        playAnimation(a, animationName == null || animationName.isBlank() ? ANIM_WALK : animationName);
        applyObjectTransform(0);
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

    private void updateAI(int i, float dt, List<GhostAvatar> ghosts) {
        switch (agents[i].phase) {
            case HIDDEN   -> updateHidden(i, dt, ghosts);
            case WANDER   -> updateWander(i, dt, ghosts);
            case CHASE    -> updateChase(i, dt);
            case EXPLODED -> updateExploded(i, dt);
            default       -> {}
        }
    }

    private void updateHidden(int i, float dt, List<GhostAvatar> ghosts) {
        agents[i].spawnTimer -= dt;
        if (agents[i].spawnTimer > 0f) return;
        spawnAgent(i);
    }

    private void updateWander(int i, float dt, List<GhostAvatar> ghosts) {
        MyGameMushroomMonAgent a = agents[i];
        UUID nearestHuman = nearestHumanInRange(a.position, GameConstants.MUSHROOM_MON_DETECTION_RADIUS, ghosts);
        if (nearestHuman != null) {
            a.targetId = nearestHuman;
            a.phase = MyGameMushroomMonPhase.CHASE;
            return;
        }

        a.wanderTimer -= dt;
        boolean arrived = a.position.distance(a.destination) < 1.0f;
        if (a.wanderTimer <= 0f || arrived) {
            chooseWanderDestination(a);
            a.wanderTimer = 5.0f + random.nextFloat() * 5.0f;
        }
        moveToward(a, a.destination, WANDER_SPEED, dt);
        playAnimation(a, ANIM_WALK);
    }

    private void updateChase(int i, float dt) {
        MyGameMushroomMonAgent a = agents[i];
        Vector3f targetPos = humanPosition(a.targetId);
        if (targetPos == null) {
            a.targetId = null;
            a.phase = MyGameMushroomMonPhase.WANDER;
            return;
        }

        float dist = a.position.distance(targetPos);
        if (dist <= GameConstants.MUSHROOM_MON_EXPLOSION_RADIUS) {
            triggerExplosion(i);
            return;
        }

        moveToward(a, targetPos, CHASE_SPEED, dt);
        playAnimation(a, ANIM_WALK);
    }

    private void updateExploded(int i, float dt) {
        agents[i].explodeTimer -= dt;
        if (agents[i].explodeTimer <= 0f) hideAgent(i);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void triggerExplosion(int i) {
        MyGameMushroomMonAgent a = agents[i];
        a.phase = MyGameMushroomMonPhase.EXPLODED;
        a.explodeTimer = EXPLODE_LINGER;
        if (game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendMushroomMonExplosionMessage(a.targetId);
        }
        applyExplosion(a.targetId);
        game.soundSystem.playMushroomMonPoof(new Vector3f(a.position));
        if (i == 0) forceBroadcastState(0);
    }

    private void spawnAgent(int i) {
        MyGameMushroomMonAgent a = agents[i];
        if (homePositions[i] != null) {
            a.position.set(homePositions[i]);
            a.position.y = game.physicsSystem.terrainHeightAt(a.position.x, a.position.z);
        }
        chooseWanderDestination(a);
        if (homePositions[i] == null) a.position.set(a.destination); // random-spawn: place at chosen spot
        a.spawned = true;
        a.phase = MyGameMushroomMonPhase.WANDER;
        if (a.object != null) a.object.getRenderStates().enableRendering();
        if (i == 0) forceBroadcastState(0);
    }

    private void hideAgent(int i) {
        MyGameMushroomMonAgent a = agents[i];
        if (a == null) return;
        a.spawned = false;
        a.targetId = null;
        a.phase = MyGameMushroomMonPhase.HIDDEN;
        a.spawnTimer = SPAWN_DELAY;
        a.position.set(0f, -1000f, 0f);
        a.activeAnimation = "";
        if (a.object != null) {
            a.object.setLocalLocation(new Vector3f(a.position));
            a.object.getRenderStates().disableRendering();
        }
        if (a.shape != null) a.shape.stopAnimation();
    }

    private void resetRoundState() {
        wasNetworkOwner = false;
        for (int i = 0; i < TOTAL_COUNT; i++) {
            if (agents[i] == null) continue;
            agents[i].explodeTimer = 0f;
            agents[i].wanderTimer = 0f;
            agents[i].stateBroadcastTimer = 0f;
            if (i == 0) {
                hideAgent(0); // random-spawn mon waits for timer
            } else {
                spawnAgent(i); // static mons spawn immediately
            }
        }
    }

    private void moveToward(MyGameMushroomMonAgent a, Vector3f target, float speed, float dt) {
        Vector3f desired = new Vector3f(target).sub(a.position);
        desired.y = 0f;
        float distance = desired.length();
        if (distance < 0.65f) return;
        desired.div(distance);

        float targetYaw = (float) Math.atan2(desired.x, desired.z);
        float diff = targetYaw - a.yaw;
        while (diff > Math.PI)  diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        float maxTurn = TURN_RATE * dt;
        a.yaw += Math.max(-maxTurn, Math.min(diff, maxTurn));

        Vector3f forward = new Vector3f((float) Math.sin(a.yaw), 0f, (float) Math.cos(a.yaw));
        float alignment = Math.max(0.25f, forward.dot(desired));
        float step = speed * dt * alignment;
        Vector3f next = new Vector3f(a.position).add(new Vector3f(forward).mul(step));
        next.y = game.physicsSystem.terrainHeightAt(next.x, next.z);

        if (!game.physicsSystem.isNpcBlockedByWorld(next, NPC_RADIUS)) {
            a.position.set(next);
            return;
        }

        Vector3f openStep = findOpenAvoidanceStep(a, desired, step);
        if (openStep != null) {
            a.position.set(openStep);
            return;
        }

        chooseWanderDestination(a);
    }

    private Vector3f findOpenAvoidanceStep(MyGameMushroomMonAgent a, Vector3f desired, float step) {
        float[] angles = {35f, -35f, 70f, -70f, 110f, -110f, 155f, -155f};
        for (float angle : angles) {
            Vector3f dir = rotateY(desired, (float) Math.toRadians(angle));
            Vector3f next = new Vector3f(a.position).add(new Vector3f(dir).mul(Math.max(0.25f, step)));
            next.y = game.physicsSystem.terrainHeightAt(next.x, next.z);
            if (!game.physicsSystem.isNpcBlockedByWorld(next, NPC_RADIUS)) return next;
        }
        return null;
    }

    private Vector3f rotateY(Vector3f dir, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector3f(dir.x * cos + dir.z * sin, 0f, -dir.x * sin + dir.z * cos).normalize();
    }

    private void chooseWanderDestination(MyGameMushroomMonAgent a) {
        for (int i = 0; i < 20; i++) {
            float x = (random.nextFloat() - 0.5f) * 60f;
            float z = (random.nextFloat() - 0.5f) * 60f;
            float y = game.physicsSystem.terrainHeightAt(x, z);
            a.destination.set(x, y, z);
            if (a.position.distance(a.destination) > 3.0f) return;
        }
        a.destination.set(0f, game.physicsSystem.terrainHeightAt(0f, 0f), 0f);
    }

    private UUID nearestHumanInRange(Vector3f position, float radius, List<GhostAvatar> ghosts) {
        UUID best = null;
        float bestDist = radius;

        if (game.isLocalHumanTarget()) {
            Vector3f pos = game.getPlayerPosition();
            if (pos != null) {
                float d = position.distance(pos);
                if (d < bestDist) { bestDist = d; best = game.getLocalPlayerId(); }
            }
        }

        for (GhostAvatar ghost : ghosts) {
            UUID id = ghost.getID();
            if (!isRemoteHumanTarget(id)) continue;
            float d = position.distance(ghost.getWorldLocation());
            if (d < bestDist) { bestDist = d; best = id; }
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

    private void broadcastStateIfNeeded(int i, float dt) {
        if (i != 0) return; // only broadcast the random-spawn mon
        if (game.state.protClient == null || !game.state.isClientConnected) return;
        agents[0].stateBroadcastTimer -= dt;
        if (agents[0].stateBroadcastTimer > 0f) return;
        forceBroadcastState(0);
    }

    private void forceBroadcastState(int i) {
        agents[i].stateBroadcastTimer = STATE_BROADCAST_INTERVAL;
        if (game.state.protClient == null || !game.state.isClientConnected) return;
        MyGameMushroomMonAgent a = agents[i];
        game.state.protClient.sendMushroomMonStateMessage(a.spawned, a.position, a.yaw, a.activeAnimation);
    }

    private void applyObjectTransform(int i) {
        MyGameMushroomMonAgent a = agents[i];
        if (a == null || a.object == null) return;
        a.position.y = game.physicsSystem.terrainHeightAt(a.position.x, a.position.z);
        a.object.setLocalLocation(new Vector3f(a.position));
        a.object.setLocalRotation(new Matrix4f().rotationY(a.yaw));
    }

    private void playAnimation(MyGameMushroomMonAgent a, String name) {
        if (a.shape == null || name.equals(a.activeAnimation)) return;
        if (a.shape.getAnimation(name) == null) return;
        a.activeAnimation = name;
        a.shape.playAnimation(name, 0.5f, tage.shapes.AnimatedShape.EndType.LOOP, 0);
    }

    private void updateAnimationIfVisible(int i) {
        MyGameMushroomMonAgent a = agents[i];
        if (a != null && a.spawned && a.shape != null) a.shape.updateAnimation();
    }

    private void updateMushroomMonAudio() {
        Vector3f playerPos = game.getPlayerPosition();
        MyGameMushroomMonAgent nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (int i = 0; i < TOTAL_COUNT; i++) {
            MyGameMushroomMonAgent a = agents[i];
            if (a == null || !a.spawned) continue;
            float d = (playerPos != null) ? a.position.distance(playerPos) : 0f;
            if (d < nearestDist) { nearestDist = d; nearest = a; }
        }
        if (nearest == null) {
            game.soundSystem.stopMushroomMon();
            return;
        }
        boolean wandering = nearest.phase == MyGameMushroomMonPhase.WANDER;
        boolean chasing   = nearest.phase == MyGameMushroomMonPhase.CHASE;
        boolean exploded  = nearest.phase == MyGameMushroomMonPhase.EXPLODED;
        game.soundSystem.updateMushroomMonLoop(new Vector3f(nearest.position), wandering, chasing, exploded);
    }

    private void ensureAgents() {
        if (game.assets.mushMons.size() < TOTAL_COUNT) return;
        for (int i = 0; i < TOTAL_COUNT; i++) {
            if (agents[i] != null) continue;
            if (game.assets.mushMonAnimatedS == null) continue;
            agents[i] = new MyGameMushroomMonAgent(game.assets.mushMons.get(i), game.assets.mushMonAnimatedS);
            agents[i].spawnTimer = (i == 0) ? SPAWN_DELAY : 0f;
        }
    }

    private boolean assetsUsable() {
        return game.assets.mushMons.size() >= TOTAL_COUNT && game.assets.mushMonAnimatedS != null;
    }
}
