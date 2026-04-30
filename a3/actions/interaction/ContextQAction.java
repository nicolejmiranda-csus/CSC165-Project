package a3.actions.interaction;

import a3.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class ContextQAction extends AbstractInputAction {
    private final MyGame game;

    public ContextQAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.handleQAction();
    }
}
