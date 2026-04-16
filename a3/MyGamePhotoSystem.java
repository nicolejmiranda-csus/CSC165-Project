package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tage.*;

public class MyGamePhotoSystem {
    private final MyGame game;

    public MyGamePhotoSystem(MyGame game) {
        this.game = game;
    }

    public Vector3f avatarForward() {
        return (new Vector3f(game.assets.avatar.getWorldForwardVector())).normalize();
    }

    public float distanceToHome() {
        return game.assets.avatar.getWorldLocation().distance(new Vector3f(0f, 0f, 0f));
    }

    public float pyramidCollisionRadius(int i) {
        float s = game.assets.pyramids[i].getLocalScale().m00();
        return (s * GameConstants.PYR_COLLIDER_MULT) + GameConstants.PYR_COLLIDER_PAD;
    }

    private float pyramidRadius(int i) {
        return game.assets.pyramids[i].getLocalScale().m00();
    }

    public int closestPyramidWithinRange() {
        Vector3f avatarLoc = game.assets.avatar.getWorldLocation();
        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (game.state.iconCollected[i]) continue;
            float dist = avatarLoc.distance(game.assets.pyramids[i].getWorldLocation());
            float ok = pyramidRadius(i) + 7.0f;
            if (dist < ok && dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    public void checkCrash() {
        if (game.state.lost || game.state.won) return;
        if (game.state.damageCooldown > 0.0) return;
        Vector3f avatarLoc = game.assets.avatar.getWorldLocation();
        for (int i = 0; i < 3; i++) {
            float dist = avatarLoc.distance(game.assets.pyramids[i].getWorldLocation());
            if (dist < pyramidCollisionRadius(i)) {
                game.itemSystem.damagePlayer(game.state.pyramidDamage);
                game.state.damageCooldown = game.state.damageCooldownTime;
                game.hudSystem.showEvent("TOOK DAMAGE -" + game.state.pyramidDamage, 1.0);
                if (game.state.health <= 0) {
                    game.state.lost = true;
                    game.hudSystem.showEvent("OUT OF HEALTH", 2.0);
                }
                return;
            }
        }
    }

    public void tryTakePhoto() {
        if (game.state.lost || game.state.won) return;
        int idx = closestPyramidWithinRange();
        if (idx == -1) {
            game.hudSystem.showEvent("NOT CLOSE ENOUGH", 1.0);
            return;
        }
        if (game.state.iconCollected[idx]) {
            game.hudSystem.showEvent("ALREADY PHOTOGRAPHED", 1.0);
            return;
        }
        game.state.iconCollected[idx] = true;
        game.state.score++;
        game.hudSystem.showEvent("PICTURE TAKEN", 1.0);
        game.assets.hudIcons[idx].setLocalScale(game.getCollectedHudIconScale());
        game.assets.photoSpin.addTarget(game.assets.pyramids[idx]);
        game.assets.activatedPyramidBob.addTarget(game.assets.hudIcons[idx]);
        if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendPhotoMessage(idx);
        if (game.state.score == 3) game.hudSystem.showEvent("ALL PHOTOS TAKEN  RETURN HOME", 2.0);
    }

    public void tryPlacePhotos() {
        if (game.state.lost || game.state.won) return;
        if (game.state.score < 3) {
            game.hudSystem.showEvent("GET ALL 3 PHOTOS FIRST", 1.5);
            return;
        }
        if (distanceToHome() > game.state.homeRange) {
            game.hudSystem.showEvent("RETURN HOME TO PLACE PHOTOS", 1.5);
            return;
        }
        placePhotosOnWall();
        game.state.won = true;
        if (game.state.protClient != null && game.state.isClientConnected) game.state.protClient.sendPlacePhotosMessage();
        game.assets.activatedPyramidBob.disable();
        game.hudSystem.showEvent("PHOTOS PLACED", 2.0);
    }

    public void applyRemotePhoto(int idx) {
        if (idx < 0 || idx >= 3 || game.state.iconCollected[idx]) return;
        game.state.iconCollected[idx] = true;
        game.state.score++;
        game.assets.hudIcons[idx].setLocalScale(game.getCollectedHudIconScale());
        game.assets.photoSpin.addTarget(game.assets.pyramids[idx]);
        game.assets.activatedPyramidBob.addTarget(game.assets.hudIcons[idx]);
        game.hudSystem.showEvent("REMOTE PLAYER TOOK PHOTO", 1.5);
    }

    public void applyRemotePlacePhotos() {
        if (game.state.won) return;
        placePhotosOnWall();
        game.state.won = true;
        game.assets.activatedPyramidBob.disable();
        game.hudSystem.showEvent("REMOTE PLAYER PLACED PHOTOS", 2.0);
    }

    private void placePhotosOnWall() {
        float s = GameConstants.HOME_SCALE;
        float w = GameConstants.HOME_W * s;
        float h = GameConstants.HOME_H * s;
        float d = GameConstants.HOME_D * s;
        float spacing = w * 0.30f;
        float[] x = new float[] { -spacing, 0.0f, spacing };
        float wallZ = -(d / 2f) + 0.02f;
        float wallY = GameConstants.HOME_FLOOR_Y + (h / 2f);
        for (int i = 0; i < 3; i++) {
            if (!game.state.iconCollected[i]) continue;
            game.assets.hudIcons[i].setParent(GameObject.root());
            game.assets.hudIcons[i].getRenderStates().hasLighting(true);
            game.assets.hudIcons[i].setLocalScale((new Matrix4f()).scaling(1.0f, 0.7f, 1f));
            game.assets.hudIcons[i].setLocalTranslation((new Matrix4f()).translation(x[i], wallY, wallZ));
            game.assets.hudIcons[i].setLocalRotation((new Matrix4f()).identity());
        }
    }
}
