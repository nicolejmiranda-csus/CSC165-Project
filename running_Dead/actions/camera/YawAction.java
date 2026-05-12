package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Keyboard input wrapper for rotating the player/camera heading.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class YawAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public YawAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Fixed direction lets left and right yaw share the same action class.
        game.doYaw(dir, time);
    }
}
