package running_Dead.actions.interaction;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * Xbox Y button wrapper.
 * Y equips potion during normal human play, but cycles build materials while the build preview is active.
 * Connected to: Created by MyGameInputBinder; forwards button input to MyGameItemSystem or MyGameBuildSystem through MyGame.
 */
public class XboxYAction extends AbstractInputAction {
    private final MyGame game;

    public XboxYAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event event) {
        if (game.isBuildMode()) game.cycleBuildMaterial();
        else game.equipPotion();
    }
}
