package running_Dead.actions.equipables;

import running_Dead.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

/**
 * Input wrapper for equipping or raising the human flashlight item.
 * Connected to: Created by MyGameInputBinder; forwards item input to MyGameItemSystem through MyGame.
 */
public class EquipFlashlightAction extends AbstractInputAction {
    private MyGame game;

    public EquipFlashlightAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.equipFlashlight();
    }
}
