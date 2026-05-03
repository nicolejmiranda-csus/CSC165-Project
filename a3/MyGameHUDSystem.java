package a3;

import org.joml.Vector3f;
import tage.HUDmanager;

public class MyGameHUDSystem {
    private final MyGame game;
    private final Vector3f hudLight = new Vector3f(0.95f, 0.94f, 0.72f);

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
        String role = game.state.localPlayerZombie ? "ZOMBIE" : "HUMAN";
        String round;
        if (game.state.matchHumanWon) round = "HUMANS WIN";
        else if (game.state.matchZombieWon) round = "ZOMBIES WIN";
        else if (game.state.zombieRoundActive) round = "SURVIVE " + Math.max(0, (int) Math.ceil(game.state.survivalTimeRemaining));
        else if (game.state.isMultiplayer && !game.state.zombieIntermissionStarted) round = "WAITING FOR 2 PLAYERS";
        else round = "INTERMISSION " + Math.max(0, (int) Math.ceil(game.state.zombieIntermissionRemaining));
        String top = role + "   |   " + round + "   |   " + buildRoleStatus();
        if (game.state.slowTimer > 0.0) top += "   |   SLOWED";
        if (game.state.invisTimer > 0.0) top += "   |   INVISIBLE";
        if (!game.state.eventMsg.isEmpty()) top += "   |   " + game.state.eventMsg;
        hud.setHUD1(top, hudLight, 15, 15);

        if (game.state.helpPage == 1) {
            String[] lines = keyboardHelpLines();
            hud.setHUD2(lines[0], hudLight, 15, 40);
            hud.setHUD3(lines[1], hudLight, 15, 65);
        } else if (game.state.helpPage == 2) {
            hud.setHUD2("PAD LS MOVE STRAFE   RS X TURN   Y JUMP   B AXES   PAD LB RB ORBIT", hudLight, 15, 40);
            hud.setHUD3("RS Y ELEVATE   BACK ZOOM IN   START ZOOM OUT   DPAD PAN OVER   LT OVER IN   RT OVER OUT", hudLight, 15, 65);
        } else {
            String[] lines = buildGuideLines();
            hud.setHUD2(lines[0], hudLight, 15, 40);
            hud.setHUD3(lines[1], hudLight, 15, 65);
        }

        if (!game.state.fullMapMode) {
            Vector3f p = game.assets.avatar.getWorldLocation();
            String pos = String.format("POS (%.1f, %.1f, %.1f)", p.x, p.y, p.z);
            hud.setHUD4(pos, hudLight, game.overHudLeftPx() + 12, game.overHudTopPx() + 18);
        } else {
            hud.setHUD4("", hudLight, 0, 0);
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
        if (game.state.matchZombieWon) return new String[]{"ZOMBIES WIN", "ALL HUMANS WERE TAGGED BEFORE TIME RAN OUT"};
        if (game.state.matchHumanWon) return new String[]{"HUMANS WIN", "AT LEAST ONE HUMAN SURVIVED 300 SECONDS"};
        if (game.state.localPlayerZombie) {
            return new String[]{
                "ZOMBIE: TAG EVERY HUMAN BEFORE TIME RUNS OUT",
                "F INVISIBILITY   R EQUIP BABY ZOMBIE   LEFT CLICK THROW/ATTACK   BABIES " + game.state.babyZombieCharges
            };
        }
        if (game.state.buildMode) {
            String pieceName = game.state.buildPieceType == 0 ? "WALL" : "ROOF";
            return new String[]{
                "BUILD MODE: LEFT CLICK PLACE   R ROTATE   Q SWITCH PIECE   . UP   N DOWN",
                "PIECE " + pieceName + "   HEIGHT " + game.state.buildHeightLevel + "   MATERIALS " + game.state.buildMaterials + "   PRESS E TO EXIT"
            };
        }
        return new String[]{
            "HUMAN: SURVIVE UNTIL THE TIMER HITS ZERO",
            "F FLASHLIGHT   C POTION   Q ROCK   R DASH   E BUILD   LEFT CLICK USE"
        };
    }

    private String buildRoleStatus() {
        if (game.state.localPlayerZombie) {
            String invis = game.state.invisCharges > 0 ? "READY" : "EMPTY";
            return "INVIS " + invis + "   BABIES " + game.state.babyZombieCharges + "   HEALTH --";
        }
        String dash = game.state.dashCharges > 0 ? "READY" : "EMPTY";
        return "HEALTH " + game.state.health + "/" + game.state.maxHealth + "   DASH " + dash
                + "   ROCKS " + game.state.rockCharges + "   MATERIALS " + game.state.buildMaterials;
    }

    private String[] keyboardHelpLines() {
        if (game.state.localPlayerZombie) {
            return new String[]{
                "KEY W S MOVE   A D STRAFE   LEFT RIGHT TURN   SHIFT RUN   SPACE JUMP",
                "F INVISIBILITY   R BABY ZOMBIE   LEFT CLICK THROW/ATTACK   TAB MOUSE   F2 PHYSICS VIEW"
            };
        }
        return new String[]{
            "KEY W S MOVE   A D STRAFE   LEFT RIGHT TURN   SHIFT RUN   SPACE JUMP",
            "F LIGHT   C POTION   Q ROCK   R DASH   E BUILD   LEFT CLICK USE/PLACE   TAB MOUSE"
        };
    }
}
