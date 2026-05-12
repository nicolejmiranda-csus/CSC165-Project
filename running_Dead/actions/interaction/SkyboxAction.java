package running_Dead.actions.interaction;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper used while testing skybox switching.
 * Connected to: Created by MyGameInputBinder; forwards context/primary input to MyGame gameplay systems.
 */
public class SkyboxAction extends AbstractInputAction {
    private MyGame game;
    private int which;

    public SkyboxAction(MyGame g, int w) {
        game = g;
        which = w;
    }

    @Override
    public void performAction(float time, Event e) {
        if (which == 1) {
            game.useSky04();
        } else if (which == 2) {
            game.useSky15Night();
        } else {
            game.toggleSkyboxOff();
        }
    }
}
