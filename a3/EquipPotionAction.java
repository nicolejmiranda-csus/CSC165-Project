package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

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
