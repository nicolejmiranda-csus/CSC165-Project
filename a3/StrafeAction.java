package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class StrafeAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public StrafeAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Fixed direction lets LEFT and RIGHT share the same action class.
        game.doStrafe(dir, time);
        ProtocolClient protClient = game.getProtocolClient();
        if (protClient != null)
            protClient.sendMoveMessage(game.getAvatar().getWorldLocation());
    }
}