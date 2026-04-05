package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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