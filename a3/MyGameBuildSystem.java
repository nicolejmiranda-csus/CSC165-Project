package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

public class MyGameBuildSystem {
    private final MyGame game;

    public MyGameBuildSystem(MyGame game) {
        this.game = game;
    }

    public void toggleBuildMode() {
        if (game.state.lost || game.state.won) return;
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
        game.state.buildPieceType = (game.state.buildPieceType + 1) % 2;
        game.hudSystem.showEvent(game.state.buildPieceType == 0 ? "BUILD PIECE: WALL" : "BUILD PIECE: ROOF", 0.8);
    }

    public void updateBuildPreview() {
        if (!game.state.buildMode || game.state.lost || game.state.won) {
            game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
            return;
        }
        Vector3f p = getBuildPiecePosition();
        if (game.state.buildPieceType == 0) game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        else game.assets.buildPreview.setLocalScale((new Matrix4f()).scaling(1.414f, game.state.buildWallHalfH, 1f));
        game.assets.buildPreview.setLocalRotation(getBuildPieceRotation());
        game.assets.buildPreview.setLocalTranslation((new Matrix4f()).translation(p));
    }

    public void placeBuildWall() {
        if (!game.state.buildMode || game.state.lost || game.state.won) return;
        Vector3f p = getBuildPiecePosition();
        String key = buildPieceKey(p);
        if (game.state.wallMap.containsKey(key)) {
            game.hudSystem.showEvent("PIECE ALREADY THERE", 1.0);
            return;
        }
        GameObject piece = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
        if (game.state.buildPieceType == 0) piece.setLocalScale((new Matrix4f()).scaling(game.state.buildWallHalfW, game.state.buildWallHalfH, 1f));
        else piece.setLocalScale((new Matrix4f()).scaling(1.414f, game.state.buildWallHalfH, 1f));
        piece.setLocalRotation(getBuildPieceRotation());
        piece.setLocalTranslation((new Matrix4f()).translation(p));
        piece.getRenderStates().hasLighting(true);
        game.state.placedWalls.add(piece);
        game.state.wallMap.put(key, piece);
        if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendBuildMessage(game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir, p);
        game.hudSystem.showEvent(game.state.buildPieceType == 0 ? "WALL PLACED" : "ROOF PLACED", 0.8);
    }

    public void removeBuildWall() {
        if (!game.state.buildMode || game.state.lost || game.state.won) return;
        Vector3f p = getBuildPiecePosition();
        String key = buildPieceKey(p);
        GameObject piece = game.state.wallMap.get(key);
        if (piece == null) {
            game.hudSystem.showEvent("NO PIECE THERE", 0.8);
            return;
        }
        piece.getRenderStates().disableRendering();
        game.state.placedWalls.remove(piece);
        game.state.wallMap.remove(key);
        if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendRemoveBuildMessage(game.state.buildPieceType, game.state.buildModeType, game.state.buildRoofDir, p);
        game.hudSystem.showEvent("PIECE REMOVED", 0.8);
    }

    public void applyRemoteBuild(int pieceType, int modeType, int roofDir, Vector3f p) {
        String key = pieceType == 0 ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType) : String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
        if (game.state.wallMap.containsKey(key)) return;
        GameObject piece = new GameObject(GameObject.root(), game.assets.quadS, game.assets.homeTx);
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
    }

    public void applyRemoteRemoveBuild(int pieceType, int modeType, int roofDir, Vector3f p) {
        String key = pieceType == 0 ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType) : String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
        GameObject piece = game.state.wallMap.get(key);
        if (piece != null) {
            piece.getRenderStates().disableRendering();
            game.state.placedWalls.remove(piece);
            game.state.wallMap.remove(key);
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
        Vector3f fwd = game.photoSystem.avatarForward();
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
            y = game.state.buildHeightLevel * game.state.buildGrid + game.state.buildGrid + roofLift;
            switch (game.state.buildRoofDir) {
                case 0: x += roofInset; break;
                case 1: x -= roofInset; break;
                case 2: z += roofInset; break;
                case 3: z -= roofInset; break;
            }
        }
        return new Vector3f(x, y, z);
    }

    private String buildPieceKey(Vector3f p) {
        return game.state.buildPieceType == 0 ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, game.state.buildModeType) : String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, game.state.buildRoofDir);
    }
}
