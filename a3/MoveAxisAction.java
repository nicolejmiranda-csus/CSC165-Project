package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class MoveAxisAction extends AbstractInputAction {
    private MyGame game;
    private float deadzone = 0.20f;

    public MoveAxisAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Deadzone removes small stick noise so the dolphin does not creep forward.
        float v = e.getValue();
        if (java.lang.Math.abs(v) < deadzone)
            return;

        // Many controllers report pushing up as a negative value, so the sign is
        // flipped.
        game.setPadMove(-v);
    }
}