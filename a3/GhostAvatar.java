package a3;

import java.util.UUID;

import tage.*;
import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject {
	private UUID uuid;
	private String avatarType;

	public GhostAvatar(UUID id, String type, ObjShape s, TextureImage t, Vector3f p) {
		super(GameObject.root(), s, t);
		uuid = id;
		avatarType = type;
		setPosition(p);
	}

	public UUID getID() {
		return uuid;
	}

	public String getAvatarType() {
		return avatarType;
	}

	public void setPosition(Vector3f m) {
		setLocalLocation(m);
	}

	public Vector3f getPosition() {
		return getWorldLocation();
	}

	public void setYaw(float yawDegrees) {
		setLocalRotation((new Matrix4f()).rotationY((float) java.lang.Math.toRadians(yawDegrees)));
	}
}
