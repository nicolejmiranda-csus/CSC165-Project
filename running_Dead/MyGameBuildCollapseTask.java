package running_Dead;

/**
 * Timed removal request for a damaged build piece.
 * The delay lets the break sound/effect finish before the object is hidden and unregistered.
 * Connected to: Created by MyGameBuildSystem after build damage; processed during build-system updates.
 */
class MyGameBuildCollapseTask {
    final String key;
    float timer;

    MyGameBuildCollapseTask(String key, float timer) {
        this.key = key;
        this.timer = timer;
    }
}
