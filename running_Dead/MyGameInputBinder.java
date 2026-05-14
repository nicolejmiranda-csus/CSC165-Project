package running_Dead;

import running_Dead.actions.build.*;
import running_Dead.actions.camera.*;
import running_Dead.actions.equipables.*;
import running_Dead.actions.interaction.*;
import running_Dead.actions.movement.*;
import tage.input.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier.*;

/**
 * Central keyboard and gamepad binding table.
 * The action classes are intentionally tiny wrappers, so graders can look here for the full control scheme.
 * Connected to: Called by MyGameInitializer; creates every action class in running_Dead.actions.*.
 */
public class MyGameInputBinder {
    private final MyGame game;

    public MyGameInputBinder(MyGame game) {
        this.game = game;
    }

    public void bindAll() {
        bindKeyboard();
        bindGamepad();
    }

    private AbstractInputAction keyboardAction(AbstractInputAction action) {
        return new MyGameInputDeviceAction(game, GameConstants.INPUT_DEVICE_KEYBOARD_MOUSE, action);
    }

    private AbstractInputAction xboxAction(AbstractInputAction action) {
        return new MyGameInputDeviceAction(game, GameConstants.INPUT_DEVICE_XBOX, action);
    }

    public void bindKeyboard() {
        InputManager im = game.state.im;

        im.associateActionWithAllKeyboards(Key.W, keyboardAction(new MoveAction(game, 1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.S, keyboardAction(new MoveAction(game, -1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.LEFT, keyboardAction(new OrbitAction(game, -1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.RIGHT, keyboardAction(new OrbitAction(game, +1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.A, keyboardAction(new StrafeAction(game, -1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.D, keyboardAction(new StrafeAction(game, 1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.LSHIFT, keyboardAction(new RunAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.SPACE, keyboardAction(new JumpAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.F1, keyboardAction(new ToggleHelpAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.UP, keyboardAction(new ElevateAction(game, -1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.DOWN, keyboardAction(new ElevateAction(game, +1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.PAGEUP, keyboardAction(new ZoomAction(game, -1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.PAGEDOWN, keyboardAction(new ZoomAction(game, +1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.I, keyboardAction(new ToggleHelpAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.K, keyboardAction(new OverPanAction(game, 0f, +1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.J, keyboardAction(new OverPanAction(game, -1f, 0f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.L, keyboardAction(new OverPanAction(game, +1f, 0f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.U, keyboardAction(new OverZoomAction(game, -1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.O, keyboardAction(new OverZoomAction(game, +1f)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.T, keyboardAction(new ToggleAxesAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F, keyboardAction(new ContextFAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.C, keyboardAction(new EquipPotionAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.Q, keyboardAction(new ContextQAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.R, keyboardAction(new ContextRAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F2, keyboardAction(new TogglePhysicsDebugAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F3, keyboardAction(new ToggleZombieRoleAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.E, keyboardAction(new ToggleBuildAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.B, keyboardAction(new CycleBuildMaterialAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.TAB, keyboardAction(new ToggleMouseLookAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.PERIOD, keyboardAction(new RaiseBuildHeightAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.M, keyboardAction(new ToggleFullMapAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.N, keyboardAction(new LowerBuildHeightAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.G, keyboardAction(new ToggleCameraModeAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.Y, keyboardAction(new SwapShoulderAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    }

    public void bindGamepad() {
        InputManager im = game.state.im;

        // Xbox One controller layout:
        // left stick moves, right stick looks, A/B/X/Y are TAGE buttons 0/1/2/3,
        // LB/RB are 4/5, select/start are 6/7, stick clicks are 8/9, and guide is 10.
        im.associateActionWithAllGamepads(Axis.Y, xboxAction(new MoveAxisAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.X, xboxAction(new StrafeAxisAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Axis.RX, xboxAction(new YawAxisAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.RY, xboxAction(new ElevateAxisAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Axis.POV, xboxAction(new XboxPovAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.Z, xboxAction(new XboxTriggerAction(game, XboxTriggerAction.LEFT_TRIGGER)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.RZ, xboxAction(new XboxTriggerAction(game, XboxTriggerAction.RIGHT_TRIGGER)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Button._0, xboxAction(new JumpAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._1, xboxAction(new ToggleBuildAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._2, xboxAction(new ContextFAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._3, xboxAction(new XboxYAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._4, xboxAction(new ContextQAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._5, xboxAction(new ContextRAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._6, xboxAction(new ToggleFullMapAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._7, xboxAction(new ToggleHelpAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._8, xboxAction(new RunAction(game)),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Button._9, xboxAction(new ToggleCameraModeAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._10, xboxAction(new ToggleHelpAction(game)),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    }
}
