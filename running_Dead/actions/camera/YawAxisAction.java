package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad-axis input wrapper for rotating the player/camera heading.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class YawAxisAction extends AbstractInputAction {
    private MyGame game;
    private float deadzone = 0.20f;

    public YawAxisAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Deadzone removes small stick noise so yaw does not drift.
        float v = e.getValue();
        if (java.lang.Math.abs(v) < deadzone)
            return;

        // Stick sign is inverted so turning direction matches the keyboard mapping.
        game.doYaw(-v, time);
    }
}
