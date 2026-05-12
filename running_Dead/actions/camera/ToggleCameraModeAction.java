package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for switching between first-person and third-person camera modes.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class ToggleCameraModeAction extends AbstractInputAction {
    private MyGame game;

    public ToggleCameraModeAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.toggleCameraMode();
    }
}
