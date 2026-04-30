package a3;

import org.joml.Vector3f;
import tage.GameObject;
import tage.physics.PhysicsObject;

class MyGameBuildRecord {
    final String key;
    final GameObject object;
    final PhysicsObject body;
    final int pieceType;
    final int modeType;
    final int roofDir;
    final Vector3f position;
    final boolean blocksPlayer;
    int hits = 0;

    MyGameBuildRecord(String key, GameObject object, PhysicsObject body, int pieceType, int modeType, int roofDir, Vector3f position) {
        this.key = key;
        this.object = object;
        this.body = body;
        this.pieceType = pieceType;
        this.modeType = modeType;
        this.roofDir = roofDir;
        this.position = new Vector3f(position);
        this.blocksPlayer = pieceType == 0 && (modeType == 0 || modeType == 1);
    }
}
