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
        game.movementSystem.processPlayerInput();
        game.movementSystem.syncPlayerToTerrain();
        game.cameraSystem.updateCameraLimits();
        game.photoSystem.checkCrash();
        game.cameraSystem.updateMainCamera();
        game.buildSystem.updateBuildPreview();
        game.itemSystem.updateWorldState();
        game.networking.processNetworking((float) game.state.elapsedTime);
        game.hudSystem.updateHUD();
        game.movementSystem.updateRunState((float) game.state.elapsedTime);
    }
}
