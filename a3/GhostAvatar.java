package a3;

import tage.*;
import tage.shapes.*;
import org.joml.*;
import java.util.UUID;

/**
 * Represents a remote player's avatar in the local 3D scene.
 * Each GhostAvatar is a regular GameObject (so the engine renders it normally)
 * but carries the remote client's UUID so it can be looked up and updated
 * whenever a move packet arrives.
 */
public class GhostAvatar extends GameObject {
    private UUID id; // the remote client's unique identifier

    /**
     * Creates a ghost avatar and places it in the scene.
     * @param id UUID of the remote client this ghost represents
     * @param s  3D shape (ObjShape) to render for this avatar
     * @param t  texture image to apply to the shape
     * @param p  initial world-space position
     */
    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t); // attach to scene root so the engine renders it
        this.id = id;                   // store the remote client's UUID for lookup later
        setLocalLocation(p);            // place the avatar at the remote player's starting position
    }

    /** Returns the remote client's UUID that this ghost represents. */
    public UUID getID() { return id; }

    /** Moves the ghost to the given world-space position (called when a move packet arrives). */
    public void setPosition(Vector3f p) { setLocalLocation(p); }

    /** Returns the ghost's current world-space position. */
    public Vector3f getPosition() { return getWorldLocation(); }
}