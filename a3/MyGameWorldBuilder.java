package a3;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

public class MyGameWorldBuilder {
    private final MyGame game;
    private final Random worldRandom = new Random(1652026L);
    private final Random spawnRandom = new Random();
    private final ArrayList<MyGameScatterSpot> scatterSpots = new ArrayList<>();
    private static final float WORLD_SCATTER_RADIUS = GameConstants.WORLD_EDGE_LIMIT - 10.0f;
    private static final float PLAYER_SPAWN_RADIUS = GameConstants.WORLD_EDGE_LIMIT - 22.0f;

    public MyGameWorldBuilder(MyGame game) {
        this.game = game;
    }

    public void buildObjects() {
        buildTerrain();
        buildSceneryProps();
        buildTableCovers();
        buildCoverStructures();
        buildLocalAvatar();
        buildSmilingMan();
        buildAxes();
        buildBuildPreview();
    }

    private void buildLocalAvatar() {
        MyGameAssets a = game.assets;
        MyGameState s = game.state;
        boolean useAnimatedPlayer1 = !game.isPlayerModel2Selected() && game.canUseAnimatedPlayerModel1();
        boolean useAnimatedPlayer2 = !game.isPlayerModel1Selected() && game.canUseAnimatedPlayerModel2();
        ObjShape avatarShape = game.isPlayerModel2Selected()
            ? (useAnimatedPlayer2 ? a.playerModel2AnimatedS : a.playerModel2S)
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

    private void buildSmilingMan() {
        MyGameAssets a = game.assets;
        if (a.smilingManAnimatedShapes == null || a.smilingManTx == null) return;
        a.smilingMen.clear();
        for (int i = 0; i < a.smilingManAnimatedShapes.length; i++) {
            if (a.smilingManAnimatedShapes[i] == null) continue;
            GameObject smilingMan = new GameObject(GameObject.root(), a.smilingManAnimatedShapes[i], a.smilingManTx);
            smilingMan.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
            smilingMan.setLocalScale((new Matrix4f()).scaling(GameConstants.SMILING_MAN_SCALE));
            smilingMan.setLocalRotation((new Matrix4f()).identity());
            smilingMan.getRenderStates().hasLighting(true);
            smilingMan.getRenderStates().setModelOrientationCorrection((new Matrix4f()).identity());
            smilingMan.getRenderStates().disableRendering();
            a.smilingMen.add(smilingMan);
            if (i == 0) a.smilingMan = smilingMan;
        }
    }

    private void buildTableCovers() {
        MyGameAssets a = game.assets;
        a.tableCovers.clear();
        for (int i = 0; i < GameConstants.TABLE_COVER_COUNT; i++) {
            float scale = randomRange(0.48f, 0.62f);
            Vector3f p = randomTerrainLocation(0f, 16.0f, 5.5f);
            p.y = plantedLift(a.tableS, scale, 0.02f);
            GameObject table = new GameObject(GameObject.root(), a.tableS, a.tableTx);
            table.setLocalTranslation((new Matrix4f()).translation(p));
            table.setLocalScale((new Matrix4f()).scaling(scale));
            table.setLocalRotation((new Matrix4f()).rotationY(randomRadians()));
            table.getRenderStates().hasLighting(true);
            a.tableCovers.add(table);
            if (i == 0) a.table = table;
        }
    }

    private void buildTerrain() {
        MyGameAssets a = game.assets;
        a.terrain = new GameObject(GameObject.root(), a.terrainShape, a.grassTx);
        a.terrain.setLocalTranslation(new Matrix4f().translation(0, 0, 0));
        a.terrain.setLocalScale(new Matrix4f().scaling(GameConstants.WORLD_TERRAIN_SIZE, 20f, GameConstants.WORLD_TERRAIN_SIZE));
        a.terrain.setHeightMap(a.heightMaptx);
        a.terrain.getRenderStates().hasLighting(true);
        a.terrain.getRenderStates().setTiling(1);
        a.terrain.getRenderStates().setTileFactor(150);
    }

    private void buildSceneryProps() {
        buildRockScenery(150);
        buildTreeScenery(145);
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
            float scale = index >= 10 ? randomRange(1.25f, 1.85f) : randomRange(0.85f, 1.25f);
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
            float needed = spot.radius + 2.0f;
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

    public void selectRoundHeightMap(UUID zombieId, long roundToken) {
        MyGameAssets a = game.assets;
        if (a.heightMapTextures == null || a.heightMapTextures.length == 0 || a.terrain == null) return;
        int index = chooseHeightMapIndex(zombieId, roundToken, a.heightMapTextures.length);
        if (index < 0 || index >= a.heightMapTextures.length) index = 0;
        a.activeHeightMapIndex = index;
        a.heightMaptx = a.heightMapTextures[index];
        a.terrain.setHeightMap(a.heightMaptx);
        game.physicsSystem.rebuildTerrainPhysics();
        game.itemSystem.replantEnvironmentForCurrentTerrain();
        game.hudSystem.showEvent("MAP " + (index + 1), 1.0);
    }

    private int chooseHeightMapIndex(UUID zombieId, long roundToken, int count) {
        long seed = roundToken == 0L ? System.currentTimeMillis() : roundToken;
        if (zombieId != null) {
            seed ^= zombieId.getMostSignificantBits();
            seed = Long.rotateLeft(seed, 19) ^ zombieId.getLeastSignificantBits();
        }
        seed ^= 0x1652026A3L;
        seed ^= (seed >>> 33);
        return (int) Math.floorMod(seed, count);
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

    private void buildCoverStructures() {
        game.assets.staticCoverBuilds.clear();
        for (int i = 0; i < GameConstants.RANDOM_HOUSE_COUNT; i++) {
            Vector3f base = randomTerrainBase(24.0f, 14.0f);
            buildHome(base, randomRadians());
        }
        buildRandomWallCover(GameConstants.RANDOM_WALL_COVER_COUNT);
        buildRandomRoofCover(GameConstants.RANDOM_ROOF_COVER_COUNT);
    }

    private Vector3f randomTerrainBase(float minCenterDistance, float clearanceRadius) {
        Vector3f p = randomTerrainLocation(0f, minCenterDistance, clearanceRadius);
        p.y = 0f;
        return p;
    }

    private void buildRandomWallCover(int count) {
        for (int i = 0; i < count; i++) {
            Vector3f base = randomTerrainBase(18.0f, 7.0f);
            float yaw = randomRadians();
            int pattern = worldRandom.nextInt(3);
            int material = randomBuildMaterial();
            if (pattern == 0) {
                addCoverWall(base, yaw, 0f, 1.15f, 0f, 0f, material, randomRange(1.0f, 1.8f), 1.15f);
            } else if (pattern == 1) {
                float half = 2.2f;
                addCoverWall(base, yaw, 0f, 1.15f, -half, 0f, material, 2.2f, 1.15f);
                addCoverWall(base, yaw, 0f, 1.15f, half, (float) Math.toRadians(180f), material, 2.2f, 1.15f);
                addCoverWall(base, yaw, -half, 1.15f, 0f, (float) Math.toRadians(90f), material, 2.2f, 1.15f);
                addCoverWall(base, yaw, half, 1.15f, 0f, (float) Math.toRadians(-90f), material, 2.2f, 1.15f);
            } else {
                float halfH = 1.0f;
                float localYaw = randomRange(-0.35f, 0.35f);
                for (int layer = 0; layer < 3; layer++) {
                    addCoverWall(base, yaw, 0f, halfH + layer * halfH * 2.0f, 0f, localYaw,
                            material, 1.2f, halfH);
                }
            }
        }
    }

    private void buildRandomRoofCover(int count) {
        for (int i = 0; i < count; i++) {
            Vector3f base = randomTerrainBase(18.0f, 7.0f);
            float yaw = randomRadians();
            int material = randomBuildMaterial();
            int pattern = worldRandom.nextInt(3);
            int roofDir = worldRandom.nextInt(4);
            float halfSlant = randomRange(1.35f, 1.85f);
            float halfDepth = randomRange(1.15f, 1.85f);

            if (pattern == 0) {
                addCoverRoof(base, yaw, 0f, 0.95f, 0f, roofDir, material, halfSlant, halfDepth);
            } else if (pattern == 1) {
                addCoverRoof(base, yaw, -1.25f, 0.95f, 0f, roofDir, material, halfSlant, halfDepth);
                addCoverRoof(base, yaw, 1.25f, 0.95f, 0f, roofDir, material, halfSlant, halfDepth);
            } else {
                addRoofStairPair(base, yaw, material);
            }
        }
    }

    private void addRoofStairPair(Vector3f base, float yaw, int material) {
        int roofDir = worldRandom.nextBoolean() ? 2 : 3;
        float direction = roofDir == 2 ? 1f : -1f;
        float halfSlant = 1.45f;
        float halfDepth = 1.45f;
        float roofAngle = (float) Math.toRadians(45.0f);
        float run = halfSlant * (float) Math.cos(roofAngle) * 2.0f;
        float rise = halfSlant * (float) Math.sin(roofAngle) * 2.0f;
        float lowerY = rise * 0.5f + 0.05f;
        float upperY = lowerY + rise;
        float centerStep = run * 0.98f;
        addCoverRoof(base, yaw, 0f, lowerY, -centerStep * 0.5f * direction, roofDir, material, halfSlant, halfDepth);
        addCoverRoof(base, yaw, 0f, upperY, centerStep * 0.5f * direction, roofDir, material, halfSlant, halfDepth);
    }

    private void buildHome(Vector3f base, float yaw) {
        float s = GameConstants.HOME_SCALE;
        float w = GameConstants.HOME_W * s;
        float h = GameConstants.HOME_H * s;
        float d = GameConstants.HOME_D * s;
        float floorY = GameConstants.HOME_FLOOR_Y;
        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;

        int material = randomBuildMaterial();
        buildStaticCoverPiece(0, 2, 0, material, transformLocal(base, yaw, 0f, floorY, 0f),
                coverRotation(yaw, (new Matrix4f()).rotationX((float) Math.toRadians(-90f))),
                (new Matrix4f()).scaling(hw, hd, 1f));
        buildStaticCoverPiece(0, 0, 0, material, transformLocal(base, yaw, 0f, floorY + hh, -hd),
                coverRotation(yaw, null), (new Matrix4f()).scaling(hw, hh, 1f));
        buildStaticCoverPiece(0, 1, 0, material, transformLocal(base, yaw, -hw, floorY + hh, 0f),
                coverRotation(yaw, (new Matrix4f()).rotationY((float) Math.toRadians(90f))),
                (new Matrix4f()).scaling(hd, hh, 1f));
        buildStaticCoverPiece(0, 1, 0, material, transformLocal(base, yaw, hw, floorY + hh, 0f),
                coverRotation(yaw, (new Matrix4f()).rotationY((float) Math.toRadians(-90f))),
                (new Matrix4f()).scaling(hd, hh, 1f));

        float doorW = 2.6f * s;
        float sideW = (w - doorW) / 2f;
        float hSideW = sideW / 2f;
        Matrix4f frontRotation = (new Matrix4f()).rotationY((float) Math.toRadians(180f));
        buildStaticCoverPiece(0, 0, 0, material, transformLocal(base, yaw, -(doorW / 2f + hSideW), floorY + hh, hd),
                coverRotation(yaw, frontRotation), (new Matrix4f()).scaling(hSideW, hh, 1f));
        buildStaticCoverPiece(0, 0, 0, material, transformLocal(base, yaw, (doorW / 2f + hSideW), floorY + hh, hd),
                coverRotation(yaw, frontRotation), (new Matrix4f()).scaling(hSideW, hh, 1f));
        buildHomeRoof(base, yaw, w, d, floorY + h, material);
    }

    private void buildHomeRoof(Vector3f base, float yaw, float w, float d, float wallTopY, int materialType) {
        float halfW = w / 2f;
        float halfD = d / 2f;
        float roofAngle = (float) Math.toRadians(45f);
        float roofRise = (float) Math.tan(roofAngle) * halfW;
        float slantLen = halfW / (float) Math.cos(roofAngle);
        float roofHalfSlant = slantLen / 2f;
        buildStaticCoverPiece(1, 0, 0, materialType, transformLocal(base, yaw, -halfW / 2f, wallTopY + roofRise / 2f, 0f),
                coverRotation(yaw, (new Matrix4f()).rotationZ(roofAngle).rotateX((float) Math.toRadians(90f))),
                (new Matrix4f()).scaling(roofHalfSlant, halfD, 1f));
        buildStaticCoverPiece(1, 0, 1, materialType, transformLocal(base, yaw, halfW / 2f, wallTopY + roofRise / 2f, 0f),
                coverRotation(yaw, (new Matrix4f()).rotationZ(-roofAngle).rotateX((float) Math.toRadians(90f))),
                (new Matrix4f()).scaling(roofHalfSlant, halfD, 1f));
    }

    private void addCoverWall(Vector3f base, float yaw, float localX, float localY, float localZ,
                              float localYaw, int materialType, float halfW, float halfH) {
        buildStaticCoverPiece(0, 0, 0, materialType, transformLocal(base, yaw, localX, localY, localZ),
                coverRotation(yaw, (new Matrix4f()).rotationY(localYaw)), (new Matrix4f()).scaling(halfW, halfH, 1f));
    }

    private void addCoverRoof(Vector3f base, float yaw, float localX, float localY, float localZ,
                              int roofDir, int materialType, float halfSlant, float halfDepth) {
        buildStaticCoverPiece(1, 0, roofDir, materialType, transformLocal(base, yaw, localX, localY, localZ),
                coverRotation(yaw, roofRotation(roofDir)), (new Matrix4f()).scaling(halfSlant, halfDepth, 1f));
    }

    private Matrix4f roofRotation(int roofDir) {
        float roofAngle = (float) Math.toRadians(45.0f);
        switch (roofDir) {
            case 0: return new Matrix4f().rotationZ(roofAngle).rotateX((float) Math.toRadians(90.0f));
            case 1: return new Matrix4f().rotationZ(-roofAngle).rotateX((float) Math.toRadians(90.0f));
            case 2: return new Matrix4f().rotationY((float) Math.toRadians(90.0f)).rotateZ(roofAngle).rotateX((float) Math.toRadians(90.0f));
            case 3: return new Matrix4f().rotationY((float) Math.toRadians(90.0f)).rotateZ(-roofAngle).rotateX((float) Math.toRadians(90.0f));
            default: return new Matrix4f().identity();
        }
    }

    private void buildStaticCoverPiece(int pieceType, int modeType, int roofDir, int materialType,
                                       Vector3f position, Matrix4f rotation, Matrix4f scale) {
        String key = staticBuildKey(position, pieceType, modeType, roofDir);
        if (game.state.wallMap.containsKey(key)) return;
        GameObject piece = new GameObject(GameObject.root(), game.assets.quadS, textureForMaterial(materialType));
        piece.setLocalScale(scale);
        piece.setLocalRotation(rotation == null ? (new Matrix4f()).identity() : rotation);
        piece.setLocalTranslation((new Matrix4f()).translation(position));
        styleCoverPiece(piece, materialType, 0.48f);
        game.state.placedWalls.add(piece);
        game.state.wallMap.put(key, piece);
        game.assets.staticCoverBuilds.add(new MyGameBuildMeta(key, pieceType, modeType, roofDir, materialType, position));
    }

    private String staticBuildKey(Vector3f p, int pieceType, int modeType, int roofDir) {
        if (pieceType == 0) return String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType);
        return String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
    }

    private Matrix4f coverRotation(float yaw, Matrix4f localRotation) {
        Matrix4f rot = new Matrix4f().rotationY(yaw);
        if (localRotation != null) rot.mul(localRotation);
        return rot;
    }

    private Vector3f transformLocal(Vector3f base, float yaw, float x, float y, float z) {
        float cos = (float) Math.cos(yaw);
        float sin = (float) Math.sin(yaw);
        return new Vector3f(base.x + x * cos + z * sin, base.y + y, base.z - x * sin + z * cos);
    }

    private int randomBuildMaterial() {
        return worldRandom.nextInt(3);
    }

    private TextureImage textureForMaterial(int materialType) {
        if (materialType == GameConstants.BUILD_MATERIAL_METAL) return game.assets.metalBuildTx;
        if (materialType == GameConstants.BUILD_MATERIAL_GLASS) return game.assets.glassBuildTx;
        return game.assets.woodBlockTx;
    }

    private void styleCoverPiece(GameObject piece, int materialType, float glassOpacity) {
        piece.getRenderStates().hasLighting(true);
        piece.getRenderStates().setHasSolidColor(false);
        if (materialType == GameConstants.BUILD_MATERIAL_GLASS) {
            piece.getRenderStates().isTransparent(true);
            piece.getRenderStates().setOpacity(glassOpacity);
        } else {
            piece.getRenderStates().isTransparent(false);
            piece.getRenderStates().setOpacity(1.0f);
        }
    }

    public boolean isInsideTableCover(Vector3f position) {
        if (position == null) return false;
        for (GameObject table : game.assets.tableCovers) {
            if (isInsideObjectFootprint(table, position, 0.30f)) return true;
        }
        return false;
    }

    private boolean isInsideObjectFootprint(GameObject object, Vector3f position, float margin) {
        if (object == null || object.getShape() == null || object.getShape().getVertices() == null) return false;
        MyGameSceneryBounds bounds = getObjectBounds(object);
        if (bounds == null) return false;
        float scale = object.getWorldScale().m00();
        Vector3f local = new Vector3f(position).sub(object.getWorldLocation());
        Matrix4f invRot = new Matrix4f(object.getWorldRotation()).invert();
        local.mulDirection(invRot);
        return local.x >= bounds.minX * scale - margin && local.x <= bounds.maxX * scale + margin
                && local.z >= bounds.minZ * scale - margin && local.z <= bounds.maxZ * scale + margin;
    }

    private MyGameSceneryBounds getObjectBounds(GameObject object) {
        float[] vertices = object.getShape().getVertices();
        if (vertices == null || vertices.length < 3) return null;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 3) {
            minX = Math.min(minX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            minZ = Math.min(minZ, vertices[i + 2]);
            maxX = Math.max(maxX, vertices[i]);
            maxY = Math.max(maxY, vertices[i + 1]);
            maxZ = Math.max(maxZ, vertices[i + 2]);
        }
        return new MyGameSceneryBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void buildBuildPreview() {
        game.assets.buildPreview = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
        game.assets.buildPreview.getRenderStates().hasLighting(true);
    }

}
