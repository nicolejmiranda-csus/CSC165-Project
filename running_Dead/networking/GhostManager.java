package running_Dead.networking;

import java.io.IOException;
import java.util.UUID;
import java.util.Vector;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import running_Dead.GameConstants;
import running_Dead.MyGame;
import tage.GameObject;
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
		newAvatar.setDetailTextureImage(game.getGhostDetailTexture(avatarType));
		newAvatar.getRenderStates().setTextureDetailBlend(true);
		newAvatar.getRenderStates().castsShadow(true);
		newAvatar.getRenderStates().receivesShadow(true);
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
			removeHeldItem(ghostAvatar);
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

	public void updateGhostHeldItem(UUID id, int itemType, boolean flashlightOn) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar == null) return;
		itemType = normalizeHeldItemType(itemType);
		ghostAvatar.heldFlashlightOn = flashlightOn;

		if (itemType == GameConstants.ITEM_NONE) {
			removeHeldItem(ghostAvatar);
			return;
		}

		if (ghostAvatar.heldItem == null || ghostAvatar.heldItemType != itemType) {
			removeHeldItem(ghostAvatar);
			ObjShape shape = game.getRemoteHeldItemShape(itemType);
			TextureImage texture = game.getRemoteHeldItemTexture(itemType);
			if (shape == null) return;
			ghostAvatar.heldItem = texture == null
					? new GameObject(ghostAvatar, shape)
					: new GameObject(ghostAvatar, shape, texture);
			ghostAvatar.heldItemType = itemType;
			ghostAvatar.heldItem.propagateRotation(true);
			ghostAvatar.heldItem.applyParentRotationToPosition(true);
			ghostAvatar.heldItem.propagateScale(false);
			game.styleRemoteHeldItem(ghostAvatar.heldItem, itemType);
		}

		ghostAvatar.heldItem.setLocalTranslation(new Matrix4f().translation(game.getRemoteHeldItemOffset(ghostAvatar.getAvatarType(), itemType)));
		ghostAvatar.heldItem.setLocalScale(new Matrix4f().scaling(game.getRemoteHeldItemScale(ghostAvatar.getAvatarType(), itemType)));
		ghostAvatar.heldItem.setLocalRotation(game.getRemoteHeldItemRotation(itemType));
		ghostAvatar.heldItem.getRenderStates().enableRendering();
	}

	private int normalizeHeldItemType(int itemType) {
		if (itemType == GameConstants.ITEM_FLASHLIGHT || itemType == GameConstants.ITEM_POTION
				|| itemType == GameConstants.ITEM_ROCK || itemType == GameConstants.ITEM_BABY_ZOMBIE)
			return itemType;
		return GameConstants.ITEM_NONE;
	}

	private void removeHeldItem(GhostAvatar ghostAvatar) {
		if (ghostAvatar == null || ghostAvatar.heldItem == null) return;
		ghostAvatar.heldItem.getRenderStates().disableRendering();
		MyGame.getEngine().getSceneGraph().removeGameObject(ghostAvatar.heldItem);
		ghostAvatar.heldItem = null;
		ghostAvatar.heldItemType = GameConstants.ITEM_NONE;
		ghostAvatar.heldFlashlightOn = false;
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
