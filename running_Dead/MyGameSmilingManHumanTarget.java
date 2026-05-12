package running_Dead;

import java.util.UUID;

import org.joml.Vector3f;

/**
 * Simplified target snapshot for Smiling Man detection.
 * The AI can use the same logic for the local human and remote humans without depending on player internals.
 * Connected to: Built by MyGameSmilingManSystem from local and ghost player state.
 */
class MyGameSmilingManHumanTarget {
    final UUID id;
    final Vector3f position;
    final boolean local;
    final boolean hidingInTable;

    MyGameSmilingManHumanTarget(UUID id, Vector3f position, boolean local, boolean hidingInTable) {
        this.id = id;
        this.position = new Vector3f(position);
        this.local = local;
        this.hidingInTable = hidingInTable;
    }
}
