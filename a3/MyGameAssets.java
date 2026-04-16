package a3;

import tage.GameObject;
import tage.Light;
import tage.ObjShape;
import tage.TextureImage;
import tage.nodeControllers.BobbingController;
import tage.nodeControllers.RotationController;
import tage.shapes.TerrainPlane;

public class MyGameAssets {
    // Shapes
    ObjShape quadS, linxS, linyS, linzS;
    ObjShape pyrS;
    TerrainPlane terrainShape;
    ObjShape playerModel1S, playerModel2S, flashlightS, tableS, healthPotionS;

    // Textures
    TextureImage playerModel1Tx, playerModel2Tx, healthPotionTx;
    TextureImage grassTx, homeTx, heightMaptx, flashlightTx, tableTx;
    final TextureImage[] pyramidTx = new TextureImage[3];

    // Scene objects
    GameObject avatar;
    GameObject healthPotion;
    final GameObject[] pyramids = new GameObject[3];
    GameObject terrain;
    GameObject flashlight;
    GameObject table;
    final GameObject[] hudIcons = new GameObject[3];
    GameObject axisX, axisY, axisZ;
    GameObject buildPreview;

    // Lights/controllers
    final Light[] lightPyramid = new Light[3];
    Light flashlightSpotlight;
    RotationController photoSpin;
    BobbingController activatedPyramidBob;

    // Skyboxes
    int sky04;
    int sky15Night;
}
