package a3;

import org.joml.Vector3f;
import tage.*;
import tage.shapes.*;

public class MyGameLoaders {
    private final MyGame game;

    public MyGameLoaders(MyGame game) {
        this.game = game;
    }

    public void loadShapes() {
        MyGameAssets a = game.assets;
        a.dolS = new ImportedModel("dolphinHighPoly.obj");
        a.playerModel1S = new ImportedModel("boy_character_textured.obj");
        a.playerModel2S = new ImportedModel("playerModel.obj");
        a.flashlightS = new ImportedModel("flashlight.obj");
        a.tableS = new ImportedModel("table.obj");
        a.healthPotionS = new ImportedModel("healthpotion1.obj");
        a.quadS = new ManualQuad();
        a.pyrS = new ManualPyramid();
        a.terrainShape = new TerrainPlane(1000);
        a.linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(3f, 0f, 0f));
        a.linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 3f, 0f));
        a.linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -3f));
    }

    public void loadTextures() {
        MyGameAssets a = game.assets;
        a.dolTx = new TextureImage("Dolphin_HighPolyUV.jpg");
        a.playerModel1Tx = new TextureImage("boy_textured.png");
        a.playerModel2Tx = new TextureImage("playerModel.png");
        a.pyramidTx[0] = new TextureImage("3d-geometric-texture-copper_512x512.jpg");
        a.pyramidTx[1] = new TextureImage("pyramid2_512x512.jpg");
        a.pyramidTx[2] = new TextureImage("pyramidbyme_512x512.jpg");
        a.grassTx = new TextureImage("grass.png");
        a.homeTx = new TextureImage("brick1.jpg");
        a.heightMaptx = new TextureImage("heightmap.png");
        a.flashlightTx = new TextureImage("flashlightTx.png");
        a.tableTx = new TextureImage("tableTx.png");
        a.healthPotionTx = new TextureImage("health_potion.png");
    }

    public void loadSkyBoxes() {
        MyGameAssets a = game.assets;
        a.sky04 = MyGame.getEngine().getSceneGraph().loadCubeMap("sky04_cubemap");
        a.sky15Night = MyGame.getEngine().getSceneGraph().loadCubeMap("sky15_night");
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(a.sky04);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
    }
}
