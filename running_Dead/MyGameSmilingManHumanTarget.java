package running_Dead;

import java.util.UUID;

import org.joml.Vector3f;

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
