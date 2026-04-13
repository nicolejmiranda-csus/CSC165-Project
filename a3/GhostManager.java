package a3;

import tage.*;
import tage.shapes.*;
import org.joml.*;
import java.util.*;
import java.io.IOException;

/**
 * Manages the collection of GhostAvatars — the 3D representations of all
 * other players currently connected to the same game session.
 *
 * The game calls createGhost() when a remote player joins, updateGhostAvatar()
 * each time that player moves, and removeGhostAvatar() when they disconnect.
 */
public class GhostManager {
    private MyGame game;                                                    // reference to the game for scene/shape/texture lookups
    private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();   // list of all currently active ghost avatars

    /**
     * @param vfrg the running game instance; used to fetch shapes, textures, and the scene graph
     */
    public GhostManager(MyGame vfrg) {
        game = vfrg;
    }

    /**
     * Spawns a new ghost avatar in the scene for a remote player who just joined.
     * @param id         UUID of the remote client
     * @param p          starting world-space position
     * @param avatarName "player" or "boy" — selects which shape and texture to use
     */
    public void createGhost(UUID id, Vector3f p, String avatarName) throws IOException {
        ObjShape s = game.getGhostShape(avatarName);     // look up the correct 3D shape for this avatar type
        TextureImage t = game.getGhostTexture(avatarName); // look up the matching texture
        GhostAvatar newAvatar = new GhostAvatar(id, s, t, p); // create the scene object at the remote player's position
        Matrix4f initialScale = (new Matrix4f()).scaling(1.0f); // scale ghosts down (they use the same raw model as local player)
        newAvatar.setLocalScale(initialScale);
        ghostAvatars.add(newAvatar); // register in the managed list so we can find it by UUID later
    }

    /**
     * Removes a ghost from the scene and the internal list when a remote player disconnects.
     * @param id UUID of the remote client who left
     */
    public void removeGhostAvatar(UUID id) {
        GhostAvatar ghostAv = findAvatar(id);                               // locate the ghost by UUID
        if (ghostAv != null) {
            game.getEngine().getSceneGraph().removeGameObject(ghostAv);     // detach from scene so the engine stops rendering it
            ghostAvatars.remove(ghostAv);                                    // remove from our tracking list
        } else {
            System.out.println("unable to find ghost in list"); // UUID not found — likely a duplicate bye packet
        }
    }

    /**
     * Updates a ghost avatar's position when a move packet arrives from that client.
     * @param id       UUID of the remote client who moved
     * @param position new world-space position
     */
    public void updateGhostAvatar(UUID id, Vector3f position) {
        GhostAvatar ghostAv = findAvatar(id);                               // locate the ghost by UUID
        if (ghostAv != null)
            ghostAv.setPosition(position);                                  // move the ghost to the new position
        else
            System.out.println("unable to find ghost in list");             // UUID not found — move arrived before create?
    }

    /**
     * Linear search through the ghost list to find the avatar with the matching UUID.
     * @param id UUID to search for
     * @return the matching GhostAvatar, or null if not found
     */
    private GhostAvatar findAvatar(UUID id) {
        Iterator<GhostAvatar> it = ghostAvatars.iterator();
        while (it.hasNext()) {
            GhostAvatar ga = it.next();
            if (ga.getID().compareTo(id) == 0) // UUID.compareTo returns 0 when equal
                return ga;
        }
        return null; // no match found
    }
}