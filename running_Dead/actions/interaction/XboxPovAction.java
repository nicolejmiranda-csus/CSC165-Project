package running_Dead.actions.interaction;

import running_Dead.MyGame;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * Xbox D-pad wrapper for the POV hat component.
 * In normal play it gives quick access to role actions; in full-map mode it pans the overhead map.
 * Connected to: Created by MyGameInputBinder; forwards D-pad input to MyGame gameplay, build, and camera systems.
 */
public class XboxPovAction extends AbstractInputAction {
    private static final int NONE = 0;
    private static final int UP = 1;
    private static final int RIGHT = 2;
    private static final int DOWN = 3;
    private static final int LEFT = 4;

    private static final float EPSILON = 0.0001f;
    private static final float MAP_PAN_SPEED = 18.0f;

    private final MyGame game;
    private int lastDirection = NONE;

    public XboxPovAction(MyGame game) {
        this.game = game;
    }

    @Override
    public void performAction(float time, Event event) {
        float value = event.getValue();

        if (game.isFullMapMode()) {
            panFullMap(value, time);
            lastDirection = NONE;
            return;
        }

        int direction = decodeCardinal(value);
        if (direction == NONE) {
            lastDirection = NONE;
            return;
        }
        if (direction == lastDirection) return;
        lastDirection = direction;

        if (game.isBuildMode()) {
            handleBuildDpad(direction);
        } else {
            handleGameplayDpad(direction);
        }
    }

    private void handleGameplayDpad(int direction) {
        if (direction == UP) game.handleFAction();
        else if (direction == RIGHT) game.handleRAction();
        else if (direction == DOWN) game.toggleBuildMode();
        else if (direction == LEFT) game.handleQAction();
    }

    private void handleBuildDpad(int direction) {
        if (direction == UP) game.raiseBuildHeight();
        else if (direction == RIGHT) game.handleRAction();
        else if (direction == DOWN) game.lowerBuildHeight();
        else if (direction == LEFT) game.handleQAction();
    }

    private void panFullMap(float value, float time) {
        if (close(value, 0.0f)) return;

        float dx = 0f;
        float dz = 0f;

        if (close(value, 0.25f) || close(value, 0.125f) || close(value, 0.375f)) dz = -1f;
        if (close(value, 0.75f) || close(value, 0.625f) || close(value, 0.875f)) dz = 1f;
        if (close(value, 1.0f) || close(value, 0.875f) || close(value, 0.125f)) dx = -1f;
        if (close(value, 0.5f) || close(value, 0.375f) || close(value, 0.625f)) dx = 1f;

        if (dx != 0f || dz != 0f) game.panOverhead(dx * MAP_PAN_SPEED * time, dz * MAP_PAN_SPEED * time);
    }

    private int decodeCardinal(float value) {
        if (close(value, 0.25f)) return UP;
        if (close(value, 0.5f)) return RIGHT;
        if (close(value, 0.75f)) return DOWN;
        if (close(value, 1.0f)) return LEFT;
        return NONE;
    }

    private boolean close(float a, float b) {
        return java.lang.Math.abs(a - b) < EPSILON;
    }
}
