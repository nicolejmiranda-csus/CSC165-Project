package running_Dead.actions.movement;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Keyboard input wrapper for forward/backward movement.
 * Connected to: Created by MyGameInputBinder; forwards movement input to MyGameMovementSystem through MyGame.
 */
public class MoveAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public MoveAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Direction is fixed per binding, so W and S can share the same class.
        game.doMove(dir, time);
    }
}
