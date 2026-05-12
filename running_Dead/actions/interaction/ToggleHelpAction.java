package running_Dead.actions.interaction;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for cycling the help overlay.
 * Connected to: Created by MyGameInputBinder; forwards context/primary input to MyGame gameplay systems.
 */
public class ToggleHelpAction extends AbstractInputAction {
    // Reference back to the game so the action can toggle the HUD overlay
    private MyGame game;

    public ToggleHelpAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Toggle help pages off keyboard gamepad
        game.toggleHelp();
    }
}
