package a3;

import a3.actions.build.*;
import a3.actions.camera.*;
import a3.actions.equipables.*;
import a3.actions.interaction.*;
import a3.actions.movement.*;
import tage.input.*;
import net.java.games.input.Component.Identifier.*;

public class MyGameInputBinder {
    private final MyGame game;

    public MyGameInputBinder(MyGame game) {
        this.game = game;
    }

    public void bindAll() {
        bindKeyboard();
        bindGamepad();
    }

    public void bindKeyboard() {
        InputManager im = game.state.im;

        im.associateActionWithAllKeyboards(Key.W, new MoveAction(game, 1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.S, new MoveAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.LEFT, new OrbitAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.RIGHT, new OrbitAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.A, new StrafeAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.D, new StrafeAction(game, 1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.LSHIFT, new RunAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.SPACE, new JumpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.F1, new ToggleHelpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.UP, new ElevateAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.DOWN, new ElevateAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.PAGEUP, new ZoomAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.PAGEDOWN, new ZoomAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.I, new ToggleHelpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.K, new OverPanAction(game, 0f, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.J, new OverPanAction(game, -1f, 0f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.L, new OverPanAction(game, +1f, 0f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.U, new OverZoomAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.O, new OverZoomAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.T, new ToggleAxesAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F, new ContextFAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.C, new EquipPotionAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.Q, new ContextQAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.R, new ContextRAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F2, new TogglePhysicsDebugAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F3, new ToggleZombieRoleAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.E, new ToggleBuildAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.B, new CycleBuildMaterialAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.TAB, new ToggleMouseLookAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.PERIOD, new RaiseBuildHeightAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.M, new ToggleFullMapAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.N, new LowerBuildHeightAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.G, new ToggleCameraModeAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.Y, new SwapShoulderAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    }

    public void bindGamepad() {
        InputManager im = game.state.im;

        im.associateActionWithAllGamepads(Axis.Y, new MoveAxisAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.X, new StrafeAxisAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Axis.RX, new YawAxisAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.RY, new ElevateAxisAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Button._4, new OrbitAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Button._5, new OrbitAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Button._7, new ZoomAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Button._6, new ZoomAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Axis.POV, new PovOverPanAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(Axis.Z, new OverZoomAxisAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllGamepads(Button._1, new ToggleAxesAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._3, new JumpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._9, new ToggleHelpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    }
}
