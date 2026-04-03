package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PlaceBuildWallAction extends AbstractInputAction {
    private MyGame game;

    public PlaceBuildWallAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.placeBuildWall();
    }
}