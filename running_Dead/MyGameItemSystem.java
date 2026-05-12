package running_Dead;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import running_Dead.networking.GhostAvatar;
import tage.Camera;
import tage.GameObject;

/**
 * Manages held items, pickups, flashlight battery/blinding, and zombie baby projectiles.
 * It also updates network-visible held-item props so other clients can see what a player is carrying.
 * Connected to: Owned by MyGame; called by equip/use actions, updater, networking, GhostManager, and physics.
 */
public class MyGameItemSystem {
    private final MyGame game;
    private float collectibleBobTime = 0.0f;
    private boolean sceneryPlaced = false;
    private boolean tableCoversPlaced = false;
    private boolean staticCoverPlaced = false;
    private final float collectibleBobAmplitude = 0.28f;
    private final float collectibleBobSpeed = 3.2f;
    private final float collectibleGroundClearance = 0.04f;
    private final float treeTerrainSink = 0.0f;

    public MyGameItemSystem(MyGame game) {
        this.game = game;
    }

    public void updateWorldState() {
        collectibleBobTime += (float) game.state.elapsedTime;
        updateCarriedFlashlightPose();
        updateCarriedPotionPose();
        updateCarriedRockPose();
        updateCarriedBabyZombiePose();
        updateFlashlightBattery((float) game.state.elapsedTime);
        updateSceneryHeights();
        updateFlashlightHeight();
        updateHealthPotionHeight();
        checkItemPickups();
        updateFlashlightSpotlight();
        updateFlashlightBlind((float) game.state.elapsedTime);
    }

    public void requestEnvironmentReplant() {
        sceneryPlaced = false;
        tableCoversPlaced = false;
        staticCoverPlaced = false;
    }

    public void replantEnvironmentForCurrentTerrain() {
        updateSceneryHeights();
    }

    public void applyRemoteBlind(UUID sourceId, UUID targetId, float seconds) {
        if (targetId == null || !targetId.equals(game.getLocalPlayerId())) return;
        if (!game.state.localPlayerZombie) return;
        if (game.state.blindCooldownTimer > 0.0) return;
        game.state.blindTimer = Math.max(game.state.blindTimer, Math.max(0.1f, seconds));
        game.state.blindCooldownTimer = GameConstants.FLASHLIGHT_BLIND_COOLDOWN_SECONDS;
        game.hudSystem.showEvent("FLASH BLIND", 1.0);
    }

    public void healPlayer(int amount) {
        if (game.state.localPlayerZombie) return;
        game.state.health += amount;
        if (game.state.health > game.state.maxHealth) game.state.health = game.state.maxHealth;
        sendHealth();
    }

    public void damagePlayer(int amount) {
        if (game.state.localPlayerZombie || game.state.health <= 0) return;
        game.state.health -= amount;
        if (game.state.health < 0) game.state.health = 0;
        if (game.state.health <= 0) {
            game.physicsSystem.convertLocalPlayerToZombieFromHealth();
            return;
        }
        sendHealth();
    }

    public void equipFlashlight() {
        if (!game.state.hasFlashlight) { game.hudSystem.showEvent("NO FLASHLIGHT", 0.8); return; }
        if (game.state.equippedItem == GameConstants.ITEM_FLASHLIGHT) {
            unequipItem();
            return;
        }
        turnFlashlightOff(false);
        game.state.equippedItem = GameConstants.ITEM_FLASHLIGHT;
        updateEquippedItemVisibility();
        game.soundSystem.playItemEquip();
        game.hudSystem.showEvent("FLASHLIGHT EQUIPPED", 0.8);
    }

    public void equipPotion() {
        if (!game.state.hasPotion) { game.hudSystem.showEvent("NO POTION", 0.8); return; }
        if (game.state.equippedItem == GameConstants.ITEM_POTION) {
            unequipItem();
            return;
        }
        turnFlashlightOff(false);
        game.state.equippedItem = GameConstants.ITEM_POTION;
        updateEquippedItemVisibility();
        game.soundSystem.playItemEquip();
        game.hudSystem.showEvent("POTION EQUIPPED", 0.8);
    }

    public void equipRock() {
        if (game.state.localPlayerZombie) { game.hudSystem.showEvent("ZOMBIES CANNOT USE ROCKS", 0.8); return; }
        if (game.state.rockCharges <= 0) { game.hudSystem.showEvent("NO ROCK STORED", 0.8); return; }
        if (game.state.equippedItem == GameConstants.ITEM_ROCK) {
            unequipItem();
            return;
        }
        turnFlashlightOff(false);
        game.state.equippedItem = GameConstants.ITEM_ROCK;
        ensureCarriedRockObject();
        updateCarriedRockPose();
        updateEquippedItemVisibility();
        game.soundSystem.playItemEquip();
        game.hudSystem.showEvent("ROCK EQUIPPED", 0.8);
    }

    public void equipBabyZombie() {
        if (!game.state.localPlayerZombie) { game.hudSystem.showEvent("ONLY ZOMBIES CAN USE BABY ZOMBIES", 0.8); return; }
        if (game.state.babyZombieCharges <= 0) { game.hudSystem.showEvent("NO BABY ZOMBIE STORED", 0.8); return; }
        if (game.state.equippedItem == GameConstants.ITEM_BABY_ZOMBIE) {
            unequipItem();
            return;
        }
        turnFlashlightOff(false);
        game.state.equippedItem = GameConstants.ITEM_BABY_ZOMBIE;
        ensureCarriedBabyZombieObject();
        updateCarriedBabyZombiePose();
        updateEquippedItemVisibility();
        game.soundSystem.playItemEquip();
        game.hudSystem.showEvent("BABY ZOMBIE EQUIPPED", 0.8);
    }

    public void unequipItem() {
        if (game.state.equippedItem == GameConstants.ITEM_NONE) { game.hudSystem.showEvent("NO ITEM EQUIPPED", 0.8); return; }
        boolean flashlightWasEquipped = game.state.equippedItem == GameConstants.ITEM_FLASHLIGHT;
        game.state.equippedItem = GameConstants.ITEM_NONE;
        if (flashlightWasEquipped) turnFlashlightOff(true);
        updateEquippedItemVisibility();
        game.soundSystem.playItemEquip();
        game.hudSystem.showEvent("ITEM UNEQUIPPED", 0.8);
    }

    public void toggleEquippedFlashlight() {
        if (!game.state.hasFlashlight || game.state.equippedItem != GameConstants.ITEM_FLASHLIGHT) {
            game.hudSystem.showEvent("EQUIP FLASHLIGHT FIRST", 0.8);
            return;
        }
        if (!game.state.flashlightOn && game.state.flashlightBatterySeconds <= 0.05f) {
            game.hudSystem.showEvent("FLASHLIGHT BATTERY EMPTY", 0.9);
            return;
        }
        game.state.flashlightOn = !game.state.flashlightOn;
        updateFlashlightSpotlight();
        game.networking.sendPlayerTransform();
        game.soundSystem.playFlashlightClick();
        game.hudSystem.showEvent(game.state.flashlightOn ? "FLASHLIGHT ON" : "FLASHLIGHT OFF", 0.7);
    }

    public void usePotion() {
        if (!game.state.hasPotion) { game.hudSystem.showEvent("NO POTION", 0.8); return; }
        if (game.state.health >= game.state.maxHealth) { game.hudSystem.showEvent("HEALTH FULL", 0.8); return; }
        healPlayer(game.state.potionHealAmount);
        game.state.hasPotion = false;
        game.state.potionUsed = false;
        if (game.state.equippedItem == GameConstants.ITEM_POTION) {
            game.state.equippedItem = GameConstants.ITEM_NONE;
        }
        if (game.assets.healthPotion != null) {
            game.assets.healthPotion.getRenderStates().disableRendering();
            game.assets.healthPotion = null;
        }
        updateEquippedItemVisibility();
        game.soundSystem.playPotionHeal();
        game.hudSystem.showEvent("HEALTH RESTORED +" + game.state.potionHealAmount, 1.2);
    }

    public boolean canGrantFlashlightFromPickup() {
        return !game.state.localPlayerZombie && !game.state.hasFlashlight;
    }

    public boolean canGrantPotionFromPickup() {
        return !game.state.localPlayerZombie && !game.state.hasPotion;
    }

    public boolean grantFlashlightFromPickup() {
        if (!canGrantFlashlightFromPickup()) return false;
        GameObject flashlight = new GameObject(GameObject.root(), game.assets.flashlightS, game.assets.flashlightTx);
        flashlight.setLocalScale((new Matrix4f()).scaling(getFlashlightCarryScale()));
        styleFlashlightRender(flashlight);
        game.assets.flashlight = flashlight;
        game.state.hasFlashlight = true;
        game.state.flashlightOn = false;
        game.state.flashlightBatterySeconds = GameConstants.FLASHLIGHT_BATTERY_SECONDS;
        updateCarriedFlashlightPose();
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("FLASHLIGHT PICKED UP", 1.0);
        return true;
    }

    private void styleFlashlightRender(GameObject flashlight) {
        if (flashlight == null) return;
        flashlight.getRenderStates().hasLighting(true);
        flashlight.getRenderStates().isEnvironmentMapped(false);
        flashlight.getRenderStates().setHasSolidColor(true);
        flashlight.getRenderStates().setColor(new Vector3f(0.018f, 0.017f, 0.015f));
        flashlight.getRenderStates().setTextureDetailBlend(false);
        flashlight.getRenderStates().setNormalMapping(false);
        flashlight.getRenderStates().castsShadow(true);
    }

    public boolean grantPotionFromPickup() {
        if (!canGrantPotionFromPickup()) return false;
        GameObject potion = new GameObject(GameObject.root(), game.assets.healthPotionS, game.assets.healthPotionTx);
        potion.setLocalScale((new Matrix4f()).scaling(getPotionCarryScale()));
        game.physicsSystem.stylePotionGlass(potion, 0.42f);
        game.assets.healthPotion = potion;
        game.state.hasPotion = true;
        game.state.potionUsed = false;
        updateCarriedPotionPose();
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("POTION PICKED UP  PRESS C", 1.2);
        return true;
    }

    public void clearHumanCarryItems() {
        game.state.hasFlashlight = false;
        game.state.hasPotion = false;
        game.state.flashlightOn = false;
        game.state.flashlightBatterySeconds = 0f;
        if (game.state.equippedItem == GameConstants.ITEM_FLASHLIGHT
                || game.state.equippedItem == GameConstants.ITEM_POTION
                || game.state.equippedItem == GameConstants.ITEM_ROCK) {
            game.state.equippedItem = GameConstants.ITEM_NONE;
        }
        if (game.assets.flashlight != null) {
            game.assets.flashlight.getRenderStates().disableRendering();
            game.assets.flashlight = null;
        }
        if (game.assets.healthPotion != null) {
            game.assets.healthPotion.getRenderStates().disableRendering();
            game.assets.healthPotion = null;
        }
        if (game.assets.heldRock != null) {
            game.assets.heldRock.getRenderStates().disableRendering();
            game.assets.heldRock = null;
        }
        if (game.assets.flashlightSpotlight != null) game.assets.flashlightSpotlight.disable();
    }

    public void clearZombieCarryItems() {
        if (game.state.equippedItem == GameConstants.ITEM_BABY_ZOMBIE) {
            game.state.equippedItem = GameConstants.ITEM_NONE;
        }
        if (game.assets.heldBabyZombie != null) {
            game.assets.heldBabyZombie.getRenderStates().disableRendering();
            game.assets.heldBabyZombie = null;
        }
    }

    public void onThrowableChargesChanged() {
        if (game.state.equippedItem == GameConstants.ITEM_ROCK && game.state.rockCharges <= 0) {
            game.state.equippedItem = GameConstants.ITEM_NONE;
        }
        if (game.state.equippedItem == GameConstants.ITEM_BABY_ZOMBIE && game.state.babyZombieCharges <= 0) {
            game.state.equippedItem = GameConstants.ITEM_NONE;
        }
        updateEquippedItemVisibility();
    }

    private void updateHealthPotionHeight() {
        if (game.assets.terrain == null) return;
        for (int i = 0; i < game.assets.healthPotions.size(); i++) {
            tage.GameObject potion = game.assets.healthPotions.get(i);
            if (potion == null || !potion.getRenderStates().renderingEnabled()) continue;
            if (potion == game.assets.healthPotion && game.state.hasPotion) continue;
            Vector3f p = potion.getWorldLocation();
            float groundY = game.assets.terrain.getHeight(p.x, p.z);
            potion.setLocalTranslation((new Matrix4f()).translation(
                    p.x, groundY + collectibleBaseLift(potion) + collectibleBobOffset(i), p.z));
        }
    }

    private void updateFlashlightHeight() {
        if (game.assets.terrain == null) return;
        for (int i = 0; i < game.assets.flashlights.size(); i++) {
            tage.GameObject flashlight = game.assets.flashlights.get(i);
            if (flashlight == null || !flashlight.getRenderStates().renderingEnabled()) continue;
            if (flashlight == game.assets.flashlight && game.state.hasFlashlight) continue;
            Vector3f p = flashlight.getWorldLocation();
            float groundY = game.assets.terrain.getHeight(p.x, p.z);
            flashlight.setLocalTranslation((new Matrix4f()).translation(
                    p.x, groundY + collectibleBaseLift(flashlight) + collectibleBobOffset(i + 31), p.z));
        }
    }

    private float collectibleBobOffset(int index) {
        return (float) Math.sin(collectibleBobTime * collectibleBobSpeed + index * 1.37f) * collectibleBobAmplitude;
    }

    private float collectibleBaseLift(tage.GameObject object) {
        return meshBottomLift(object, collectibleGroundClearance + collectibleBobAmplitude);
    }

    private void updateSceneryHeights() {
        if (game.assets.terrain == null || !game.state.physicsReady) return;
        if (!sceneryPlaced) {
            for (tage.GameObject prop : game.assets.sceneryProps) {
                if (prop == null) continue;
                if (game.assets.sceneryTrees.contains(prop)) continue;
                Vector3f p = prop.getWorldLocation();
                prop.setLocalTranslation((new Matrix4f()).translation(p.x, terrainCenterPlantedY(prop, 0.03f), p.z));
            }
            sceneryPlaced = true;
        }
        if (!tableCoversPlaced) {
            for (tage.GameObject table : game.assets.tableCovers) {
                if (table == null) continue;
                Vector3f p = table.getWorldLocation();
                table.setLocalTranslation((new Matrix4f()).translation(p.x, terrainPlantedY(table, 0.02f), p.z));
            }
            tableCoversPlaced = true;
        }
        if (!staticCoverPlaced) {
            for (MyGameBuildMeta meta : game.assets.staticCoverBuilds) {
                if (meta == null) continue;
                tage.GameObject piece = game.state.wallMap.get(meta.key);
                if (piece == null) continue;
                float y = game.assets.terrain.getHeight(meta.position.x, meta.position.z) + meta.terrainOffsetY;
                meta.position.y = y;
                piece.setLocalTranslation((new Matrix4f()).translation(meta.position));
            }
            staticCoverPlaced = true;
        }
        game.physicsSystem.syncSceneryColliders();
    }

    private float meshBottomLift(tage.GameObject object, float clearance) {
        if (object == null || object.getShape() == null || object.getShape().getVertices() == null) return clearance;
        float[] vertices = object.getShape().getVertices();
        if (vertices.length < 3) return clearance;
        float minY = Float.MAX_VALUE;
        for (int i = 1; i < vertices.length; i += 3) {
            if (vertices[i] < minY) minY = vertices[i];
        }
        return -minY * object.getWorldScale().m00() + clearance;
    }

    private float terrainPlantedY(tage.GameObject object, float clearance) {
        if (object == null || object.getShape() == null || object.getShape().getVertices() == null) return clearance;
        float[] vertices = object.getShape().getVertices();
        if (vertices.length < 3) return clearance;

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 1; i < vertices.length; i += 3) {
            if (vertices[i] < minY) minY = vertices[i];
            if (vertices[i] > maxY) maxY = vertices[i];
        }

        float scale = object.getWorldScale().m00();
        float bottomBand = Math.max(0.45f / Math.max(scale, 0.001f), (maxY - minY) * 0.08f);
        float bottomLimit = minY + bottomBand;
        Matrix4f rot = object.getWorldRotation();
        Vector3f objectPos = object.getWorldLocation();
        float plantedY = -Float.MAX_VALUE;
        int samples = 0;

        for (int i = 0; i < vertices.length; i += 3) {
            float localY = vertices[i + 1];
            if (localY > bottomLimit) continue;
            Vector3f local = new Vector3f(vertices[i] * scale, localY * scale, vertices[i + 2] * scale);
            local.mulDirection(rot);
            float groundY = game.assets.terrain.getHeight(objectPos.x + local.x, objectPos.z + local.z);
            plantedY = Math.max(plantedY, groundY - local.y + clearance);
            samples++;
        }

        if (samples == 0 || plantedY == -Float.MAX_VALUE) {
            return game.assets.terrain.getHeight(objectPos.x, objectPos.z) + meshBottomLift(object, clearance);
        }
        return plantedY;
    }

    private float terrainCenterPlantedY(tage.GameObject object, float clearance) {
        if (object == null) return clearance;
        Vector3f p = object.getWorldLocation();
        return game.assets.terrain.getHeight(p.x, p.z) + meshBottomLift(object, clearance);
    }

    private void updateCarriedFlashlightPose() {
        if (game.assets.flashlight == null || !game.state.hasFlashlight) return;
        if (game.assets.flashlight.getParent() != game.assets.avatar) {
            game.assets.flashlight.setParent(game.assets.avatar);
            game.assets.flashlight.propagateRotation(true);
            game.assets.flashlight.applyParentRotationToPosition(true);
            game.assets.flashlight.propagateScale(false);
        }
        game.assets.flashlight.setLocalTranslation((new Matrix4f()).translation(getFlashlightCarryLocalOffset()));
        game.assets.flashlight.setLocalScale((new Matrix4f()).scaling(getFlashlightCarryScale()));
        game.assets.flashlight.setLocalRotation((new Matrix4f()).identity());
    }

    private void updateCarriedPotionPose() {
        if (game.assets.healthPotion == null || !game.state.hasPotion) return;
        if (game.assets.healthPotion.getParent() != game.assets.avatar) {
            game.assets.healthPotion.setParent(game.assets.avatar);
            game.assets.healthPotion.propagateRotation(true);
            game.assets.healthPotion.applyParentRotationToPosition(true);
            game.assets.healthPotion.propagateScale(false);
        }
        game.assets.healthPotion.setLocalTranslation((new Matrix4f()).translation(getPotionCarryLocalOffset()));
        game.assets.healthPotion.setLocalScale((new Matrix4f()).scaling(getPotionCarryScale()));
        game.assets.healthPotion.setLocalRotation((new Matrix4f()).identity());
    }

    private void updateCarriedRockPose() {
        if (game.assets.heldRock == null || game.state.rockCharges <= 0) return;
        if (game.assets.heldRock.getParent() != game.assets.avatar) {
            game.assets.heldRock.setParent(game.assets.avatar);
            game.assets.heldRock.propagateRotation(true);
            game.assets.heldRock.applyParentRotationToPosition(true);
            game.assets.heldRock.propagateScale(false);
        }
        game.assets.heldRock.setLocalTranslation((new Matrix4f()).translation(getThrowableCarryLocalOffset()));
        game.assets.heldRock.setLocalScale((new Matrix4f()).scaling(getRockCarryScale()));
        game.assets.heldRock.setLocalRotation((new Matrix4f()).rotationY((float) Math.toRadians(30.0f)));
    }

    private void updateCarriedBabyZombiePose() {
        if (game.assets.heldBabyZombie == null || game.state.babyZombieCharges <= 0) return;
        if (game.assets.heldBabyZombie.getParent() != game.assets.avatar) {
            game.assets.heldBabyZombie.setParent(game.assets.avatar);
            game.assets.heldBabyZombie.propagateRotation(true);
            game.assets.heldBabyZombie.applyParentRotationToPosition(true);
            game.assets.heldBabyZombie.propagateScale(false);
        }
        game.assets.heldBabyZombie.setLocalTranslation((new Matrix4f()).translation(getThrowableCarryLocalOffset()));
        game.assets.heldBabyZombie.setLocalScale((new Matrix4f()).scaling(getBabyZombieCarryScale()));
        game.assets.heldBabyZombie.setLocalRotation((new Matrix4f()).rotationY((float) Math.toRadians(180.0f)));
    }

    private void updateFlashlightSpotlight() {
        if (game.assets.flashlightSpotlight == null) return;
        if (!game.state.hasFlashlight || game.state.equippedItem != GameConstants.ITEM_FLASHLIGHT
                || !game.state.flashlightOn || game.state.flashlightBatterySeconds <= 0.0f) {
            game.assets.flashlightSpotlight.disable();
            return;
        }
        Vector3f forward = new Vector3f(game.avatarForward());
        forward.set(getFlashlightBeamDirection());
        Vector3f lightPos = new Vector3f(game.assets.flashlight.getWorldLocation())
                .add(new Vector3f(forward).mul(game.state.flashlightBeamForwardOffset))
                .add(0f, game.state.flashlightBeamLift, 0f);
        game.assets.flashlightSpotlight.setLocation(lightPos);
        game.assets.flashlightSpotlight.setDirection(forward);
        game.assets.flashlightSpotlight.enable();
    }

    private void updateFlashlightBattery(float dt) {
        if (!game.state.hasFlashlight) return;
        if (game.state.flashlightOn) {
            game.state.flashlightBatterySeconds -= dt;
            if (game.state.flashlightBatterySeconds <= 0.0f) {
                game.state.flashlightBatterySeconds = 0.0f;
                turnFlashlightOff(false);
                game.hudSystem.showEvent("FLASHLIGHT BATTERY EMPTY", 1.0);
            }
        } else if (game.state.flashlightBatterySeconds < GameConstants.FLASHLIGHT_BATTERY_SECONDS) {
            game.state.flashlightBatterySeconds += dt;
            if (game.state.flashlightBatterySeconds > GameConstants.FLASHLIGHT_BATTERY_SECONDS) {
                game.state.flashlightBatterySeconds = GameConstants.FLASHLIGHT_BATTERY_SECONDS;
            }
        }
    }

    private boolean consumeFlashlightBattery(float seconds) {
        if (!game.state.hasFlashlight) return false;
        game.state.flashlightBatterySeconds -= Math.max(0.0f, seconds);
        if (game.state.flashlightBatterySeconds <= 0.0f) {
            game.state.flashlightBatterySeconds = 0.0f;
            turnFlashlightOff(false);
            game.hudSystem.showEvent("FLASHLIGHT BATTERY EMPTY", 0.9);
        }
        return game.state.flashlightBatterySeconds > 0.0f;
    }

    private void updateFlashlightBlind(float dt) {
        updateFlashlightBlindCooldowns(dt);
        if (!isFlashlightBeamActive()) return;

        Vector3f origin = getFlashlightBeamOrigin();
        Vector3f forward = getFlashlightBeamDirection();

        if (game.state.gm != null) {
            for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
                if (game.state.flashlightBatterySeconds <= 0.0f) return;
                UUID id = ghost.getID();
                if (!game.state.remoteZombieStates.getOrDefault(id, false)) continue;
                if (game.state.flashlightBlindCooldowns.getOrDefault(id, 0.0) > 0.0) continue;
                Vector3f head = new Vector3f(ghost.getWorldLocation()).add(0f, 1.55f, 0f);
                if (!isPointInFlashlightBeam(origin, forward, head)) continue;
                game.state.flashlightBlindCooldowns.put(id, (double) GameConstants.FLASHLIGHT_BLIND_COOLDOWN_SECONDS);
                if (game.state.protClient != null && game.state.isClientConnected) {
                    game.state.protClient.sendBlindMessage(id, GameConstants.FLASHLIGHT_BLIND_SECONDS);
                }
                consumeFlashlightBattery(GameConstants.FLASHLIGHT_BLIND_BATTERY_COST);
                game.hudSystem.showEvent("ZOMBIE BLINDED", 1.0);
            }
        }

        if (game.state.flashlightBatterySeconds > 0.0f
                && game.smilingManSystem.tryBlindFromFlashlight(origin, forward, game.getLocalPlayerId())) {
            consumeFlashlightBattery(GameConstants.FLASHLIGHT_BLIND_BATTERY_COST);
        }
    }

    private void updateFlashlightBlindCooldowns(float dt) {
        if (game.state.flashlightBlindCooldowns.isEmpty()) return;
        ArrayList<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : game.state.flashlightBlindCooldowns.entrySet()) {
            double value = entry.getValue() - dt;
            if (value <= 0.0) done.add(entry.getKey());
            else entry.setValue(value);
        }
        for (UUID id : done) game.state.flashlightBlindCooldowns.remove(id);
    }

    private boolean isFlashlightBeamActive() {
        return game.state.hasFlashlight
                && game.state.equippedItem == GameConstants.ITEM_FLASHLIGHT
                && game.state.flashlightOn
                && game.state.flashlightBatterySeconds > 0.0f
                && game.assets.flashlight != null;
    }

    private Vector3f getFlashlightBeamOrigin() {
        return new Vector3f(game.assets.flashlight.getWorldLocation())
                .add(new Vector3f(getFlashlightBeamDirection()).mul(game.state.flashlightBeamForwardOffset))
                .add(0f, game.state.flashlightBeamLift, 0f);
    }

    private Vector3f getFlashlightBeamDirection() {
        if (!game.state.fullMapMode) {
            Camera cam = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
            if (cam != null) {
                Vector3f camForward = new Vector3f(cam.getN());
                if (camForward.lengthSquared() > 0.0001f) {
                    return camForward.normalize();
                }
            }
        }
        Vector3f avatarForward = new Vector3f(game.avatarForward());
        if (avatarForward.lengthSquared() < 0.0001f) avatarForward.set(0f, 0f, -1f);
        return avatarForward.normalize();
    }

    public Vector3f getFlashlightBeamDirectionForNetwork() {
        return getFlashlightBeamDirection();
    }

    private boolean isPointInFlashlightBeam(Vector3f origin, Vector3f forward, Vector3f point) {
        Vector3f toPoint = new Vector3f(point).sub(origin);
        float distance = toPoint.length();
        if (distance > GameConstants.FLASHLIGHT_BLIND_RANGE || distance < 0.0001f) return false;
        toPoint.div(distance);
        return forward.dot(toPoint) >= GameConstants.FLASHLIGHT_BLIND_DOT;
    }

    public void updateEquippedItemVisibility() {
        boolean localInvisible = game.state.localPlayerZombie && game.state.invisTimer > 0.0;
        if (game.assets.flashlight != null && game.state.hasFlashlight) {
            if (!localInvisible && game.state.equippedItem == GameConstants.ITEM_FLASHLIGHT) game.assets.flashlight.getRenderStates().enableRendering();
            else game.assets.flashlight.getRenderStates().disableRendering();
        }
        if (game.assets.healthPotion != null) {
            if (game.state.hasPotion) {
                if (!localInvisible && game.state.equippedItem == GameConstants.ITEM_POTION) game.assets.healthPotion.getRenderStates().enableRendering();
                else game.assets.healthPotion.getRenderStates().disableRendering();
            } else {
                game.assets.healthPotion.getRenderStates().disableRendering();
            }
        }
        if (game.assets.heldRock != null) {
            if (!localInvisible && game.state.rockCharges > 0 && game.state.equippedItem == GameConstants.ITEM_ROCK) game.assets.heldRock.getRenderStates().enableRendering();
            else game.assets.heldRock.getRenderStates().disableRendering();
        }
        if (game.assets.heldBabyZombie != null) {
            if (!localInvisible && game.state.babyZombieCharges > 0 && game.state.equippedItem == GameConstants.ITEM_BABY_ZOMBIE) game.assets.heldBabyZombie.getRenderStates().enableRendering();
            else game.assets.heldBabyZombie.getRenderStates().disableRendering();
        }
        updateFlashlightSpotlight();
        game.networking.sendPlayerTransform();
    }

    private void tryPickupFlashlight() {
        if (game.state.hasFlashlight) return;
        for (tage.GameObject flashlight : game.assets.flashlights) {
            if (flashlight == null || !flashlight.getRenderStates().renderingEnabled()) continue;
            float dist = game.assets.avatar.getWorldLocation().distance(flashlight.getWorldLocation());
            if (dist > game.state.itemPickupRange) continue;
            game.assets.flashlight = flashlight;
            game.state.hasFlashlight = true;
            updateCarriedFlashlightPose();
            game.state.flashlightOn = false;
            updateEquippedItemVisibility();
            game.soundSystem.playPickupCollect();
            game.hudSystem.showEvent("FLASHLIGHT PICKED UP", 1.0);
            return;
        }
    }

    private void tryPickupPotion() {
        if (game.state.hasPotion) return;
        for (tage.GameObject potion : game.assets.healthPotions) {
            if (potion == null || !potion.getRenderStates().renderingEnabled()) continue;
            float dist = game.assets.avatar.getWorldLocation().distance(potion.getWorldLocation());
            if (dist > game.state.itemPickupRange) continue;
            game.assets.healthPotion = potion;
            game.state.hasPotion = true;
            game.state.potionUsed = false;
            game.physicsSystem.stylePotionGlass(potion, 0.42f);
            updateCarriedPotionPose();
            updateEquippedItemVisibility();
            game.soundSystem.playPickupCollect();
            game.hudSystem.showEvent("POTION PICKED UP  PRESS C", 1.2);
            return;
        }
    }

    private void checkItemPickups() {
        tryPickupFlashlight();
        tryPickupPotion();
    }

    private void ensureCarriedRockObject() {
        if (game.assets.heldRock != null) return;
        GameObject rock = new GameObject(GameObject.root(), game.assets.rock2S, game.assets.rockTx);
        rock.setLocalScale((new Matrix4f()).scaling(getRockCarryScale()));
        rock.getRenderStates().hasLighting(true);
        rock.getRenderStates().setBumpMapping(true);
        rock.setNormalMap(game.assets.rockNormalTx);
        rock.getRenderStates().setNormalMapping(true);
        rock.getRenderStates().setTextureMappingMode(1);
        rock.getRenderStates().setTextureDetailBlend(true);
        rock.getRenderStates().setSurfaceScale(0.9f);
        rock.setDetailTextureImage(game.assets.rockFarTx);
        game.assets.heldRock = rock;
    }

    private void ensureCarriedBabyZombieObject() {
        if (game.assets.heldBabyZombie != null) return;
        GameObject baby = new GameObject(GameObject.root(), game.assets.babyZombieS, game.assets.babyZombieTx);
        baby.setLocalScale((new Matrix4f()).scaling(getBabyZombieCarryScale()));
        baby.getRenderStates().hasLighting(true);
        baby.setDetailTextureImage(game.assets.babyZombieFarTx);
        baby.getRenderStates().setTextureDetailBlend(true);
        game.assets.heldBabyZombie = baby;
    }

    private void turnFlashlightOff(boolean playClick) {
        if (!game.state.flashlightOn) return;
        game.state.flashlightOn = false;
        if (game.assets.flashlightSpotlight != null) game.assets.flashlightSpotlight.disable();
        if (playClick) game.soundSystem.playFlashlightClick();
    }

    private boolean isPlayerModel2() { return game.isPlayerModel2Selected(); }
    private void sendHealth() {
        if (game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendHealthMessage(game.state.health);
        }
    }
    private float getFlashlightCarryScale() { return isPlayerModel2() ? 0.18f : 0.23f; }
    private float getFlashlightCarrySideOffset() { return isPlayerModel2() ? -1.18f : -0.95f; }
    private float getFlashlightCarryForwardOffset() { return isPlayerModel2() ? 0.08f : 0.10f; }
    private float getFlashlightCarryHeight() { return isPlayerModel2() ? 1.14f : 1.34f; }
    private float getPotionCarryScale() { return isPlayerModel2() ? 0.12f : 0.10f; }
    private float getPotionCarrySideOffset() { return isPlayerModel2() ? -1.14f : -0.92f; }
    private float getPotionCarryForwardOffset() { return isPlayerModel2() ? 0.10f : 0.12f; }
    private float getPotionCarryHeight() { return isPlayerModel2() ? 1.12f : 1.32f; }
    private float getThrowableCarryScale() { return isPlayerModel2() ? 0.16f : 0.18f; }
    private float getRockCarryScale() { return isPlayerModel2() ? 0.18f : 0.20f; }
    private float getBabyZombieCarryScale() { return isPlayerModel2() ? 0.13f : 0.15f; }
    private float getThrowableCarrySideOffset() { return isPlayerModel2() ? -1.12f : -0.92f; }
    private float getThrowableCarryForwardOffset() { return isPlayerModel2() ? 0.12f : 0.14f; }
    private float getThrowableCarryHeight() { return isPlayerModel2() ? 1.12f : 1.32f; }
    private Vector3f getFlashlightCarryLocalOffset() { return new Vector3f(getFlashlightCarrySideOffset(), getFlashlightCarryHeight(), getFlashlightCarryForwardOffset()); }
    private Vector3f getPotionCarryLocalOffset() { return new Vector3f(getPotionCarrySideOffset(), getPotionCarryHeight(), getPotionCarryForwardOffset()); }
    private Vector3f getThrowableCarryLocalOffset() { return new Vector3f(getThrowableCarrySideOffset(), getThrowableCarryHeight(), getThrowableCarryForwardOffset()); }
}
