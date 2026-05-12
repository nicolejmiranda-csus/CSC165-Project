package running_Dead.actions.equipables;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for clearing the currently held item.
 * Connected to: Created by MyGameInputBinder; forwards item input to MyGameItemSystem through MyGame.
 */
public class UnequipItemAction extends AbstractInputAction {
    private MyGame game;

    public UnequipItemAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.unequipItem();
    }
}
