package a2;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PitchAction extends AbstractInputAction
{
    private MyGame game;
    private float dir;

    public PitchAction(MyGame g, float direction)
    {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e)
    {
        // Pitch direction is stored for reuse, even though pitch is disabled for A2 movement.
        // Keeping the action class prevents input wiring changes if pitch returns later.
        game.doPitch();
    }
}