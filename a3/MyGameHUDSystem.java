package a3;

import org.joml.Vector3f;
import tage.HUDmanager;

public class MyGameHUDSystem {
    private final MyGame game;

    public MyGameHUDSystem(MyGame game) {
        this.game = game;
    }

    public void showEvent(String msg, double seconds) {
        game.state.eventMsg = msg;
        game.state.eventHold = seconds;
    }

    public void updateTransientTimers() {
        if (game.state.damageCooldown > 0.0) game.state.damageCooldown -= game.state.elapsedTime;
        if (game.state.eventHold > 0.0) {
            game.state.eventHold -= game.state.elapsedTime;
            if (game.state.eventHold <= 0.0) game.state.eventMsg = "";
        }
    }

    public void updateHUD() {
        HUDmanager hud = MyGame.getEngine().getHUDmanager();
        String top = "HEALTH " + game.state.health + "/" + game.state.maxHealth + "   |   COLLECTED " + game.state.score + "/3";
        if (!game.state.eventMsg.isEmpty()) top += "   |   " + game.state.eventMsg;
        hud.setHUD1(top, new Vector3f(0.75f, 0.75f, 0.35f), 15, 15);

        if (game.state.helpPage == 1) {
            hud.setHUD2("KEY W S MOVE   A D STRAFE   LEFT RIGHT TURN   SHIFT RUN   SPACE JUMP   P PHOTO   ENTER PLACE", new Vector3f(0, 0, 0), 15, 40);
            hud.setHUD3("T AXES G CAMERA Y SWAP SHOULDER Q E ORBIT UP DOWN ELEVATE Z X ZOOM I K J L PAN OVER U O OVER ZOOM", new Vector3f(0, 0, 0), 15, 65);
        } else if (game.state.helpPage == 2) {
            hud.setHUD2("PAD LS MOVE STRAFE   RS X TURN   Y JUMP   X PHOTO   A PLACE   B AXES   PAD LB RB ORBIT", new Vector3f(0, 0, 0), 15, 40);
            hud.setHUD3("RS Y ELEVATE   BACK ZOOM IN   START ZOOM OUT   DPAD PAN OVER   LT OVER IN   RT OVER OUT", new Vector3f(0, 0, 0), 15, 65);
        } else {
            String[] lines = buildGuideLines();
            hud.setHUD2(lines[0], new Vector3f(0, 0, 0), 15, 40);
            hud.setHUD3(lines[1], new Vector3f(0, 0, 0), 15, 65);
        }

        if (!game.state.fullMapMode) {
            Vector3f p = game.assets.avatar.getWorldLocation();
            String pos = String.format("POS (%.1f, %.1f, %.1f)", p.x, p.y, p.z);
            hud.setHUD4(pos, new Vector3f(0, 0, 0), game.overHudLeftPx() + 12, game.overHudTopPx() + 18);
        } else {
            hud.setHUD4("", new Vector3f(0, 0, 0), 0, 0);
        }
    }

    public void toggleHelp() {
        game.state.helpPage++;
        if (game.state.helpPage > 2) game.state.helpPage = 0;
        if (game.state.helpPage == 1) showEvent("HELP KEYBOARD", 1.0);
        else if (game.state.helpPage == 2) showEvent("HELP GAMEPAD", 1.0);
        else showEvent("HELP OFF", 1.0);
    }

    private String[] buildGuideLines() {
        if (game.state.lost) return new String[]{"CRASHED!", "YOU LOSE"};
        if (game.state.won) return new String[]{"PHOTOS PLACED!", "YOU WIN"};
        if (game.state.buildMode) {
            String pieceName = game.state.buildPieceType == 0 ? "WALL" : "ROOF";
            return new String[]{
                "BUILD MODE: V PLACE   C REMOVE   R ROTATE   F SWITCH TYPE   TAB LOCK/UNLOCK   . UP   N DOWN",
                "PIECE " + pieceName + "   HEIGHT " + game.state.buildHeightLevel + "   PRESS B TO EXIT BUILD MODE"
            };
        }
        if (game.state.score == 3) {
            if (game.photoSystem.distanceToHome() <= game.state.homeRange) {
                return new String[]{"HOME REACHED: PLACE PHOTOS", "PRESS ENTER OR A ON CONTROLLER"};
            }
            return new String[]{"ALL PHOTOS TAKEN: RETURN HOME", "FLY HOME THEN PRESS ENTER OR A ON CONTROLLER"};
        }
        int near = game.photoSystem.closestPyramidWithinRange();
        if (near >= 0) return new String[]{"PYRAMID IN RANGE: TAKE PHOTO", "PRESS P OR X ON CONTROLLER"};
        return new String[]{"FIND A PYRAMID AND GET CLOSER", "PRESS B TO ENTER BUILD MODE TAB LOCK/UNLOCK MOUSE G CAMERA Y SWAP"};
    }
}
