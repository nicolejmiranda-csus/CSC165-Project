package running_Dead.actions.movement;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class JumpAction extends AbstractInputAction {
    private MyGame game;

    public JumpAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        // Jump logic is handled in MyGame so the action stays simple.
        game.doJump();
    }
}
