package running_Dead;

import org.joml.Vector3f;

class MyGameBuildMeta {
    final String key;
    final int pieceType;
    final int modeType;
    final int roofDir;
    final int materialType;
    final float terrainOffsetY;
    final Vector3f position;

    MyGameBuildMeta(String key, int pieceType, int modeType, int roofDir, int materialType, Vector3f position) {
        this.key = key;
        this.pieceType = pieceType;
        this.modeType = modeType;
        this.roofDir = roofDir;
        this.materialType = materialType;
        this.terrainOffsetY = position.y;
        this.position = new Vector3f(position);
    }
}
