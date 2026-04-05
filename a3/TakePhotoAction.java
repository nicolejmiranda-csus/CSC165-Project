package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class TakePhotoAction extends AbstractInputAction {
    private MyGame game;

    public TakePhotoAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Photo logic handles range checks, duplicate photos, and controller
        // activation.
        game.tryTakePhoto();
    }
}