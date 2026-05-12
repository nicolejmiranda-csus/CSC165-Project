package running_Dead.actions.interaction;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for showing or hiding debug world axes.
 * Connected to: Created by MyGameInputBinder; forwards context/primary input to MyGame gameplay systems.
 */
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
