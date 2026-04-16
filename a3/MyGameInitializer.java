package a3;

import org.joml.Vector3f;
import tage.nodeControllers.BobbingController;

public class MyGameInitializer {
    private final MyGame game;

    public MyGameInitializer(MyGame game) {
        this.game = game;
    }

    public void initializeGame() {
        initializeFrameTiming();
        MyGame.getEngine().getRenderSystem().setWindowDimensions(1900, 1000);
        game.cameraSystem.setupCameras();
        setupNodeControllers();
        game.state.im = MyGame.getEngine().getInputManager();
        game.inputBinder.bindAll();
        game.networking.setupNetworkingIfNeeded();
        game.networking.installShutdownHandlers();
    }

    private void initializeFrameTiming() {
        game.state.lastFrameTime = System.currentTimeMillis();
        game.state.currFrameTime = System.currentTimeMillis();
        game.state.elapsedTime = 0.0;
    }

    private void setupNodeControllers() {
        game.assets.photoSpin = new tage.nodeControllers.RotationController(MyGame.getEngine(), new Vector3f(0, 1, 0), 0.002f);
        MyGame.getEngine().getSceneGraph().addNodeController(game.assets.photoSpin);
        game.assets.photoSpin.enable();
        game.assets.activatedPyramidBob = new BobbingController(MyGame.getEngine(), 0.35f, 1.2f);
        MyGame.getEngine().getSceneGraph().addNodeController(game.assets.activatedPyramidBob);
        game.assets.activatedPyramidBob.enable();
    }
}
