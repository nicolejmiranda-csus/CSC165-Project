package running_Dead.actions.build;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for lowering the build preview to a lower terrain-relative level.
 * Connected to: Created by MyGameInputBinder; forwards build input to MyGameBuildSystem through MyGame.
 */
public class LowerBuildHeightAction extends AbstractInputAction {
    private MyGame game;

    public LowerBuildHeightAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.lowerBuildHeight();
    }
}
