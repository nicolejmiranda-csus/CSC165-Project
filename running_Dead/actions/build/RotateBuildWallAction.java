package running_Dead.actions.build;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for rotating wall and roof placement directions.
 * Connected to: Created by MyGameInputBinder; forwards build input to MyGameBuildSystem through MyGame.
 */
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
