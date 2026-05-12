package running_Dead.actions.equipables;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for equipping the healing potion.
 * Connected to: Created by MyGameInputBinder; forwards item input to MyGameItemSystem through MyGame.
 */
public class EquipPotionAction extends AbstractInputAction {
    private MyGame game;

    public EquipPotionAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.equipPotion();
    }
}
