package a3;

public class MyGameUpdater {
    private final MyGame game;

    public MyGameUpdater(MyGame game) {
        this.game = game;
    }

    public void update() {
        game.mouseLookSystem.ensureMouseModeInitialized();
        game.state.lastFrameTime = game.state.currFrameTime;
        game.state.currFrameTime = System.currentTimeMillis();
        game.state.elapsedTime = (game.state.currFrameTime - game.state.lastFrameTime) / 1000.0;
        game.hudSystem.updateTransientTimers();
        game.physicsSystem.updateTimers((float) game.state.elapsedTime);
        game.movementSystem.processPlayerInput();
        game.movementSystem.syncPlayerToTerrain();
        game.physicsSystem.update((float) game.state.elapsedTime);
        game.animationSystem.updateLocalAvatarAnimation();
        if (game.state.gm != null) game.state.gm.updateGhostAnimations();
        game.cameraSystem.updateCameraLimits();
        game.cameraSystem.updateMainCamera();
        game.soundSystem.update((float) game.state.elapsedTime);
        game.buildSystem.updateBuildPreview();
        game.itemSystem.updateWorldState();
        game.networking.processNetworking((float) game.state.elapsedTime);
        game.hudSystem.updateHUD();
        game.movementSystem.updateRunState((float) game.state.elapsedTime);
    }
}
