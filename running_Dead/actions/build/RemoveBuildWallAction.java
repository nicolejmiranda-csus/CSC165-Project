package running_Dead.actions.build;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for removing a build piece at the current preview grid location.
 * Connected to: Created by MyGameInputBinder; forwards build input to MyGameBuildSystem through MyGame.
 */
public class RemoveBuildWallAction extends AbstractInputAction {
    private MyGame game;

    public RemoveBuildWallAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.removeBuildWall();
    }
}
