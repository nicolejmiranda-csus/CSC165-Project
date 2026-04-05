package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class RotateBuildWallAction extends AbstractInputAction {
    private MyGame game;

    public RotateBuildWallAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.rotateBuildWall();
    }
}
