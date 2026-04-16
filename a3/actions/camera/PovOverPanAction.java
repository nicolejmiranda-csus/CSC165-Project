package a3.actions.camera;

import a3.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PovOverPanAction extends AbstractInputAction {
    private MyGame game;

    // Speed is separated so overhead movement can be tuned without changing POV
    // decoding.
    private float panSpeed = 18.0f;

    public PovOverPanAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        float v = e.getValue();

        // POV values are discrete floats, so direct comparisons are used to decode
        // direction.
        if (v == 0.0f)
            return;

        float dx = 0f;
        float dz = 0f;

        // Each diagonal is treated as both directions so panning feels natural.
        if (v == 0.25f || v == 0.125f || v == 0.375f)
            dz = -1f;
        if (v == 0.75f || v == 0.625f || v == 0.875f)
            dz = +1f;
        if (v == 1.0f || v == 0.875f || v == 0.125f)
            dx = -1f;
        if (v == 0.5f || v == 0.375f || v == 0.625f)
            dx = +1f;

        if (dx == 0f && dz == 0f)
            return;

        game.panOverhead(dx * panSpeed * time, dz * panSpeed * time);
    }
}
