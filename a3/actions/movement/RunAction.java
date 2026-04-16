package a3.actions.movement;

import a3.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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
