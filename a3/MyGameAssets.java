package a3;

import java.util.ArrayList;
import java.util.HashMap;

import tage.GameObject;
import tage.Light;
import tage.ObjShape;
import tage.TextureImage;
import tage.shapes.AnimatedShape;
import tage.shapes.TerrainPlane;

public class MyGameAssets {
    // Shapes
    ObjShape quadS, linxS, linyS, linzS;
    TerrainPlane terrainShape;
    AnimatedShape playerModel1AnimatedS;
    AnimatedShape[] playerModel1GhostAnimatedS;
    AnimatedShape smilingManAnimatedS;
    AnimatedShape[] smilingManAnimatedShapes;
    AnimatedShape playerModel2AnimatedS;
    AnimatedShape[] playerModel2GhostAnimatedS;
    AnimatedShape mushMonAnimatedS;
    ObjShape playerModel1S, playerModel2S, flashlightS, tableS, healthPotionS, dolphinS;
    ObjShape cubeS, rock2S, lowpolyHoodS, dashPickupS, babyZombieS;
    ObjShape[] sceneryRockS, sceneryTreeS;
    String[] sceneryTreeModelFiles;

    // Textures
    TextureImage playerModel1Tx, zombiePlayerModel1Tx, playerModel2Tx, zombiePlayerModel2Tx, healthPotionTx;
    TextureImage smilingManTx, mushroomMonTx;
    TextureImage grassTx, homeTx, heightMaptx, flashlightTx, tableTx, dolphinTx;
    TextureImage[] heightMapTextures;
    int activeHeightMapIndex = 0;
    TextureImage woodBlockTx, metalBuildTx, glassBuildTx, rockTx, hoodTx, babyZombieTx;
    TextureImage rockTx2, leafTx, deadOakTreeTx, deadSpruceTreeTx, oakTreeTx, spruceTreeTx, treeWoodTx;
    TextureImage[] sceneryRockTx, sceneryTreeTx;

    // Scene objects
    GameObject avatar;
    GameObject smilingMan;
    final ArrayList<GameObject> mushMons = new ArrayList<>();
    final ArrayList<GameObject> smilingMen = new ArrayList<>();
    GameObject healthPotion;
    GameObject terrain;
    GameObject flashlight;
    GameObject heldRock;
    GameObject heldBabyZombie;
    GameObject table;
    GameObject axisX, axisY, axisZ;
    GameObject buildPreview;
    final ArrayList<GameObject> tableCovers = new ArrayList<>();
    final ArrayList<MyGameBuildMeta> staticCoverBuilds = new ArrayList<>();
    final ArrayList<GameObject> healthPotions = new ArrayList<>();
    final ArrayList<GameObject> flashlights = new ArrayList<>();
    final ArrayList<GameObject> sceneryProps = new ArrayList<>();
    final ArrayList<GameObject> sceneryRocks = new ArrayList<>();
    final ArrayList<GameObject> sceneryTrees = new ArrayList<>();
    final HashMap<GameObject, Float> terrainLiftOffsets = new HashMap<>();
    final HashMap<GameObject, Integer> sceneryTreeIndices = new HashMap<>();

    // Lights
    Light flashlightSpotlight;

    // Skyboxes
    int sky04;
    int sky15Night;
}
