package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class OrbitAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public OrbitAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Keyboard orbit uses a fixed direction so Q and E can share the same class.
        float input = dir;

        // Axis support is kept available by using direction 0 and reading the event
        // value.
        if (e != null && dir == 0f)
            input = e.getValue();

        game.doOrbit(input, time);
    }
}