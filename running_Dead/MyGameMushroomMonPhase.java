package running_Dead;

/**
 * State machine phases for Mushroom Mon.
 * The enemy stays cheap while dormant and only becomes dangerous during chase/explosion phases.
 * Connected to: Used by MyGameMushroomMonSystem to branch enemy behavior.
 */
enum MyGameMushroomMonPhase {
    HIDDEN,
    WANDER,
    CHASE,
    EXPLODED,
    REMOTE_CONTROLLED
}
