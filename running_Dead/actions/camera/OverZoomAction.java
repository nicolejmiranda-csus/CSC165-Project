package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Keyboard input wrapper for zooming the overhead map camera.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class OverZoomAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public OverZoomAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Zoom is applied by changing the overhead camera height.
        float zoomSpeed = 30.0f;

        game.zoomOverhead(dir * zoomSpeed * time);
    }
}
