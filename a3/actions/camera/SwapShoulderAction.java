package a3.actions.camera;

import a3.MyGame;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class SwapShoulderAction extends AbstractInputAction {
    private MyGame game;

    public SwapShoulderAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event evt) {
        game.swapShoulderCamera();
    }
}
