package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MyGameItemSystem {
    private final MyGame game;

    public MyGameItemSystem(MyGame game) {
        this.game = game;
    }

    public void updateWorldState() {
        updateCarriedFlashlightPose();
        updateCarriedPotionPose();
        updateFlashlightHeight();
        updateHealthPotionHeight();
        checkItemPickups();
        updateFlashlightSpotlight();
    }

    public void healPlayer(int amount) {
        game.state.health += amount;
        if (game.state.health > game.state.maxHealth) game.state.health = game.state.maxHealth;
    }

    public void damagePlayer(int amount) {
        game.state.health -= amount;
        if (game.state.health < 0) game.state.health = 0;
    }

    public void equipFlashlight() {
        if (!game.state.hasFlashlight) { game.hudSystem.showEvent("NO FLASHLIGHT", 0.8); return; }
        game.state.equippedItem = GameConstants.ITEM_FLASHLIGHT;
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("FLASHLIGHT EQUIPPED", 0.8);
    }

    public void equipPotion() {
        if (!game.state.hasPotion || game.state.potionUsed) { game.hudSystem.showEvent("NO POTION", 0.8); return; }
        game.state.equippedItem = GameConstants.ITEM_POTION;
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("POTION EQUIPPED", 0.8);
    }

    public void unequipItem() {
        if (game.state.equippedItem == GameConstants.ITEM_NONE) { game.hudSystem.showEvent("NO ITEM EQUIPPED", 0.8); return; }
        game.state.equippedItem = GameConstants.ITEM_NONE;
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("ITEM UNEQUIPPED", 0.8);
    }

    public void usePotion() {
        if (!game.state.hasPotion) { game.hudSystem.showEvent("NO POTION", 0.8); return; }
        if (game.state.health >= game.state.maxHealth) { game.hudSystem.showEvent("HEALTH FULL", 0.8); return; }
        healPlayer(game.state.potionHealAmount);
        game.state.hasPotion = false;
        game.state.potionUsed = true;
        if (game.state.equippedItem == GameConstants.ITEM_POTION) {
            game.state.equippedItem = game.state.hasFlashlight ? GameConstants.ITEM_FLASHLIGHT : GameConstants.ITEM_NONE;
        }
        game.assets.healthPotion.getRenderStates().disableRendering();
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("HEALTH RESTORED +" + game.state.potionHealAmount, 1.2);
    }

    private void updateHealthPotionHeight() {
        if (game.assets.healthPotion == null || game.assets.terrain == null) return;
        if (game.state.healthPotionSnappedToTerrain || game.state.hasPotion || game.state.potionUsed) return;
        Vector3f p = game.assets.healthPotion.getWorldLocation();
        float groundY = game.assets.terrain.getHeight(p.x, p.z);
        game.assets.healthPotion.setLocalTranslation((new Matrix4f()).translation(p.x, groundY + game.state.potionLift, p.z));
        game.state.healthPotionSnappedToTerrain = true;
    }

    private void updateFlashlightHeight() {
        if (game.assets.flashlight == null || game.assets.terrain == null) return;
        if (game.state.flashlightSnappedToTerrain || game.state.hasFlashlight) return;
        Vector3f p = game.assets.flashlight.getWorldLocation();
        float groundY = game.assets.terrain.getHeight(p.x, p.z);
        game.assets.flashlight.setLocalTranslation((new Matrix4f()).translation(p.x, groundY + game.state.flashlightLift, p.z));
        game.state.flashlightSnappedToTerrain = true;
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
        if (game.assets.healthPotion == null || !game.state.hasPotion || game.state.potionUsed) return;
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

    private void updateFlashlightSpotlight() {
        if (game.assets.flashlightSpotlight == null) return;
        if (!game.state.hasFlashlight || game.state.equippedItem != GameConstants.ITEM_FLASHLIGHT) {
            game.assets.flashlightSpotlight.disable();
            return;
        }
        Vector3f forward = new Vector3f(game.photoSystem.avatarForward());
        forward.y = 0f;
        if (forward.lengthSquared() < 0.0001f) forward.set(0f, 0f, -1f);
        forward.normalize();
        Vector3f lightPos = new Vector3f(game.assets.flashlight.getWorldLocation())
                .add(new Vector3f(forward).mul(game.state.flashlightBeamForwardOffset))
                .add(0f, game.state.flashlightBeamLift, 0f);
        game.assets.flashlightSpotlight.setLocation(lightPos);
        game.assets.flashlightSpotlight.setDirection(forward);
        game.assets.flashlightSpotlight.enable();
    }

    private void updateEquippedItemVisibility() {
        if (game.assets.flashlight != null && game.state.hasFlashlight) {
            if (game.state.equippedItem == GameConstants.ITEM_FLASHLIGHT) game.assets.flashlight.getRenderStates().enableRendering();
            else game.assets.flashlight.getRenderStates().disableRendering();
        }
        if (game.assets.healthPotion != null) {
            if (game.state.hasPotion && !game.state.potionUsed) {
                if (game.state.equippedItem == GameConstants.ITEM_POTION) game.assets.healthPotion.getRenderStates().enableRendering();
                else game.assets.healthPotion.getRenderStates().disableRendering();
            } else if (game.state.potionUsed) {
                game.assets.healthPotion.getRenderStates().disableRendering();
            }
        }
        updateFlashlightSpotlight();
    }

    private void tryPickupFlashlight() {
        if (game.assets.flashlight == null || game.state.hasFlashlight) return;
        float dist = game.assets.avatar.getWorldLocation().distance(game.assets.flashlight.getWorldLocation());
        if (dist > game.state.itemPickupRange) return;
        game.state.hasFlashlight = true;
        updateCarriedFlashlightPose();
        if (game.state.equippedItem == GameConstants.ITEM_NONE) game.state.equippedItem = GameConstants.ITEM_FLASHLIGHT;
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("FLASHLIGHT PICKED UP", 1.0);
    }

    private void tryPickupPotion() {
        if (game.assets.healthPotion == null || game.state.hasPotion || game.state.potionUsed) return;
        float dist = game.assets.avatar.getWorldLocation().distance(game.assets.healthPotion.getWorldLocation());
        if (dist > game.state.itemPickupRange) return;
        game.state.hasPotion = true;
        updateCarriedPotionPose();
        game.assets.healthPotion.getRenderStates().isTransparent(false);
        game.assets.healthPotion.getRenderStates().setOpacity(1.0f);
        if (game.state.equippedItem == GameConstants.ITEM_NONE) game.state.equippedItem = GameConstants.ITEM_POTION;
        updateEquippedItemVisibility();
        game.hudSystem.showEvent("POTION PICKED UP  PRESS H TO HEAL", 1.2);
    }

    private void checkItemPickups() {
        tryPickupFlashlight();
        tryPickupPotion();
    }

    private boolean isPlayerModel2() { return game.isPlayerModel2Selected(); }
    private float getFlashlightCarryScale() { return isPlayerModel2() ? 0.14f : 0.18f; }
    private float getFlashlightCarrySideOffset() { return isPlayerModel2() ? -1.18f : -0.95f; }
    private float getFlashlightCarryForwardOffset() { return isPlayerModel2() ? 0.08f : 0.10f; }
    private float getFlashlightCarryHeight() { return isPlayerModel2() ? 1.14f : 1.34f; }
    private float getPotionCarryScale() { return isPlayerModel2() ? 0.12f : 0.10f; }
    private float getPotionCarrySideOffset() { return isPlayerModel2() ? -1.14f : -0.92f; }
    private float getPotionCarryForwardOffset() { return isPlayerModel2() ? 0.10f : 0.12f; }
    private float getPotionCarryHeight() { return isPlayerModel2() ? 1.12f : 1.32f; }
    private Vector3f getFlashlightCarryLocalOffset() { return new Vector3f(getFlashlightCarrySideOffset(), getFlashlightCarryHeight(), getFlashlightCarryForwardOffset()); }
    private Vector3f getPotionCarryLocalOffset() { return new Vector3f(getPotionCarrySideOffset(), getPotionCarryHeight(), getPotionCarryForwardOffset()); }
}
