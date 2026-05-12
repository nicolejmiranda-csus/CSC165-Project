package running_Dead;

import org.joml.Vector3f;
import tage.*;

/**
 * Owns third-person follow, first-person flashlight aiming, and the overhead map camera.
 * Separating this from movement lets the player body and camera rotate together only when needed.
 * Connected to: Owned by MyGame; called by camera actions, mouse look, movement, and MyGameUpdater.
 */
public class MyGameCameraSystem {
    private final MyGame game;

    public MyGameCameraSystem(MyGame game) {
        this.game = game;
    }

    public void setupCameras() {
        Camera mainCam = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
        game.state.orbitCam = new CameraOrbit3D(mainCam, game.assets.avatar);
        game.state.orbitCam.setRadius(game.state.orbitRadius);
        game.state.orbitCam.setElevationDeg(game.state.orbitElevationDeg);
        game.state.orbitCam.setAzimuthDeg(game.state.orbitAzimuthDeg);
        game.state.orbitCam.update();
        applyCameraModeOffset();
        MyGame.getEngine().getRenderSystem().addViewport("OVER", GameConstants.OVER_BOTTOM, GameConstants.OVER_LEFT, GameConstants.OVER_W, GameConstants.OVER_H);
        setOverheadCamera(MyGame.getEngine().getRenderSystem().getViewport("OVER").getCamera());
        MyGame.getEngine().getRenderSystem().getViewport("OVER").setEnabled(false);
    }

    public void setOverheadCamera(Camera cam) {
        cam.setLocation(new Vector3f(game.state.overX, game.state.overY, game.state.overZ));
        cam.setN(new Vector3f(0f, -1f, 0f));
        cam.setU(new Vector3f(1f, 0f, 0f));
        cam.setV(new Vector3f(0f, 0f, -1f));
    }

    public void panOverhead(float dx, float dz) {
        game.state.overX += dx;
        game.state.overZ += dz;
        setOverheadCamera(MyGame.getEngine().getRenderSystem().getViewport("OVER").getCamera());
    }

    public void zoomOverhead(float dy) {
        game.state.overY += dy;
        if (game.state.overY < 10f) game.state.overY = 10f;
        if (game.state.overY > 120f) game.state.overY = 120f;
        setOverheadCamera(MyGame.getEngine().getRenderSystem().getViewport("OVER").getCamera());
    }

    public void toggleFullMap() {
        game.state.fullMapMode = !game.state.fullMapMode;
        MyGame.getEngine().getRenderSystem().getViewport("OVER").setEnabled(false);
        game.hudSystem.showEvent(game.state.fullMapMode ? "MAP OPEN" : "MAP CLOSED", 1.0);
    }

    public void updateMainCamera() {
        Camera mainCam = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
        if (game.state.fullMapMode) {
            Vector3f p = game.assets.avatar.getWorldLocation();
            mainCam.setLocation(new Vector3f(p.x, 60f, p.z));
            mainCam.setN(new Vector3f(0f, -1f, 0f));
            mainCam.setU(new Vector3f(1f, 0f, 0f));
            mainCam.setV(new Vector3f(0f, 0f, -1f));
        } else if (game.state.orbitCam != null) {
            game.state.orbitCam.update();
            applyCameraModeOffset();
        }
    }

    public void updateCameraLimits() {
        if (game.isPlayerModel2Selected()) {
            game.state.minOrbitElevationDeg = -10.0f;
            game.state.maxOrbitElevationDeg = 76.0f;
        } else {
            game.state.minOrbitElevationDeg = -10.0f;
            game.state.maxOrbitElevationDeg = 80.0f;
        }
    }

    public void toggleCameraMode() {
        game.state.cameraMode = (game.state.cameraMode + 1) % 4;
        switch (game.state.cameraMode) {
            case 0: game.state.orbitRadius = 4.5f; game.hudSystem.showEvent("CAMERA BEHIND", 1.0); break;
            case 1: game.state.orbitRadius = 4.5f; game.hudSystem.showEvent("CAMERA RIGHT SHOULDER", 1.0); break;
            case 2: game.state.orbitRadius = 4.5f; game.hudSystem.showEvent("CAMERA LEFT SHOULDER", 1.0); break;
            case 3: game.state.orbitRadius = 0.0f; game.hudSystem.showEvent("CAMERA FIRST PERSON", 1.0); break;
        }
        game.state.orbitCam.setRadius(game.state.orbitRadius);
        game.state.orbitCam.update();
        updateCameraLimits();
        applyCameraModeOffset();
    }

    public void swapShoulderCamera() {
        if (game.state.cameraMode == 1) {
            game.state.cameraMode = 2;
            game.hudSystem.showEvent("CAMERA LEFT SHOULDER", 1.0);
        } else if (game.state.cameraMode == 2) {
            game.state.cameraMode = 1;
            game.hudSystem.showEvent("CAMERA RIGHT SHOULDER", 1.0);
        } else return;
        applyCameraModeOffset();
    }

    public void doOrbit(float input, float time) {
        if (game.state.orbitCam == null) return;
        game.state.orbitAzimuthDeg += input * 90.0f * time;
        game.state.orbitCam.setAzimuthDeg(game.state.orbitAzimuthDeg);
        game.state.orbitCam.update();
        applyCameraModeOffset();
    }

    public void doElevate(float input, float time) {
        if (game.state.orbitCam == null) return;
        game.state.orbitElevationDeg += input * 70.0f * time;
        if (game.state.orbitElevationDeg < game.state.minOrbitElevationDeg) game.state.orbitElevationDeg = game.state.minOrbitElevationDeg;
        if (game.state.orbitElevationDeg > game.state.maxOrbitElevationDeg) game.state.orbitElevationDeg = game.state.maxOrbitElevationDeg;
        game.state.orbitCam.setElevationDeg(game.state.orbitElevationDeg);
        game.state.orbitCam.update();
        applyCameraModeOffset();
    }

    public void doZoom(float input, float time) {
        if (game.state.orbitCam == null) return;
        game.state.orbitRadius += input * 6.0f * time;
        if (game.state.orbitRadius < 0.05f) game.state.orbitRadius = 0.05f;
        if (game.state.orbitRadius > 12.0f) game.state.orbitRadius = 12.0f;
        game.state.orbitCam.setRadius(game.state.orbitRadius);
        game.state.orbitCam.update();
        applyCameraModeOffset();
    }

    public void applyCameraModeOffset() {
        if (game.state.orbitCam == null) return;
        Camera cam = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
        if (game.state.cameraMode == 0) return;
        if (game.state.cameraMode == 1 || game.state.cameraMode == 2) {
            Vector3f loc = cam.getLocation();
            Vector3f u = new Vector3f(cam.getU()).normalize();
            Vector3f v = new Vector3f(cam.getV()).normalize();
            float side = game.state.cameraMode == 2 ? -game.state.shoulderOffsetX : game.state.shoulderOffsetX;
            cam.setLocation(new Vector3f(loc).add(new Vector3f(u).mul(side)).add(new Vector3f(v).mul(game.state.shoulderOffsetY)));
            return;
        }
        if (game.state.cameraMode == 3) {
            Vector3f avatarLoc = game.assets.avatar.getWorldLocation();
            Vector3f forward = new Vector3f(game.avatarForward()).normalize();
            float pitchDeg = 15.0f - game.state.orbitElevationDeg;
            float pitchRad = (float) Math.toRadians(pitchDeg);
            float horiz = (float) Math.cos(pitchRad);
            forward.x *= horiz;
            forward.z *= horiz;
            forward.y = (float) Math.sin(pitchRad);
            forward.normalize();
            Vector3f newLoc = new Vector3f(avatarLoc).add(0f, getFirstPersonEyeHeight(), 0f).add(new Vector3f(forward).mul(getFirstPersonForwardOffset()));
            Vector3f worldUp = new Vector3f(0f, 1f, 0f);
            Vector3f u = new Vector3f(forward).cross(worldUp).normalize();
            Vector3f v = new Vector3f(u).cross(forward).normalize();
            cam.setLocation(newLoc);
            cam.setN(forward);
            cam.setU(u);
            cam.setV(v);
        }
    }

    public float getFirstPersonEyeHeight() { return game.isPlayerModel2Selected() ? 1.65f : 1.80f; }
    public float getFirstPersonForwardOffset() { return game.isPlayerModel2Selected() ? 0.32f : 0.35f; }
}
