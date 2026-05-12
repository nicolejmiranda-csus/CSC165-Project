package running_Dead;

/**
 * Reserved scenery placement circle.
 * The world builder records these spots so generated trees, rocks, tables, and houses do not overlap.
 * Connected to: Created by MyGameWorldBuilder to reserve random scenery and spawn locations.
 */
class MyGameScatterSpot {
    final float x;
    final float z;
    final float radius;

    MyGameScatterSpot(float x, float z, float radius) {
        this.x = x;
        this.z = z;
        this.radius = radius;
    }
}
