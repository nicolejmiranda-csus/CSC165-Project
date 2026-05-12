package running_Dead.actions.camera;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Keyboard input wrapper for raising or lowering the third-person camera angle.
 * Connected to: Created by MyGameInputBinder; forwards camera input to MyGameCameraSystem through MyGame.
 */
public class ElevateAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public ElevateAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Keyboard elevation uses a fixed direction value from the constructor.
        float input = dir;

        // Axis input can share this class by passing direction as 0 and reading the
        // event value.
        if (e != null && dir == 0f)
            input = e.getValue();

        game.doElevate(input, time);
    }
}
