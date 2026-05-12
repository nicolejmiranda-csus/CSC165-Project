package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for enabling or disabling mouse-look control.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class ToggleMouseLookAction extends AbstractInputAction {
    private MyGame game;

    public ToggleMouseLookAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.toggleMouseLook();
    }
}
