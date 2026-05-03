package a3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import a3.networking.GhostAvatar;
import tage.GameObject;
import tage.RenderStates;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;

public class MyGamePhysicsSystem {
    public static final int PROJECTILE_ROCK = 0;
    public static final int PROJECTILE_BABY_ZOMBIE = 1;
    private static final int PICKUP_TYPE_DASH = GameConstants.PICKUP_DASH;
    private static final int PICKUP_TYPE_INVIS = GameConstants.PICKUP_INVIS;
    private static final int PICKUP_TYPE_BUILD = GameConstants.PICKUP_BUILD;
    private static final int PICKUP_TYPE_ROCK = GameConstants.PICKUP_ROCK;
    private static final int PICKUP_TYPE_BABY_ZOMBIE = GameConstants.PICKUP_BABY_ZOMBIE;
    private static final int PICKUP_TYPE_FLASHLIGHT = GameConstants.PICKUP_FLASHLIGHT;
    private static final int PICKUP_TYPE_POTION = GameConstants.PICKUP_POTION;

    private final MyGame game;
    private PhysicsEngine physicsEngine;
    private PhysicsObject localPlayerBody;
    private PhysicsObject terrainBody;
    private final HashMap<PhysicsObject, MyGameBodyInfo> bodyInfo = new HashMap<>();
    private final HashMap<UUID, PhysicsObject> remotePlayerBodies = new HashMap<>();
    private final HashMap<String, MyGameBuildRecord> buildRecords = new HashMap<>();
    private final ArrayList<MyGameSceneryRecord> sceneryRecords = new ArrayList<>();
    private final HashMap<String, ArrayList<MyGameTreeColliderSpec>> treeColliderSpecs = new HashMap<>();
    private final ArrayList<MyGamePickupRecord> pickups = new ArrayList<>();
    private final ArrayList<MyGameProjectileRecord> projectiles = new ArrayList<>();
    private final Random pickupRandom = new Random();
    private Vector3f lastSafePlayerLocation = new Vector3f();
    private boolean hasLastSafePlayerLocation = false;
    private boolean serverPickupsAuthoritative = false;
    private boolean serverRoundAuthoritative = false;
    private boolean sceneryCollidersReady = false;
    private long serverIntermissionEndEpochMs = 0L;
    private long serverRoundEndEpochMs = 0L;

    private final float playerBodyYOffset = 1.05f;
    private final float playerBodyRadius = 0.45f;
    private final float playerBodyHeight = 1.35f;
    private final float pickupColliderRadius = 0.6f;
    private final float projectileRadius = 0.35f;
    private final float projectileTTL = 5.0f;
    private final float pickupSpawnRadius = 90.0f;
    private final float pickupMinPlayerDistance = 10.0f;
    private final float pickupMinItemDistance = 8.0f;
    private final float pickupBaseLift = 0.55f;
    private final float pickupBobAmplitude = 0.35f;
    private final float pickupBobSpeed = 3.0f;
    private final double pickupRespawnSeconds = 30.0;

    public MyGamePhysicsSystem(MyGame game) {
        this.game = game;
    }

    public void initializePhysicsObjects() {
        physicsEngine = MyGame.getEngine().getSceneGraph().getPhysicsEngine();
        physicsEngine.setGravity(new float[] {0f, -9.8f, 0f});
        MyGame.getEngine().disablePhysicsWorldRender();

        createTerrainPhysics();
        createLocalPlayerBody();
        createPickups();

        game.state.physicsReady = true;
        game.state.zombieIntermissionRemaining = game.state.zombieIntermissionTime;
        game.state.survivalTimeRemaining = game.state.survivalRoundDuration;
        lastSafePlayerLocation = new Vector3f(game.assets.avatar.getWorldLocation());
        hasLastSafePlayerLocation = true;
        applyLocalRoleVisual();
    }

    public void updateTimers(float dt) {
        if (game.state.invisTimer > 0.0) {
            game.state.invisTimer -= dt;
            if (game.state.invisTimer <= 0.0) {
                game.state.invisTimer = 0.0;
                applyLocalRoleVisual();
                sendAbility("invis", false);
                game.hudSystem.showEvent("INVISIBILITY ENDED", 0.8);
            }
        }
        if (game.state.slowTimer > 0.0) {
            game.state.slowTimer -= dt;
            if (game.state.slowTimer < 0.0) game.state.slowTimer = 0.0;
        }
        if (game.state.projectileCooldown > 0.0) {
            game.state.projectileCooldown -= dt;
            if (game.state.projectileCooldown < 0.0) game.state.projectileCooldown = 0.0;
        }
        if (game.state.buildAttackCooldown > 0.0) {
            game.state.buildAttackCooldown -= dt;
            if (game.state.buildAttackCooldown < 0.0) game.state.buildAttackCooldown = 0.0;
        }
    }

    public void update(float dt) {
        if (!game.state.physicsReady || physicsEngine == null) return;

        updateRound(dt);
        ensureRemotePlayerBodies();
        updatePickupRespawns(dt);
        updateActivePickupBobs(dt);
        syncManualBodies();

        physicsEngine.update(dt);
        syncProjectileGraphics();
        physicsEngine.detectCollisions();

        handleLocalPlayerCollisions();
        handleProjectileCollisions();
        resolveBuildPieceBlocking();
        cleanupProjectiles(dt);
    }

    public boolean tryMoveLocalPlayer(Vector3f newPos) {
        if (isBlockedBySceneryCollider(newPos)) return false;
        game.assets.avatar.setLocalLocation(new Vector3f(newPos));
        return true;
    }

    public float getWalkableGroundHeight(float x, float z, float currentPlayerY) {
        float best = terrainHeight(x, z);
        if (!game.state.physicsReady) return best;
        for (MyGameSceneryRecord record : sceneryRecords) {
            if (record.tree || !record.walkableTop) continue;
            if (!isInsideSceneryFootprint(record, x, z, -playerBodyRadius * 0.35f)) continue;
            float top = record.walkableTopY;
            boolean canStepOnto = top <= currentPlayerY + 0.85f && currentPlayerY >= top - 1.35f;
            boolean canLandOn = !game.state.onGround && currentPlayerY >= top - 0.25f;
            if ((canStepOnto || canLandOn) && top > best) best = top;
        }
        for (MyGameBuildRecord record : buildRecords.values()) {
            float buildY = getWalkableBuildPieceHeight(record, x, z, currentPlayerY);
            if (buildY > best) best = buildY;
        }
        return best;
    }

    public void registerBuildPiece(GameObject piece, String key, int pieceType, int modeType, int roofDir, Vector3f pos) {
        if (!game.state.physicsReady || physicsEngine == null || piece == null || key == null) return;
        removeBuildPieceCollider(key);

        Quaternionf rot = new Quaternionf();
        piece.getWorldRotation().getNormalizedRotation(rot);
        float[] size = getBuildPieceColliderSize(pieceType, modeType);
        PhysicsObject body = MyGame.getEngine().getSceneGraph().addPhysicsBox(0f, new Vector3f(pos), rot, size);
        body.setFriction(1.0f);
        body.setBounciness(0.0f);

        MyGameBuildRecord record = new MyGameBuildRecord(key, piece, body, pieceType, modeType, roofDir, pos);
        buildRecords.put(key, record);
        bodyInfo.put(body, MyGameBodyInfo.buildPiece(record));
    }

    public void removeBuildPieceCollider(String key) {
        MyGameBuildRecord record = buildRecords.remove(key);
        if (record == null || record.body == null) return;
        bodyInfo.remove(record.body);
        MyGame.getEngine().getSceneGraph().removePhysicsObject(record.body);
    }

    public void syncSceneryColliders() {
        if (!game.state.physicsReady || physicsEngine == null) return;
        if (sceneryCollidersReady) return;
        int before = sceneryRecords.size();
        for (GameObject rock : game.assets.sceneryRocks) syncSceneryCollider(rock, false);
        for (GameObject tree : game.assets.sceneryTrees) syncSceneryCollider(tree, true);
        sceneryCollidersReady = true;
        System.out.println("created scenery physics colliders: " + (sceneryRecords.size() - before));
    }

    public void removeRemotePlayerBody(UUID id) {
        PhysicsObject body = remotePlayerBodies.remove(id);
        if (body != null) {
            bodyInfo.remove(body);
            MyGame.getEngine().getSceneGraph().removePhysicsObject(body);
        }
        game.state.remoteZombieStates.remove(id);
        game.state.remoteInvisibleStates.remove(id);
        game.state.remoteHealthStates.remove(id);
        game.state.remoteAnimationStates.remove(id);
    }

    public void useAbility() {
        if (game.isMatchOver()) return;
        if (game.state.localPlayerZombie) useZombieInvisibility();
        else useHumanDash();
    }

    public void throwProjectile() {
        if (game.isMatchOver()) return;
        if (!game.state.physicsReady || game.state.projectileCooldown > 0.0) return;
        int type = game.state.localPlayerZombie ? PROJECTILE_BABY_ZOMBIE : PROJECTILE_ROCK;
        if (type == PROJECTILE_ROCK && game.state.rockCharges <= 0) {
            game.hudSystem.showEvent("NO ROCK STORED", 0.8);
            return;
        }
        if (type == PROJECTILE_ROCK && game.state.equippedItem != GameConstants.ITEM_ROCK) {
            game.hudSystem.showEvent("EQUIP ROCK FIRST", 0.8);
            return;
        }
        if (type == PROJECTILE_BABY_ZOMBIE && game.state.babyZombieCharges <= 0) {
            game.hudSystem.showEvent("NO BABY ZOMBIE STORED", 0.8);
            return;
        }
        if (type == PROJECTILE_BABY_ZOMBIE && game.state.equippedItem != GameConstants.ITEM_BABY_ZOMBIE) {
            game.hudSystem.showEvent("EQUIP BABY ZOMBIE FIRST", 0.8);
            return;
        }
        Vector3f forward = horizontalForward();
        Vector3f pos = new Vector3f(game.assets.avatar.getWorldLocation())
                .add(new Vector3f(forward).mul(1.2f))
                .add(0f, 1.1f, 0f);
        Vector3f velocity = new Vector3f(forward).mul(15.0f).add(0f, 2.5f, 0f);
        spawnProjectile(getLocalPlayerId(), type, pos, velocity, true);
        if (game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendProjectileMessage(type, pos, velocity);
        }
        if (type == PROJECTILE_ROCK) game.state.rockCharges--;
        else game.state.babyZombieCharges--;
        game.itemSystem.onThrowableChargesChanged();
        game.state.projectileCooldown = game.state.projectileCooldownTime;
        game.hudSystem.showEvent(type == PROJECTILE_ROCK ? "ROCK THROWN" : "BABY ZOMBIE THROWN", 0.8);
    }

    public void spawnRemoteProjectile(UUID sourceId, int projectileType, Vector3f pos, Vector3f velocity) {
        if (!game.state.physicsReady || sourceId == null || sourceId.equals(getLocalPlayerId())) return;
        spawnProjectile(sourceId, projectileType, pos, velocity, false);
    }

    public void attackBuildPiece() {
        if (game.isMatchOver()) return;
        if (!game.state.localPlayerZombie) {
            game.hudSystem.showEvent("ONLY ZOMBIES CAN BREAK BUILDS", 1.0);
            return;
        }
        if (game.state.buildAttackCooldown > 0.0) return;
        MyGameBuildRecord target = findBuildPieceInFront();
        if (target == null) {
            game.hudSystem.showEvent("NO BUILD PIECE IN RANGE", 0.8);
            game.state.buildAttackCooldown = game.state.buildAttackCooldownTime;
            return;
        }
        target.hits++;
        game.state.buildAttackCooldown = game.state.buildAttackCooldownTime;
        if (target.hits >= 2) {
            game.buildSystem.destroyBuildPiece(target.key, target.pieceType, target.modeType, target.roofDir, target.position, true);
            game.hudSystem.showEvent("BUILD PIECE DESTROYED", 1.0);
        } else {
            game.hudSystem.showEvent("BUILD HIT 1/2", 0.8);
        }
    }

    public void attackAsZombie() {
        if (game.isMatchOver() || !game.state.localPlayerZombie) return;
        if (game.state.buildAttackCooldown > 0.0) return;
        UUID humanTarget = findHumanInFront();
        if (humanTarget != null) {
            if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendTagMessage(humanTarget);
            game.state.remoteZombieStates.put(humanTarget, true);
            applyRemoteRoleVisual(humanTarget);
            game.state.buildAttackCooldown = game.state.buildAttackCooldownTime;
            game.hudSystem.showEvent("HUMAN TAGGED", 1.0);
            return;
        }
        attackBuildPiece();
    }

    public void togglePhysicsVisualization() {
        game.state.physicsVisualizationEnabled = !game.state.physicsVisualizationEnabled;
        if (game.state.physicsVisualizationEnabled) {
            MyGame.getEngine().enablePhysicsWorldRender();
            game.hudSystem.showEvent("PHYSICS VIEW ON", 0.8);
        } else {
            MyGame.getEngine().disablePhysicsWorldRender();
            game.hudSystem.showEvent("PHYSICS VIEW OFF", 0.8);
        }
    }

    public void toggleLocalRoleForTesting() {
        game.state.matchHumanWon = false;
        game.state.matchZombieWon = false;
        setLocalZombie(!game.state.localPlayerZombie, true);
        game.state.zombieRoundActive = true;
        game.state.zombieIntermissionStarted = true;
        game.state.survivalTimeRemaining = game.state.survivalRoundDuration;
        game.hudSystem.showEvent(game.state.localPlayerZombie ? "TEST ROLE: ZOMBIE" : "TEST ROLE: HUMAN", 1.0);
    }

    public void applyRemoteRole(UUID id, boolean zombie) {
        if (id == null) return;
        if (id.equals(getLocalPlayerId())) {
            setLocalZombie(zombie, false);
            return;
        }
        game.state.remoteZombieStates.put(id, zombie);
        if (!zombie) game.state.remoteInvisibleStates.put(id, false);
        applyRemoteRoleVisual(id);
    }

    public void applyRemoteTag(UUID sourceId, UUID targetId) {
        if (targetId == null) return;
        if (targetId.equals(getLocalPlayerId())) {
            becomeZombie("TAGGED BY ZOMBIE");
            return;
        }
        game.state.remoteZombieStates.put(targetId, true);
        game.state.remoteInvisibleStates.put(targetId, false);
        applyRemoteRoleVisual(targetId);
        if (sourceId != null && !sourceId.equals(getLocalPlayerId())) {
            game.state.remoteZombieStates.put(sourceId, true);
            applyRemoteRoleVisual(sourceId);
        }
    }

    public void applyRemoteAbility(UUID sourceId, String ability, boolean active) {
        if (sourceId == null || sourceId.equals(getLocalPlayerId())) return;
        if ("invis".equals(ability)) {
            game.state.remoteInvisibleStates.put(sourceId, active);
            applyRemoteRoleVisual(sourceId);
        }
    }

    public void applyRemoteSlow(UUID sourceId, UUID targetId, float seconds) {
        if (targetId == null || !targetId.equals(getLocalPlayerId())) return;
        applySlow(seconds, sourceId);
    }

    public void applyRemoteHealth(UUID id, int health) {
        if (id == null) return;
        int clamped = Math.max(0, Math.min(game.state.maxHealth, health));
        if (id.equals(getLocalPlayerId())) {
            game.state.health = clamped;
            if (clamped <= 0 && !game.state.localPlayerZombie) setLocalZombie(true, false);
            return;
        }
        game.state.remoteHealthStates.put(id, clamped);
        if (clamped <= 0) {
            game.state.remoteZombieStates.put(id, true);
            game.state.remoteInvisibleStates.put(id, false);
            applyRemoteRoleVisual(id);
        }
    }

    public void applyRemoteAnimation(UUID id, String animationName) {
        if (id == null || animationName == null || id.equals(getLocalPlayerId())) return;
        game.state.remoteAnimationStates.put(id, animationName);
        game.getGhostManager().applyGhostAnimation(id, animationName);
    }

    public void applyServerPickupState(int pickupId, int pickupType, boolean active, float x, float z, long respawnEpochMs) {
        serverPickupsAuthoritative = true;
        MyGamePickupRecord pickup = findPickup(pickupId);
        if (pickup == null) return;
        setServerPickupLocation(pickup, x, z);
        if (active) activatePickup(pickup);
        else hidePickupFromServer(pickup, respawnEpochMs);
    }

    public void applyServerPickupSpawn(int pickupId, int pickupType, float x, float z) {
        serverPickupsAuthoritative = true;
        MyGamePickupRecord pickup = findPickup(pickupId);
        if (pickup == null) return;
        setServerPickupLocation(pickup, x, z);
        activatePickup(pickup);
    }

    public void applyServerPickupHide(int pickupId, long respawnEpochMs) {
        serverPickupsAuthoritative = true;
        MyGamePickupRecord pickup = findPickup(pickupId);
        if (pickup == null) return;
        hidePickupFromServer(pickup, respawnEpochMs);
    }

    public void applyServerPickupGrant(int pickupId, int pickupType) {
        serverPickupsAuthoritative = true;
        MyGamePickupRecord pickup = findPickup(pickupId);
        if (pickup == null) return;
        grantPickup(pickup, true);
    }

    public void applyServerRoundWaiting() {
        serverRoundAuthoritative = true;
        serverIntermissionEndEpochMs = 0L;
        serverRoundEndEpochMs = 0L;
        game.state.zombieRoundActive = false;
        game.state.zombieIntermissionStarted = false;
        game.state.zombieIntermissionRemaining = game.state.zombieIntermissionTime;
        game.state.survivalTimeRemaining = game.state.survivalRoundDuration;
        game.state.matchHumanWon = false;
        game.state.matchZombieWon = false;
        game.state.postRoundRestartTimer = 0.0;
        resetLocalForIntermission(false);
    }

    public void applyServerRoundCountdown(long endEpochMs) {
        serverRoundAuthoritative = true;
        serverIntermissionEndEpochMs = endEpochMs;
        serverRoundEndEpochMs = 0L;
        game.state.zombieRoundActive = false;
        game.state.zombieIntermissionStarted = true;
        game.state.matchHumanWon = false;
        game.state.matchZombieWon = false;
        game.state.postRoundRestartTimer = 0.0;
        updateServerRoundTimers();
        resetLocalForIntermission(true);
        game.hudSystem.showEvent("INTERMISSION", 1.0);
    }

    public void applyServerRoundStart(UUID zombieId, long endEpochMs) {
        serverRoundAuthoritative = true;
        serverIntermissionEndEpochMs = 0L;
        serverRoundEndEpochMs = endEpochMs;
        game.state.matchHumanWon = false;
        game.state.matchZombieWon = false;
        game.state.zombieRoundActive = true;
        game.state.zombieIntermissionStarted = true;
        game.state.zombieIntermissionRemaining = 0.0;
        game.state.survivalTimeRemaining = secondsUntil(endEpochMs);
        respawnLocalPlayerAtRandomSpot();
        setLocalZombie(zombieId != null && zombieId.equals(getLocalPlayerId()), false);
        game.visualSystem.randomizeRoundSkybox(zombieId, endEpochMs);
        game.soundSystem.playScaryLaughForPlayer(zombieId);
        game.hudSystem.showEvent(game.state.localPlayerZombie ? "YOU ARE THE ZOMBIE" : "RUN FROM THE ZOMBIE", 2.0);
    }

    public void applyServerRoundEnd(String winner) {
        serverRoundAuthoritative = true;
        serverIntermissionEndEpochMs = 0L;
        serverRoundEndEpochMs = 0L;
        boolean humansWon = "humans".equals(winner);
        finishMatch(humansWon);
    }

    public void convertLocalPlayerToZombieFromHealth() {
        if (game.state.localPlayerZombie) return;
        game.state.health = 0;
        becomeZombie("OUT OF HEALTH  YOU ARE A ZOMBIE");
    }

    private void resetLocalForIntermission(boolean respawn) {
        game.state.matchHumanWon = false;
        game.state.matchZombieWon = false;
        game.state.postRoundRestartTimer = 0.0;
        game.state.zombieRoundActive = false;
        game.state.buildMode = false;
        game.state.dashCharges = 0;
        game.state.rockCharges = 0;
        game.state.buildMaterials = 0;
        game.state.babyZombieCharges = 0;
        game.state.invisCharges = 0;
        game.state.invisTimer = 0.0;
        game.state.slowTimer = 0.0;
        game.state.projectileCooldown = 0.0;
        game.state.buildAttackCooldown = 0.0;
        game.itemSystem.clearHumanCarryItems();
        setLocalZombie(false, false);
        if (respawn) respawnLocalPlayerAtRandomSpot();
    }

    private void respawnLocalPlayerAtRandomSpot() {
        Vector3f spawn = findSafeSpawnLocation();
        game.assets.avatar.setLocalLocation(spawn);
        game.state.onGround = true;
        game.state.yVel = 0.0f;
        lastSafePlayerLocation = new Vector3f(spawn);
        hasLastSafePlayerLocation = true;
        syncLocalPlayerBody();
        game.networking.sendPlayerTransform();
    }

    private Vector3f findSafeSpawnLocation() {
        for (int i = 0; i < 80; i++) {
            Vector3f candidate = game.worldBuilder.randomSpawnLocation(true);
            if (!isBlockedBySceneryCollider(candidate)) return candidate;
        }
        Vector3f fallback = game.worldBuilder.randomSpawnLocation(true);
        fallback.y = terrainHeight(fallback.x, fallback.z);
        return fallback;
    }

    private void createTerrainPhysics() {
        Vector3f loc = game.assets.terrain.getWorldLocation();
        Quaternionf rot = new Quaternionf();
        game.assets.terrain.getWorldRotation().getNormalizedRotation(rot);
        terrainBody = MyGame.getEngine().getSceneGraph().addPhysicsStaticTerrainMesh(
                loc, rot, game.assets.heightMaptx, 200.0f, 20.0f, 100);
        terrainBody.setFriction(1.0f);
        terrainBody.setBounciness(0.0f);
        bodyInfo.put(terrainBody, new MyGameBodyInfo(MyGameBodyKind.TERRAIN));
        game.assets.terrain.setPhysicsObject(terrainBody);
    }

    private void createLocalPlayerBody() {
        Quaternionf rot = getObjectRotation(game.assets.avatar);
        localPlayerBody = MyGame.getEngine().getSceneGraph().addPhysicsCapsule(
                1.0f, getPlayerBodyLocation(game.assets.avatar.getWorldLocation()), rot, 1, playerBodyRadius, playerBodyHeight);
        localPlayerBody.setFriction(0.2f);
        localPlayerBody.setBounciness(0.0f);
        localPlayerBody.setDamping(0.6f, 1.0f);
        localPlayerBody.disableSleeping();
        localPlayerBody.setAngularFactor(0f);
        game.assets.avatar.setPhysicsObject(localPlayerBody);
        bodyInfo.put(localPlayerBody, new MyGameBodyInfo(MyGameBodyKind.LOCAL_PLAYER));
    }

    private void createPickups() {
        int[] pickupTypes = GameConstants.ZOMBIE_TAG_PICKUP_TYPES;
        for (int i = 0; i < pickupTypes.length; i++) {
            int pickupType = pickupTypes[i];
            pickups.add(createPickup(i, pickupType, pickupKindForType(pickupType), pickupColorForType(pickupType), pickupLabelForType(pickupType)));
        }
        for (MyGamePickupRecord pickup : pickups) activatePickup(pickup);
    }

    private MyGameBodyKind pickupKindForType(int pickupType) {
        if (pickupType == PICKUP_TYPE_DASH) return MyGameBodyKind.DASH_PICKUP;
        if (pickupType == PICKUP_TYPE_INVIS) return MyGameBodyKind.INVIS_PICKUP;
        if (pickupType == PICKUP_TYPE_BUILD) return MyGameBodyKind.BUILD_PICKUP;
        if (pickupType == PICKUP_TYPE_ROCK) return MyGameBodyKind.ROCK_PICKUP;
        if (pickupType == PICKUP_TYPE_BABY_ZOMBIE) return MyGameBodyKind.BABY_ZOMBIE_PICKUP;
        if (pickupType == PICKUP_TYPE_FLASHLIGHT) return MyGameBodyKind.FLASHLIGHT_PICKUP;
        if (pickupType == PICKUP_TYPE_POTION) return MyGameBodyKind.POTION_PICKUP;
        return MyGameBodyKind.BUILD_PICKUP;
    }

    private Vector3f pickupColorForType(int pickupType) {
        if (pickupType == PICKUP_TYPE_DASH) return new Vector3f(0.1f, 0.6f, 1.0f);
        if (pickupType == PICKUP_TYPE_INVIS) return new Vector3f(0.22f, 0.12f, 0.08f);
        if (pickupType == PICKUP_TYPE_BUILD) return new Vector3f(1.0f, 0.8f, 0.15f);
        if (pickupType == PICKUP_TYPE_ROCK) return new Vector3f(0.55f, 0.55f, 0.55f);
        if (pickupType == PICKUP_TYPE_BABY_ZOMBIE) return new Vector3f(0.2f, 0.85f, 0.25f);
        if (pickupType == PICKUP_TYPE_FLASHLIGHT) return new Vector3f(1.0f, 0.95f, 0.55f);
        return new Vector3f(0.9f, 0.2f, 0.25f);
    }

    private String pickupLabelForType(int pickupType) {
        if (pickupType == PICKUP_TYPE_DASH) return "DASH";
        if (pickupType == PICKUP_TYPE_INVIS) return "INVIS";
        if (pickupType == PICKUP_TYPE_BUILD) return "MAT";
        if (pickupType == PICKUP_TYPE_ROCK) return "ROCK";
        if (pickupType == PICKUP_TYPE_BABY_ZOMBIE) return "BABY";
        if (pickupType == PICKUP_TYPE_FLASHLIGHT) return "LIGHT";
        if (pickupType == PICKUP_TYPE_POTION) return "POTION";
        return "ITEM";
    }

    private MyGamePickupRecord createPickup(int id, int pickupType, MyGameBodyKind kind, Vector3f color, String label) {
        GameObject object;
        if (kind == MyGameBodyKind.BUILD_PICKUP) object = new GameObject(GameObject.root(), game.assets.cubeS, game.assets.woodBlockTx);
        else if (kind == MyGameBodyKind.ROCK_PICKUP) object = new GameObject(GameObject.root(), game.assets.rock2S, game.assets.rockTx);
        else if (kind == MyGameBodyKind.INVIS_PICKUP) object = new GameObject(GameObject.root(), game.assets.lowpolyHoodS, game.assets.hoodTx);
        else if (kind == MyGameBodyKind.DASH_PICKUP) object = new GameObject(GameObject.root(), game.assets.dashPickupS);
        else if (kind == MyGameBodyKind.BABY_ZOMBIE_PICKUP) object = new GameObject(GameObject.root(), game.assets.babyZombieS, game.assets.babyZombieTx);
        else if (kind == MyGameBodyKind.FLASHLIGHT_PICKUP) object = new GameObject(GameObject.root(), game.assets.flashlightS, game.assets.flashlightTx);
        else if (kind == MyGameBodyKind.POTION_PICKUP) object = new GameObject(GameObject.root(), game.assets.healthPotionS, game.assets.healthPotionTx);
        else object = new GameObject(GameObject.root(), game.assets.cubeS);
        object.setLocalLocation(new Vector3f(0f, -1000f, 0f));
        object.setLocalScale(new Matrix4f().scaling(getPickupVisualScale(kind)));
        object.getRenderStates().hasLighting(kind != MyGameBodyKind.POTION_PICKUP);
        object.getRenderStates().setHasSolidColor(kind == MyGameBodyKind.DASH_PICKUP);
        if (kind == MyGameBodyKind.DASH_PICKUP) object.getRenderStates().setColor(color);
        if (kind == MyGameBodyKind.POTION_PICKUP) {
            stylePotionGlass(object, 0.28f);
        }
        object.getRenderStates().disableRendering();
        return new MyGamePickupRecord(id, pickupType, kind, object, label);
    }

    private float getPickupVisualScale(MyGameBodyKind kind) {
        if (kind == MyGameBodyKind.BUILD_PICKUP) return 0.28f;
        if (kind == MyGameBodyKind.ROCK_PICKUP) return 0.28f;
        if (kind == MyGameBodyKind.INVIS_PICKUP) return 0.55f;
        if (kind == MyGameBodyKind.DASH_PICKUP) return 0.09f;
        if (kind == MyGameBodyKind.BABY_ZOMBIE_PICKUP) return 0.15f;
        if (kind == MyGameBodyKind.FLASHLIGHT_PICKUP) return 0.18f;
        if (kind == MyGameBodyKind.POTION_PICKUP) return 0.15f;
        return 0.45f;
    }

    public void stylePotionGlass(GameObject object, float opacity) {
        if (object == null) return;
        object.getRenderStates().hasLighting(false);
        object.getRenderStates().setHasSolidColor(true);
        object.getRenderStates().setColor(new Vector3f(0.72f, 0.96f, 1.0f));
        object.getRenderStates().isTransparent(true);
        object.getRenderStates().setOpacity(opacity);
    }

    private void activatePickup(MyGamePickupRecord pickup) {
        if (!serverPickupsAuthoritative) assignRandomPickupLocation(pickup);
        if (pickup.active) {
            Vector3f display = getPickupDisplayLocation(pickup);
            pickup.object.setLocalLocation(display);
            if (pickup.body != null) pickup.body.setTransform(display, getObjectRotation(pickup.object));
            return;
        }
        pickup.object.getRenderStates().enableRendering();
        Quaternionf rot = getObjectRotation(pickup.object);
        pickup.body = MyGame.getEngine().getSceneGraph().addPhysicsSphere(0f, getPickupDisplayLocation(pickup), rot, pickupColliderRadius);
        pickup.body.setBounciness(0.0f);
        pickup.body.setFriction(0.5f);
        pickup.object.setPhysicsObject(pickup.body);
        pickup.active = true;
        pickup.claimPending = false;
        pickup.respawnTimer = 0.0;
        bodyInfo.put(pickup.body, MyGameBodyInfo.pickup(pickup.kind, pickup));
    }

    private void hidePickup(MyGamePickupRecord pickup, double respawnSeconds) {
        if (!pickup.active) return;
        pickup.object.getRenderStates().disableRendering();
        bodyInfo.remove(pickup.body);
        MyGame.getEngine().getSceneGraph().removePhysicsObject(pickup.body);
        pickup.object.setPhysicsObject(null);
        pickup.body = null;
        pickup.active = false;
        pickup.claimPending = false;
        pickup.respawnTimer = respawnSeconds;
    }

    private void hidePickupFromServer(MyGamePickupRecord pickup, long respawnEpochMs) {
        boolean shouldPlayPickupSound = pickup.active && !pickup.claimPending;
        Vector3f soundLocation = pickup.object.getWorldLocation();
        if (pickup.active) hidePickup(pickup, secondsUntil(respawnEpochMs));
        else {
            pickup.claimPending = false;
            pickup.respawnTimer = secondsUntil(respawnEpochMs);
            pickup.object.getRenderStates().disableRendering();
        }
        if (shouldPlayPickupSound) game.soundSystem.playPickupCollect(soundLocation);
    }

    private void setServerPickupLocation(MyGamePickupRecord pickup, float x, float z) {
        pickup.location.set(x, terrainHeight(x, z) + pickupBaseLift, z);
        pickup.bobTime = 0.0f;
        if (pickup.bobPhase == 0.0f) pickup.bobPhase = randomRange(0f, (float) (Math.PI * 2.0));
        Vector3f display = getPickupDisplayLocation(pickup);
        pickup.object.setLocalLocation(display);
        if (pickup.body != null) pickup.body.setTransform(display, getObjectRotation(pickup.object));
    }

    private MyGamePickupRecord findPickup(int pickupId) {
        for (MyGamePickupRecord pickup : pickups) {
            if (pickup.id == pickupId) return pickup;
        }
        return null;
    }

    private void updatePickupRespawns(float dt) {
        if (serverPickupsAuthoritative) return;
        for (MyGamePickupRecord pickup : pickups) {
            if (pickup.active) continue;
            pickup.respawnTimer -= dt;
            if (pickup.respawnTimer <= 0.0) activatePickup(pickup);
        }
    }

    private void updateActivePickupBobs(float dt) {
        for (MyGamePickupRecord pickup : pickups) {
            if (!pickup.active) continue;
            pickup.bobTime += dt;
            Vector3f display = getPickupDisplayLocation(pickup);
            pickup.object.setLocalLocation(display);
            if (pickup.body != null) pickup.body.setTransform(display, getObjectRotation(pickup.object));
        }
    }

    private void assignRandomPickupLocation(MyGamePickupRecord pickup) {
        Vector3f playerPos = game.assets.avatar.getWorldLocation();
        Vector3f chosen = null;
        for (int i = 0; i < 50; i++) {
            float x = randomRange(-pickupSpawnRadius, pickupSpawnRadius);
            float z = randomRange(-pickupSpawnRadius, pickupSpawnRadius);
            Vector3f candidate = new Vector3f(x, terrainHeight(x, z) + pickupBaseLift, z);
            if (candidate.distance(playerPos) < pickupMinPlayerDistance) continue;
            if (!farEnoughFromOtherPickups(pickup, candidate)) continue;
            chosen = candidate;
            break;
        }
        if (chosen == null) {
            float x = randomRange(-pickupSpawnRadius, pickupSpawnRadius);
            float z = randomRange(-pickupSpawnRadius, pickupSpawnRadius);
            chosen = new Vector3f(x, terrainHeight(x, z) + pickupBaseLift, z);
        }
        pickup.location.set(chosen);
        pickup.bobTime = 0.0f;
        pickup.bobPhase = randomRange(0f, (float) (Math.PI * 2.0));
        pickup.object.setLocalLocation(getPickupDisplayLocation(pickup));
    }

    private boolean farEnoughFromOtherPickups(MyGamePickupRecord pickup, Vector3f candidate) {
        for (MyGamePickupRecord other : pickups) {
            if (other == pickup) continue;
            if (other.location.distance(candidate) < pickupMinItemDistance) return false;
        }
        return true;
    }

    private Vector3f getPickupDisplayLocation(MyGamePickupRecord pickup) {
        float bob = (float) Math.sin((pickup.bobTime * pickupBobSpeed) + pickup.bobPhase) * pickupBobAmplitude;
        return new Vector3f(pickup.location).add(0f, bob, 0f);
    }

    private float randomRange(float min, float max) {
        return min + pickupRandom.nextFloat() * (max - min);
    }

    private void syncManualBodies() {
        syncLocalPlayerBody();

        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            PhysicsObject body = remotePlayerBodies.get(ghost.getID());
            if (body == null) continue;
            body.setTransform(getPlayerBodyLocation(ghost.getWorldLocation()), getObjectRotation(ghost));
            body.setLinearVelocity(new float[] {0f, 0f, 0f});
            body.setAngularVelocity(new float[] {0f, 0f, 0f});
        }
    }

    private void syncLocalPlayerBody() {
        if (localPlayerBody == null) return;
        localPlayerBody.setTransform(getPlayerBodyLocation(game.assets.avatar.getWorldLocation()), getObjectRotation(game.assets.avatar));
        localPlayerBody.setLinearVelocity(new float[] {0f, 0f, 0f});
        localPlayerBody.setAngularVelocity(new float[] {0f, 0f, 0f});
    }

    private void ensureRemotePlayerBodies() {
        HashSet<UUID> liveGhostIds = new HashSet<>();
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            UUID id = ghost.getID();
            liveGhostIds.add(id);
            if (remotePlayerBodies.containsKey(id)) continue;

            PhysicsObject body = MyGame.getEngine().getSceneGraph().addPhysicsCapsule(
                    1.0f, getPlayerBodyLocation(ghost.getWorldLocation()), getObjectRotation(ghost), 1, playerBodyRadius, playerBodyHeight);
            body.setFriction(0.2f);
            body.setBounciness(0.0f);
            body.setDamping(0.6f, 1.0f);
            body.disableSleeping();
            body.setAngularFactor(0f);
            remotePlayerBodies.put(id, body);
            bodyInfo.put(body, MyGameBodyInfo.remotePlayer(id));
            game.state.remoteZombieStates.putIfAbsent(id, false);
            game.state.remoteInvisibleStates.putIfAbsent(id, false);
            applyRemoteRoleVisual(id);
        }

        ArrayList<UUID> stale = new ArrayList<>();
        for (UUID id : remotePlayerBodies.keySet()) {
            if (!liveGhostIds.contains(id)) stale.add(id);
        }
        for (UUID id : stale) removeRemotePlayerBody(id);
    }

    private void syncSceneryCollider(GameObject object, boolean tree) {
        if (object == null) return;
        MyGameSceneryBounds bounds = getSceneryBounds(object);
        if (bounds == null) return;

        Vector3f loc;
        Quaternionf rot;
        if (tree) {
            createTreeTrunkColliders(object, bounds);
            return;
        }

        alignObjectBottomToTerrain(object, bounds, 0.02f);
        float scale = object.getWorldScale().m00();
        float[] size = getRockColliderHalfExtents(bounds, scale);
        Vector3f offset = new Vector3f(bounds.centerX * object.getWorldScale().m00(),
                0f,
                bounds.centerZ * object.getWorldScale().m00());
        offset.mulDirection(object.getWorldRotation());
        Vector3f objectLoc = object.getWorldLocation();
        float bottomY = objectLoc.y + bounds.minY * scale;
        loc = new Vector3f(objectLoc.x + offset.x, bottomY + size[1], objectLoc.z + offset.z);
        object.getWorldRotation().getNormalizedRotation(rot = new Quaternionf());
        PhysicsObject body = MyGame.getEngine().getSceneGraph().addPhysicsBox(0f, loc, rot, size);
        body.setFriction(1.0f);
        body.setBounciness(0.0f);
        float visualTop = objectLoc.y + bounds.maxY * scale + 0.03f;
        MyGameSceneryRecord record = new MyGameSceneryRecord(object, body, false, loc, size[0], size[1], size[2], true);
        record.walkableTopY = visualTop;
        sceneryRecords.add(record);
        bodyInfo.put(body, MyGameBodyInfo.scenery(record));
    }

    private void createTreeTrunkColliders(GameObject object, MyGameSceneryBounds bounds) {
        float scale = object.getWorldScale().m00();
        ArrayList<MyGameTreeColliderSpec> specs = getTreeColliderSpecs(object, bounds);
        alignTreeVisualToColliderGround(object, specs, scale);
        Vector3f treeLoc = object.getWorldLocation();
        Quaternionf rot = new Quaternionf();

        for (MyGameTreeColliderSpec spec : specs) {
            Vector3f offset = new Vector3f(spec.localCenterX * scale, 0f, spec.localCenterZ * scale);
            offset.mulDirection(object.getWorldRotation());
            float x = treeLoc.x + offset.x;
            float z = treeLoc.z + offset.z;
            float radius = clamp(spec.localRadius * scale, 0.28f, 0.95f);
            float halfHeight = clamp(spec.localHalfHeight * scale, 1.15f, 3.35f);
            Vector3f loc = new Vector3f(x, terrainHeight(x, z) + halfHeight, z);

            PhysicsObject body = MyGame.getEngine().getSceneGraph().addPhysicsCylinder(0f, loc, rot, 1, radius, halfHeight);
            body.setFriction(1.0f);
            body.setBounciness(0.0f);

            MyGameSceneryRecord record = new MyGameSceneryRecord(object, body, true, loc, radius, halfHeight, radius, false);
            sceneryRecords.add(record);
            bodyInfo.put(body, MyGameBodyInfo.scenery(record));
        }
    }

    private void alignTreeVisualToColliderGround(GameObject object, ArrayList<MyGameTreeColliderSpec> specs, float scale) {
        if (object == null || specs == null || specs.isEmpty()) return;
        MyGameTreeColliderSpec anchor = specs.get(0);
        float bestDistance = Float.MAX_VALUE;
        for (MyGameTreeColliderSpec spec : specs) {
            float distance = (spec.localCenterX * spec.localCenterX) + (spec.localCenterZ * spec.localCenterZ);
            if (distance < bestDistance) {
                bestDistance = distance;
                anchor = spec;
            }
        }

        Vector3f treeLoc = object.getWorldLocation();
        Vector3f offset = new Vector3f(anchor.localCenterX * scale, 0f, anchor.localCenterZ * scale);
        offset.mulDirection(object.getWorldRotation());
        float x = treeLoc.x + offset.x;
        float z = treeLoc.z + offset.z;
        float y = terrainHeight(x, z) - (anchor.localMinY * scale) + 0.01f;
        object.setLocalTranslation((new Matrix4f()).translation(treeLoc.x, y, treeLoc.z));
    }

    private void alignObjectBottomToTerrain(GameObject object, MyGameSceneryBounds bounds, float clearance) {
        if (object == null || bounds == null) return;
        float scale = object.getWorldScale().m00();
        Vector3f objectLoc = object.getWorldLocation();
        float[] vertices = object.getShape() == null ? null : object.getShape().getVertices();
        if (vertices != null && vertices.length >= 3) {
            Matrix4f rot = object.getWorldRotation();
            float bottomBand = Math.max(0.35f / Math.max(scale, 0.001f), bounds.height * 0.10f);
            float bottomLimit = bounds.minY + bottomBand;
            float plantedY = -Float.MAX_VALUE;
            int samples = 0;
            for (int i = 0; i < vertices.length; i += 3) {
                float localY = vertices[i + 1];
                if (localY > bottomLimit) continue;
                Vector3f local = new Vector3f(vertices[i] * scale, localY * scale, vertices[i + 2] * scale);
                local.mulDirection(rot);
                float groundY = terrainHeight(objectLoc.x + local.x, objectLoc.z + local.z);
                plantedY = Math.max(plantedY, groundY - local.y + clearance);
                samples++;
            }
            if (samples > 0 && plantedY != -Float.MAX_VALUE) {
                object.setLocalTranslation((new Matrix4f()).translation(objectLoc.x, plantedY, objectLoc.z));
                return;
            }
        }

        Vector3f offset = new Vector3f(bounds.centerX * scale, 0f, bounds.centerZ * scale);
        offset.mulDirection(object.getWorldRotation());
        float groundY = terrainHeight(objectLoc.x + offset.x, objectLoc.z + offset.z);
        float y = groundY - bounds.minY * scale + clearance;
        object.setLocalTranslation((new Matrix4f()).translation(objectLoc.x, y, objectLoc.z));
    }

    private ArrayList<MyGameTreeColliderSpec> getTreeColliderSpecs(GameObject object, MyGameSceneryBounds bounds) {
        int index = game.assets.sceneryTreeIndices.getOrDefault(object, -1);
        String filename = index >= 0 && game.assets.sceneryTreeModelFiles != null && index < game.assets.sceneryTreeModelFiles.length
                ? game.assets.sceneryTreeModelFiles[index]
                : "";
        if (!filename.isEmpty() && !treeColliderSpecs.containsKey(filename)) {
            treeColliderSpecs.put(filename, loadTreeColliderSpecs(filename));
        }
        ArrayList<MyGameTreeColliderSpec> specs = filename.isEmpty() ? null : treeColliderSpecs.get(filename);
        if (specs != null && !specs.isEmpty()) return specs;

        ArrayList<MyGameTreeColliderSpec> fallback = new ArrayList<>();
        fallback.add(new MyGameTreeColliderSpec(0f, 0f, 1.05f, Math.max(2.8f, bounds.height * 0.35f), bounds.minY));
        return fallback;
    }

    private ArrayList<MyGameTreeColliderSpec> loadTreeColliderSpecs(String filename) {
        ArrayList<MyGameTreeColliderSpec> specs = new ArrayList<>();
        File objFile = new File("assets/models/" + filename);
        if (!objFile.exists()) return specs;

        HashMap<String, Vector3f> materialColors = loadMaterialColors(objFile);
        ArrayList<Vector3f> vertices = new ArrayList<>();
        HashMap<String, MyGameSceneryBoundsBuilder> trunkBoundsByMaterial = new HashMap<>();
        String currentMaterial = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(objFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if ("v".equals(parts[0]) && parts.length >= 4) {
                    vertices.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                } else if ("usemtl".equals(parts[0]) && parts.length >= 2) {
                    currentMaterial = parts[1];
                } else if ("f".equals(parts[0]) && isTreeTrunkMaterial(currentMaterial, materialColors)) {
                    MyGameSceneryBoundsBuilder builder = trunkBoundsByMaterial.computeIfAbsent(currentMaterial, key -> new MyGameSceneryBoundsBuilder());
                    for (int i = 1; i < parts.length; i++) {
                        int vertexIndex = parseObjVertexIndex(parts[i], vertices.size());
                        if (vertexIndex >= 0 && vertexIndex < vertices.size()) builder.include(vertices.get(vertexIndex));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            return specs;
        }

        for (MyGameSceneryBoundsBuilder builder : trunkBoundsByMaterial.values()) {
            MyGameSceneryBounds b = builder.toBounds();
            if (b == null || b.height < 0.5f) continue;
            float localRadius = Math.max(0.38f, Math.min(b.width, b.depth) * 0.45f);
            float localHalfHeight = Math.max(2.2f, b.height * 0.45f);
            specs.add(new MyGameTreeColliderSpec(b.centerX, b.centerZ, localRadius, localHalfHeight, b.minY));
        }
        return specs;
    }

    private HashMap<String, Vector3f> loadMaterialColors(File objFile) {
        HashMap<String, Vector3f> colors = new HashMap<>();
        String mtlName = objFile.getName().replaceAll("\\.obj$", ".mtl");
        File mtlFile = new File(objFile.getParentFile(), mtlName);
        if (!mtlFile.exists()) return colors;

        try (BufferedReader reader = new BufferedReader(new FileReader(mtlFile))) {
            String material = "";
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if ("newmtl".equals(parts[0]) && parts.length >= 2) {
                    material = parts[1];
                } else if ("Kd".equals(parts[0]) && parts.length >= 4 && !material.isEmpty()) {
                    colors.put(material, new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                }
            }
        } catch (IOException | NumberFormatException e) {
            return colors;
        }
        return colors;
    }

    private boolean isTreeTrunkMaterial(String material, HashMap<String, Vector3f> colors) {
        if (material == null || material.isEmpty()) return false;
        String lower = material.toLowerCase();
        if (lower.contains("trunk")) return true;
        Vector3f color = colors.get(material);
        return color != null && color.x > color.y * 1.6f && color.y >= color.z;
    }

    private int parseObjVertexIndex(String token, int vertexCount) {
        String[] refs = token.split("/", -1);
        if (refs.length == 0 || refs[0].isEmpty()) return -1;
        int index = Integer.parseInt(refs[0]);
        if (index < 0) index = vertexCount + index + 1;
        return index - 1;
    }

    private float[] getRockColliderHalfExtents(MyGameSceneryBounds bounds, float scale) {
        return new float[] {
                clamp(bounds.width * scale * 0.36f, 0.20f, 2.3f),
                clamp(bounds.height * scale * 0.36f, 0.16f, 1.65f),
                clamp(bounds.depth * scale * 0.36f, 0.20f, 2.3f)
        };
    }

    private MyGameSceneryBounds getSceneryBounds(GameObject object) {
        if (object == null || object.getShape() == null || object.getShape().getVertices() == null) return null;
        float[] vertices = object.getShape().getVertices();
        if (vertices.length < 3) return null;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }
        return new MyGameSceneryBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void updateRound(float dt) {
        if (game.isMatchOver()) {
            if (!serverRoundAuthoritative) updateLocalPostRoundRestart(dt);
            return;
        }
        if (serverRoundAuthoritative) {
            updateServerRoundTimers();
            return;
        }
        if (game.state.zombieRoundActive) {
            game.state.survivalTimeRemaining -= dt;
            if (game.state.survivalTimeRemaining <= 0.0) {
                game.state.survivalTimeRemaining = 0.0;
                finishMatch(true);
                return;
            }
            if (allKnownPlayersAreZombies()) {
                finishMatch(false);
            }
            return;
        }

        if (game.state.isMultiplayer && getKnownPlayerIds().size() < 2) {
            game.state.zombieIntermissionStarted = false;
            game.state.zombieIntermissionRemaining = game.state.zombieIntermissionTime;
            return;
        }

        if (!game.state.zombieIntermissionStarted) {
            game.state.zombieIntermissionStarted = true;
            game.state.zombieIntermissionRemaining = game.state.zombieIntermissionTime;
            game.hudSystem.showEvent("INTERMISSION", 1.0);
        }

        game.state.zombieIntermissionRemaining -= dt;
        if (game.state.zombieIntermissionRemaining <= 0.0) startZombieRound();
    }

    private void updateLocalPostRoundRestart(float dt) {
        if (game.state.postRoundRestartTimer <= 0.0) game.state.postRoundRestartTimer = 5.0;
        game.state.postRoundRestartTimer -= dt;
        if (game.state.postRoundRestartTimer > 0.0) return;
        resetLocalForIntermission(true);
        game.state.zombieIntermissionStarted = true;
        game.state.zombieIntermissionRemaining = game.state.zombieIntermissionTime;
        game.hudSystem.showEvent("INTERMISSION", 1.0);
    }

    private void updateServerRoundTimers() {
        if (serverIntermissionEndEpochMs > 0L) {
            game.state.zombieIntermissionRemaining = secondsUntil(serverIntermissionEndEpochMs);
        }
        if (serverRoundEndEpochMs > 0L && game.state.zombieRoundActive) {
            game.state.survivalTimeRemaining = secondsUntil(serverRoundEndEpochMs);
        }
    }

    private double secondsUntil(long epochMs) {
        if (epochMs <= 0L) return 0.0;
        return Math.max(0.0, (epochMs - System.currentTimeMillis()) / 1000.0);
    }

    private void startZombieRound() {
        game.state.matchHumanWon = false;
        game.state.matchZombieWon = false;
        game.state.postRoundRestartTimer = 0.0;
        game.state.survivalTimeRemaining = game.state.survivalRoundDuration;
        respawnLocalPlayerAtRandomSpot();
        UUID zombieId = chooseStartingZombie();
        setLocalZombie(zombieId != null && zombieId.equals(getLocalPlayerId()), true);

        for (UUID id : getKnownPlayerIds()) {
            if (id.equals(getLocalPlayerId())) continue;
            boolean zombie = zombieId != null && id.equals(zombieId);
            game.state.remoteZombieStates.put(id, zombie);
            game.state.remoteInvisibleStates.put(id, false);
            applyRemoteRoleVisual(id);
        }

        game.state.zombieRoundActive = true;
        game.state.zombieIntermissionStarted = true;
        game.state.zombieIntermissionRemaining = 0.0;
        game.visualSystem.randomizeRoundSkybox(zombieId, System.currentTimeMillis());
        game.soundSystem.playScaryLaughForPlayer(zombieId);
        game.hudSystem.showEvent(game.state.localPlayerZombie ? "YOU ARE THE ZOMBIE" : "RUN FROM THE ZOMBIE", 2.0);
    }

    private void finishMatch(boolean humansWon) {
        game.state.zombieRoundActive = false;
        game.state.matchHumanWon = humansWon;
        game.state.matchZombieWon = !humansWon;
        game.state.postRoundRestartTimer = serverRoundAuthoritative ? 0.0 : 5.0;
        game.hudSystem.showEvent(humansWon ? "HUMANS WIN" : "ZOMBIES WIN", 3.0);
    }

    private UUID chooseStartingZombie() {
        ArrayList<UUID> ids = getKnownPlayerIds();
        if (ids.size() < 2) return null;
        Collections.sort(ids, Comparator.comparing(UUID::toString));
        long seed = 0x1652026L;
        for (UUID id : ids) {
            seed ^= id.getMostSignificantBits();
            seed = Long.rotateLeft(seed, 13) ^ id.getLeastSignificantBits();
        }
        return ids.get(new Random(seed).nextInt(ids.size()));
    }

    private ArrayList<UUID> getKnownPlayerIds() {
        ArrayList<UUID> ids = new ArrayList<>();
        ids.add(getLocalPlayerId());
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) ids.add(ghost.getID());
        return ids;
    }

    private boolean allKnownPlayersAreZombies() {
        ArrayList<UUID> ids = getKnownPlayerIds();
        if (ids.size() < 2) return false;
        if (!game.state.localPlayerZombie || game.state.health > 0) return false;
        for (UUID id : ids) {
            if (id.equals(getLocalPlayerId())) continue;
            if (!game.state.remoteZombieStates.getOrDefault(id, false)) return false;
        }
        return true;
    }

    private void handleLocalPlayerCollisions() {
        if (localPlayerBody == null) return;
        for (PhysicsObject other : localPlayerBody.getNewlyCollidedSet()) {
            MyGameBodyInfo info = bodyInfo.get(other);
            if (info == null) continue;

            if (isPickupKind(info.kind)) {
                tryPickup(info.pickup);
            } else if (info.kind == MyGameBodyKind.REMOTE_PLAYER) {
                handlePlayerTagCollision(info.remoteId);
            }
        }
    }

    private boolean isPickupKind(MyGameBodyKind kind) {
        return kind == MyGameBodyKind.DASH_PICKUP || kind == MyGameBodyKind.INVIS_PICKUP
                || kind == MyGameBodyKind.BUILD_PICKUP || kind == MyGameBodyKind.ROCK_PICKUP
                || kind == MyGameBodyKind.BABY_ZOMBIE_PICKUP || kind == MyGameBodyKind.FLASHLIGHT_PICKUP
                || kind == MyGameBodyKind.POTION_PICKUP;
    }

    private void handlePlayerTagCollision(UUID remoteId) {
        if (!game.state.zombieRoundActive || remoteId == null) return;
        boolean remoteZombie = game.state.remoteZombieStates.getOrDefault(remoteId, false);
        if (game.state.localPlayerZombie && !remoteZombie) {
            if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendTagMessage(remoteId);
            game.state.remoteZombieStates.put(remoteId, true);
            applyRemoteRoleVisual(remoteId);
            game.hudSystem.showEvent("HUMAN TAGGED", 1.0);
        } else if (!game.state.localPlayerZombie && remoteZombie) {
            becomeZombie("TAGGED BY ZOMBIE");
        }
    }

    private void tryPickup(MyGamePickupRecord pickup) {
        if (pickup == null || !pickup.active || pickup.claimPending) return;
        if (serverPickupsAuthoritative && game.state.protClient != null && game.state.isClientConnected) {
            if (!canUsePickup(pickup)) return;
            pickup.claimPending = true;
            game.state.protClient.sendPickupCollectMessage(pickup.id);
            return;
        }
        grantPickup(pickup, true);
    }

    private boolean grantPickup(MyGamePickupRecord pickup, boolean hideAfterGrant) {
        if (pickup == null) return false;
        if (pickup.kind == MyGameBodyKind.DASH_PICKUP) {
            if (game.state.localPlayerZombie) return false;
            if (game.state.dashCharges > 0) {
                game.hudSystem.showEvent("DASH ALREADY STORED", 0.8);
                return false;
            }
            game.state.dashCharges = 1;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            game.hudSystem.showEvent("DASH PICKED UP  PRESS R", 1.2);
            return true;
        } else if (pickup.kind == MyGameBodyKind.INVIS_PICKUP) {
            if (!game.state.localPlayerZombie) return false;
            if (game.state.invisCharges > 0) {
                game.hudSystem.showEvent("INVIS ALREADY STORED", 0.8);
                return false;
            }
            game.state.invisCharges = 1;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            game.hudSystem.showEvent("INVIS PICKED UP  PRESS F", 1.2);
            return true;
        } else if (pickup.kind == MyGameBodyKind.BUILD_PICKUP) {
            if (game.state.localPlayerZombie) return false;
            game.state.buildMaterials++;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            game.hudSystem.showEvent("BUILD MATERIAL +" + game.state.buildMaterials, 1.0);
            return true;
        } else if (pickup.kind == MyGameBodyKind.ROCK_PICKUP) {
            if (game.state.localPlayerZombie) return false;
            if (game.state.rockCharges >= game.state.maxRockCharges) {
                game.hudSystem.showEvent("ROCKS FULL", 0.8);
                return false;
            }
            game.state.rockCharges++;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            game.hudSystem.showEvent("ROCK PICKED UP  PRESS Q", 1.0);
            return true;
        } else if (pickup.kind == MyGameBodyKind.BABY_ZOMBIE_PICKUP) {
            if (!game.state.localPlayerZombie) return false;
            if (game.state.babyZombieCharges >= game.state.maxBabyZombieCharges) {
                game.hudSystem.showEvent("BABY ZOMBIES FULL", 0.8);
                return false;
            }
            game.state.babyZombieCharges++;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            game.hudSystem.showEvent("BABY ZOMBIE PICKED UP  PRESS R", 1.0);
            return true;
        } else if (pickup.kind == MyGameBodyKind.FLASHLIGHT_PICKUP) {
            if (!game.itemSystem.grantFlashlightFromPickup()) return false;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            return true;
        } else if (pickup.kind == MyGameBodyKind.POTION_PICKUP) {
            if (!game.itemSystem.grantPotionFromPickup()) return false;
            if (hideAfterGrant) hidePickup(pickup, pickupRespawnSeconds);
            game.soundSystem.playPickupCollect(pickup.object.getWorldLocation());
            return true;
        }
        return false;
    }

    private boolean canUsePickup(MyGamePickupRecord pickup) {
        if (pickup.kind == MyGameBodyKind.DASH_PICKUP) return !game.state.localPlayerZombie && game.state.dashCharges <= 0;
        if (pickup.kind == MyGameBodyKind.INVIS_PICKUP) return game.state.localPlayerZombie && game.state.invisCharges <= 0;
        if (pickup.kind == MyGameBodyKind.BUILD_PICKUP) return !game.state.localPlayerZombie;
        if (pickup.kind == MyGameBodyKind.ROCK_PICKUP) return !game.state.localPlayerZombie && game.state.rockCharges < game.state.maxRockCharges;
        if (pickup.kind == MyGameBodyKind.BABY_ZOMBIE_PICKUP) return game.state.localPlayerZombie && game.state.babyZombieCharges < game.state.maxBabyZombieCharges;
        if (pickup.kind == MyGameBodyKind.FLASHLIGHT_PICKUP) return game.itemSystem.canGrantFlashlightFromPickup();
        if (pickup.kind == MyGameBodyKind.POTION_PICKUP) return game.itemSystem.canGrantPotionFromPickup();
        return false;
    }

    private void useHumanDash() {
        if (game.state.dashCharges <= 0) {
            game.hudSystem.showEvent("NO DASH STORED", 0.8);
            return;
        }
        Vector3f fwd = horizontalForward();
        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f target = new Vector3f(pos).add(fwd.mul(game.state.dashDistance));
        target.y = game.state.onGround ? terrainHeight(target.x, target.z) : pos.y;
        tryMoveLocalPlayer(target);
        syncLocalPlayerBody();
        game.networking.sendPlayerTransform();
        game.state.dashCharges--;
        game.hudSystem.showEvent("DASH", 0.8);
    }

    private void useZombieInvisibility() {
        if (game.state.invisTimer > 0.0) {
            game.hudSystem.showEvent("ALREADY INVISIBLE", 0.8);
            return;
        }
        if (game.state.invisCharges <= 0) {
            game.hudSystem.showEvent("NO INVIS STORED", 0.8);
            return;
        }
        game.state.invisCharges--;
        game.state.invisTimer = game.state.invisDuration;
        applyLocalRoleVisual();
        sendAbility("invis", true);
        game.hudSystem.showEvent("INVISIBLE", 1.0);
    }

    private void handleProjectileCollisions() {
        ArrayList<MyGameProjectileRecord> toRemove = new ArrayList<>();
        for (MyGameProjectileRecord projectile : projectiles) {
            if (projectile.body == null) continue;
            for (PhysicsObject other : projectile.body.getNewlyCollidedSet()) {
                MyGameBodyInfo info = bodyInfo.get(other);
                if (info == null || info.kind == MyGameBodyKind.PROJECTILE) continue;
                if (isProjectileOwner(projectile, info)) continue;

                if (info.kind == MyGameBodyKind.TERRAIN || info.kind == MyGameBodyKind.BUILD_PIECE || info.kind == MyGameBodyKind.SCENERY_BLOCKER) {
                    toRemove.add(projectile);
                    break;
                }

                if (info.kind == MyGameBodyKind.LOCAL_PLAYER) {
                    if (!projectile.ownerId.equals(getLocalPlayerId()) && projectileCanSlow(projectile.type, game.state.localPlayerZombie)) {
                        applySlow((float) game.state.slowDuration, projectile.ownerId);
                        toRemove.add(projectile);
                        break;
                    }
                } else if (info.kind == MyGameBodyKind.REMOTE_PLAYER) {
                    boolean remoteZombie = game.state.remoteZombieStates.getOrDefault(info.remoteId, false);
                    if (projectile.ownerId.equals(getLocalPlayerId()) && projectileCanSlow(projectile.type, remoteZombie)) {
                        if (game.state.protClient != null && game.state.isClientConnected) {
                            game.state.protClient.sendSlowMessage(info.remoteId, (float) game.state.slowDuration);
                        }
                        toRemove.add(projectile);
                        game.hudSystem.showEvent("PROJECTILE HIT", 0.8);
                        break;
                    }
                }
            }
        }
        for (MyGameProjectileRecord projectile : toRemove) removeProjectile(projectile);
    }

    private void cleanupProjectiles(float dt) {
        ArrayList<MyGameProjectileRecord> expired = new ArrayList<>();
        for (MyGameProjectileRecord projectile : projectiles) {
            projectile.ttl -= dt;
            if (projectile.ttl <= 0.0f) expired.add(projectile);
        }
        for (MyGameProjectileRecord projectile : expired) removeProjectile(projectile);
    }

    private boolean isProjectileOwner(MyGameProjectileRecord projectile, MyGameBodyInfo info) {
        if (info.kind == MyGameBodyKind.LOCAL_PLAYER) return projectile.ownerId.equals(getLocalPlayerId());
        if (info.kind == MyGameBodyKind.REMOTE_PLAYER) return projectile.ownerId.equals(info.remoteId);
        return false;
    }

    private boolean projectileCanSlow(int projectileType, boolean targetZombie) {
        if (projectileType == PROJECTILE_ROCK) return targetZombie;
        if (projectileType == PROJECTILE_BABY_ZOMBIE) return !targetZombie;
        return false;
    }

    private void applySlow(float seconds, UUID sourceId) {
        game.state.slowTimer = Math.max(game.state.slowTimer, seconds);
        game.hudSystem.showEvent("SLOWED", 1.0);
    }

    private void spawnProjectile(UUID ownerId, int type, Vector3f pos, Vector3f velocity, boolean localProjectile) {
        if (physicsEngine == null) return;
        GameObject object = type == PROJECTILE_ROCK
                ? new GameObject(GameObject.root(), game.assets.rock2S, game.assets.rockTx)
                : new GameObject(GameObject.root(), game.assets.babyZombieS, game.assets.babyZombieTx);
        object.setLocalLocation(new Vector3f(pos));
        object.setLocalScale(new Matrix4f().scaling(type == PROJECTILE_ROCK ? 0.28f : 0.26f));
        object.getRenderStates().hasLighting(true);
        object.getRenderStates().setHasSolidColor(false);

        Quaternionf rot = getObjectRotation(game.assets.avatar);
        Matrix4f rotMat = new Matrix4f();
        rot.get(rotMat);
        object.setLocalRotation(rotMat);

        PhysicsObject body = MyGame.getEngine().getSceneGraph().addPhysicsSphere(1.0f, new Vector3f(pos), rot, projectileRadius);
        body.setLinearVelocity(new float[] {velocity.x, velocity.y, velocity.z});
        body.setDamping(0.05f, 0.2f);
        body.setBounciness(0.2f);
        body.disableSleeping();

        MyGameProjectileRecord projectile = new MyGameProjectileRecord(ownerId, type, object, body, localProjectile, projectileTTL);
        projectiles.add(projectile);
        bodyInfo.put(body, MyGameBodyInfo.projectile(projectile));
    }

    private void syncProjectileGraphics() {
        for (MyGameProjectileRecord projectile : projectiles) {
            if (projectile.body == null || projectile.object == null) continue;
            projectile.object.setLocalLocation(projectile.body.getLocation());
            Matrix4f rot = new Matrix4f();
            projectile.body.getRotation().get(rot);
            projectile.object.setLocalRotation(rot);
        }
    }

    private void removeProjectile(MyGameProjectileRecord projectile) {
        if (projectile == null) return;
        if (projectile.body != null) {
            bodyInfo.remove(projectile.body);
            MyGame.getEngine().getSceneGraph().removePhysicsObject(projectile.body);
        }
        if (projectile.object != null) {
            projectile.object.getRenderStates().disableRendering();
            MyGame.getEngine().getSceneGraph().removeGameObject(projectile.object);
        }
        projectiles.remove(projectile);
    }

    private void resolveBuildPieceBlocking() {
        if (localPlayerBody == null) return;
        boolean blocked = false;
        Vector3f blockingPosition = null;
        for (PhysicsObject other : localPlayerBody.getFullCollidedSet()) {
            MyGameBodyInfo info = bodyInfo.get(other);
            if (info == null) continue;
            if (info.kind == MyGameBodyKind.BUILD_PIECE && info.buildRecord != null && info.buildRecord.blocksPlayer) {
                blocked = true;
                blockingPosition = info.buildRecord.position;
                break;
            }
            if (info.kind == MyGameBodyKind.SCENERY_BLOCKER && info.sceneryRecord != null) {
                if (isWalkableTopContact(info.sceneryRecord)) continue;
                blocked = true;
                blockingPosition = info.sceneryRecord.object.getWorldLocation();
                break;
            }
        }

        if (blocked && hasLastSafePlayerLocation) {
            Vector3f corrected = getUnstuckCollisionLocation(blockingPosition);
            game.assets.avatar.setLocalLocation(corrected);
            syncLocalPlayerBody();
            game.networking.sendPlayerTransform();
        } else {
            lastSafePlayerLocation = new Vector3f(game.assets.avatar.getWorldLocation());
            hasLastSafePlayerLocation = true;
        }
    }

    private Vector3f getUnstuckCollisionLocation(Vector3f blockingPosition) {
        Vector3f current = game.assets.avatar.getWorldLocation();
        Vector3f corrected = new Vector3f(lastSafePlayerLocation);
        if (blockingPosition != null) {
            Vector3f push = new Vector3f(current).sub(blockingPosition);
            push.y = 0f;
            if (push.lengthSquared() < 0.0001f) push = new Vector3f(horizontalForward()).negate();
            push.normalize().mul(0.35f);
            corrected.add(push);
        }
        corrected.y = game.state.onGround ? getWalkableGroundHeight(corrected.x, corrected.z, current.y) : current.y;
        return corrected;
    }

    private boolean isWalkableTopContact(MyGameSceneryRecord record) {
        if (record == null || record.tree || !record.walkableTop) return false;
        Vector3f player = game.assets.avatar.getWorldLocation();
        return player.y >= record.walkableTopY - 0.35f
                && isInsideSceneryFootprint(record, player.x, player.z, playerBodyRadius * 0.25f);
    }

    private boolean isBlockedBySceneryCollider(Vector3f playerFeet) {
        if (playerFeet == null || sceneryRecords.isEmpty()) return false;
        for (MyGameSceneryRecord record : sceneryRecords) {
            if (record == null) continue;
            if (!isInsideSceneryFootprint(record, playerFeet.x, playerFeet.z, playerBodyRadius * 0.65f)) continue;
            if (!playerVerticalOverlapsScenery(playerFeet.y, record)) continue;

            if (!record.tree && record.walkableTop && playerFeet.y >= record.walkableTopY - 0.30f) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean playerVerticalOverlapsScenery(float playerFeetY, MyGameSceneryRecord record) {
        float playerBottom = playerFeetY + 0.05f;
        float playerTop = playerFeetY + 2.10f;
        float objectBottom = record.center.y - record.halfY;
        float objectTop = record.center.y + record.halfY;
        return playerTop >= objectBottom && playerBottom <= objectTop;
    }

    private boolean isInsideSceneryFootprint(MyGameSceneryRecord record, float x, float z, float margin) {
        if (record == null) return false;
        float dx = x - record.center.x;
        float dz = z - record.center.z;
        float broad = Math.max(record.halfX, record.halfZ) + Math.abs(margin) + playerBodyRadius;
        if ((dx * dx) + (dz * dz) > broad * broad) return false;

        Vector3f local = new Vector3f(dx, 0f, dz);
        Matrix4f invRot = new Matrix4f(record.object.getWorldRotation()).invert();
        local.mulDirection(invRot);
        return Math.abs(local.x) <= Math.max(0.05f, record.halfX + margin)
                && Math.abs(local.z) <= Math.max(0.05f, record.halfZ + margin);
    }

    private float getWalkableBuildPieceHeight(MyGameBuildRecord record, float x, float z, float currentPlayerY) {
        if (record == null || record.object == null) return -Float.MAX_VALUE;
        if (record.pieceType == 0 && (record.modeType == 0 || record.modeType == 1)) return -Float.MAX_VALUE;

        Vector3f normal = new Vector3f(record.object.getWorldForwardVector());
        if (normal.lengthSquared() < 0.0001f || Math.abs(normal.y) < 0.08f) return -Float.MAX_VALUE;
        normal.normalize();

        float y = record.position.y - ((normal.x * (x - record.position.x)) + (normal.z * (z - record.position.z))) / normal.y;
        if (!isInsideBuildPieceSurface(record, x, y, z, playerBodyRadius * 0.45f)) return -Float.MAX_VALUE;

        boolean canStepOnto = y <= currentPlayerY + 1.20f && currentPlayerY >= y - 1.70f;
        boolean canLandOn = !game.state.onGround && currentPlayerY >= y - 0.35f;
        return (canStepOnto || canLandOn) ? y : -Float.MAX_VALUE;
    }

    private boolean isInsideBuildPieceSurface(MyGameBuildRecord record, float x, float y, float z, float margin) {
        Vector3f local = new Vector3f(x - record.position.x, y - record.position.y, z - record.position.z);
        Matrix4f invRot = new Matrix4f(record.object.getWorldRotation()).invert();
        local.mulDirection(invRot);
        Matrix4f scale = record.object.getWorldScale();
        float halfX = Math.abs(scale.m00()) + margin;
        float halfY = Math.abs(scale.m11()) + margin;
        return Math.abs(local.x) <= halfX && Math.abs(local.y) <= halfY;
    }

    private MyGameBuildRecord findBuildPieceInFront() {
        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f forward = horizontalForward();
        MyGameBuildRecord best = null;
        float bestDistance = Float.MAX_VALUE;
        for (MyGameBuildRecord record : buildRecords.values()) {
            Vector3f toPiece = new Vector3f(record.position).sub(pos);
            toPiece.y = 0f;
            float dist = toPiece.length();
            if (dist > 3.0f || dist < 0.001f) continue;
            float dot = toPiece.normalize().dot(forward);
            if (dot < 0.55f) continue;
            if (dist < bestDistance) {
                bestDistance = dist;
                best = record;
            }
        }
        return best;
    }

    private UUID findHumanInFront() {
        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f forward = horizontalForward();
        UUID best = null;
        float bestDistance = Float.MAX_VALUE;
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            UUID id = ghost.getID();
            if (game.state.remoteZombieStates.getOrDefault(id, false)) continue;
            Vector3f toPlayer = new Vector3f(ghost.getWorldLocation()).sub(pos);
            float vertical = Math.abs(toPlayer.y);
            toPlayer.y = 0f;
            float dist = toPlayer.length();
            if (dist > 2.7f || dist < 0.001f || vertical > 1.5f) continue;
            float dot = toPlayer.normalize().dot(forward);
            if (dot < 0.45f) continue;
            if (dist < bestDistance) {
                bestDistance = dist;
                best = id;
            }
        }
        return best;
    }

    private void becomeZombie(String message) {
        if (game.state.localPlayerZombie) return;
        setLocalZombie(true, true);
        game.hudSystem.showEvent(message, 1.5);
    }

    private void setLocalZombie(boolean zombie, boolean broadcast) {
        game.state.localPlayerZombie = zombie;
        if (zombie) {
            game.state.health = 0;
            game.state.dashCharges = 0;
            game.state.rockCharges = 0;
            game.state.buildMode = false;
            game.itemSystem.clearHumanCarryItems();
        } else {
            game.state.health = game.state.maxHealth;
            game.state.babyZombieCharges = 0;
            game.state.invisCharges = 0;
            game.state.invisTimer = 0.0;
            game.itemSystem.clearZombieCarryItems();
        }
        applyLocalRoleVisual();
        if (broadcast && game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendRoleMessage(zombie);
            game.state.protClient.sendHealthMessage(game.state.health);
        }
    }

    private void sendAbility(String ability, boolean active) {
        if (game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendAbilityMessage(ability, active);
        }
    }

    private void applyLocalRoleVisual() {
        RenderStates rs = game.assets.avatar.getRenderStates();
        boolean playerModel1 = !game.isPlayerModel2Selected();
        if (playerModel1) {
            game.assets.avatar.setTextureImage(game.state.localPlayerZombie ? game.assets.zombiePlayerModel1Tx : game.assets.playerModel1Tx);
            rs.setHasSolidColor(false);
        } else {
            game.assets.avatar.setTextureImage(game.state.localPlayerZombie ? game.assets.zombiePlayerModel2Tx : game.assets.playerModel2Tx);
            rs.setHasSolidColor(false);
        }
        boolean invisible = game.state.localPlayerZombie && game.state.invisTimer > 0.0;
        if (invisible) rs.disableRendering();
        else rs.enableRendering();
        rs.isTransparent(false);
        rs.setOpacity(1.0f);
        game.itemSystem.updateEquippedItemVisibility();
    }

    private void applyRemoteRoleVisual(UUID id) {
        GhostAvatar ghost = game.getGhostManager().getGhostAvatar(id);
        if (ghost == null) return;
        boolean zombie = game.state.remoteZombieStates.getOrDefault(id, false);
        boolean invisible = zombie && game.state.remoteInvisibleStates.getOrDefault(id, false);
        RenderStates rs = ghost.getRenderStates();
        boolean playerModel1 = !"playerModel2".equals(ghost.getAvatarType());
        if (playerModel1) {
            ghost.setTextureImage(zombie ? game.assets.zombiePlayerModel1Tx : game.assets.playerModel1Tx);
            rs.setHasSolidColor(false);
        } else {
            ghost.setTextureImage(zombie ? game.assets.zombiePlayerModel2Tx : game.assets.playerModel2Tx);
            rs.setHasSolidColor(false);
        }
        if (invisible) rs.disableRendering();
        else rs.enableRendering();
        rs.isTransparent(false);
        rs.setOpacity(1.0f);
    }

    private Vector3f horizontalForward() {
        Vector3f fwd = game.avatarForward();
        fwd.y = 0f;
        if (fwd.lengthSquared() < 0.0001f) fwd.set(0f, 0f, -1f);
        return fwd.normalize();
    }

    private Vector3f getPlayerBodyLocation(Vector3f playerLocation) {
        return new Vector3f(playerLocation).add(0f, playerBodyYOffset, 0f);
    }

    private Quaternionf getObjectRotation(GameObject object) {
        Quaternionf rot = new Quaternionf();
        object.getWorldRotation().getNormalizedRotation(rot);
        return rot;
    }

    private float terrainHeight(float x, float z) {
        return game.assets.terrain == null ? 0f : game.assets.terrain.getHeight(x, z);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private UUID getLocalPlayerId() {
        if (game.state.protClient != null) return game.state.protClient.getID();
        return game.state.offlineLocalId;
    }

    private float[] getBuildPieceColliderSize(int pieceType, int modeType) {
        float halfThickness = 0.10f;
        if (pieceType == 0) {
            return new float[] {game.state.buildWallHalfW * 0.96f, game.state.buildWallHalfH * 0.96f, halfThickness};
        }
        return new float[] {1.36f, game.state.buildWallHalfH * 0.96f, halfThickness};
    }

}
