package running_Dead;

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
        a.playerModel1AnimatedS = new AnimatedShape("playerModel1_Mesh.rkm", "playerModel1_Skeleton.rks");
        a.playerModel1AnimatedS.loadAnimation("IDLE", "playerModel1_Idle.rka");
        a.playerModel1AnimatedS.loadAnimation("RUN", "playerModel1_RunForward.rka");
        a.playerModel1AnimatedS.loadAnimation("WALK", "playerModel1_WalkForward.rka");
        a.smilingManAnimatedShapes = new AnimatedShape[GameConstants.MAX_SMILING_MEN];
        for (int i = 0; i < a.smilingManAnimatedShapes.length; i++) {
            a.smilingManAnimatedShapes[i] = loadSmilingManShape();
        }
        a.smilingManAnimatedS = a.smilingManAnimatedShapes[0];
        a.playerModel1GhostAnimatedS = new AnimatedShape[8];
        for (int i = 0; i < a.playerModel1GhostAnimatedS.length; i++) {
            a.playerModel1GhostAnimatedS[i] = new AnimatedShape("playerModel1_Mesh.rkm", "playerModel1_Skeleton.rks");
            a.playerModel1GhostAnimatedS[i].loadAnimation("IDLE", "playerModel1_Idle.rka");
            a.playerModel1GhostAnimatedS[i].loadAnimation("RUN", "playerModel1_RunForward.rka");
            a.playerModel1GhostAnimatedS[i].loadAnimation("WALK", "playerModel1_WalkForward.rka");
        }
        a.playerModel1S = new ImportedModel("boy_character_textured.obj");

        a.playerModel2AnimatedS = new AnimatedShape("playerModel2_Mesh.rkm", "playerModel2_Skeleton.rks");
        a.playerModel2AnimatedS.loadAnimation("IDLE", "playerModel2_Idle.rka");
        a.playerModel2AnimatedS.loadAnimation("RUN", "playerModel2_Run.rka");
        a.playerModel2AnimatedS.loadAnimation("WALK", "playerModel2_Walk.rka");
        a.playerModel2AnimatedS.loadAnimation("ZOMBIE_WALK", "playerModel2_ZombieWalk.rka");
        a.playerModel2AnimatedS.loadAnimation("ZOMBIE_RUN", "playerModel2_ZombieRun.rka");
        a.playerModel2GhostAnimatedS = new AnimatedShape[8];
        for (int i = 0; i < a.playerModel2GhostAnimatedS.length; i++) {
            a.playerModel2GhostAnimatedS[i] = new AnimatedShape("playerModel2_Mesh.rkm", "playerModel2_Skeleton.rks");
            a.playerModel2GhostAnimatedS[i].loadAnimation("IDLE", "playerModel2_Idle.rka");
            a.playerModel2GhostAnimatedS[i].loadAnimation("RUN", "playerModel2_Run.rka");
            a.playerModel2GhostAnimatedS[i].loadAnimation("WALK", "playerModel2_Walk.rka");
            a.playerModel2GhostAnimatedS[i].loadAnimation("ZOMBIE_WALK", "playerModel2_ZombieWalk.rka");
            a.playerModel2GhostAnimatedS[i].loadAnimation("ZOMBIE_RUN", "playerModel2_ZombieRun.rka");
        }
        a.playerModel2S = new ImportedModel("playerModel2.obj");

        a.mushMonAnimatedS = new AnimatedShape("mushMon_Mesh.rkm", "mushMon_Skeleton.rks");
        a.mushMonAnimatedS.loadAnimation("WALK", "mushMon_Walk.rka");

        a.flashlightS = new ImportedModel("flashlight.obj");
        a.tableS = new ImportedModel("table.obj");
        a.healthPotionS = new ImportedModel("healthpotion1.obj");
        a.dolphinS = new ImportedModel("dolphinHighPoly.obj");
        a.cubeS = new Cube();
        a.rock2S = new ImportedModel("Rock2.obj");
        a.lowpolyHoodS = new ImportedModel("lowpolyhood.obj");
        a.dashPickupS = new ImportedModel("dashPickup.obj");
        a.babyZombieS = new ImportedModel("babyZombie.obj");
        a.sceneryRockS = new ObjShape[] {
            new ImportedModel("Rock1.obj"),
            new ImportedModel("Rock2.obj"),
            new ImportedModel("Rock10.obj"),
            new ImportedModel("Rock11.obj"),
            new ImportedModel("RockPile1.obj"),
            new ImportedModel("BigRock1.obj"),
            new ImportedModel("BigRock2.obj")
        };
        a.sceneryTreeModelFiles = new String[] {
            "DeadOak1.obj",
            "DeadOak2.obj",
            "DeadOak3.obj",
            "DeadSpruce2.obj",
            "OakTree1.obj",
            "OakTree2.obj",
            "OakTree3.obj",
            "SpruceTree1.obj",
            "SpruceTree2.obj",
            "SpruceTree3.obj",
            "tree1.obj",
            "tree2.obj"
        };
        a.sceneryTreeS = new ObjShape[a.sceneryTreeModelFiles.length];
        for (int i = 0; i < a.sceneryTreeModelFiles.length; i++) {
            a.sceneryTreeS[i] = new ImportedModel(a.sceneryTreeModelFiles[i]);
        }
        a.quadS = new ManualQuad();
        a.terrainShape = new TerrainPlane(1000);
        a.linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(3f, 0f, 0f));
        a.linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 3f, 0f));
        a.linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -3f));
    }

    private AnimatedShape loadSmilingManShape() {
        AnimatedShape shape = new AnimatedShape("smilingMan.rkm", "smilingMan.rks");
        shape.loadAnimation("smilingManIdle", "smilingMan_Idle.rka");
        shape.loadAnimation("smilingManWalkForward", "smilingMan_WalkForward.rka");
        shape.loadAnimation("smilingManIdleWave", "smilingMan_IdleWave.rka");
        shape.loadAnimation("smilingManRunForwardTransition", "smilingMan_RunForwardTransition.rka");
        shape.loadAnimation("smilingManRunForwardInitial", "smilingMan_RunForwardInitial.rka");
        shape.loadAnimation("smilingManRunForward", "smilingMan_RunForward.rka");
        shape.loadAnimation("smilingManRunForwardOutro", "smilingMan_RunForwardOutro.rka");
        shape.loadAnimation("smilingManRunForwardToStand", "smilingMan_RunForwardToStand.rka");
        return shape;
    }

    public void loadTextures() {
        MyGameAssets a = game.assets;
        a.playerModel1Tx = new TextureImage("playerModel1.jpg");
        a.zombiePlayerModel1Tx = new TextureImage("ZombiePlayerModel1.jpg");
        a.smilingManTx = new TextureImage("SmilingMan.png");
        a.mushroomMonTx = new TextureImage("mushMon_Txt.png");
        a.playerModel2Tx = new TextureImage("playerModel2.png");
        a.zombiePlayerModel2Tx = new TextureImage("ZombiePlayerModel2.png");
        a.playerModel1FarTx = new TextureImage("generated/player1_far.png");
        a.zombiePlayerModel1FarTx = new TextureImage("generated/zombie_player1_far.png");
        a.playerModel2FarTx = new TextureImage("generated/player2_far.png");
        a.zombiePlayerModel2FarTx = new TextureImage("generated/zombie_player2_far.png");
        a.smilingManFarTx = new TextureImage("generated/smilingman_far.png");
        a.mushroomMonFarTx = new TextureImage("generated/mushmon_far.png");
        a.grassTx = new TextureImage("grass2.png");
        a.homeTx = new TextureImage("brick1.jpg");
        a.heightMapTextures = new TextureImage[] {
            new TextureImage("heightmap.png")
        };
        a.heightMaptx = a.heightMapTextures[0];
        a.flashlightTx = new TextureImage("flashlightTx.png");
        a.tableTx = new TextureImage("tableTx.png");
        a.healthPotionTx = new TextureImage("health_potion.png");
        a.dolphinTx = new TextureImage("Dolphin_HighPolyUV.jpg");
        a.grassFarTx = new TextureImage("generated/grass_far.png");
        a.woodFarTx = new TextureImage("generated/wood_far.png");
        a.metalFarTx = new TextureImage("generated/metal_far.png");
        a.glassFarTx = new TextureImage("generated/glass_far.png");
        a.rockFarTx = new TextureImage("generated/rock_far.png");
        a.brickFarTx = new TextureImage("generated/brick_far.png");
        a.tableFarTx = new TextureImage("generated/table_far.png");
        a.flashlightFarTx = new TextureImage("generated/flashlight_far.png");
        a.potionFarTx = new TextureImage("generated/potion_far.png");
        a.dolphinFarTx = new TextureImage("generated/dolphin_far.png");
        a.leafFarTx = new TextureImage("generated/leaf_far.png");
        a.leatherFarTx = new TextureImage("generated/leather_far.png");
        a.babyZombieFarTx = new TextureImage("generated/babyzombie_far.png");
        a.woodBlockTx = new TextureImage("woodplanks.png");
        a.metalBuildTx = new TextureImage("rustymetal.png");
        a.glassBuildTx = new TextureImage("abstract-background-with-patterned-glass-texture 1024x1024.jpg");
        a.grassNormalTx = new TextureImage("generated/grass_nrm.png");
        a.woodNormalTx = new TextureImage("Wood Texture 4_NRM.png");
        a.rockNormalTx = new TextureImage("generated/rock_nrm.png");
        a.metalNormalTx = new TextureImage("generated/metal_nrm.png");
        a.glassNormalTx = new TextureImage("generated/glass_nrm.png");
        a.brickNormalTx = new TextureImage("generated/brick_nrm.png");
        a.leafNormalTx = new TextureImage("generated/leaf_nrm.png");
        a.leatherNormalTx = new TextureImage("generated/leather_nrm.png");
        a.potionNormalTx = new TextureImage("generated/potion_nrm.png");
        a.flashlightNormalTx = new TextureImage("generated/flashlight_nrm.png");
        a.tableNormalTx = new TextureImage("generated/table_nrm.png");
        a.roughNormalTx = new TextureImage("Wood Texture 2_NRM.png");
        a.rockTx = a.rockFarTx;
        a.hoodTx = new TextureImage("hood_leather.png");
        a.babyZombieTx = new TextureImage("babyZombie.png");
        a.rockTx2 = a.rockFarTx;
        a.leafTx = new TextureImage("leaf1.jpg");
        a.deadOakTreeTx = a.leafTx;
        a.deadSpruceTreeTx = a.leafTx;
        a.oakTreeTx = a.leafTx;
        a.spruceTreeTx = a.leafTx;
        a.treeWoodTx = a.leafTx;
        a.sceneryRockTx = new TextureImage[] {
            a.rockTx, a.rockTx, a.rockTx2, a.rockTx2, a.rockTx2, a.rockTx, a.rockTx2
        };
        a.sceneryTreeTx = new TextureImage[] {
            a.deadOakTreeTx,
            a.deadOakTreeTx,
            a.deadOakTreeTx,
            a.deadSpruceTreeTx,
            a.oakTreeTx,
            a.oakTreeTx,
            a.oakTreeTx,
            a.spruceTreeTx,
            a.spruceTreeTx,
            a.spruceTreeTx,
            a.treeWoodTx,
            a.treeWoodTx
        };
    }

    public void loadSkyBoxes() {
        MyGameAssets a = game.assets;
        a.sky04 = MyGame.getEngine().getSceneGraph().loadCubeMap("sky04_cubemap");
        a.sky15Night = MyGame.getEngine().getSceneGraph().loadCubeMap("sky15_night");
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(a.sky04);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
    }
}
