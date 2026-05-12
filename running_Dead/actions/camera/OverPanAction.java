package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Keyboard input wrapper for panning the overhead map camera.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class OverPanAction extends AbstractInputAction {
    private MyGame game;
    private float dirX;
    private float dirZ;

    public OverPanAction(MyGame g, float dx, float dz) {
        game = g;
        dirX = dx;
        dirZ = dz;
    }

    @Override
    public void performAction(float time, Event e) {
        // Pan uses a constant speed scaled by frame time so movement stays consistent.
        float panSpeed = 18.0f;

        float dx = dirX * panSpeed * time;
        float dz = dirZ * panSpeed * time;

        game.panOverhead(dx, dz);
    }
}
