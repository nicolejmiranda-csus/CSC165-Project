package a2;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class YawAxisAction extends AbstractInputAction
{
    private MyGame game;
    private float deadzone = 0.20f;

    public YawAxisAction(MyGame g) { game = g; }

    @Override
    public void performAction(float time, Event e)
    {
        // Deadzone removes small stick noise so yaw does not drift.
        float v = e.getValue();
        if (java.lang.Math.abs(v) < deadzone) return;

        // Stick sign is inverted so turning direction matches the keyboard mapping.
        game.doYaw(-v, time);
    }
}