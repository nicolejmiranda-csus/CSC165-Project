package running_Dead.actions.interaction;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * Xbox trigger wrapper for analog LT/RT components.
 * LT and RT are axes instead of buttons, so this class turns a trigger pull into a single gameplay press.
 * Connected to: Created by MyGameInputBinder; forwards trigger input to MyGame gameplay and camera systems.
 */
public class XboxTriggerAction extends AbstractInputAction {
    public static final int LEFT_TRIGGER = 0;
    public static final int RIGHT_TRIGGER = 1;

    private static final float PRESS_THRESHOLD = 0.55f;
    private static final float RELEASE_THRESHOLD = 0.35f;
    private static final float MAP_ZOOM_SPEED = 35.0f;

    private final MyGame game;
    private final int trigger;
    private boolean pressed = false;

    public XboxTriggerAction(MyGame game, int trigger) {
        this.game = game;
        this.trigger = trigger;
    }

    @Override
    public void performAction(float time, Event event) {
        float value = event.getValue();

        // In full-map mode the triggers behave like analog zoom controls.
        if (game.isFullMapMode()) {
            if (value > RELEASE_THRESHOLD) {
                float dir = trigger == LEFT_TRIGGER ? -1f : 1f;
                game.zoomOverhead(dir * MAP_ZOOM_SPEED * value * time);
            }
            pressed = false;
            return;
        }

        boolean nowPressed = value > PRESS_THRESHOLD;
        if (!nowPressed) {
            if (value < RELEASE_THRESHOLD) pressed = false;
            return;
        }
        if (pressed) return;
        pressed = true;

        if (trigger == LEFT_TRIGGER) {
            if (game.isBuildMode()) game.lowerBuildHeight();
            else game.handleFAction();
        } else {
            game.handlePrimaryAction();
        }
    }
}
