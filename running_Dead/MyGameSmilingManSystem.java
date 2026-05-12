package running_Dead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import running_Dead.networking.GhostAvatar;
import tage.shapes.AnimatedShape;

/**
 * Runs Smiling Man spawning, sight checks, noise reaction, chasing, and attacks.
 * This is separate from Mushroom Mon because its rules depend on whether humans look at it.
 * Connected to: Owned by MyGame; built by MyGameWorldBuilder and updated by MyGameUpdater.
 */
public class MyGameSmilingManSystem {
    private static final String ANIM_IDLE = "smilingManIdle";
    private static final String ANIM_WALK = "smilingManWalkForward";
    private static final String ANIM_WAVE = "smilingManIdleWave";
    private static final String ANIM_RUN_TRANSITION = "smilingManRunForwardTransition";
    private static final String ANIM_RUN_INITIAL = "smilingManRunForwardInitial";
    private static final String ANIM_RUN = "smilingManRunForward";
    private static final String ANIM_RUN_OUTRO = "smilingManRunForwardOutro";
    private static final String ANIM_RUN_TO_STAND = "smilingManRunForwardToStand";

    private static final float WALK_ANIM_SPEED = 0.55f;
    private static final float RUN_ANIM_SPEED = 0.85f;
    private static final float ONE_SHOT_ANIM_SPEED = 0.55f;
    private static final float WALK_TURN_RATE = 1.45f;
    private static final float RUN_TURN_RATE = 2.75f;
    private static final float NPC_RADIUS = 0.72f;
    private static final float HUMAN_MOVED_EPSILON = 0.06f;
    private static final float STATE_BROADCAST_INTERVAL = 0.18f;

    private final MyGame game;
    private final Random random = new Random();
    private final ArrayList<MyGameSmilingManAgent> agents = new ArrayList<>();
    private final ArrayList<MyGameNoiseEvent> noiseEvents = new ArrayList<>();
    private final HashMap<UUID, Vector3f> lastHumanPositions = new HashMap<>();
    private final HashMap<UUID, Float> stillSeconds = new HashMap<>();
    private final HashMap<UUID, Float> remoteRunNoiseTimers = new HashMap<>();

    private boolean roundWasActive = false;
    private boolean wasNetworkOwner = false;
    private boolean assetWarningShown = false;
    private boolean halftimeSpawnDone = false;
    private float sharedStillSeconds = 0f;

    public MyGameSmilingManSystem(MyGame game) {
        this.game = game;
    }

    public void update(float dt) {
        ensureAgents();
        if (dt <= 0f || agents.isEmpty()) return;
        if (!smilingManAssetsUsable()) {
            hideAll();
            return;
        }

        boolean roundActive = game.state.zombieRoundActive && !game.isMatchOver();
        if (roundActive && !roundWasActive) resetRoundState();
        if (!roundActive) {
            if (roundWasActive || isSpawned()) resetRoundState();
            roundWasActive = false;
            updateAnimationsIfVisible();
            updateSmilingManAudio();
            return;
        }
        roundWasActive = true;

        updateSharedTimers(dt);

        boolean networkOwner = isNetworkOwner();
        if (!networkOwner) {
            wasNetworkOwner = false;
            updateAnimationsIfVisible();
            updateSmilingManAudio();
            return;
        }

        if (!wasNetworkOwner) {
            for (MyGameSmilingManAgent agent : agents) {
                if (agent.phase == MyGameSmilingManPhase.REMOTE_CONTROLLED) {
                    agent.phase = agent.spawned ? MyGameSmilingManPhase.WANDER_WALK : MyGameSmilingManPhase.HIDDEN;
                    chooseWanderDestination(agent);
                }
            }
        }
        wasNetworkOwner = true;

        updateHumanStillTimers(dt);
        maybeSpawnSmilingMan(dt);

        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned) continue;
            if (agent.blindTimer > 0f) {
                playAnimation(agent, ANIM_IDLE, true);
            } else {
                updateAI(agent, dt);
            }
            applyObjectTransform(agent);
            broadcastStateIfNeeded(agent, dt);
        }

        updateAnimationsIfVisible();
        updateSmilingManAudio();
    }

    public boolean isSpawned() {
        for (MyGameSmilingManAgent agent : agents) {
            if (agent.spawned) return true;
        }
        return false;
    }

    public Vector3f getEyePosition() {
        MyGameSmilingManAgent agent = firstSpawnedAgent();
        return agent == null ? new Vector3f(0f, -1000f, 0f) : getEyePosition(agent);
    }

    public boolean tryBlindFromFlashlight(UUID sourceId) {
        MyGameSmilingManAgent agent = firstSpawnedAgent();
        if (agent == null) return false;
        return tryBlindFromFlashlight(0, sourceId);
    }

    public boolean tryBlindFromFlashlight(Vector3f origin, Vector3f forward, UUID sourceId) {
        boolean blindedAny = false;
        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned || agent.blindCooldownTimer > 0f) continue;
            if (!isPointInBeam(origin, forward, getEyePosition(agent))) continue;
            applyBlind(agent, sourceId, true);
            blindedAny = true;
        }
        return blindedAny;
    }

    public boolean tryBlindFromFlashlight(int index, UUID sourceId) {
        MyGameSmilingManAgent agent = agentByIndex(index);
        if (agent == null || !agent.spawned || agent.blindCooldownTimer > 0f) return false;
        applyBlind(agent, sourceId, true);
        return true;
    }

    public void applyRemoteBlind(UUID sourceId) {
        applyRemoteBlind(0, sourceId);
    }

    public void applyRemoteBlind(int index, UUID sourceId) {
        if (sourceId != null && sourceId.equals(game.getLocalPlayerId())) return;
        MyGameSmilingManAgent agent = agentByIndex(index);
        if (agent == null || !agent.spawned || agent.blindCooldownTimer > 0f) return;
        applyBlind(agent, sourceId, false);
    }

    public void applyRemoteState(UUID sourceId, boolean remoteSpawned, Vector3f remotePosition, float remoteYaw, String animationName) {
        applyRemoteState(0, sourceId, remoteSpawned, remotePosition, remoteYaw, animationName);
    }

    public void applyRemoteState(int index, UUID sourceId, boolean remoteSpawned, Vector3f remotePosition, float remoteYaw, String animationName) {
        ensureAgents();
        if (sourceId != null && sourceId.equals(game.getLocalPlayerId())) return;
        if (isNetworkOwner()) return;
        if (!smilingManAssetsUsable()) {
            hideAll();
            return;
        }

        MyGameSmilingManAgent agent = agentByIndex(index);
        if (agent == null) return;

        if (!remoteSpawned) {
            hide(agent);
            agent.phase = MyGameSmilingManPhase.REMOTE_CONTROLLED;
            return;
        }

        agent.spawned = true;
        agent.phase = MyGameSmilingManPhase.REMOTE_CONTROLLED;
        agent.position.set(remotePosition);
        agent.yaw = remoteYaw;
        show(agent);
        String anim = animationName == null || animationName.isBlank() ? ANIM_IDLE : animationName;
        playAnimation(agent, anim, isLoopAnimation(anim));
        applyObjectTransform(agent);
    }

    public void reportNoise(Vector3f position, float radius) {
        if (position == null || radius <= 0f) return;
        noiseEvents.add(new MyGameNoiseEvent(position, radius, 4.0f));
    }

    public float distanceToClosestSpawned(Vector3f point) {
        if (point == null) return Float.MAX_VALUE;
        float best = Float.MAX_VALUE;
        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned) continue;
            best = Math.min(best, horizontalDistance(point, agent.position));
        }
        return best;
    }

    public void onHumanLastChanceEscape(UUID humanId) {
        if (humanId == null) return;
        ensureAgents();
        if (!isNetworkOwner()) return;
        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned || !humanId.equals(agent.targetId)) continue;
            agent.targetCandidates.remove(humanId);
            UUID alternate = chooseAlternateRememberedTarget(agent, humanId);
            if (alternate != null) {
                agent.targetId = alternate;
                resetChaseStuckTracker(agent);
                playAnimation(agent, ANIM_RUN, true);
                game.hudSystem.showEvent("SMILING MAN CHANGED TARGET", 1.0);
            } else {
                forgetTargetAndWander(agent);
            }
            forceBroadcastState(agent);
        }
    }

    private void updateAI(MyGameSmilingManAgent agent, float dt) {
        switch (agent.phase) {
            case WANDER_IDLE:
                updateWanderIdle(agent, dt);
                break;
            case WANDER_WALK:
                updateWanderWalk(agent, dt);
                break;
            case FAKE_IDLE:
                updateFakeIdle(agent, dt);
                break;
            case GATHER_TARGETS:
                updateGatherTargets(agent, dt);
                break;
            case TARGET_WAVE:
            case RUN_TRANSITION:
            case RUN_INITIAL:
            case HIT_OUTRO:
            case HIT_STAND:
            case HIT_WAVE:
            case HIT_RETRANSITION:
            case HIT_REINITIAL:
                updateSequence(agent, dt);
                break;
            case CHASE:
                updateChase(agent, dt);
                break;
            default:
                startWandering(agent);
                break;
        }
    }

    private void updateWanderIdle(MyGameSmilingManAgent agent, float dt) {
        playAnimation(agent, ANIM_IDLE, true);
        agent.yaw = normalizeAngle(agent.yaw + agent.idleYawDrift * dt);
        collectTargetsInRange(agent);
        if (!agent.targetCandidates.isEmpty()) {
            beginGatheringOrFakeIdle(agent);
            return;
        }
        if (moveTowardNoiseIfHeard(agent)) return;
        agent.idlePauseTimer -= dt;
        if (agent.idlePauseTimer <= 0f) {
            chooseWanderDestination(agent);
            agent.phase = MyGameSmilingManPhase.WANDER_WALK;
        }
    }

    private void updateWanderWalk(MyGameSmilingManAgent agent, float dt) {
        playAnimation(agent, ANIM_WALK, true);
        collectTargetsInRange(agent);
        if (!agent.targetCandidates.isEmpty()) {
            beginGatheringOrFakeIdle(agent);
            return;
        }
        if (moveTowardNoiseIfHeard(agent)) return;

        agent.wanderWalkTimer -= dt;
        boolean reached = moveToward(agent, agent.destination, game.state.humanWalkSpeed, WALK_TURN_RATE, dt, false);
        if (reached || agent.wanderWalkTimer <= 0f || random.nextFloat() < dt * 0.08f) beginIdlePause(agent);
    }

    private void updateFakeIdle(MyGameSmilingManAgent agent, float dt) {
        playAnimation(agent, ANIM_IDLE, true);
        collectTargetsInRange(agent);
        agent.yaw = normalizeAngle(agent.yaw + agent.idleYawDrift * dt);
        agent.fakeIdleTimer -= dt;
        if (agent.fakeIdleTimer > 0f) return;
        if (agent.targetCandidates.isEmpty()) {
            startWandering(agent);
            return;
        }
        beginGathering(agent);
    }

    private void updateGatherTargets(MyGameSmilingManAgent agent, float dt) {
        playAnimation(agent, ANIM_WALK, true);
        collectTargetsInRange(agent);
        if (agent.wanderWalkTimer <= 0f || agent.position.distance(agent.destination) < 1.2f) chooseWanderDestination(agent);
        moveToward(agent, agent.destination, game.state.humanWalkSpeed, WALK_TURN_RATE, dt, false);

        agent.gatherTimer -= dt;
        if (agent.gatherTimer > 0f) return;

        UUID chosen = chooseCandidateTarget(agent);
        agent.targetCandidates.clear();
        if (chosen == null) {
            startWandering(agent);
            return;
        }

        agent.targetId = chosen;
        beginOneShot(agent, MyGameSmilingManPhase.TARGET_WAVE, ANIM_WAVE, durationFromFrames(78, ONE_SHOT_ANIM_SPEED, 1.6f));
        faceTarget(agent, dt, WALK_TURN_RATE * 3.0f);
        resetChaseStuckTracker(agent);
    }

    private void updateSequence(MyGameSmilingManAgent agent, float dt) {
        MyGameSmilingManHumanTarget target = findHumanTarget(agent.targetId);
        if (target != null) {
            if (target.local) triggerStareWarning();
            facePoint(agent, target.position, RUN_TURN_RATE, dt);
        }

        agent.sequenceTimer -= dt;
        if (agent.sequenceTimer > 0f) return;

        switch (agent.phase) {
            case TARGET_WAVE:
                beginOneShot(agent, MyGameSmilingManPhase.RUN_TRANSITION, ANIM_RUN_TRANSITION, durationFromFrames(10, ONE_SHOT_ANIM_SPEED, 0.55f));
                break;
            case RUN_TRANSITION:
                beginOneShot(agent, MyGameSmilingManPhase.RUN_INITIAL, ANIM_RUN_INITIAL, durationFromFrames(5, ONE_SHOT_ANIM_SPEED, 0.45f));
                break;
            case RUN_INITIAL:
                agent.phase = MyGameSmilingManPhase.CHASE;
                resetChaseStuckTracker(agent);
                playAnimation(agent, ANIM_RUN, true);
                break;
            case HIT_OUTRO:
                beginOneShot(agent, MyGameSmilingManPhase.HIT_STAND, ANIM_RUN_TO_STAND, durationFromFrames(10, ONE_SHOT_ANIM_SPEED, 0.65f));
                break;
            case HIT_STAND:
                beginOneShot(agent, MyGameSmilingManPhase.HIT_WAVE, ANIM_WAVE, durationFromFrames(78, ONE_SHOT_ANIM_SPEED, 1.6f));
                break;
            case HIT_WAVE:
                beginOneShot(agent, MyGameSmilingManPhase.HIT_RETRANSITION, ANIM_RUN_TRANSITION, durationFromFrames(10, ONE_SHOT_ANIM_SPEED, 0.55f));
                break;
            case HIT_RETRANSITION:
                beginOneShot(agent, MyGameSmilingManPhase.HIT_REINITIAL, ANIM_RUN_INITIAL, durationFromFrames(5, ONE_SHOT_ANIM_SPEED, 0.45f));
                break;
            case HIT_REINITIAL:
                agent.phase = MyGameSmilingManPhase.CHASE;
                resetChaseStuckTracker(agent);
                playAnimation(agent, ANIM_RUN, true);
                break;
            default:
                startWandering(agent);
                break;
        }
    }

    private void updateChase(MyGameSmilingManAgent agent, float dt) {
        MyGameSmilingManHumanTarget target = findHumanTarget(agent.targetId);
        if (target == null || isTargetClaimedByOther(agent.targetId, agent)) {
            forgetTargetAndWander(agent);
            return;
        }
        if (target.hidingInTable && horizontalDistance(agent.position, target.position) > GameConstants.SMILING_MAN_TABLE_HIDDEN_DETECTION_RADIUS) {
            forgetTargetAndWander(agent);
            return;
        }

        playAnimation(agent, ANIM_RUN, true);
        if (target.local) triggerStareWarning();
        Vector3f before = new Vector3f(agent.position);
        moveToward(agent, target.position, game.state.humanRunSpeed * 1.25f, RUN_TURN_RATE, dt, true);
        checkHumanCollisions(agent);
        updateChaseStuckTimer(agent, before, dt);

        if (findHumanTarget(agent.targetId) == null) forgetTargetAndWander(agent);
    }

    private boolean moveToward(MyGameSmilingManAgent agent, Vector3f target, float speed, float turnRate, float dt, boolean breakBuilds) {
        Vector3f desired = new Vector3f(target).sub(agent.position);
        desired.y = 0f;
        float distance = desired.length();
        if (distance < 0.65f) return true;
        desired.div(distance);

        faceDirection(agent, desired, turnRate, dt);
        Vector3f forward = forwardFromYaw(agent.yaw);
        float alignment = Math.max(0.25f, forward.dot(desired));
        float step = speed * dt * alignment;
        Vector3f next = new Vector3f(agent.position).add(forward.mul(step));
        next.y = game.physicsSystem.terrainHeightAt(next.x, next.z);

        MyGameBuildRecord buildBlock = game.physicsSystem.findNpcBuildBlock(next, NPC_RADIUS);
        if (buildBlock != null && breakBuilds) {
            agent.buildHitTimer += dt;
            if (agent.buildHitTimer >= 1.0f) {
                agent.buildHitTimer = 0f;
                game.physicsSystem.hitBuildPieceFromSmilingMan(buildBlock);
            }
            return false;
        }
        agent.buildHitTimer = 0f;

        if (!game.physicsSystem.isNpcBlockedByWorld(next, NPC_RADIUS)) {
            agent.position.set(next);
            return agent.position.distance(target) < 0.75f;
        }

        Vector3f openStep = findOpenAvoidanceStep(agent, desired, speed * dt);
        if (openStep != null) {
            agent.position.set(openStep);
            return false;
        }

        if (!breakBuilds) chooseWanderDestination(agent);
        return false;
    }

    private Vector3f findOpenAvoidanceStep(MyGameSmilingManAgent agent, Vector3f desired, float step) {
        float[] angles = {35f, -35f, 70f, -70f, 110f, -110f, 155f, -155f};
        for (float angle : angles) {
            Vector3f dir = rotateY(desired, (float) Math.toRadians(angle));
            Vector3f next = new Vector3f(agent.position).add(dir.mul(Math.max(0.25f, step)));
            next.y = game.physicsSystem.terrainHeightAt(next.x, next.z);
            if (!game.physicsSystem.isNpcBlockedByWorld(next, NPC_RADIUS)) {
                faceDirection(agent, new Vector3f(next).sub(agent.position).normalize(), RUN_TURN_RATE, 0.2f);
                return next;
            }
        }
        return null;
    }

    private void checkHumanCollisions(MyGameSmilingManAgent agent) {
        for (MyGameSmilingManHumanTarget human : getHumanTargets()) {
            float dist = horizontalDistance(agent.position, human.position);
            if (dist > GameConstants.SMILING_MAN_COLLISION_RADIUS) continue;
            if (agent.hitCooldowns.getOrDefault(human.id, 0f) > 0f) continue;

            damageHuman(human);
            agent.hitCooldowns.put(human.id, 2.2f);

            if (human.id.equals(agent.targetId)) {
                beginOneShot(agent, MyGameSmilingManPhase.HIT_OUTRO, ANIM_RUN_OUTRO, durationFromFrames(5, ONE_SHOT_ANIM_SPEED, 0.45f));
            }
        }
    }

    private void damageHuman(MyGameSmilingManHumanTarget human) {
        if (human.local) {
            game.itemSystem.damagePlayer(GameConstants.SMILING_MAN_DAMAGE);
            game.hudSystem.showEvent("SMILING MAN HIT YOU", 1.2);
            return;
        }

        if (game.state.protClient != null && game.state.isClientConnected) {
            int currentHealth = game.state.remoteHealthStates.getOrDefault(human.id, game.state.maxHealth);
            int newHealth = Math.max(0, currentHealth - GameConstants.SMILING_MAN_DAMAGE);
            game.applyRemoteHealth(human.id, newHealth);
            game.state.protClient.sendSmilingManDamageMessage(human.id, GameConstants.SMILING_MAN_DAMAGE);
        }
    }

    private void beginGathering(MyGameSmilingManAgent agent) {
        agent.phase = MyGameSmilingManPhase.GATHER_TARGETS;
        agent.gatherTimer = 5.0f;
        agent.rememberedTargets.clear();
        agent.rememberedTargets.addAll(agent.targetCandidates);
        if (agent.wanderWalkTimer <= 0f) chooseWanderDestination(agent);
        game.hudSystem.showEvent("SMILING MAN IS WATCHING", 1.2);
    }

    private void beginGatheringOrFakeIdle(MyGameSmilingManAgent agent) {
        if (agent.phase != MyGameSmilingManPhase.FAKE_IDLE
                && random.nextFloat() < GameConstants.SMILING_MAN_FAKE_IDLE_CHANCE) {
            beginFakeIdle(agent);
            return;
        }
        beginGathering(agent);
    }

    private void beginFakeIdle(MyGameSmilingManAgent agent) {
        agent.phase = MyGameSmilingManPhase.FAKE_IDLE;
        agent.fakeIdleTimer = randomRange(1.1f, 2.6f);
        agent.idleYawDrift = randomRange(-0.45f, 0.45f);
        playAnimation(agent, ANIM_IDLE, true);
    }

    private void beginOneShot(MyGameSmilingManAgent agent, MyGameSmilingManPhase nextPhase, String animationName, float duration) {
        agent.phase = nextPhase;
        agent.sequenceTimer = duration;
        playAnimation(agent, animationName, false);
    }

    private void beginIdlePause(MyGameSmilingManAgent agent) {
        agent.phase = MyGameSmilingManPhase.WANDER_IDLE;
        agent.idlePauseTimer = randomRange(0.65f, 2.2f);
        agent.idleYawDrift = randomRange(-0.65f, 0.65f);
        playAnimation(agent, ANIM_IDLE, true);
    }

    private void startWandering(MyGameSmilingManAgent agent) {
        agent.targetId = null;
        agent.targetCandidates.clear();
        agent.chaseStuckTimer = 0f;
        agent.fakeIdleTimer = 0f;
        if (random.nextBoolean()) beginIdlePause(agent);
        else {
            chooseWanderDestination(agent);
            agent.phase = MyGameSmilingManPhase.WANDER_WALK;
            playAnimation(agent, ANIM_WALK, true);
        }
    }

    private void forgetTargetAndWander(MyGameSmilingManAgent agent) {
        agent.targetId = null;
        agent.targetCandidates.clear();
        agent.rememberedTargets.clear();
        agent.buildHitTimer = 0f;
        agent.chaseStuckTimer = 0f;
        startWandering(agent);
    }

    private void chooseWanderDestination(MyGameSmilingManAgent agent) {
        MyGameSmilingManHumanTarget nearby = nearestProximityHuman(agent);
        if (nearby != null) {
            Vector3f offset = rotateY(new Vector3f(1f, 0f, 0f), randomRange(0f, (float) (Math.PI * 2.0))).mul(randomRange(6f, 14f));
            Vector3f destination = new Vector3f(nearby.position).add(offset);
            destination.x = clamp(destination.x, -GameConstants.WORLD_EDGE_LIMIT + 3f, GameConstants.WORLD_EDGE_LIMIT - 3f);
            destination.z = clamp(destination.z, -GameConstants.WORLD_EDGE_LIMIT + 3f, GameConstants.WORLD_EDGE_LIMIT - 3f);
            destination.y = game.physicsSystem.terrainHeightAt(destination.x, destination.z);
            if (!game.physicsSystem.isNpcBlockedByWorld(destination, NPC_RADIUS)) {
                agent.destination.set(destination);
                agent.wanderWalkTimer = randomRange(4.0f, 8.5f);
                return;
            }
        }

        for (int i = 0; i < 60; i++) {
            Vector3f candidate = game.worldBuilder.randomSpawnLocation(true);
            if (horizontalDistance(candidate, agent.position) < 10f) continue;
            if (game.physicsSystem.isNpcBlockedByWorld(candidate, NPC_RADIUS)) continue;
            agent.destination.set(candidate);
            agent.wanderWalkTimer = randomRange(4.0f, 8.5f);
            return;
        }
        float fallbackRange = GameConstants.WORLD_EDGE_LIMIT - 20.0f;
        Vector3f fallback = new Vector3f(randomRange(-fallbackRange, fallbackRange), 0f, randomRange(-fallbackRange, fallbackRange));
        fallback.y = game.physicsSystem.terrainHeightAt(fallback.x, fallback.z);
        agent.destination.set(fallback);
        agent.wanderWalkTimer = randomRange(3.0f, 6.0f);
    }

    private void maybeSpawnSmilingMan(float dt) {
        if (spawnedCount() >= GameConstants.MAX_SMILING_MEN) return;

        if (!halftimeSpawnDone && isHalftimeReached()) {
            halftimeSpawnDone = true;
            int spawned = spawnHiddenAgentsUntilMax();
            if (spawned > 0) game.hudSystem.showEvent("THE SMILING MEN HAVE ENTERED", 2.0);
            return;
        }

        while (sharedStillSeconds >= GameConstants.SMILING_MAN_STILL_SPAWN_SECONDS
                && spawnedCount() < GameConstants.MAX_SMILING_MEN) {
            MyGameSmilingManAgent agent = firstHiddenAgent();
            if (agent == null) return;
            if (!spawnAtRandomLocation(agent, false)) return;
            sharedStillSeconds -= GameConstants.SMILING_MAN_STILL_SPAWN_SECONDS;
            game.hudSystem.showEvent("STILLNESS SUMMONED A SMILING MAN", 1.6);
        }
    }

    private int spawnHiddenAgentsUntilMax() {
        int count = 0;
        while (spawnedCount() < GameConstants.MAX_SMILING_MEN) {
            MyGameSmilingManAgent agent = firstHiddenAgent();
            if (agent == null) break;
            if (!spawnAtRandomLocation(agent, false)) break;
            count++;
        }
        return count;
    }

    private boolean isHalftimeReached() {
        return game.state.survivalTimeRemaining > 0.0
                && game.state.survivalTimeRemaining <= GameConstants.SMILING_MAN_HALFTIME_SPAWN_SECONDS;
    }

    private boolean spawnAtRandomLocation(MyGameSmilingManAgent agent, boolean showMessage) {
        if (!smilingManAssetsUsable()) return false;

        ArrayList<MyGameSmilingManHumanTarget> humans = getHumanTargets();

        Vector3f spawn = null;
        for (int i = 0; i < 80; i++) {
            Vector3f candidate = game.worldBuilder.randomSpawnLocation(true);
            if (!humans.isEmpty() && tooCloseToHumans(candidate, humans, 18f)) continue;
            if (tooCloseToOtherSmilingMen(candidate, 15f)) continue;
            if (game.physicsSystem.isNpcBlockedByWorld(candidate, NPC_RADIUS)) continue;
            spawn = candidate;
            break;
        }
        if (spawn == null) spawn = game.worldBuilder.randomSpawnLocation(true);

        agent.position.set(spawn);
        agent.yaw = randomRange(0f, (float) (Math.PI * 2.0));
        agent.spawned = true;
        show(agent);
        chooseWanderDestination(agent);
        beginIdlePause(agent);
        if (showMessage) game.hudSystem.showEvent("A SMILING MAN HAS ENTERED", 2.0);
        forceBroadcastState(agent);
        return true;
    }

    private void updateHumanStillTimers(float dt) {
        HashSet<UUID> liveHumans = new HashSet<>();
        for (MyGameSmilingManHumanTarget human : getHumanTargets()) {
            liveHumans.add(human.id);
            Vector3f last = lastHumanPositions.get(human.id);
            if (last != null && horizontalDistance(last, human.position) <= HUMAN_MOVED_EPSILON) {
                stillSeconds.put(human.id, stillSeconds.getOrDefault(human.id, 0f) + dt);
                sharedStillSeconds += dt;
            }
            lastHumanPositions.put(human.id, new Vector3f(human.position));
        }
        lastHumanPositions.keySet().removeIf(id -> !liveHumans.contains(id));
    }

    private void collectTargetsInRange(MyGameSmilingManAgent agent) {
        for (MyGameSmilingManHumanTarget human : getHumanTargets()) {
            if (canSeeHuman(agent, human)) {
                agent.targetCandidates.add(human.id);
                agent.rememberedTargets.add(human.id);
                if (human.local) triggerStareWarning();
            }
        }
    }

    private boolean moveTowardNoiseIfHeard(MyGameSmilingManAgent agent) {
        MyGameNoiseEvent heard = bestHeardNoise(agent);
        if (heard == null) return false;
        agent.destination.set(heard.position);
        agent.destination.y = game.physicsSystem.terrainHeightAt(agent.destination.x, agent.destination.z);
        agent.wanderWalkTimer = randomRange(3.0f, 6.5f);
        agent.phase = MyGameSmilingManPhase.WANDER_WALK;
        playAnimation(agent, ANIM_WALK, true);
        return true;
    }

    private MyGameNoiseEvent bestHeardNoise(MyGameSmilingManAgent agent) {
        MyGameNoiseEvent best = null;
        float bestDistance = Float.MAX_VALUE;
        for (MyGameNoiseEvent noise : noiseEvents) {
            float distance = horizontalDistance(agent.position, noise.position);
            if (distance > noise.radius) continue;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = noise;
            }
        }
        return best;
    }

    private UUID chooseCandidateTarget(MyGameSmilingManAgent agent) {
        ArrayList<UUID> valid = new ArrayList<>();
        for (UUID id : agent.targetCandidates) {
            if (isTargetClaimedByOther(id, agent)) continue;
            if (findHumanTarget(id) != null) valid.add(id);
        }
        if (valid.isEmpty()) return null;
        return valid.get(random.nextInt(valid.size()));
    }

    private UUID chooseAlternateRememberedTarget(MyGameSmilingManAgent agent, UUID excludedTargetId) {
        ArrayList<UUID> valid = new ArrayList<>();
        for (UUID id : agent.rememberedTargets) {
            if (id.equals(excludedTargetId)) continue;
            if (isTargetClaimedByOther(id, agent)) continue;
            if (findHumanTarget(id) != null) valid.add(id);
        }
        if (valid.isEmpty()) return null;
        return valid.get(random.nextInt(valid.size()));
    }

    private boolean canSeeHuman(MyGameSmilingManAgent agent, MyGameSmilingManHumanTarget human) {
        Vector3f toHuman = new Vector3f(human.position).sub(agent.position);
        toHuman.y = 0f;
        float distance = toHuman.length();
        float detectionRadius = human.hidingInTable
                ? GameConstants.SMILING_MAN_TABLE_HIDDEN_DETECTION_RADIUS
                : GameConstants.SMILING_MAN_DETECTION_RADIUS;
        if (distance > detectionRadius) return false;
        if (distance < 0.0001f) return true;
        toHuman.div(distance);
        boolean close = distance <= GameConstants.SMILING_MAN_DETECTION_CLOSE_RADIUS;
        if (!close && forwardFromYaw(agent.yaw).dot(toHuman) < GameConstants.SMILING_MAN_DETECTION_DOT) return false;
        Vector3f from = getEyePosition(agent);
        Vector3f to = new Vector3f(human.position).add(0f, 1.4f, 0f);
        return !game.physicsSystem.isLineBlockedByBuildSight(from, to);
    }

    private MyGameSmilingManHumanTarget nearestProximityHuman(MyGameSmilingManAgent agent) {
        MyGameSmilingManHumanTarget best = null;
        float bestDist = Float.MAX_VALUE;
        for (MyGameSmilingManHumanTarget human : getHumanTargets()) {
            if (canSeeHuman(agent, human)) continue;
            float dist = horizontalDistance(agent.position, human.position);
            float radius = human.hidingInTable
                    ? GameConstants.SMILING_MAN_TABLE_HIDDEN_DETECTION_RADIUS
                    : GameConstants.SMILING_MAN_PROXIMITY_RADIUS;
            if (dist > radius || dist >= bestDist) continue;
            best = human;
            bestDist = dist;
        }
        return best;
    }

    private boolean isTargetClaimedByOther(UUID targetId, MyGameSmilingManAgent requester) {
        if (targetId == null) return false;
        for (MyGameSmilingManAgent agent : agents) {
            if (agent == requester || !agent.spawned) continue;
            if (!targetId.equals(agent.targetId)) continue;
            if (agent.phase == MyGameSmilingManPhase.TARGET_WAVE || agent.phase == MyGameSmilingManPhase.RUN_TRANSITION
                    || agent.phase == MyGameSmilingManPhase.RUN_INITIAL || agent.phase == MyGameSmilingManPhase.CHASE
                    || agent.phase == MyGameSmilingManPhase.HIT_OUTRO || agent.phase == MyGameSmilingManPhase.HIT_STAND
                    || agent.phase == MyGameSmilingManPhase.HIT_WAVE || agent.phase == MyGameSmilingManPhase.HIT_RETRANSITION
                    || agent.phase == MyGameSmilingManPhase.HIT_REINITIAL) return true;
        }
        return false;
    }

    private MyGameSmilingManHumanTarget findHumanTarget(UUID id) {
        if (id == null) return null;
        for (MyGameSmilingManHumanTarget target : getHumanTargets()) {
            if (id.equals(target.id)) return target;
        }
        return null;
    }

    private ArrayList<MyGameSmilingManHumanTarget> getHumanTargets() {
        ArrayList<MyGameSmilingManHumanTarget> humans = new ArrayList<>();
        UUID localId = game.getLocalPlayerId();
        if (game.isLocalHumanTarget()) {
            Vector3f pos = game.assets.avatar.getWorldLocation();
            humans.add(new MyGameSmilingManHumanTarget(localId, pos, true, game.worldBuilder.isInsideTableCover(pos)));
        }
        if (game.state.gm != null) {
            for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
                UUID id = ghost.getID();
                if (game.state.remoteZombieStates.getOrDefault(id, false)) continue;
                if (game.state.remoteHealthStates.getOrDefault(id, game.state.maxHealth) <= 0) continue;
                Vector3f pos = ghost.getWorldLocation();
                humans.add(new MyGameSmilingManHumanTarget(id, pos, false, game.worldBuilder.isInsideTableCover(pos)));
            }
        }
        return humans;
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

    private boolean isRemoteHumanTarget(UUID id) {
        if (id == null) return false;
        if (game.state.remoteZombieStates.getOrDefault(id, false)) return false;
        return game.state.remoteHealthStates.getOrDefault(id, game.state.maxHealth) > 0;
    }

    private void faceTarget(MyGameSmilingManAgent agent, float dt, float turnRate) {
        MyGameSmilingManHumanTarget target = findHumanTarget(agent.targetId);
        if (target != null) facePoint(agent, target.position, turnRate, dt);
    }

    private void facePoint(MyGameSmilingManAgent agent, Vector3f point, float turnRate, float dt) {
        Vector3f dir = new Vector3f(point).sub(agent.position);
        dir.y = 0f;
        if (dir.lengthSquared() < 0.0001f) return;
        faceDirection(agent, dir.normalize(), turnRate, dt);
    }

    private void faceDirection(MyGameSmilingManAgent agent, Vector3f direction, float turnRate, float dt) {
        float targetYaw = (float) Math.atan2(direction.x, direction.z);
        float delta = angleDelta(agent.yaw, targetYaw);
        float maxStep = Math.max(0.001f, turnRate * dt);
        if (Math.abs(delta) <= maxStep) agent.yaw = targetYaw;
        else agent.yaw += Math.signum(delta) * maxStep;
        agent.yaw = normalizeAngle(agent.yaw);
    }

    private void playAnimation(MyGameSmilingManAgent agent, String animationName, boolean loop) {
        if (agent == null || agent.shape == null) return;
        if (animationName == null || animationName.equals(agent.activeAnimation)) return;
        if (agent.shape.getAnimation(animationName) == null) return;

        agent.shape.stopAnimation();
        float speed = animationSpeed(animationName);
        agent.shape.playAnimation(animationName, speed, loop ? AnimatedShape.EndType.LOOP : AnimatedShape.EndType.PAUSE, loop ? 0 : 1);
        agent.activeAnimation = animationName;
        if (ANIM_WAVE.equals(animationName)) game.soundSystem.playSmilingManHello(agent.index, agent.position);
    }

    private float animationSpeed(String animationName) {
        if (ANIM_WALK.equals(animationName)) return WALK_ANIM_SPEED;
        if (ANIM_RUN.equals(animationName)) return RUN_ANIM_SPEED;
        return ONE_SHOT_ANIM_SPEED;
    }

    private boolean isLoopAnimation(String animationName) {
        return ANIM_IDLE.equals(animationName) || ANIM_WALK.equals(animationName) || ANIM_RUN.equals(animationName);
    }

    private void updateAnimationsIfVisible() {
        for (MyGameSmilingManAgent agent : agents) {
            if (agent.spawned && agent.shape != null) agent.shape.updateAnimation();
        }
    }

    private void updateSmilingManAudio() {
        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned) {
                game.soundSystem.stopSmilingMan(agent.index);
                continue;
            }
            boolean walking = ANIM_WALK.equals(agent.activeAnimation);
            boolean running = isRunningAudioAnimation(agent.activeAnimation);
            game.soundSystem.updateSmilingManLoop(agent.index, agent.position, walking, running);
        }
    }

    private boolean isRunningAudioAnimation(String animationName) {
        return ANIM_RUN.equals(animationName)
                || ANIM_RUN_TRANSITION.equals(animationName)
                || ANIM_RUN_INITIAL.equals(animationName);
    }

    private void applyObjectTransform(MyGameSmilingManAgent agent) {
        if (agent.object == null) return;
        agent.position.y = game.physicsSystem.terrainHeightAt(agent.position.x, agent.position.z);
        agent.object.setLocalLocation(new Vector3f(agent.position));
        agent.object.setLocalRotation(new Matrix4f().rotationY(agent.yaw));
    }

    private void show(MyGameSmilingManAgent agent) {
        if (agent.object != null) agent.object.getRenderStates().enableRendering();
    }

    private void hide(MyGameSmilingManAgent agent) {
        agent.spawned = false;
        agent.targetId = null;
        agent.targetCandidates.clear();
        agent.rememberedTargets.clear();
        agent.activeAnimation = "";
        agent.phase = MyGameSmilingManPhase.HIDDEN;
        agent.position.set(0f, -1000f, 0f);
        if (agent.object != null) {
            agent.object.setLocalLocation(new Vector3f(agent.position));
            agent.object.getRenderStates().disableRendering();
        }
        if (agent.shape != null) agent.shape.stopAnimation();
        game.soundSystem.stopSmilingMan(agent.index);
    }

    private void hideAll() {
        for (MyGameSmilingManAgent agent : agents) hide(agent);
    }

    private void resetRoundState() {
        hideAll();
        stillSeconds.clear();
        lastHumanPositions.clear();
        noiseEvents.clear();
        remoteRunNoiseTimers.clear();
        sharedStillSeconds = 0f;
        halftimeSpawnDone = false;
        wasNetworkOwner = false;
        for (MyGameSmilingManAgent agent : agents) {
            agent.hitCooldowns.clear();
            agent.buildHitTimer = 0f;
            agent.chaseStuckTimer = 0f;
            agent.blindTimer = 0f;
            agent.blindCooldownTimer = 0f;
            agent.stateBroadcastTimer = 0f;
            agent.fakeIdleTimer = 0f;
        }
    }

    private boolean smilingManAssetsUsable() {
        ensureAgents();
        if (agents.isEmpty()) return false;
        for (MyGameSmilingManAgent agent : agents) {
            if (agent.shape == null) return false;
            int boneCount = agent.shape.getBoneCount();
            if (boneCount <= GameConstants.TAGE_MAX_SKIN_BONES) continue;

            if (agent.object != null) agent.object.getRenderStates().disableRendering();
            if (!assetWarningShown) {
                assetWarningShown = true;
                System.out.println("WARNING: Smiling Man has " + boneCount
                        + " bones, but this TAGE shader supports only "
                        + GameConstants.TAGE_MAX_SKIN_BONES
                        + ". Re-export smilingMan.rks, smilingMan.rkm, and all smilingMan_*.rka files with Rigify Deform Skeleton Only.");
                if (game.hudSystem != null) game.hudSystem.showEvent("RE-EXPORT SMILING MAN COMPACT RIG", 2.5);
            }
            return false;
        }
        return true;
    }

    private void broadcastStateIfNeeded(MyGameSmilingManAgent agent, float dt) {
        if (game.state.protClient == null || !game.state.isClientConnected) return;
        agent.stateBroadcastTimer -= dt;
        if (agent.stateBroadcastTimer > 0f) return;
        forceBroadcastState(agent);
    }

    private void forceBroadcastState(MyGameSmilingManAgent agent) {
        agent.stateBroadcastTimer = STATE_BROADCAST_INTERVAL;
        if (game.state.protClient == null || !game.state.isClientConnected) return;
        game.state.protClient.sendSmilingManStateMessage(agent.index, agent.spawned, agent.position, agent.yaw, agent.activeAnimation);
    }

    private void updateSharedTimers(float dt) {
        updateNoiseEvents(dt);
        updateRemoteRunNoises(dt);
        for (MyGameSmilingManAgent agent : agents) {
            updateHitCooldowns(agent, dt);
            updateBlindTimers(agent, dt);
        }
    }

    private void updateNoiseEvents(float dt) {
        if (noiseEvents.isEmpty()) return;
        ArrayList<MyGameNoiseEvent> expired = new ArrayList<>();
        for (MyGameNoiseEvent noise : noiseEvents) {
            noise.life -= dt;
            if (noise.life <= 0f) expired.add(noise);
        }
        noiseEvents.removeAll(expired);
    }

    private void updateRemoteRunNoises(float dt) {
        if (!isNetworkOwner() || game.state.gm == null) return;
        HashSet<UUID> liveIds = new HashSet<>();
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            UUID id = ghost.getID();
            liveIds.add(id);
            if (game.state.remoteZombieStates.getOrDefault(id, false)) continue;
            if (!"RUN".equals(game.getRemoteAnimationState(id))) continue;
            float timer = remoteRunNoiseTimers.getOrDefault(id, 0f) - dt;
            if (timer <= 0f) {
                reportNoise(ghost.getWorldLocation(), GameConstants.NOISE_SPRINT_RADIUS);
                timer = 0.55f;
            }
            remoteRunNoiseTimers.put(id, timer);
        }
        remoteRunNoiseTimers.keySet().removeIf(id -> !liveIds.contains(id));
    }

    private void updateHitCooldowns(MyGameSmilingManAgent agent, float dt) {
        ArrayList<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Float> entry : agent.hitCooldowns.entrySet()) {
            float value = entry.getValue() - dt;
            if (value <= 0f) done.add(entry.getKey());
            else entry.setValue(value);
        }
        for (UUID id : done) agent.hitCooldowns.remove(id);
    }

    private void updateBlindTimers(MyGameSmilingManAgent agent, float dt) {
        if (agent.blindTimer > 0f) {
            agent.blindTimer -= dt;
            if (agent.blindTimer < 0f) agent.blindTimer = 0f;
        }
        if (agent.blindCooldownTimer > 0f) {
            agent.blindCooldownTimer -= dt;
            if (agent.blindCooldownTimer < 0f) agent.blindCooldownTimer = 0f;
        }
    }

    private void applyBlind(MyGameSmilingManAgent agent, UUID sourceId, boolean broadcast) {
        agent.blindTimer = GameConstants.FLASHLIGHT_BLIND_SECONDS;
        agent.blindCooldownTimer = GameConstants.FLASHLIGHT_BLIND_COOLDOWN_SECONDS;
        agent.buildHitTimer = 0f;
        agent.chaseStuckTimer = 0f;
        playAnimation(agent, ANIM_IDLE, true);
        game.hudSystem.showEvent("SMILING MAN BLINDED", 1.0);
        if (broadcast && game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendSmilingManBlindMessage(agent.index);
        }
    }

    private void resetChaseStuckTracker(MyGameSmilingManAgent agent) {
        agent.chaseStuckTimer = 0f;
        agent.lastChaseProgressPosition.set(agent.position);
    }

    private void updateChaseStuckTimer(MyGameSmilingManAgent agent, Vector3f before, float dt) {
        if (horizontalDistance(before, agent.position) > 0.08f
                || horizontalDistance(agent.lastChaseProgressPosition, agent.position) > 0.75f) {
            resetChaseStuckTracker(agent);
            return;
        }

        agent.chaseStuckTimer += dt;
        if (agent.chaseStuckTimer < 5.0f) return;

        UUID alternate = chooseAlternateRememberedTarget(agent, agent.targetId);
        if (alternate != null) {
            agent.targetId = alternate;
            resetChaseStuckTracker(agent);
            game.hudSystem.showEvent("SMILING MAN CHANGED TARGET", 1.0);
            return;
        }
        forgetTargetAndWander(agent);
    }

    private boolean tooCloseToHumans(Vector3f point, ArrayList<MyGameSmilingManHumanTarget> humans, float minDistance) {
        for (MyGameSmilingManHumanTarget human : humans) {
            if (horizontalDistance(point, human.position) < minDistance) return true;
        }
        return false;
    }

    private boolean tooCloseToOtherSmilingMen(Vector3f point, float minDistance) {
        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned) continue;
            if (horizontalDistance(point, agent.position) < minDistance) return true;
        }
        return false;
    }

    private float horizontalDistance(Vector3f a, Vector3f b) {
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private Vector3f forwardFromYaw(float angle) {
        return new Vector3f((float) Math.sin(angle), 0f, (float) Math.cos(angle)).normalize();
    }

    private Vector3f rotateY(Vector3f dir, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector3f(dir.x * cos + dir.z * sin, 0f, -dir.x * sin + dir.z * cos).normalize();
    }

    private float angleDelta(float from, float to) {
        return normalizeAngle(to - from);
    }

    private float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= (float) (Math.PI * 2.0);
        while (angle < -Math.PI) angle += (float) (Math.PI * 2.0);
        return angle;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float durationFromFrames(int frames, float speed, float minimum) {
        return Math.max(minimum, frames / Math.max(0.01f, speed) / 60.0f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isPointInBeam(Vector3f origin, Vector3f forward, Vector3f point) {
        if (origin == null || forward == null || point == null) return false;
        Vector3f toPoint = new Vector3f(point).sub(origin);
        float distance = toPoint.length();
        if (distance > GameConstants.FLASHLIGHT_BLIND_RANGE || distance < 0.0001f) return false;
        toPoint.div(distance);
        return forward.dot(toPoint) >= GameConstants.FLASHLIGHT_BLIND_DOT;
    }

    private void triggerStareWarning() {
        game.state.stareWarningTimer = Math.max(game.state.stareWarningTimer, GameConstants.SMILING_MAN_STARE_WARNING_SECONDS);
    }

    private Vector3f getEyePosition(MyGameSmilingManAgent agent) {
        return new Vector3f(agent.position).add(0f, 3.6f, 0f);
    }

    private int spawnedCount() {
        int count = 0;
        for (MyGameSmilingManAgent agent : agents) {
            if (agent.spawned) count++;
        }
        return count;
    }

    private MyGameSmilingManAgent firstSpawnedAgent() {
        for (MyGameSmilingManAgent agent : agents) {
            if (agent.spawned) return agent;
        }
        return null;
    }

    private MyGameSmilingManAgent firstHiddenAgent() {
        for (MyGameSmilingManAgent agent : agents) {
            if (!agent.spawned) return agent;
        }
        return null;
    }

    private MyGameSmilingManAgent agentByIndex(int index) {
        ensureAgents();
        if (index < 0 || index >= agents.size()) return null;
        return agents.get(index);
    }

    private void ensureAgents() {
        if (!agents.isEmpty()) return;
        if (game.assets.smilingMen.isEmpty()) return;
        for (int i = 0; i < game.assets.smilingMen.size(); i++) {
            AnimatedShape shape = null;
            if (game.assets.smilingManAnimatedShapes != null && i < game.assets.smilingManAnimatedShapes.length) {
                shape = game.assets.smilingManAnimatedShapes[i];
            }
            agents.add(new MyGameSmilingManAgent(i, game.assets.smilingMen.get(i), shape));
        }
    }
}


