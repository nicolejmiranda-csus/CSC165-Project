package a3.actions.interaction;

import a3.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class ThrowZombieProjectileAction extends AbstractInputAction {
    private final MyGame game;

    public ThrowZombieProjectileAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.throwZombieTagProjectile();
    }
}
