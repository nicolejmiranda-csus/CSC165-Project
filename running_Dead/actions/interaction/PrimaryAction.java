package running_Dead.actions.interaction;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * Input wrapper for the left-click primary action.
 * The actual result depends on role and equipped item, so it delegates back to MyGame.
 * Connected to: Created by MyGameInputBinder; forwards context/primary input to MyGame gameplay systems.
 */
public class PrimaryAction extends AbstractInputAction {
    private final MyGame game;

    public PrimaryAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.handlePrimaryAction();
    }
}
