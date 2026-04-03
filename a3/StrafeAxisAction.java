package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class StrafeAxisAction extends AbstractInputAction {
    private MyGame game;
    private float deadzone = 0.20f;

    public StrafeAxisAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Deadzone removes small stick noise so strafing does not drift.
        float v = e.getValue();
        if (java.lang.Math.abs(v) < deadzone)
            return;

        // Axis value is stored and later applied once per frame in MyGame.
        game.setPadStrafe(v);
    }
}