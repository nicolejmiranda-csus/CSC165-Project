package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ElevateAxisAction extends AbstractInputAction {
    private MyGame game;
    private float deadzone = 0.20f;

    public ElevateAxisAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Deadzone removes small stick noise so the camera does not drift.
        float v = e.getValue();
        if (java.lang.Math.abs(v) < deadzone)
            return;

        game.doElevate(v, time);
    }
}
