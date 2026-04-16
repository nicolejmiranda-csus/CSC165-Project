package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

public class MyGameWorldBuilder {
    private final MyGame game;

    public MyGameWorldBuilder(MyGame game) {
        this.game = game;
    }

    public void buildObjects() {
        buildLocalAvatar();
        buildInteractiveProps();
        buildPyramids();
        buildWorldDecor();
        buildTerrain();
        buildHealthPotion();
        buildAxes();
        buildHome();
        buildHudIcons();
        buildBuildPreview();
    }

    private void buildLocalAvatar() {
        MyGameAssets a = game.assets;
        MyGameState s = game.state;
        ObjShape avatarShape = s.selectedAvatar.compareTo("playerModel2") == 0 ? a.playerModel2S : a.playerModel1S;
        TextureImage avatarTexture = s.selectedAvatar.compareTo("playerModel2") == 0 ? a.playerModel2Tx : a.playerModel1Tx;
        a.avatar = new GameObject(GameObject.root(), avatarShape, avatarTexture);
        a.avatar.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 6f));
        a.avatar.setLocalScale((new Matrix4f()).scaling(1.0f));
        a.avatar.setLocalRotation((new Matrix4f()).rotationY((float) Math.toRadians(180f)));
        a.avatar.getRenderStates().hasLighting(true);
        a.avatar.getRenderStates().setModelOrientationCorrection((new Matrix4f()).identity());
        s.playerYaw = 180.0f;
    }

    private void buildInteractiveProps() {
        MyGameAssets a = game.assets;
        a.flashlight = new GameObject(GameObject.root(), a.flashlightS, a.flashlightTx);
        a.flashlight.setLocalTranslation((new Matrix4f()).translation(6.5f, 0.5f, 8f));
        a.flashlight.setLocalScale((new Matrix4f()).scaling(0.18f));
        a.flashlight.setLocalRotation((new Matrix4f()).rotationY((float) Math.toRadians(180f)));
        a.table = new GameObject(GameObject.root(), a.tableS, a.tableTx);
        a.table.setLocalTranslation((new Matrix4f()).translation(5f, 0f, 8f));
        a.table.setLocalScale((new Matrix4f()).scaling(0.4f));
    }

    private void buildPyramids() {
        MyGameAssets a = game.assets;
        for (int i = 0; i < 3; i++) {
            a.pyramids[i] = new GameObject(GameObject.root(), a.pyrS, a.pyramidTx[i]);
            a.pyramids[i].setLocalScale((new Matrix4f()).scaling(5f));
            a.pyramids[i].getRenderStates().hasLighting(true);
        }
        a.pyramids[0].setLocalTranslation((new Matrix4f()).translation(-24f, 0f, -10f));
        a.pyramids[1].setLocalTranslation((new Matrix4f()).translation(34f, 0f, 0f));
        a.pyramids[2].setLocalTranslation((new Matrix4f()).translation(18f, 0f, 10f));
    }

    private void buildWorldDecor() {
        MyGameAssets a = game.assets;
        a.dol = new GameObject(GameObject.root(), a.dolS, a.dolTx);
        a.dol.setLocalTranslation((new Matrix4f()).translation(-8f, 0f, -12f));
        a.dol.setLocalScale((new Matrix4f()).scaling(1.0f));
        a.dol.setLocalRotation((new Matrix4f()).rotationY((float) Math.toRadians(45f)));
        a.dol.getRenderStates().hasLighting(true);
    }

    private void buildTerrain() {
        MyGameAssets a = game.assets;
        a.terrain = new GameObject(GameObject.root(), a.terrainShape, a.grassTx);
        a.terrain.setLocalTranslation(new Matrix4f().translation(0, 0, 0));
        a.terrain.setLocalScale(new Matrix4f().scaling(200.0f, 20f, 200.0f));
        a.terrain.setHeightMap(a.heightMaptx);
        a.terrain.getRenderStates().setTiling(1);
        a.terrain.getRenderStates().setTileFactor(10);
    }

    private void buildHealthPotion() {
        MyGameAssets a = game.assets;
        a.healthPotion = new GameObject(GameObject.root(), a.healthPotionS, a.healthPotionTx);
        a.healthPotion.setLocalTranslation((new Matrix4f()).translation(3f, 0.15f, -3f));
        a.healthPotion.setLocalScale((new Matrix4f()).scaling(0.15f));
        a.healthPotion.setLocalRotation((new Matrix4f()).identity());
        a.healthPotion.getRenderStates().hasLighting(true);
        a.healthPotion.getRenderStates().isTransparent(true);
        a.healthPotion.getRenderStates().setOpacity(0.4f);
    }

    private void buildAxes() {
        MyGameAssets a = game.assets;
        a.axisX = new GameObject(GameObject.root(), a.linxS);
        a.axisY = new GameObject(GameObject.root(), a.linyS);
        a.axisZ = new GameObject(GameObject.root(), a.linzS);
        a.axisX.getRenderStates().setColor(new Vector3f(1f, 0f, 0f));
        a.axisY.getRenderStates().setColor(new Vector3f(0f, 1f, 0f));
        a.axisZ.getRenderStates().setColor(new Vector3f(0f, 0f, 1f));
        Matrix4f lift = new Matrix4f().translation(0f, 0.1f, 0f);
        a.axisX.setLocalTranslation(lift);
        a.axisY.setLocalTranslation(lift);
        a.axisZ.setLocalTranslation(lift);
    }

    private void buildHome() {
        float s = GameConstants.HOME_SCALE;
        float w = GameConstants.HOME_W * s;
        float h = GameConstants.HOME_H * s;
        float d = GameConstants.HOME_D * s;
        float floorY = GameConstants.HOME_FLOOR_Y;
        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;

        buildHomeQuad((new Matrix4f()).rotationX((float) Math.toRadians(-90f)), (new Matrix4f()).scaling(hw, hd, 1f), (new Matrix4f()).translation(0f, floorY, 0f));
        buildHomeQuad(null, (new Matrix4f()).scaling(hw, hh, 1f), (new Matrix4f()).translation(0f, floorY + hh, -hd));
        buildHomeQuad((new Matrix4f()).rotationY((float) Math.toRadians(90f)), (new Matrix4f()).scaling(hd, hh, 1f), (new Matrix4f()).translation(-hw, floorY + hh, 0f));
        buildHomeQuad((new Matrix4f()).rotationY((float) Math.toRadians(-90f)), (new Matrix4f()).scaling(hd, hh, 1f), (new Matrix4f()).translation(hw, floorY + hh, 0f));

        float doorW = 2.6f * s;
        float sideW = (w - doorW) / 2f;
        float hSideW = sideW / 2f;
        Matrix4f frontRotation = (new Matrix4f()).rotationY((float) Math.toRadians(180f));
        buildHomeQuad(frontRotation, (new Matrix4f()).scaling(hSideW, hh, 1f), (new Matrix4f()).translation(-(doorW / 2f + hSideW), floorY + hh, hd));
        buildHomeQuad(frontRotation, (new Matrix4f()).scaling(hSideW, hh, 1f), (new Matrix4f()).translation((doorW / 2f + hSideW), floorY + hh, hd));
        buildHomeRoof(w, d, floorY + h);
    }

    private GameObject buildHomeQuad(Matrix4f rotation, Matrix4f scale, Matrix4f translation) {
        GameObject quad = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        if (rotation != null) quad.setLocalRotation(rotation);
        quad.setLocalScale(scale);
        quad.setLocalTranslation(translation);
        quad.getRenderStates().hasLighting(true);
        return quad;
    }

    private void buildHomeRoof(float w, float d, float wallTopY) {
        float halfW = w / 2f;
        float halfD = d / 2f;
        float roofAngle = (float) Math.toRadians(45f);
        float roofRise = (float) Math.tan(roofAngle) * halfW;
        float slantLen = halfW / (float) Math.cos(roofAngle);
        float roofHalfSlant = slantLen / 2f;
        GameObject roofLeft = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        roofLeft.getRenderStates().hasLighting(true);
        roofLeft.setLocalScale((new Matrix4f()).scaling(roofHalfSlant, halfD, 1f));
        roofLeft.setLocalRotation((new Matrix4f()).rotationZ(roofAngle).rotateX((float) Math.toRadians(90f)));
        roofLeft.setLocalTranslation((new Matrix4f()).translation(-halfW / 2f, wallTopY + roofRise / 2f, 0f));
        GameObject roofRight = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        roofRight.getRenderStates().hasLighting(true);
        roofRight.setLocalScale((new Matrix4f()).scaling(roofHalfSlant, halfD, 1f));
        roofRight.setLocalRotation((new Matrix4f()).rotationZ(-roofAngle).rotateX((float) Math.toRadians(90f)));
        roofRight.setLocalTranslation((new Matrix4f()).translation(halfW / 2f, wallTopY + roofRise / 2f, 0f));
    }

    private void buildHudIcons() {
        for (int i = 0; i < 3; i++) {
            game.assets.hudIcons[i] = new GameObject(game.assets.avatar, game.assets.quadS, game.assets.pyramidTx[i]);
            game.assets.hudIcons[i].getRenderStates().hasLighting(false);
            game.assets.hudIcons[i].setLocalScale((new Matrix4f()).scaling(0f));
            float xOff = 0.55f - (i * 0.55f);
            game.assets.hudIcons[i].setLocalTranslation((new Matrix4f()).translation(xOff, 1.1f, -1.0f));
            game.assets.hudIcons[i].setLocalRotation((new Matrix4f()).identity());
            game.state.iconCollected[i] = false;
        }
    }

    private void buildBuildPreview() {
        game.assets.buildPreview = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
        game.assets.buildPreview.getRenderStates().hasLighting(true);
    }
}
