package running_Dead.actions.movement;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for holding sprint/run state.
 * Connected to: Created by MyGameInputBinder; forwards movement input to MyGameMovementSystem through MyGame.
 */
public class RunAction extends AbstractInputAction {
    private MyGame game;

    public RunAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.triggerRun();
    }
}
