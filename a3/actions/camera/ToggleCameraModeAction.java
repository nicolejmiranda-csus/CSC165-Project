package a3.actions.camera;

import a3.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class ToggleCameraModeAction extends AbstractInputAction {
    private MyGame game;

    public ToggleCameraModeAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.toggleCameraMode();
    }
}
