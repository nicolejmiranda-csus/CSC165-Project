package running_Dead.actions.interaction;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class AttackBuildPieceAction extends AbstractInputAction {
    private final MyGame game;

    public AttackBuildPieceAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.attackBuildPiece();
    }
}
