package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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