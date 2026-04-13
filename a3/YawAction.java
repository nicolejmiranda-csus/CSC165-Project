package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class YawAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public YawAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        // Fixed direction lets left and right yaw share the same action class.
        game.doYaw(dir, time);
        ProtocolClient protClient = game.getProtocolClient();
        if (protClient != null)
            protClient.sendMoveMessage(game.getAvatar().getWorldLocation());
    }
}