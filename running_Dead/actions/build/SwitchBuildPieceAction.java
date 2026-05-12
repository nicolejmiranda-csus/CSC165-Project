package running_Dead.actions.build;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for switching between wall and roof build pieces.
 * Connected to: Created by MyGameInputBinder; forwards build input to MyGameBuildSystem through MyGame.
 */
public class SwitchBuildPieceAction extends AbstractInputAction {
    private MyGame game;

    public SwitchBuildPieceAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        game.switchBuildPiece();
    }
}
