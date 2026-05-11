package running_Dead.actions.equipables;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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
