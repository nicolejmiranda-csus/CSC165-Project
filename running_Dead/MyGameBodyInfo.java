package running_Dead;

import java.util.UUID;

/**
 * Physics metadata attached to a TAGE GameObject.
 * This lets collision handling distinguish trees, rocks, builds, enemies, and projectiles.
 * Connected to: Created and read by MyGamePhysicsSystem when a Bullet body maps back to gameplay.
 */
class MyGameBodyInfo {
    final MyGameBodyKind kind;
    UUID remoteId;
    MyGamePickupRecord pickup;
    MyGameBuildRecord buildRecord;
    MyGameSceneryRecord sceneryRecord;
    MyGameProjectileRecord projectile;

    MyGameBodyInfo(MyGameBodyKind kind) {
        this.kind = kind;
    }

    static MyGameBodyInfo remotePlayer(UUID id) {
        MyGameBodyInfo info = new MyGameBodyInfo(MyGameBodyKind.REMOTE_PLAYER);
        info.remoteId = id;
        return info;
    }

    static MyGameBodyInfo pickup(MyGameBodyKind kind, MyGamePickupRecord pickup) {
        MyGameBodyInfo info = new MyGameBodyInfo(kind);
        info.pickup = pickup;
        return info;
    }

    static MyGameBodyInfo buildPiece(MyGameBuildRecord record) {
        MyGameBodyInfo info = new MyGameBodyInfo(MyGameBodyKind.BUILD_PIECE);
        info.buildRecord = record;
        return info;
    }

    static MyGameBodyInfo scenery(MyGameSceneryRecord record) {
        MyGameBodyInfo info = new MyGameBodyInfo(MyGameBodyKind.SCENERY_BLOCKER);
        info.sceneryRecord = record;
        return info;
    }

    static MyGameBodyInfo projectile(MyGameProjectileRecord projectile) {
        MyGameBodyInfo info = new MyGameBodyInfo(MyGameBodyKind.PROJECTILE);
        info.projectile = projectile;
        return info;
    }
}
