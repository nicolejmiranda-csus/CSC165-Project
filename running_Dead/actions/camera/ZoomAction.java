package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for changing the third-person camera distance.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class ZoomAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public ZoomAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Keyboard zoom uses a fixed direction, with optional axis support if direction
        // is set to 0.
        float input = dir;

        if (e != null && dir == 0f)
            input = e.getValue();

        game.doZoom(input, time);
    }
}
