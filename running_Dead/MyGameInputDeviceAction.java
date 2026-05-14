package running_Dead;

import net.java.games.input.Component;
import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

/**
 * Decorates an input action so the HUD can show the right keyboard or Xbox control text.
 * This keeps device detection in one place instead of scattering "last input" flags through every action.
 * Connected to: Created by MyGameInputBinder and forwards to the existing running_Dead.actions classes.
 */
public class MyGameInputDeviceAction extends AbstractInputAction {
    private static final float STICK_DEADZONE = 0.20f;
    private static final float TRIGGER_DEADZONE = 0.35f;

    private final MyGame game;
    private final AbstractInputAction delegate;
    private final int deviceType;

    public MyGameInputDeviceAction(MyGame game, int deviceType, AbstractInputAction delegate) {
        this.game = game;
        this.deviceType = deviceType;
        this.delegate = delegate;
    }

    @Override
    public void performAction(float time, Event event) {
        if (deviceType == GameConstants.INPUT_DEVICE_XBOX) {
            if (isMeaningfulGamepadInput(event)) game.noteXboxInput();
        } else {
            game.noteKeyboardMouseInput();
        }

        delegate.performAction(time, event);
    }

    private boolean isMeaningfulGamepadInput(Event event) {
        if (event == null || event.getComponent() == null) return true;

        Component.Identifier id = event.getComponent().getIdentifier();
        float value = event.getValue();

        if (id == Component.Identifier.Axis.POV) return value != 0.0f;
        if (id == Component.Identifier.Axis.Z || id == Component.Identifier.Axis.RZ) return value > TRIGGER_DEADZONE;
        if (id instanceof Component.Identifier.Axis) return java.lang.Math.abs(value) > STICK_DEADZONE;
        if (id instanceof Component.Identifier.Button) return value > 0.5f;

        return true;
    }
}
