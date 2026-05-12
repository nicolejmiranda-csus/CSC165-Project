package running_Dead;

/**
 * Gameplay categories for physics bodies.
 * The physics system uses these values instead of relying on object names or model filenames.
 * Connected to: Used by MyGamePhysicsSystem and collision handlers to classify physics bodies.
 */
enum MyGameBodyKind {
    LOCAL_PLAYER,
    REMOTE_PLAYER,
    DASH_PICKUP,
    INVIS_PICKUP,
    BUILD_PICKUP,
    ROCK_PICKUP,
    BABY_ZOMBIE_PICKUP,
    FLASHLIGHT_PICKUP,
    POTION_PICKUP,
    BUILD_PIECE,
    SCENERY_BLOCKER,
    PROJECTILE,
    TERRAIN
}
