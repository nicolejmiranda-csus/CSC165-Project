package a3;

import tage.shapes.AnimatedShape;

public class MyGameAnimationSystem {
    private static final String IDLE_ANIM = "IDLE";
    private static final String WALK_ANIM = "WALK";
    private static final String RUN_ANIM = "RUN";
    private static final String ZOMBIE_WALK_ANIM = "ZOMBIE_WALK";
    private static final String ZOMBIE_RUN_ANIM = "ZOMBIE_RUN";
    private static final float AVATAR_ANIM_SPEED = 0.5f;

    private final MyGame game;
    private String activeAvatarAnimation = "";
    private boolean movedThisFrame = false;
    private boolean runningThisFrame = false;

    public MyGameAnimationSystem(MyGame game) {
        this.game = game;
    }

    public void beginInputFrame() {
        movedThisFrame = false;
        runningThisFrame = false;
    }

    public void recordMovement() {
        movedThisFrame = true;
        if (game.state.runHoldTime > 0.0f) {
            runningThisFrame = true;
        }
    }

    public void startIdleIfAnimatedAvatar() {
        if (!hasAnimatedAvatar()) {
            return;
        }
        playAvatarAnimation(IDLE_ANIM);
    }

    public void updateLocalAvatarAnimation() {
        AnimatedShape avatarShape = getAnimatedAvatarShape();
        boolean isZombieModel2 = game.state.localPlayerZombie && game.isPlayerModel2Selected();
        String targetAnimation = IDLE_ANIM;
        if (movedThisFrame) {
            if (isZombieModel2) {
                targetAnimation = runningThisFrame && (avatarShape == null || hasAnimation(avatarShape, ZOMBIE_RUN_ANIM)) ? ZOMBIE_RUN_ANIM : ZOMBIE_WALK_ANIM;
            } else {
                targetAnimation = runningThisFrame && (avatarShape == null || hasAnimation(avatarShape, RUN_ANIM)) ? RUN_ANIM : WALK_ANIM;
            }
        }

        if (avatarShape == null) {
            publishAnimationState(targetAnimation);
            return;
        }

        playAvatarAnimation(targetAnimation);
        avatarShape.updateAnimation();
    }

    public boolean wasMovingThisFrame() {
        return movedThisFrame;
    }

    public boolean wasRunningThisFrame() {
        return runningThisFrame;
    }

    private boolean hasAnimatedAvatar() {
        return getAnimatedAvatarShape() != null;
    }

    private AnimatedShape getAnimatedAvatarShape() {
        if (game.isPlayerModel2Selected() || !game.canUseAnimatedPlayerModel1()) {
            return game.assets.playerModel2AnimatedS;
        }
        return game.assets.playerModel1AnimatedS;
    }

    private void playAvatarAnimation(String animationName) {
        AnimatedShape avatarShape = getAnimatedAvatarShape();
        if (avatarShape == null || animationName.equals(activeAvatarAnimation) || !hasAnimation(avatarShape, animationName)) {
            return;
        }
        avatarShape.stopAnimation();
        avatarShape.playAnimation(animationName, AVATAR_ANIM_SPEED, AnimatedShape.EndType.LOOP, 0);
        publishAnimationState(animationName);
    }

    private void publishAnimationState(String animationName) {
        if (animationName == null || animationName.equals(activeAvatarAnimation)) {
            return;
        }
        activeAvatarAnimation = animationName;
        if (game.state.protClient != null && game.state.isClientConnected) {
            game.state.protClient.sendAnimationMessage(animationName);
        }
    }

    private boolean hasAnimation(AnimatedShape avatarShape, String animationName) {
        return avatarShape.getAnimation(animationName) != null;
    }
}
