package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ZoomAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public ZoomAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Keyboard zoom uses a fixed direction, with optional axis support if direction
        // is set to 0.
        float input = dir;

        if (e != null && dir == 0f)
            input = e.getValue();

        game.doZoom(input, time);
    }
}