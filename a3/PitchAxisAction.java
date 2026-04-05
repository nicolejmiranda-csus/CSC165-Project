package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PitchAxisAction extends AbstractInputAction {
    private static final float DEADZONE = 0.20f;
    private MyGame game;

    // Inversion matches common controller feel where pushing up should look up.
    private float invert = -1f;

    public PitchAxisAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Deadzone removes stick noise so pitch does not drift if enabled again.
        float v = e.getValue() * invert;
        if (v > -DEADZONE && v < DEADZONE)
            return;

        game.doPitch();
    }
}