package running_Dead.actions.interaction;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * Input wrapper for toggling physics debug visuals.
 * Connected to: Created by MyGameInputBinder; forwards context/primary input to MyGame gameplay systems.
 */
public class TogglePhysicsDebugAction extends AbstractInputAction {
    private final MyGame game;

    public TogglePhysicsDebugAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.togglePhysicsVisualization();
    }
}
