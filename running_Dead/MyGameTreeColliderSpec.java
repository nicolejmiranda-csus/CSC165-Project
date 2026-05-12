package running_Dead;

/**
 * Tuned collider dimensions for tree models.
 * Imported trees have different origins and trunk widths, so model-specific specs make collisions feel fair.
 * Connected to: Used by MyGamePhysicsSystem when creating tree colliders from scenery records.
 */
class MyGameTreeColliderSpec {
    final float localCenterX;
    final float localCenterZ;
    final float localRadius;
    final float localHalfHeight;
    final float localMinY;

    MyGameTreeColliderSpec(float localCenterX, float localCenterZ, float localRadius, float localHalfHeight, float localMinY) {
        this.localCenterX = localCenterX;
        this.localCenterZ = localCenterZ;
        this.localRadius = localRadius;
        this.localHalfHeight = localHalfHeight;
        this.localMinY = localMinY;
    }
}
