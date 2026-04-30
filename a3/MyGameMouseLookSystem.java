package a3;

import java.awt.*;
import java.awt.event.*;
import org.joml.Vector3f;
import tage.*;

public class MyGameMouseLookSystem {
    private final MyGame game;

    public MyGameMouseLookSystem(MyGame game) {
        this.game = game;
    }

    public void ensureMouseModeInitialized() {
        if (!game.state.mouseModeInitiated) initMouseMode();
    }

    public void initMouseMode() {
        game.state.mouseModeInitiated = true;
        RenderSystem rs = MyGame.getEngine().getRenderSystem();
        game.state.isRecentering = false;
        try {
            game.state.robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Couldn't create Robot!");
        }
        game.state.canvas = rs.getGLCanvas();
        game.state.canvas.addMouseMotionListener(game);
        Toolkit tk = Toolkit.getDefaultToolkit();
        game.state.defaultCursor = Cursor.getDefaultCursor();
        game.state.invisibleCursor = tk.createCustomCursor(tk.getImage(""), new Point(), "InvisibleCursor");
        game.state.canvas.setCursor(game.state.invisibleCursor);
        updateMouseCenterOnScreen();
        recenterMouse();
        game.state.prevMouseX = game.state.centerX;
        game.state.prevMouseY = game.state.centerY;
        game.state.canvas.requestFocusInWindow();
    }

    public void toggleMouseLook() {
        if (!game.state.mouseModeInitiated || game.state.canvas == null) return;
        game.state.mouseLookEnabled = !game.state.mouseLookEnabled;
        if (game.state.mouseLookEnabled) {
            game.state.canvas.setCursor(game.state.invisibleCursor);
            recenterMouse();
            game.state.prevMouseX = game.state.centerX;
            game.state.prevMouseY = game.state.centerY;
            game.state.canvas.requestFocusInWindow();
            game.hudSystem.showEvent("MOUSE LOCKED", 1.0);
        } else {
            game.state.isRecentering = false;
            game.state.canvas.setCursor(game.state.defaultCursor);
            game.hudSystem.showEvent("MOUSE UNLOCKED", 1.0);
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (!game.state.mouseModeInitiated || !game.state.mouseLookEnabled) return;
        if (game.state.isRecentering && game.state.centerX == e.getXOnScreen() && game.state.centerY == e.getYOnScreen()) {
            game.state.isRecentering = false;
            return;
        }
        game.state.curMouseX = e.getXOnScreen();
        game.state.curMouseY = e.getYOnScreen();
        float mouseDeltaX = game.state.prevMouseX - game.state.curMouseX;
        float mouseDeltaY = game.state.prevMouseY - game.state.curMouseY;
        yawMouse(mouseDeltaX);
        pitchMouse(mouseDeltaY);
        game.state.prevMouseX = game.state.curMouseX;
        game.state.prevMouseY = game.state.curMouseY;
        recenterMouse();
        game.state.prevMouseX = game.state.centerX;
        game.state.prevMouseY = game.state.centerY;
    }

    private void recenterMouse() {
        if (!game.state.mouseLookEnabled) return;
        updateMouseCenterOnScreen();
        game.state.isRecentering = true;
        game.state.robot.mouseMove((int) game.state.centerX, (int) game.state.centerY);
    }

    private void updateMouseCenterOnScreen() {
        if (game.state.canvas == null) return;
        Point canvasLoc = game.state.canvas.getLocationOnScreen();
        game.state.centerX = canvasLoc.x + game.state.canvas.getWidth() / 2.0f;
        game.state.centerY = canvasLoc.y + game.state.canvas.getHeight() / 2.0f;
    }

    private void yawMouse(float mouseDeltaX) {
        if (Math.abs(mouseDeltaX) < 0.001f) return;
        float yawAmt = mouseDeltaX * game.state.mouseYawSpeed;
        game.assets.avatar.globalYaw(yawAmt);
        game.state.playerYaw += (float) Math.toDegrees(yawAmt);
        if (game.state.orbitCam != null) {
            game.state.orbitCam.update();
            game.cameraSystem.applyCameraModeOffset();
        }
        game.networking.sendPlayerTransform();
    }

    private void pitchMouse(float mouseDeltaY) {
        if (game.state.orbitCam == null) return;
        if (Math.abs(mouseDeltaY) < 0.001f) return;
        game.state.orbitElevationDeg += -mouseDeltaY * game.state.mouseElevSpeed;
        if (game.state.orbitElevationDeg < game.state.minOrbitElevationDeg) game.state.orbitElevationDeg = game.state.minOrbitElevationDeg;
        if (game.state.orbitElevationDeg > game.state.maxOrbitElevationDeg) game.state.orbitElevationDeg = game.state.maxOrbitElevationDeg;
        game.state.orbitCam.setElevationDeg(game.state.orbitElevationDeg);
        game.state.orbitCam.update();
        game.cameraSystem.applyCameraModeOffset();
    }
}
