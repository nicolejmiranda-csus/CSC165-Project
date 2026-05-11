package running_Dead;

import org.joml.Vector3f;

class MyGameSceneryBoundsBuilder {
    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
    int count = 0;

    void include(Vector3f p) {
        if (p.x < minX) minX = p.x;
        if (p.y < minY) minY = p.y;
        if (p.z < minZ) minZ = p.z;
        if (p.x > maxX) maxX = p.x;
        if (p.y > maxY) maxY = p.y;
        if (p.z > maxZ) maxZ = p.z;
        count++;
    }

    MyGameSceneryBounds toBounds() {
        if (count == 0) return null;
        return new MyGameSceneryBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
