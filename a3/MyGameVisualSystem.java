package a3;

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
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(game.assets.sky04);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        game.hudSystem.showEvent("SKYBOX sky04", 1.0);
    }

    public void useSky15Night() {
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(game.assets.sky15Night);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        game.hudSystem.showEvent("SKYBOX sky15_night", 1.0);
    }

    public void toggleSkyboxOff() {
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(false);
        game.hudSystem.showEvent("SKYBOX OFF", 1.0);
    }

    public void randomizeRoundSkybox(java.util.UUID roundZombieId, long roundToken) {
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
