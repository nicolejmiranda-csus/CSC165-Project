package a3;

public class MyGameInitializer {
    private final MyGame game;

    public MyGameInitializer(MyGame game) {
        this.game = game;
    }

    public void initializeGame() {
        initializeFrameTiming();
        MyGame.getEngine().getRenderSystem().setWindowDimensions(1900, 1000);
        game.cameraSystem.setupCameras();
        game.state.im = MyGame.getEngine().getInputManager();
        game.inputBinder.bindAll();
        game.networking.setupNetworkingIfNeeded();
        game.networking.installShutdownHandlers();
        game.soundSystem.startAmbience();
    }

    private void initializeFrameTiming() {
        game.state.lastFrameTime = System.currentTimeMillis();
        game.state.currFrameTime = System.currentTimeMillis();
        game.state.elapsedTime = 0.0;
    }

}
