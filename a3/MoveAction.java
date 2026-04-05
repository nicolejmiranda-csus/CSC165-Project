package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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