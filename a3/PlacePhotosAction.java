package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class PlacePhotosAction extends AbstractInputAction {
    private MyGame game;

    public PlacePhotosAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        // Placement logic checks score and home distance before switching photos to the
        // wall.
        game.tryPlacePhotos();
    }
}