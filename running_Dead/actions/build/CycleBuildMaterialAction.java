package running_Dead.actions.build;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class CycleBuildMaterialAction extends AbstractInputAction {
    private final MyGame game;

    public CycleBuildMaterialAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event event) {
        game.cycleBuildMaterial();
    }
}
