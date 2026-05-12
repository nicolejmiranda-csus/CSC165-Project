package running_Dead;

/**
 * State machine phases for Smiling Man.
 * Splitting still, fake idle, stare warning, chase, and attack makes the horror behavior readable.
 * Connected to: Used by MyGameSmilingManSystem to branch monster behavior.
 */
enum MyGameSmilingManPhase {
    HIDDEN,
    WANDER_IDLE,
    WANDER_WALK,
    FAKE_IDLE,
    GATHER_TARGETS,
    TARGET_WAVE,
    RUN_TRANSITION,
    RUN_INITIAL,
    CHASE,
    HIT_OUTRO,
    HIT_STAND,
    HIT_WAVE,
    HIT_RETRANSITION,
    HIT_REINITIAL,
    REMOTE_CONTROLLED
}
