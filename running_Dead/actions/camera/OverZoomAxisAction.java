package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Gamepad-axis input wrapper for overhead map zoom.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class OverZoomAxisAction extends AbstractInputAction {
    private MyGame game;

    private float deadzone = 0.15f;

    // Speed is separated out so trigger zoom can be tuned without changing the
    // math.
    private float zoomSpeed = 35.0f;

    public OverZoomAxisAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        float v = e.getValue();

        // Deadzone removes trigger noise so overhead zoom does not drift.
        if (java.lang.Math.abs(v) < deadzone)
            return;

        // LT reports positive values and RT reports negative values on this controller.
        // dy sign is chosen so pressing LT lowers the camera and pressing RT raises it.
        float dy;

        if (v > 0.0f) {
            dy = -zoomSpeed * v * time;
        } else {
            dy = +zoomSpeed * (-v) * time;
        }

        game.zoomOverhead(dy);
    }
}
