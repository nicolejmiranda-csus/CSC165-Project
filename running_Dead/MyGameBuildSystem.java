package running_Dead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

/**
 * Handles build mode, preview placement, support rules, material counts, and build networking.
 * Terrain height is sampled from cached terrain data when the OpenGL height call is not safe,
 * which prevents build clicks from crashing on the AWT event thread.
 * Connected to: Owned by MyGame; called by build actions, updater, ProtocolClient, and MyGamePhysicsSystem.
 */
public class MyGameBuildSystem {
    private final MyGame game;
    private final HashMap<String, MyGameBuildMeta> buildMeta = new HashMap<>();
    private final ArrayList<MyGameBuildCollapseTask> collapseTasks = new ArrayList<>();

    public MyGameBuildSystem(MyGame game) {
        this.game = game;
    }

    public void toggleBuildMode() {
        if (game.isMatchOver()) return;
        if (game.state.localPlayerZombie) {
            game.hudSystem.showEvent("ZOMBIES CANNOT BUILD", 1.0);
            return;
        }
        if (totalBuildMaterials() <= 0 && !game.state.buildMode) {
            game.hudSystem.showEvent("PICK UP BUILD MATERIALS FIRST", 1.2);
            return;
        }
        game.state.buildMode = !game.state.buildMode;
        game.hudSystem.showEvent(game.state.buildMode ? "BUILD MODE ON" : "BUILD MODE OFF", 1.0);
    }

    public void raiseBuildHeight() {
        if (!game.state.buildMode) return;
        game.state.buildHeightLevel++;
        game.hudSystem.showEvent("BUILD HEIGHT " + game.state.buildHeightLevel, 0.8);
    }

    public void lowerBuildHeight() {
        if (!game.state.buildMode) return;
        game.state.buildHeightLevel--;
        if (game.state.buildHeightLevel < 0) game.state.buildHeightLevel = 0;
        game.hudSystem.showEvent("BUILD HEIGHT " + game.state.buildHeightLevel, 0.8);
    }

    public void rotateBuildWall() {
        if (!game.state.buildMode) return;
        if (game.state.buildPieceType == 0) {
            game.state.buildModeType = (game.state.buildModeType + 1) % 4;
            String[] names = {"BUILD FRONT WALL", "BUILD SIDE WALL", "BUILD FLOOR", "BUILD CEILING"};
            game.hudSystem.showEvent(names[game.state.buildModeType], 0.8);
        } else {
            game.state.buildRoofDir = (game.state.buildRoofDir + 1) % 4;
            String[] names = {"ROOF +X", "ROOF -X", "ROOF +Z", "ROOF -Z"};
            game.hudSystem.showEvent(names[game.state.buildRoofDir], 0.8);
        }
    }

    public void switchBuildPiece() {
        if (!game.state.buildMode) return;
        game.state.buildPieceType = (game.state.buildPieceType + 1) % 2;
        game.hudSystem.showEvent(game.state.buildPieceType == 0 ? "BUILD PIECE: WALL" : "BUILD PIECE: ROOF", 0.8);
    }

    public void cycleBuildMaterial() {
        if (!game.state.buildMode) return;
        game.state.buildMaterialType = (game.state.buildMaterialType + 1) % 3;
        game.hudSystem.showEvent("BUILD MATERIAL: " + GameConstants.buildMaterialName(game.state.buildMaterialType), 0.8);
    }

    public void updateBuildPreview() {
        if (!game.state.buildMode || game.isMatchOver()) {
            game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
            return;
        }
        Vector3f p = getBuildPiecePosition();
        game.assets.buildPreview.setTextureImage(textureForMaterial(game.state.buildMaterialType));
        if (game.state.buildPieceType == 0) game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        else game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(1.414f, game.state.buildWallHalfH, 1f));
        game.assets.buildPreview.setLocalRotation(getBuildPieceRotation());
        game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(p));
        styleBuildPiece(game.assets.buildPreview, game.state.buildMaterialType, 0.70f);
        game.assets.buildPreview.getRenderStates().castsShadow(false);
    }

    public void update(float dt) {
        updateCollapseTasks(dt);
    }

    public void placeBuildWall() {
        if (!game.state.buildMode || game.isMatchOver()) return;
        if (game.state.localPlayerZombie) {
            game.hudSystem.showEvent("ZOMBIES CANNOT BUILD", 1.0);
            return;
        }
        selectAvailableBuildMaterialIfNeeded();
        if (currentBuildMaterialCount() <= 0) {
            game.hudSystem.showEvent("NO " + GameConstants.buildMaterialName(game.state.buildMaterialType) + " MATERIALS", 1.0);
            return;
        }
        Vector3f p = getBuildPiecePosition();
        String key = buildPieceKey(p);
        if (game.state.wallMap.containsKey(key)) {
            game.hudSystem.showEvent("PIECE ALREADY THERE", 1.0);
            return;
        }
        if (!isSupportedPlacement(p, game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir)) {
            game.hudSystem.showEvent("NEEDS SUPPORT BELOW", 1.0);
            return;
        }
        int materialType = game.state.buildMaterialType;
        GameObject piece = new GameObject(GameObject.root(), game.assets.quadS, textureForMaterial(materialType));
        if (game.state.buildPieceType == 0) piece.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        else piece.setLocalScale((new Matrix4f()).scaling(1.414f, game.state.buildWallHalfH, 1f));
        piece.setLocalRotation(getBuildPieceRotation());
        piece.setLocalTranslation((new Matrix4f()).translation(p));
        styleBuildPiece(piece, materialType, 0.48f);
        game.state.placedWalls.add(piece);
        game.state.wallMap.put(key, piece);
        buildMeta.put(key, new MyGameBuildMeta(key, game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir, materialType, p));
        consumeCurrentBuildMaterial();
        game.physicsSystem.registerBuildPiece(piece, key, game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir, materialType, p);
        if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendBuildMessage(game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir, materialType, p);
        game.smilingManSystem.reportNoise(p, GameConstants.NOISE_BUILD_RADIUS);
        game.soundSystem.playBuildPlace(p);
        game.hudSystem.showEvent(GameConstants.buildMaterialName(materialType) + " " + (game.state.buildPieceType == 0 ? "WALL PLACED" : "ROOF PLACED"), 0.8);
    }

    public void removeBuildWall() {
        if (!game.state.buildMode || game.isMatchOver()) return;
        Vector3f p = getBuildPiecePosition();
        String key = buildPieceKey(p);
        GameObject piece = game.state.wallMap.get(key);
        if (piece == null) {
            game.hudSystem.showEvent("NO PIECE THERE", 0.8);
            return;
        }
        MyGameBuildMeta meta = buildMeta.get(key);
        int materialType = meta == null ? game.state.buildMaterialType : meta.materialType;
        destroyBuildPiece(key, game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir, materialType, p, true);
    }

    public void destroyBuildPiece(String key, int pieceType, int modeType, int roofDir, int materialType, Vector3f p, boolean broadcast) {
        GameObject piece = game.state.wallMap.get(key);
        if (piece == null) return;
        piece.getRenderStates().disableRendering();
        game.state.placedWalls.remove(piece);
        game.state.wallMap.remove(key);
        buildMeta.remove(key);
        removeCollapseTask(key);
        game.physicsSystem.removeBuildPieceCollider(key);
        if (broadcast && game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendRemoveBuildMessage(pieceType, modeType, roofDir, materialType, p);
        game.hudSystem.showEvent("PIECE REMOVED", 0.8);
        scheduleUnsupportedCollapse();
    }

    public void applyRemoteBuild(int pieceType, int modeType, int roofDir, int materialType, Vector3f p) {
        String key = pieceType == 0 ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType) : String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
        if (game.state.wallMap.containsKey(key)) return;
        GameObject piece = new GameObject(GameObject.root(), game.assets.quadS, textureForMaterial(materialType));
        if (pieceType == 0) piece.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        else piece.setLocalScale((new Matrix4f()).scaling(1.414f, game.state.buildWallHalfH, 1f));
        int oldPiece = game.state.buildPieceType, oldMode = game.state.buildModeType, oldRoof = game.state.buildRoofDir;
        game.state.buildPieceType = pieceType;
        game.state.buildModeType = modeType;
        game.state.buildRoofDir = roofDir;
        piece.setLocalRotation(getBuildPieceRotation());
        piece.setLocalTranslation((new Matrix4f()).translation(p));
        piece.getRenderStates().hasLighting(true);
        game.state.buildPieceType = oldPiece;
        game.state.buildModeType = oldMode;
        game.state.buildRoofDir = oldRoof;
        game.state.placedWalls.add(piece);
        game.state.wallMap.put(key, piece);
        styleBuildPiece(piece, materialType, 0.48f);
        buildMeta.put(key, new MyGameBuildMeta(key, pieceType, modeType, roofDir, materialType, p));
        game.physicsSystem.registerBuildPiece(piece, key, pieceType, modeType, roofDir, materialType, p);
        game.smilingManSystem.reportNoise(p, GameConstants.NOISE_BUILD_RADIUS);
        game.soundSystem.playBuildPlace(p);
    }

    public void applyRemoteRemoveBuild(int pieceType, int modeType, int roofDir, int materialType, Vector3f p) {
        String key = pieceType == 0 ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType) : String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
        destroyBuildPiece(key, pieceType, modeType, roofDir, materialType, p, false);
    }

    private void updateCollapseTasks(float dt) {
        if (collapseTasks.isEmpty()) return;

        ArrayList<MyGameBuildCollapseTask> due = new ArrayList<>();
        for (MyGameBuildCollapseTask task : collapseTasks) {
            task.timer -= dt;
            if (task.timer <= 0f) due.add(task);
        }
        collapseTasks.removeAll(due);

        for (MyGameBuildCollapseTask task : due) {
            MyGameBuildMeta meta = buildMeta.get(task.key);
            if (meta == null) continue;
            destroyBuildPiece(meta.key, meta.pieceType, meta.modeType, meta.roofDir, meta.materialType, meta.position, true);
        }
    }

    private void scheduleUnsupportedCollapse() {
        ArrayList<MyGameBuildMeta> unsupported = findUnsupportedPieces();
        if (unsupported.isEmpty()) return;

        unsupported.sort((a, b) -> Float.compare(a.position.y, b.position.y));
        float delay = 2.0f;
        boolean queuedAny = false;
        for (MyGameBuildMeta meta : unsupported) {
            if (collapseTaskQueued(meta.key)) continue;
            collapseTasks.add(new MyGameBuildCollapseTask(meta.key, delay));
            delay += 2.0f;
            queuedAny = true;
        }
        if (queuedAny) game.hudSystem.showEvent("UNSUPPORTED BUILD COLLAPSING", 1.2);
    }

    private ArrayList<MyGameBuildMeta> findUnsupportedPieces() {
        HashSet<String> stable = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (MyGameBuildMeta meta : buildMeta.values()) {
                if (stable.contains(meta.key)) continue;
                if (isGroundSupported(meta.position) || hasStableSupport(meta, stable)) {
                    stable.add(meta.key);
                    changed = true;
                }
            }
        }

        ArrayList<MyGameBuildMeta> unsupported = new ArrayList<>();
        for (MyGameBuildMeta meta : buildMeta.values()) {
            if (!stable.contains(meta.key)) unsupported.add(meta);
        }
        return unsupported;
    }

    private boolean isSupportedPlacement(Vector3f p, int pieceType, int modeType, int roofDir) {
        if (isSupportedByCurrentPlayerGround(p)) return true;
        if (isGroundSupported(p)) return true;
        for (MyGameBuildMeta meta : buildMeta.values()) {
            if (isSupportUnder(meta, p)) return true;
        }
        return false;
    }

    private boolean hasStableSupport(MyGameBuildMeta meta, HashSet<String> stable) {
        for (MyGameBuildMeta other : buildMeta.values()) {
            if (!stable.contains(other.key)) continue;
            if (isSupportUnder(other, meta.position)) return true;
        }
        return false;
    }

    private boolean isGroundSupported(Vector3f p) {
        return (p.y - buildTerrainHeight(p.x, p.z)) <= game.state.buildGrid + 0.05f;
    }

    private boolean isSupportedByCurrentPlayerGround(Vector3f p) {
        return (p.y - currentBuildBaseHeight()) <= game.state.buildGrid + 0.05f;
    }

    private boolean isSupportUnder(MyGameBuildMeta support, Vector3f p) {
        if (support == null || support.position.y >= p.y - 0.20f) return false;
        float dx = support.position.x - p.x;
        float dz = support.position.z - p.z;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        if (horizontal > game.state.buildGrid * 1.08f) return false;
        float vertical = p.y - support.position.y;
        return vertical <= game.state.buildGrid + 0.60f;
    }

    private boolean collapseTaskQueued(String key) {
        for (MyGameBuildCollapseTask task : collapseTasks) {
            if (task.key.equals(key)) return true;
        }
        return false;
    }

    private void removeCollapseTask(String key) {
        collapseTasks.removeIf(task -> task.key.equals(key));
    }

    private int totalBuildMaterials() {
        return game.state.buildMaterials + game.state.metalBuildMaterials + game.state.glassBuildMaterials;
    }

    private int currentBuildMaterialCount() {
        if (game.state.buildMaterialType == GameConstants.BUILD_MATERIAL_METAL) return game.state.metalBuildMaterials;
        if (game.state.buildMaterialType == GameConstants.BUILD_MATERIAL_GLASS) return game.state.glassBuildMaterials;
        return game.state.buildMaterials;
    }

    private void consumeCurrentBuildMaterial() {
        if (game.state.buildMaterialType == GameConstants.BUILD_MATERIAL_METAL) game.state.metalBuildMaterials--;
        else if (game.state.buildMaterialType == GameConstants.BUILD_MATERIAL_GLASS) game.state.glassBuildMaterials--;
        else game.state.buildMaterials--;
    }

    private void selectAvailableBuildMaterialIfNeeded() {
        if (currentBuildMaterialCount() > 0) return;
        if (game.state.buildMaterials > 0) game.state.buildMaterialType = GameConstants.BUILD_MATERIAL_WOOD;
        else if (game.state.metalBuildMaterials > 0) game.state.buildMaterialType = GameConstants.BUILD_MATERIAL_METAL;
        else if (game.state.glassBuildMaterials > 0) game.state.buildMaterialType = GameConstants.BUILD_MATERIAL_GLASS;
    }

    private TextureImage textureForMaterial(int materialType) {
        if (materialType == GameConstants.BUILD_MATERIAL_METAL) return game.assets.metalBuildTx;
        if (materialType == GameConstants.BUILD_MATERIAL_GLASS) return game.assets.glassBuildTx;
        return game.assets.woodBlockTx;
    }

    private void styleBuildPiece(GameObject piece, int materialType, float glassOpacity) {
        if (piece == null) return;
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

    private Matrix4f getBuildPieceRotation() {
        if (game.state.buildPieceType == 0) {
            switch (game.state.buildModeType) {
                case 0: return new Matrix4f().rotationY(0f);
                case 1: return new Matrix4f().rotationY((float) Math.toRadians(90.0f));
                case 2: return new Matrix4f().rotationX((float) Math.toRadians(-90.0f));
                case 3: return new Matrix4f().rotationX((float) Math.toRadians(90.0f));
                default: return new Matrix4f().identity();
            }
        }
        float roofAngle = (float) Math.toRadians(45.0f);
        switch (game.state.buildRoofDir) {
            case 0: return new Matrix4f().rotationZ(roofAngle).rotateX((float) Math.toRadians(90.0f));
            case 1: return new Matrix4f().rotationZ(-roofAngle).rotateX((float) Math.toRadians(90.0f));
            case 2: return new Matrix4f().rotationY((float) Math.toRadians(90.0f)).rotateZ(roofAngle).rotateX((float) Math.toRadians(90.0f));
            case 3: return new Matrix4f().rotationY((float) Math.toRadians(90.0f)).rotateZ(-roofAngle).rotateX((float) Math.toRadians(90.0f));
            default: return new Matrix4f().identity();
        }
    }

    private float snap(float v, float step) { return Math.round(v / step) * step; }
    private float snapToCellCenter(float v, float step) { return (Math.round(v / step - 0.5f) + 0.5f) * step; }

    private Vector3f getBuildPiecePosition() {
        Vector3f pos = game.assets.avatar.getWorldLocation();
        Vector3f fwd = game.avatarForward();
        fwd.y = 0f;
        if (fwd.lengthSquared() < 0.0001f) fwd.set(0f, 0f, -1f);
        fwd.normalize();
        Vector3f t = new Vector3f(pos).add(new Vector3f(fwd).mul(game.state.buildDistance));
        float x, y, z;
        if (game.state.buildPieceType == 0) {
            float wallOffset = 0.25f;
            switch (game.state.buildModeType) {
                case 0:
                    x = snapToCellCenter(t.x, game.state.buildSnap);
                    z = snap(t.z, game.state.buildSnap) + (fwd.z >= 0 ? wallOffset : -wallOffset);
                    y = game.state.buildHeightLevel * game.state.buildGrid + game.state.buildWallHalfH;
                    break;
                case 1:
                    x = snap(t.x, game.state.buildSnap) + (fwd.x >= 0 ? wallOffset : -wallOffset);
                    z = snapToCellCenter(t.z, game.state.buildSnap);
                    y = game.state.buildHeightLevel * game.state.buildGrid + game.state.buildWallHalfH;
                    break;
                case 2:
                    x = snapToCellCenter(t.x, game.state.buildSnap);
                    z = snapToCellCenter(t.z, game.state.buildSnap);
                    y = game.state.buildHeightLevel * game.state.buildGrid;
                    break;
                case 3:
                    x = snapToCellCenter(t.x, game.state.buildSnap);
                    z = snapToCellCenter(t.z, game.state.buildSnap);
                    y = game.state.buildHeightLevel * game.state.buildGrid + game.state.buildGrid;
                    break;
                default:
                    x = snapToCellCenter(t.x, game.state.buildSnap);
                    z = snapToCellCenter(t.z, game.state.buildSnap);
                    y = game.state.buildHeightLevel * game.state.buildGrid + game.state.buildWallHalfH;
            }
        } else {
            float roofInset = game.state.buildGrid * 0.5f;
            float roofLift = game.state.buildGrid * 0.5f;
            x = snapToCellCenter(t.x, game.state.buildSnap);
            z = snapToCellCenter(t.z, game.state.buildSnap);
            y = game.state.buildHeightLevel * game.state.buildGrid + roofLift;
            switch (game.state.buildRoofDir) {
                case 0: x += roofInset; break;
                case 1: x -= roofInset; break;
                case 2: z += roofInset; break;
                case 3: z -= roofInset; break;
            }
        }
        // Build previews follow the player's current terrain y so walls do not float when the player climbs a hill.
        return new Vector3f(x, currentBuildBaseHeight() + y, z);
    }

    private String buildPieceKey(Vector3f p) {
        return game.state.buildPieceType == 0 ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, game.state.buildModeType) : String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, game.state.buildRoofDir);
    }

    private float buildTerrainHeight(float x, float z) {
        // Mouse clicks arrive on the AWT thread, where TAGE's OpenGL height lookup has no active context.
        // The CPU height-map path gives the same terrain y without touching OpenGL.
        return game.worldBuilder.terrainHeightCpu(x, z);
    }

    private float currentBuildBaseHeight() {
        if (game.assets.avatar == null) return 0f;
        return game.assets.avatar.getWorldLocation().y;
    }
}

