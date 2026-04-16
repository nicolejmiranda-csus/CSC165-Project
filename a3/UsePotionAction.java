package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class UsePotionAction extends AbstractInputAction {
    private MyGame game;

    public UsePotionAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.usePotion();
    }
}
