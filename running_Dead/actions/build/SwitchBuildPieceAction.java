package running_Dead.actions.build;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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
