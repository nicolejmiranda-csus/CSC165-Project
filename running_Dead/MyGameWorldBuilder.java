package running_Dead;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

/**
 * Builds terrain, scenery, cover, enemies, avatar, axes, and the build preview.
 * Random scenery uses deterministic seeds plus scatter spots so clients get a dense world without overlaps.
 * Connected to: Called by MyGame.buildObjects; creates objects later used by physics, AI, items, HUD, and lighting.
 */
public class MyGameWorldBuilder {
    private final MyGame game;
    private final Random worldRandom = new Random(1652026L);
    private final Random spawnRandom = new Random();
    private final ArrayList<MyGameScatterSpot> scatterSpots = new ArrayList<>();
    private BufferedImage cachedHeightMapImage;
    private String cachedHeightMapFile;
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
        buildMushroomMons();
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
        a.avatar.setDetailTextureImage(game.isPlayerModel2Selected() ? a.playerModel2FarTx : a.playerModel1FarTx);
        a.avatar.getRenderStates().setTextureDetailBlend(true);
        a.avatar.getRenderStates().castsShadow(true);
        a.avatar.getRenderStates().receivesShadow(true);
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
            smilingMan.setDetailTextureImage(a.smilingManFarTx);
            smilingMan.getRenderStates().setTextureDetailBlend(true);
            smilingMan.getRenderStates().castsShadow(true);
            smilingMan.getRenderStates().receivesShadow(true);
            smilingMan.getRenderStates().setModelOrientationCorrection((new Matrix4f()).identity());
            smilingMan.getRenderStates().disableRendering();
            a.smilingMen.add(smilingMan);
            if (i == 0) a.smilingMan = smilingMan;
        }
    }

    private void buildMushroomMons() {
        MyGameAssets a = game.assets;
        if (a.mushMonAnimatedS == null || a.mushroomMonTx == null) return;
        a.mushMons.clear();
        for (int i = 0; i < MyGameMushroomMonSystem.TOTAL_COUNT; i++) {
            GameObject obj = new GameObject(GameObject.root(), a.mushMonAnimatedS, a.mushroomMonTx);
            obj.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
            obj.setLocalScale((new Matrix4f()).scaling(GameConstants.MUSHROOM_MON_SCALE));
            obj.setLocalRotation((new Matrix4f()).identity());
            obj.getRenderStates().hasLighting(true);
            obj.setDetailTextureImage(a.mushroomMonFarTx);
            obj.getRenderStates().setTextureDetailBlend(true);
            obj.getRenderStates().castsShadow(true);
            obj.getRenderStates().receivesShadow(true);
            obj.getRenderStates().setModelOrientationCorrection((new Matrix4f()).identity());
            obj.getRenderStates().disableRendering();
            a.mushMons.add(obj);
        }
    }

    private void buildTableCovers() {
        MyGameAssets a = game.assets;
        a.tableCovers.clear();
        for (int i = 0; i < GameConstants.TABLE_COVER_COUNT; i++) {
            float scale = randomRange(0.48f, 0.62f);
            Vector3f p = randomTerrainLocation(plantedLift(a.tableS, scale, 0.02f), 16.0f, 5.5f);
            if (p == null) continue;
            GameObject table = new GameObject(GameObject.root(), a.tableS, a.tableTx);
            table.setLocalTranslation((new Matrix4f()).translation(p));
            table.setLocalScale((new Matrix4f()).scaling(scale));
            table.setLocalRotation((new Matrix4f()).rotationY(randomRadians()));
            table.getRenderStates().hasLighting(true);
            table.setNormalMap(a.tableNormalTx);
            table.setDetailTextureImage(a.tableFarTx);
            table.getRenderStates().setNormalMapping(true);
            table.getRenderStates().setTextureDetailBlend(true);
            table.getRenderStates().setSurfaceScale(0.65f);
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
        a.terrain.getRenderStates().setBumpMapping(true);
        a.terrain.getRenderStates().setBumpStyle(1);
        a.terrain.setNormalMap(a.grassNormalTx);
        a.terrain.getRenderStates().setNormalMapping(true);
        a.terrain.getRenderStates().setTextureDetailBlend(true);
        a.terrain.getRenderStates().setSurfaceScale(0.55f);
        a.terrain.setDetailTextureImage(a.grassFarTx);
        a.terrain.getRenderStates().castsShadow(false);
        a.terrain.getRenderStates().receivesShadow(true);
    }

    private void buildSceneryProps() {
        buildRockScenery(280);
        buildTreeScenery(300);
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
            float clearance = index >= 4 ? 5.5f + scale * 3.2f : 2.3f + scale * 1.6f;
            Vector3f p = randomTerrainLocation(plantedLift(a.sceneryRockS[index], scale, 0.01f), 9.0f, clearance);
            if (p == null) continue;
            rock.setLocalTranslation((new Matrix4f()).translation(p));
            rock.setLocalScale((new Matrix4f()).scaling(scale));
            rock.setLocalRotation((new Matrix4f()).rotationY(randomRadians()));
            rock.getRenderStates().hasLighting(true);
            rock.getRenderStates().setBumpMapping(true);
            rock.setNormalMap(a.rockNormalTx);
            rock.getRenderStates().setNormalMapping(true);
            rock.getRenderStates().setTextureMappingMode(1);
            rock.getRenderStates().setTextureDetailBlend(true);
            rock.getRenderStates().setSurfaceScale(0.85f);
            rock.setDetailTextureImage(a.rockFarTx);
            rock.getRenderStates().castsShadow(true);
            rock.getRenderStates().receivesShadow(true);
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
            float scale = index >= 10 ? randomRange(1.75f, 2.55f) : randomRange(1.25f, 1.85f);
            float clearance = index >= 10 ? 6.2f + scale * 3.0f : 7.6f + scale * 4.0f;
            Vector3f p = randomTerrainLocation(plantedLift(a.sceneryTreeS[index], scale, 0.01f), 14.0f, clearance);
            if (p == null) continue;
            tree.setLocalTranslation((new Matrix4f()).translation(p));
            tree.setLocalScale((new Matrix4f()).scaling(scale));
            tree.setLocalRotation((new Matrix4f()).rotationY(randomRadians()));
            tree.getRenderStates().hasLighting(true);
            tree.setDetailTextureImage(a.leafFarTx);
            tree.setNormalMap(a.leafNormalTx);
            tree.getRenderStates().setTextureDetailBlend(true);
            tree.getRenderStates().setNormalMapping(true);
            tree.getRenderStates().setSurfaceScale(0.9f);
            tree.getRenderStates().castsShadow(true);
            tree.getRenderStates().receivesShadow(true);
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
        // Random scenery is placed using terrain height plus the model's base lift so the visible mesh sits on the ground.
        // Each accepted spot is reserved to keep big props from spawning inside one another.
        for (int i = 0; i < 220; i++) {
            float x = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            float z = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            if ((x * x + z * z) < minCenterDistance * minCenterDistance) continue;
            if (Math.abs(x) < 8.0f && Math.abs(z) < 10.0f) continue;
            if (!isScatterSpotOpen(x, z, clearanceRadius)) continue;
            reserveScatterSpot(x, z, clearanceRadius);
            return new Vector3f(x, terrainHeight(x, z) + lift, z);
        }
        // If the dense forest/rocks fill too much of the map, try again with smaller spacing rather than giving up early.
        float relaxedClearance = clearanceRadius * 0.65f;
        for (int i = 0; i < 120; i++) {
            float x = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            float z = randomRange(-WORLD_SCATTER_RADIUS, WORLD_SCATTER_RADIUS);
            if ((x * x + z * z) < minCenterDistance * minCenterDistance) continue;
            if (Math.abs(x) < 8.0f && Math.abs(z) < 10.0f) continue;
            if (!isScatterSpotOpen(x, z, relaxedClearance)) continue;
            reserveScatterSpot(x, z, relaxedClearance);
            return new Vector3f(x, terrainHeight(x, z) + lift, z);
        }
        return null;
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
        return terrainHeight(x, z);
    }

    private float terrainHeight(float x, float z) {
        if (game.assets.terrain == null || game.assets.heightMaptx == null) return 0f;
        BufferedImage heightMap = loadCpuHeightMap(game.assets.heightMaptx);
        if (heightMap == null) return 0f;

        // This mirrors TerrainPlane's height-map sampling on the CPU for code that runs before or outside OpenGL display.
        Matrix4f terrainScale = game.assets.terrain.getLocalScale();
        float terrainWidth = Math.max(0.001f, terrainScale.m00());
        float heightScale = terrainScale.m11();
        float u = clamp01((x / terrainWidth + 1.0f) * 0.5f);
        float v = clamp01(1.0f - (z / terrainWidth + 1.0f) * 0.5f);
        return heightScale * sampleHeightMap(heightMap, u, v);
    }

    public float terrainHeightCpu(float x, float z) {
        return terrainHeight(x, z);
    }

    private BufferedImage loadCpuHeightMap(TextureImage heightMap) {
        String file = heightMap.getTextureFile();
        if (file == null || file.isEmpty()) return null;
        if (cachedHeightMapImage != null && file.equals(cachedHeightMapFile)) return cachedHeightMapImage;
        try {
            cachedHeightMapImage = ImageIO.read(new File(file));
            cachedHeightMapFile = file;
        } catch (Exception e) {
            cachedHeightMapImage = null;
            cachedHeightMapFile = null;
        }
        return cachedHeightMapImage;
    }

    private float sampleHeightMap(BufferedImage image, float u, float v) {
        int maxX = image.getWidth() - 1;
        int maxY = image.getHeight() - 1;
        if (maxX <= 0 || maxY <= 0) return 0f;

        float px = u * maxX;
        float py = v * maxY;
        int x0 = (int) Math.floor(px);
        int y0 = (int) Math.floor(py);
        int x1 = Math.min(maxX, x0 + 1);
        int y1 = Math.min(maxY, y0 + 1);
        float tx = px - x0;
        float ty = py - y0;

        float h00 = red(image.getRGB(x0, y0));
        float h10 = red(image.getRGB(x1, y0));
        float h01 = red(image.getRGB(x0, y1));
        float h11 = red(image.getRGB(x1, y1));
        float top = lerp(h00, h10, tx);
        float bottom = lerp(h01, h11, tx);
        return lerp(top, bottom, ty);
    }

    private float red(int argb) {
        return ((argb >> 16) & 0xFF) / 255.0f;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public void selectRoundHeightMap(UUID zombieId, long roundToken) {
        MyGameAssets a = game.assets;
        if (a.heightMapTextures == null || a.heightMapTextures.length == 0 || a.terrain == null) return;
        int index = chooseHeightMapIndex(zombieId, roundToken, a.heightMapTextures.length);
        if (index < 0 || index >= a.heightMapTextures.length) index = 0;
        if (a.heightMaptx == a.heightMapTextures[index] && a.activeHeightMapIndex == index) return;
        a.activeHeightMapIndex = index;
        a.heightMaptx = a.heightMapTextures[index];
        a.terrain.setHeightMap(a.heightMaptx);
        cachedHeightMapImage = null;
        cachedHeightMapFile = null;
        game.physicsSystem.rebuildTerrainPhysics();
        game.itemSystem.requestEnvironmentReplant();
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
            if (base == null) continue;
            buildHome(base, randomRadians());
        }
        buildRandomWallCover(GameConstants.RANDOM_WALL_COVER_COUNT);
        buildRandomRoofCover(GameConstants.RANDOM_ROOF_COVER_COUNT);
    }

    private Vector3f randomTerrainBase(float minCenterDistance, float clearanceRadius) {
        Vector3f p = randomTerrainLocation(0f, minCenterDistance, clearanceRadius);
        if (p == null) return null;
        p.y = 0f;
        return p;
    }

    private void buildRandomWallCover(int count) {
        for (int i = 0; i < count; i++) {
            Vector3f base = randomTerrainBase(18.0f, 7.0f);
            if (base == null) continue;
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
            if (base == null) continue;
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
        float upperY = lowerY + game.state.buildGrid + 0.12f;
        float centerStep = run * 0.99f;
        addCoverRoof(base, yaw, 0f, lowerY, 0f, roofDir, material, halfSlant, halfDepth);
        addCoverRoof(base, yaw, 0f, upperY, -centerStep * direction, roofDir, material, halfSlant, halfDepth);
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
        piece.getRenderStates().setBumpMapping(false);
        piece.getRenderStates().setNormalMapping(false);
        piece.getRenderStates().isEnvironmentMapped(false);
        piece.getRenderStates().castsShadow(true);
        piece.getRenderStates().receivesShadow(true);
        piece.getRenderStates().setTextureMappingMode(0);
        piece.getRenderStates().setSurfaceScale(1.0f);
        piece.getRenderStates().setTextureDetailBlend(true);
        piece.setNormalMap(null);
        if (materialType == GameConstants.BUILD_MATERIAL_GLASS) {
            piece.getRenderStates().isTransparent(true);
            piece.getRenderStates().setOpacity(glassOpacity);
            piece.getRenderStates().isEnvironmentMapped(true);
            piece.getRenderStates().castsShadow(false);
            piece.setDetailTextureImage(game.assets.glassFarTx);
            piece.setNormalMap(game.assets.glassNormalTx);
            piece.getRenderStates().setNormalMapping(true);
        } else {
            piece.getRenderStates().isTransparent(false);
            piece.getRenderStates().setOpacity(1.0f);
            if (materialType == GameConstants.BUILD_MATERIAL_METAL) {
                piece.getRenderStates().isEnvironmentMapped(true);
                piece.getRenderStates().setSurfaceScale(0.8f);
                piece.setDetailTextureImage(game.assets.metalFarTx);
                piece.setNormalMap(game.assets.metalNormalTx);
                piece.getRenderStates().setNormalMapping(true);
            } else {
                piece.setNormalMap(game.assets.woodNormalTx);
                piece.setDetailTextureImage(game.assets.woodFarTx);
                piece.getRenderStates().setNormalMapping(true);
                piece.getRenderStates().setSurfaceScale(0.75f);
            }
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
        game.assets.buildPreview.getRenderStates().castsShadow(false);
    }

}
