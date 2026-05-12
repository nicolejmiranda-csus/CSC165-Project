package running_Dead;

import org.joml.Vector3f;

/**
 * Estimates scenery bounds from imported model vertices.
 * The computed base offset is why trees and rocks can snap visually to the terrain instead of floating or sinking.
 * Connected to: Called by MyGameWorldBuilder when placing imported scenery meshes.
 */
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
