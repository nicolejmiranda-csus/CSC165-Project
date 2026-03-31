package a2;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class OverZoomAction extends AbstractInputAction
{
    private MyGame game;
    private float dir;

    public OverZoomAction(MyGame g, float direction)
    {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e)
    {
        // Zoom is applied by changing the overhead camera height.
        float zoomSpeed = 30.0f;

        game.zoomOverhead(dir * zoomSpeed * time);
    }
}