package a3;

import java.util.ArrayList;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

public class MyGameWorldBuilder {
    private final MyGame game;
    private final Random worldRandom = new Random(1652026L);
    private final Random spawnRandom = new Random();
    private final ArrayList<MyGameScatterSpot> scatterSpots = new ArrayList<>();
    private static final float WORLD_SCATTER_RADIUS = 95.0f;
    private static final float PLAYER_SPAWN_RADIUS = 82.0f;

    public MyGameWorldBuilder(MyGame game) {
        this.game = game;
    }

    public void buildObjects() {
        buildTerrain();
        buildSceneryProps();
        buildLocalAvatar();
        buildInteractiveProps();
        buildAxes();
        buildHome();
        buildBuildPreview();
    }

    private void buildLocalAvatar() {
        MyGameAssets a = game.assets;
        MyGameState s = game.state;
        boolean useAnimatedPlayer1 = !game.isPlayerModel2Selected() && game.canUseAnimatedPlayerModel1();
        ObjShape avatarShape = game.isPlayerModel2Selected()
            ? a.playerModel2S
            : (useAnimatedPlayer1 ? a.playerModel1AnimatedS : a.playerModel1S);
        TextureImage avatarTexture = game.isPlayerModel2Selected() ? a.playerModel2Tx : a.playerModel1Tx;
        a.avatar = new GameObject(GameObject.root(), avatarShape, avatarTexture);
        a.avatar.setLocalTranslation((new Matrix4f()).translation(randomSpawnLocation(false)));
        a.avatar.setLocalScale((new Matrix4f()).scaling(1.0f));
        a.avatar.setLocalRotation((new Matrix4f()).rotationY((float) Math.toRadians(180f)));
        a.avatar.getRenderStates().hasLighting(true);
        a.avatar.getRenderStates().setModelOrientationCorrection((new Matrix4f()).identity());
        s.playerYaw = 180.0f;
        game.animationSystem.startIdleIfAnimatedAvatar();
    }

    private void buildInteractiveProps() {
        MyGameAssets a = game.assets;
        a.table = new GameObject(GameObject.root(), a.tableS, a.tableTx);
        a.table.setLocalTranslation((new Matrix4f()).translation(5f, 0f, 8f));
        a.table.setLocalScale((new Matrix4f()).scaling(0.4f));
    }

    private void buildTerrain() {
        MyGameAssets a = game.assets;
        a.terrain = new GameObject(GameObject.root(), a.terrainShape, a.grassTx);
        a.terrain.setLocalTranslation(new Matrix4f().translation(0, 0, 0));
        a.terrain.setLocalScale(new Matrix4f().scaling(200.0f, 20f, 200.0f));
        a.terrain.setHeightMap(a.heightMaptx);
        a.terrain.getRenderStates().hasLighting(true);
        a.terrain.getRenderStates().setTiling(1);
        a.terrain.getRenderStates().setTileFactor(80);
    }

    private void buildSceneryProps() {
        buildRockScenery(45);
        buildTreeScenery(42);
    }

    private void buildRockScenery(int count) {
        MyGameAssets a = game.assets;
        for (int i = 0; i < count; i++) {
            int index = worldRandom.nextInt(a.sceneryRockS.length);
            GameObject rock = new GameObject(GameObject.root(), a.sceneryRockS[index], a.sceneryRockTx[index]);
            float scale;
            if (index == 4) scale = randomRange(0.55f, 0.95f);
            else if (index >= 5) scale = randomRange(0.72f, 1.12f);
            else scale = randomRange(0.24f, 0.62f);
            float clearance = index >= 4 ? 7.0f + scale * 5.0f : 2.8f + scale * 2.0f;
            Vector3f p = randomTerrainLocation(plantedLift(a.sceneryRockS[index], scale, 0.01f), 9.0f, clearance);
            rock.setLocalTranslation((new Matrix4f()).translation(p));
            rock.setLocalScale((new Matrix4f()).scaling(scale));
            rock.setLocalRotation((new Matrix4f()).rotationY(randomRadians()));
            rock.getRenderStates().hasLighting(true);
            a.sceneryProps.add(rock);
            a.sceneryRocks.add(rock);
            a.terrainLiftOffsets.put(rock, p.y);
        }
    }

    private void buildTreeScenery(int count) {
        MyGameAssets a = game.assets;
        for (int i = 0; i < count; i++) {
            int index = worldRandom.nextInt(a.sceneryTreeS.length);
            GameObject tree = new GameObject(GameObject.root(), a.sceneryTreeS[index], a.sceneryTreeTx[index]);
            float scale = index >= 10 ? randomRange(0.58f, 0.82f) : randomRange(0.40f, 0.62f);
            float clearance = index >= 10 ? 7.5f + scale * 4.0f : 10.0f + scale * 7.0f;
            Vector3f p = randomTerrainLocation(plantedLift(a.sceneryTreeS[index], scale, 0.01f), 14.0f, clearance);
            tree.setLocalTranslation((new Matrix4f()).translation(p));
            tree.setLocalScale((new Matrix4f()).scaling(scale));
            tree.setLocalRotation((new Matrix4f()).rotationY(randomRadians()));
            tree.getRenderStates().hasLighting(true);
            a.sceneryProps.add(tree);
            a.sceneryTrees.add(tree);
            a.terrainLiftOffsets.put(tree, p.y);
            a.sceneryTreeIndices.put(tree, index);
        }
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

    private Vector3f randomTerrainLocation(float lift, float minCenterDistance, float clearanceRadius) {
        for (int i = 0; i < 220; i++) {
            float x = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            float z = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            if ((x * x + z * z) < minCenterDistance * minCenterDistance) continue;
            if (Math.abs(x) < 8.0f && Math.abs(z) < 10.0f) continue;
            if (!isScatterSpotOpen(x, z, clearanceRadius)) continue;
            reserveScatterSpot(x, z, clearanceRadius);
            return new Vector3f(x, lift, z);
        }
        float relaxedClearance = clearanceRadius * 0.65f;
        for (int i = 0; i < 120; i++) {
            float x = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            float z = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            if ((x * x + z * z) < minCenterDistance * minCenterDistance) continue;
            if (Math.abs(x) < 8.0f && Math.abs(z) < 10.0f) continue;
            if (!isScatterSpotOpen(x, z, relaxedClearance)) continue;
            reserveScatterSpot(x, z, relaxedClearance);
            return new Vector3f(x, lift, z);
        }
        float x = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
        float z = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
        reserveScatterSpot(x, z, relaxedClearance);
        return new Vector3f(x, lift, z);
    }

    private boolean isScatterSpotOpen(float x, float z, float radius) {
        for (MyGameScatterSpot spot : scatterSpots) {
            float dx = x - spot.x;
            float dz = z - spot.z;
            float needed = radius + spot.radius;
            if (dx * dx + dz * dz < needed * needed) return false;
        }
        return true;
    }

    private void reserveScatterSpot(float x, float z, float radius) {
        scatterSpots.add(new MyGameScatterSpot(x, z, radius));
    }

    public Vector3f randomSpawnLocation(boolean useTerrainHeight) {
        for (int i = 0; i < 160; i++) {
            float x = randomSpawnRange();
            float z = randomSpawnRange();
            if (!isSpawnSpotOpen(x, z)) continue;
            return new Vector3f(x, spawnY(x, z, useTerrainHeight), z);
        }
        return new Vector3f(0f, spawnY(0f, 0f, useTerrainHeight), 6f);
    }

    private boolean isSpawnSpotOpen(float x, float z) {
        if ((x * x + z * z) < 12.0f * 12.0f) return false;
        if (Math.abs(x) < 8.0f && Math.abs(z) < 10.0f) return false;
        for (MyGameScatterSpot spot : scatterSpots) {
            float dx = x - spot.x;
            float dz = z - spot.z;
            float needed = spot.radius + 3.5f;
            if (dx * dx + dz * dz < needed * needed) return false;
        }
        return true;
    }

    private float randomSpawnRange() {
        return -PLAYER_SPAWN_RADIUS + spawnRandom.nextFloat() * PLAYER_SPAWN_RADIUS * 2.0f;
    }

    private float spawnY(float x, float z, boolean useTerrainHeight) {
        if (!useTerrainHeight || game.assets.terrain == null) return 0f;
        return game.assets.terrain.getHeight(x, z);
    }

    private float randomRange(float min, float max) {
        return min + worldRandom.nextFloat() * (max - min);
    }

    private float plantedLift(ObjShape shape, float scale, float clearance) {
        float[] vertices = shape == null ? null : shape.getVertices();
        if (vertices == null || vertices.length < 3) return clearance;
        float minY = Float.MAX_VALUE;
        for (int i = 1; i < vertices.length; i += 3) {
            if (vertices[i] < minY) minY = vertices[i];
        }
        return -minY * scale + clearance;
    }

    private float randomRadians() {
        return randomRange(0f, (float) (Math.PI * 2.0));
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

    private void buildBuildPreview() {
        game.assets.buildPreview = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
        game.assets.buildPreview.getRenderStates().hasLighting(true);
    }

}
