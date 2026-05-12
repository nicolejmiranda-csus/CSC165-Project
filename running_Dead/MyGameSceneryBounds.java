package running_Dead;

/**
 * Approximate footprint and vertical offset for a scenery mesh.
 * These values let the world builder plant models on the terrain even when model origins are not at the base.
 * Connected to: Produced by MyGameSceneryBoundsBuilder and consumed by MyGameWorldBuilder/MyGamePhysicsSystem.
 */
class MyGameSceneryBounds {
    final float minX, minY, minZ, maxX, maxY, maxZ;
    final float width, height, depth;
    final float centerX, centerY, centerZ;

    MyGameSceneryBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        width = maxX - minX;
        height = maxY - minY;
        depth = maxZ - minZ;
        centerX = (minX + maxX) * 0.5f;
        centerY = (minY + maxY) * 0.5f;
        centerZ = (minZ + maxZ) * 0.5f;
    }
}
