package running_Dead.actions.build;

import running_Dead.MyGame;
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
