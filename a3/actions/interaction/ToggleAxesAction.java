package a3.actions.interaction;

import a3.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ToggleAxesAction extends AbstractInputAction {
    private MyGame game;

    public ToggleAxesAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Toggle switches render states so axes can be hidden without deleting objects.
        game.toggleAxesVisibility();
    }
}
