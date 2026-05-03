package a3.networking;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import a3.MyGame;
import tage.ObjShape;
import tage.TextureImage;

public class GhostManager {
	private final MyGame game;
	private final Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(MyGame game) {
		this.game = game;
	}

	public void createGhostAvatar(UUID id, String avatarType, Vector3f position, float yaw) throws IOException {
		System.out.println("adding ghost with ID --> " + id);

		if (findAvatar(id) != null)
			return;

		ObjShape s = game.createGhostShape(id, avatarType);
		TextureImage t = game.getGhostTexture(avatarType);

		GhostAvatar newAvatar = new GhostAvatar(id, avatarType, s, t, position);
		Matrix4f initialScale = game.getGhostScale(avatarType);
		newAvatar.setLocalScale(initialScale);
		newAvatar.setYaw(yaw);
		newAvatar.playAnimationState(game.getRemoteAnimationState(id));

		ghostAvatars.add(newAvatar);
	}

	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			game.removeRemotePhysicsFor(id);
			MyGame.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
			game.releaseGhostShape(id);
		} else {
			System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id) {
		for (GhostAvatar ghostAvatar : ghostAvatars) {
			if (ghostAvatar.getID().compareTo(id) == 0)
				return ghostAvatar;
		}
		return null;
	}

	public GhostAvatar getGhostAvatar(UUID id) {
		return findAvatar(id);
	}

	public Vector<GhostAvatar> getGhostAvatarsSnapshot() {
		return new Vector<GhostAvatar>(ghostAvatars);
	}

	public void updateGhostAvatar(UUID id, Vector3f position, float yaw) {
		GhostAvatar ghostAvatar = findAvatar(id);

		if (ghostAvatar != null) {
			ghostAvatar.setPosition(position);
			ghostAvatar.setYaw(yaw);
		} else {
			System.out.println("tried to update ghost avatar, but unable to find ghost in list");
		}
	}

	public void applyGhostAnimation(UUID id, String animationName) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) ghostAvatar.playAnimationState(animationName);
	}

	public void updateGhostAnimations() {
		for (GhostAvatar ghostAvatar : ghostAvatars) {
			ghostAvatar.updateAnimation();
		}
	}
}
