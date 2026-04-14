package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PitchAction extends AbstractInputAction {
    private final MyGame game;

    public PitchAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.doPitch();
    }
}
