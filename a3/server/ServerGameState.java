package a3.server;

import a3.GameConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class ServerGameState {
    private static final int[] PICKUP_TYPES = GameConstants.ZOMBIE_TAG_PICKUP_TYPES;
    private static final long PICKUP_RESPAWN_MS = 30000L;
    private static final long INTERMISSION_MS = 30000L;
    private static final long POST_ROUND_MS = 5000L;
    private static final long ROUND_MS = 300000L;
    private static final float PICKUP_SPAWN_RADIUS = 90.0f;
    private static final float PICKUP_MIN_CENTER_DISTANCE = 10.0f;
    private static final float PICKUP_MIN_ITEM_DISTANCE = 8.0f;

    private final ServerMessenger messenger;
    private final LinkedHashSet<UUID> players = new LinkedHashSet<>();
    private final Map<UUID, Boolean> zombieStates = new HashMap<>();
    private final Map<UUID, Integer> healthStates = new HashMap<>();
    private final Map<UUID, String> animationStates = new HashMap<>();
    private final Map<UUID, Boolean> invisibleStates = new HashMap<>();
    private final Map<String, String> buildStates = new HashMap<>();
    private final PickupState[] pickups = new PickupState[PICKUP_TYPES.length];
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ZombieTagServerState");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> roundStartTask;
    private ScheduledFuture<?> roundEndTask;
    private ScheduledFuture<?> postRoundTask;
    private boolean countdownActive = false;
    private boolean roundActive = false;
    private boolean matchEnded = false;
    private long countdownEndEpochMs = 0L;
    private long roundEndEpochMs = 0L;
    private UUID startingZombieId;

    ServerGameState(ServerMessenger messenger) {
        this.messenger = messenger;
        for (int i = 0; i < pickups.length; i++) {
            pickups[i] = new PickupState(i, PICKUP_TYPES[i]);
            spawnPickupAtRandomLocation(pickups[i]);
            pickups[i].active = true;
        }
    }

    synchronized void onJoin(UUID playerId) {
        if (playerId == null) return;
        players.add(playerId);
        zombieStates.putIfAbsent(playerId, false);
        healthStates.putIfAbsent(playerId, 100);
        animationStates.putIfAbsent(playerId, "IDLE");
        invisibleStates.putIfAbsent(playerId, false);

        sendSnapshotTo(playerId);
        startCountdownIfReady();
    }

    synchronized void onBye(UUID playerId) {
        players.remove(playerId);
        zombieStates.remove(playerId);
        healthStates.remove(playerId);
        animationStates.remove(playerId);
        invisibleStates.remove(playerId);

        if (players.size() < 2) {
            resetRoundState();
            resetPlayersToHumans();
            broadcastAll("round,waiting");
            return;
        }
        checkZombieWin();
    }

    synchronized void onRole(UUID playerId, boolean zombie) {
        if (!players.contains(playerId)) return;
        zombieStates.put(playerId, zombie);
        healthStates.put(playerId, zombie ? 0 : 100);
        if (!zombie) invisibleStates.put(playerId, false);
        broadcastAll("role," + playerId + "," + (zombie ? 1 : 0));
        broadcastAll("health," + playerId + "," + (zombie ? 0 : 100));
        checkZombieWin();
    }

    synchronized void onTag(UUID sourceId, UUID targetId) {
        if (!players.contains(sourceId) || !players.contains(targetId)) return;
        if (!zombieStates.getOrDefault(sourceId, false)) return;
        if (zombieStates.getOrDefault(targetId, false)) return;

        zombieStates.put(targetId, true);
        healthStates.put(targetId, 0);
        invisibleStates.put(targetId, false);
        broadcastAll("tag," + sourceId + "," + targetId);
        broadcastAll("role," + targetId + ",1");
        broadcastAll("health," + targetId + ",0");
        checkZombieWin();
    }

    synchronized void onHealth(UUID playerId, int health) {
        if (!players.contains(playerId)) return;
        int clamped = Math.max(0, Math.min(100, health));
        healthStates.put(playerId, clamped);
        broadcastAll("health," + playerId + "," + clamped);
        if (clamped <= 0 && !zombieStates.getOrDefault(playerId, false)) {
            zombieStates.put(playerId, true);
            invisibleStates.put(playerId, false);
            broadcastAll("role," + playerId + ",1");
            checkZombieWin();
        }
    }

    synchronized void onAnimation(UUID playerId, String animationName) {
        if (!players.contains(playerId) || animationName == null || animationName.isBlank()) return;
        animationStates.put(playerId, animationName);
        broadcastExcept("anim," + playerId + "," + animationName, playerId);
    }

    synchronized boolean onAbility(UUID playerId, String ability, boolean active) {
        if (!players.contains(playerId) || ability == null) return false;
        if ("invis".equals(ability)) {
            if (!zombieStates.getOrDefault(playerId, false)) return false;
            invisibleStates.put(playerId, active);
            return true;
        }
        return false;
    }

    synchronized void onBuild(UUID playerId, String[] buildData) {
        if (!players.contains(playerId) || buildData == null || buildData.length < 6) return;
        buildStates.put(buildKey(buildData), buildMessage(playerId, buildData, "build"));
    }

    synchronized void onRemoveBuild(UUID playerId, String[] buildData) {
        if (!players.contains(playerId) || buildData == null || buildData.length < 6) return;
        buildStates.remove(buildKey(buildData));
    }

    synchronized void onPickupCollect(UUID playerId, int pickupId) {
        if (!players.contains(playerId) || pickupId < 0 || pickupId >= pickups.length) return;
        PickupState pickup = pickups[pickupId];
        if (!pickup.active) return;
        if (!roleCanCollectPickup(playerId, pickup.type)) return;

        sendTo(playerId, "pickupgrant," + pickup.id + "," + pickup.type);
        pickup.active = false;
        pickup.respawnEpochMs = System.currentTimeMillis() + PICKUP_RESPAWN_MS;
        broadcastAll("pickuphide," + pickup.id + "," + pickup.respawnEpochMs);
        scheduler.schedule(() -> respawnPickup(pickupId), PICKUP_RESPAWN_MS, TimeUnit.MILLISECONDS);
    }

    synchronized void onProjectile(UUID playerId, int projectileType, String message) {
        if (!players.contains(playerId) || message == null) return;
        boolean zombie = zombieStates.getOrDefault(playerId, false);
        if (projectileType == 0 && zombie) return;
        if (projectileType == 1 && !zombie) return;
        broadcastExcept(message, playerId);
    }

    synchronized void onSlow(UUID sourceId, UUID targetId, float seconds) {
        if (!players.contains(sourceId) || !players.contains(targetId)) return;
        boolean sourceZombie = zombieStates.getOrDefault(sourceId, false);
        boolean targetZombie = zombieStates.getOrDefault(targetId, false);
        if (sourceZombie == targetZombie) return;
        float clampedSeconds = Math.max(0.25f, Math.min(5.0f, seconds));
        broadcastExcept("slow," + sourceId + "," + targetId + "," + clampedSeconds, sourceId);
    }

    private boolean roleCanCollectPickup(UUID playerId, int pickupType) {
        boolean zombie = zombieStates.getOrDefault(playerId, false);
        if (pickupType == GameConstants.PICKUP_INVIS || pickupType == GameConstants.PICKUP_BABY_ZOMBIE) {
            return zombie;
        }
        if (pickupType == GameConstants.PICKUP_DASH || pickupType == GameConstants.PICKUP_BUILD
                || pickupType == GameConstants.PICKUP_ROCK || pickupType == GameConstants.PICKUP_FLASHLIGHT
                || pickupType == GameConstants.PICKUP_POTION) {
            return !zombie;
        }
        return false;
    }

    private synchronized void respawnPickup(int pickupId) {
        if (pickupId < 0 || pickupId >= pickups.length) return;
        PickupState pickup = pickups[pickupId];
        if (pickup.active) return;
        spawnPickupAtRandomLocation(pickup);
        pickup.active = true;
        pickup.respawnEpochMs = 0L;
        broadcastAll("pickupspawn," + pickup.id + "," + pickup.type + "," + pickup.x + "," + pickup.z);
    }

    private void sendSnapshotTo(UUID playerId) {
        for (PickupState pickup : pickups) {
            sendTo(playerId, pickupStateMessage(pickup));
        }
        sendRoundStateTo(playerId);
        for (UUID id : players) {
            sendTo(playerId, "role," + id + "," + (zombieStates.getOrDefault(id, false) ? 1 : 0));
            sendTo(playerId, "health," + id + "," + healthStates.getOrDefault(id, 100));
            sendTo(playerId, "anim," + id + "," + animationStates.getOrDefault(id, "IDLE"));
            sendTo(playerId, "ability," + id + ",invis," + (invisibleStates.getOrDefault(id, false) ? 1 : 0));
        }
        for (String buildMessage : buildStates.values()) {
            sendTo(playerId, buildMessage);
        }
    }

    private void sendRoundStateTo(UUID playerId) {
        if (roundActive) {
            sendTo(playerId, "round,start," + startingZombieId + "," + roundEndEpochMs);
        } else if (countdownActive) {
            sendTo(playerId, "round,countdown," + countdownEndEpochMs);
        } else if (matchEnded) {
            sendTo(playerId, "round,end," + getWinnerToken());
        } else {
            sendTo(playerId, "round,waiting");
        }
    }

    private String pickupStateMessage(PickupState pickup) {
        return "pickupstate," + pickup.id
                + "," + pickup.type
                + "," + (pickup.active ? 1 : 0)
                + "," + pickup.x
                + "," + pickup.z
                + "," + pickup.respawnEpochMs;
    }

    private void startCountdownIfReady() {
        if (players.size() < 2 || countdownActive || roundActive || matchEnded) return;
        countdownActive = true;
        countdownEndEpochMs = System.currentTimeMillis() + INTERMISSION_MS;
        broadcastAll("round,countdown," + countdownEndEpochMs);
        roundStartTask = scheduler.schedule(this::startRoundFromScheduler, INTERMISSION_MS, TimeUnit.MILLISECONDS);
    }

    private void startRoundFromScheduler() {
        synchronized (this) {
            startRound();
        }
    }

    private void startRound() {
        if (players.size() < 2) {
            countdownActive = false;
            broadcastAll("round,waiting");
            return;
        }

        countdownActive = false;
        roundActive = true;
        matchEnded = false;
        roundEndEpochMs = System.currentTimeMillis() + ROUND_MS;
        startingZombieId = chooseStartingZombie();

        for (UUID id : players) {
            boolean zombie = id.equals(startingZombieId);
            zombieStates.put(id, zombie);
            healthStates.put(id, zombie ? 0 : 100);
            invisibleStates.put(id, false);
        }

        broadcastAll("round,start," + startingZombieId + "," + roundEndEpochMs);
        for (UUID id : players) {
            broadcastAll("role," + id + "," + (zombieStates.getOrDefault(id, false) ? 1 : 0));
            broadcastAll("health," + id + "," + healthStates.getOrDefault(id, 100));
        }
        roundEndTask = scheduler.schedule(this::finishHumansFromScheduler, ROUND_MS, TimeUnit.MILLISECONDS);
    }

    private void finishHumansFromScheduler() {
        synchronized (this) {
            if (!roundActive) return;
            finishRound(true);
        }
    }

    private UUID chooseStartingZombie() {
        ArrayList<UUID> ids = new ArrayList<>(players);
        return ids.get(random.nextInt(ids.size()));
    }

    private void checkZombieWin() {
        if (!roundActive || players.size() < 2) return;
        for (UUID id : players) {
            if (!zombieStates.getOrDefault(id, false)) return;
        }
        finishRound(false);
    }

    private void finishRound(boolean humansWon) {
        if (roundEndTask != null) roundEndTask.cancel(false);
        roundActive = false;
        countdownActive = false;
        matchEnded = true;
        broadcastAll("round,end," + (humansWon ? "humans" : "zombies"));
        postRoundTask = scheduler.schedule(this::restartAfterRoundFromScheduler, POST_ROUND_MS, TimeUnit.MILLISECONDS);
    }

    private void restartAfterRoundFromScheduler() {
        synchronized (this) {
            resetRoundState();
            resetPlayersToHumans();
            if (players.size() < 2) {
                broadcastAll("round,waiting");
                return;
            }
            startCountdownIfReady();
        }
    }

    private void resetPlayersToHumans() {
        for (UUID id : players) {
            zombieStates.put(id, false);
            healthStates.put(id, 100);
            invisibleStates.put(id, false);
            broadcastAll("role," + id + ",0");
            broadcastAll("health," + id + ",100");
            broadcastAll("ability," + id + ",invis,0");
        }
    }

    private String getWinnerToken() {
        for (UUID id : players) {
            if (!zombieStates.getOrDefault(id, false)) return "humans";
        }
        return "zombies";
    }

    private String buildKey(String[] buildData) {
        return buildData[0] + ":" + buildData[1] + ":" + buildData[2]
                + ":" + buildData[3] + ":" + buildData[4] + ":" + buildData[5];
    }

    private String buildMessage(UUID playerId, String[] buildData, String command) {
        return command + "," + playerId
                + "," + buildData[0]
                + "," + buildData[1]
                + "," + buildData[2]
                + "," + buildData[3]
                + "," + buildData[4]
                + "," + buildData[5];
    }

    private void resetRoundState() {
        if (roundStartTask != null) roundStartTask.cancel(false);
        if (roundEndTask != null) roundEndTask.cancel(false);
        if (postRoundTask != null) postRoundTask.cancel(false);
        countdownActive = false;
        roundActive = false;
        matchEnded = false;
        countdownEndEpochMs = 0L;
        roundEndEpochMs = 0L;
        startingZombieId = null;
    }

    private void spawnPickupAtRandomLocation(PickupState pickup) {
        for (int i = 0; i < 80; i++) {
            float x = randomRange(-PICKUP_SPAWN_RADIUS, PICKUP_SPAWN_RADIUS);
            float z = randomRange(-PICKUP_SPAWN_RADIUS, PICKUP_SPAWN_RADIUS);
            if (!isPickupSpawnOpen(pickup, x, z, PICKUP_MIN_ITEM_DISTANCE)) continue;
            pickup.x = x;
            pickup.z = z;
            return;
        }
        pickup.x = randomRange(-PICKUP_SPAWN_RADIUS, PICKUP_SPAWN_RADIUS);
        pickup.z = randomRange(-PICKUP_SPAWN_RADIUS, PICKUP_SPAWN_RADIUS);
    }

    private boolean isPickupSpawnOpen(PickupState pickup, float x, float z, float minDistance) {
        if ((x * x + z * z) < PICKUP_MIN_CENTER_DISTANCE * PICKUP_MIN_CENTER_DISTANCE) return false;
        for (PickupState other : pickups) {
            if (other == null || other == pickup) continue;
            float dx = x - other.x;
            float dz = z - other.z;
            if ((dx * dx) + (dz * dz) < minDistance * minDistance) return false;
        }
        return true;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private void broadcastAll(String message) {
        for (UUID id : players) sendTo(id, message);
    }

    private void broadcastExcept(String message, UUID exceptId) {
        for (UUID id : players) {
            if (id.equals(exceptId)) continue;
            sendTo(id, message);
        }
    }

    private void sendTo(UUID targetId, String message) {
        messenger.sendTo(targetId, message);
    }

}
