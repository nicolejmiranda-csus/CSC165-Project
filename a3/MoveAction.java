package a3;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class MoveAction extends AbstractInputAction {
    private MyGame game;
    private float dir;

    public MoveAction(MyGame g, float direction) {
        game = g;
        dir = direction;
    }

    @Override
    public void performAction(float time, Event e) {
        game.doMove(dir, time);
        ProtocolClient protClient = game.getProtocolClient();
        if (protClient != null)
            protClient.sendMoveMessage(game.getAvatar().getWorldLocation());
    }
}