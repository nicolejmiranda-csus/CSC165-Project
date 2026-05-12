package running_Dead;

import java.util.UUID;

import running_Dead.networking.GhostAvatar;
import tage.RenderStates;

/**
 * Handles render-only visuals such as axes, cloud motion, and helper objects.
 * These effects are kept away from gameplay systems so they do not affect collision or networking.
 * Connected to: Owned by MyGame; called by initializer/updater for axes, clouds, and render-only helpers.
 */
public class MyGameVisualSystem {
    private final MyGame game;

    public MyGameVisualSystem(MyGame game) {
        this.game = game;
    }

    public void toggleAxesVisibility() {
        game.state.axesVisible = !game.state.axesVisible;
        if (game.state.axesVisible) {
            game.assets.axisX.getRenderStates().enableRendering();
            game.assets.axisY.getRenderStates().enableRendering();
            game.assets.axisZ.getRenderStates().enableRendering();
            game.hudSystem.showEvent("AXES ON", 0.8);
        } else {
            game.assets.axisX.getRenderStates().disableRendering();
            game.assets.axisY.getRenderStates().disableRendering();
            game.assets.axisZ.getRenderStates().disableRendering();
            game.hudSystem.showEvent("AXES OFF", 0.8);
        }
    }

    public void useSky04() {
        game.state.dayNightEnabled = false;
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(game.assets.sky04);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        game.hudSystem.showEvent("SKYBOX sky04", 1.0);
    }

    public void useSky15Night() {
        game.state.dayNightEnabled = false;
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(game.assets.sky15Night);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        game.hudSystem.showEvent("SKYBOX sky15_night", 1.0);
    }

    public void toggleSkyboxOff() {
        game.state.dayNightEnabled = false;
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(false);
        game.hudSystem.showEvent("SKYBOX OFF", 1.0);
    }

    public void updateHidingVisuals() {
        updateLocalHidingVisual();
        updateRemoteHidingVisuals();
    }

    private void updateLocalHidingVisual() {
        if (game.assets.avatar == null) return;
        RenderStates rs = game.assets.avatar.getRenderStates();
        if (game.state.localPlayerZombie && game.state.invisTimer > 0.0) {
            rs.disableRendering();
            return;
        }
        rs.enableRendering();
        boolean hidden = !game.state.localPlayerZombie && game.worldBuilder.isInsideTableCover(game.assets.avatar.getWorldLocation());
        rs.isTransparent(hidden);
        rs.setOpacity(hidden ? 0.34f : 1.0f);
    }

    private void updateRemoteHidingVisuals() {
        if (game.state.gm == null) return;
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            UUID id = ghost.getID();
            boolean zombie = game.state.remoteZombieStates.getOrDefault(id, false);
            boolean invisible = zombie && game.state.remoteInvisibleStates.getOrDefault(id, false);
            RenderStates rs = ghost.getRenderStates();
            if (invisible) {
                rs.disableRendering();
                continue;
            }
            rs.enableRendering();
            boolean hidden = !zombie && game.worldBuilder.isInsideTableCover(ghost.getWorldLocation());
            rs.isTransparent(hidden);
            rs.setOpacity(hidden ? 0.34f : 1.0f);
        }
    }

    public void randomizeRoundSkybox(java.util.UUID roundZombieId, long roundToken) {
        if (game.state.dayNightEnabled) {
            game.lighting.randomizeRoundTimeSlot(roundZombieId, roundToken);
            return;
        }
        long seed = roundToken == 0L ? System.currentTimeMillis() : roundToken;
        if (roundZombieId != null) {
            seed ^= roundZombieId.getMostSignificantBits();
            seed = Long.rotateLeft(seed, 17) ^ roundZombieId.getLeastSignificantBits();
        }
        int choice = (int) Math.floorMod(seed, 3);
        if (choice == 0) {
            MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(game.assets.sky04);
            MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        } else if (choice == 1) {
            MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(game.assets.sky15Night);
            MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        } else {
            MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(false);
        }
    }
}
