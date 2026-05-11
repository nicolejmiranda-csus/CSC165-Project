package running_Dead;

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
