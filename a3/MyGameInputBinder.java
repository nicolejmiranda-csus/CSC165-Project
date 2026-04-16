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

        im.associateActionWithAllKeyboards(Key.LEFT, new YawAction(game, 1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.RIGHT, new YawAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.A, new StrafeAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.D, new StrafeAction(game, 1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.LSHIFT, new RunAction(game),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.SPACE, new JumpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.H, new UsePotionAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F1, new ToggleHelpAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.RETURN, new PlacePhotosAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.P, new TakePhotoAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.Q, new OrbitAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.E, new OrbitAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.UP, new ElevateAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.DOWN, new ElevateAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.Z, new ZoomAction(game, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(Key.X, new ZoomAction(game, +1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateActionWithAllKeyboards(Key.I, new OverPanAction(game, 0f, -1f),
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
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
        im.associateActionWithAllKeyboards(Key._4, new EquipFlashlightAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key._5, new EquipPotionAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key._6, new UnequipItemAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key._1, new SkyboxAction(game, 1),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key._2, new SkyboxAction(game, 2),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key._3, new SkyboxAction(game, 3),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        im.associateActionWithAllKeyboards(Key.B, new ToggleBuildAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.V, new PlaceBuildWallAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.C, new RemoveBuildWallAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.R, new RotateBuildWallAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllKeyboards(Key.F, new SwitchBuildPieceAction(game),
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

        im.associateActionWithAllGamepads(Button._2, new TakePhotoAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(Button._0, new PlacePhotosAction(game),
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

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
