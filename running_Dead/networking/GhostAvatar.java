package running_Dead.networking;

import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.GameObject;
import tage.Light;
import tage.ObjShape;
import tage.TextureImage;
import tage.shapes.AnimatedShape;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

/**
 * Remote player avatar plus the extra multiplayer state TAGE GameObject does not store.
 * It remembers role, name, held item, flashlight direction, and animation state for another client.
 * Connected to: Created by GhostManager; updated by ProtocolClient and read by HUD/item/lighting code.
 */
public class GhostAvatar extends GameObject {
	private static final float AVATAR_ANIM_SPEED = 0.5f;

	private final UUID uuid;
	private final String avatarType;
	private final String playerName;
	private final AnimatedShape animatedShape;
	private String activeAnimation = "";
	GameObject heldItem;
	int heldItemType = 0;
	boolean heldFlashlightOn = false;
	Light flashlightSpotlight;
	Vector3f flashlightDirection = new Vector3f(0f, 0f, -1f);

	public GhostAvatar(UUID id, String type, String name, ObjShape s, TextureImage t, Vector3f p) {
		super(GameObject.root(), s, t);
		uuid = id;
		avatarType = type;
		playerName = cleanName(name);
		animatedShape = s instanceof AnimatedShape ? (AnimatedShape) s : null;
		setPosition(p);
	}

	private String cleanName(String name) {
		if (name == null) return "Player";
		String cleaned = name.trim();
		return cleaned.isEmpty() ? "Player" : cleaned;
	}

	public UUID getID() {
		return uuid;
	}

	public String getAvatarType() {
		return avatarType;
	}

	public String getPlayerName() {
		return playerName;
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

	public void playAnimationState(String animationName) {
		if (animatedShape == null || animationName == null || animationName.equals(activeAnimation)) return;
		if (animatedShape.getAnimation(animationName) == null) return;
		animatedShape.stopAnimation();
		animatedShape.playAnimation(animationName, AVATAR_ANIM_SPEED, AnimatedShape.EndType.LOOP, 0);
		activeAnimation = animationName;
	}

	public void updateAnimation() {
		if (animatedShape != null) animatedShape.updateAnimation();
	}
}
