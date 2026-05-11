package running_Dead;

import org.joml.Vector3f;
import running_Dead.networking.GhostAvatar;
import com.jogamp.opengl.util.gl2.GLUT;
import tage.HUDmanager;

public class MyGameHUDSystem {
    private final MyGame game;
    private final Vector3f hudLight = new Vector3f(0.95f, 0.94f, 0.72f);
    private static final int HUD_SMALL_FONT = GLUT.BITMAP_9_BY_15;
    private static final int HUD_LARGE_FONT = GLUT.BITMAP_TIMES_ROMAN_24;

    public MyGameHUDSystem(MyGame game) {
        this.game = game;
    }

    public void showEvent(String msg, double seconds) {
        game.state.eventMsg = msg;
        game.state.eventHold = seconds;
    }

    public void updateTransientTimers() {
        if (game.state.damageCooldown > 0.0) game.state.damageCooldown -= game.state.elapsedTime;
        if (game.state.blindTimer > 0.0) {
            game.state.blindTimer -= game.state.elapsedTime;
            if (game.state.blindTimer < 0.0) game.state.blindTimer = 0.0;
        }
        if (game.state.blindCooldownTimer > 0.0) {
            game.state.blindCooldownTimer -= game.state.elapsedTime;
            if (game.state.blindCooldownTimer < 0.0) game.state.blindCooldownTimer = 0.0;
        }
        if (game.state.eventHold > 0.0) {
            game.state.eventHold -= game.state.elapsedTime;
            if (game.state.eventHold <= 0.0) game.state.eventMsg = "";
        }
        if (game.state.stareWarningTimer > 0.0) {
            game.state.stareWarningTimer -= game.state.elapsedTime;
            if (game.state.stareWarningTimer < 0.0) game.state.stareWarningTimer = 0.0;
        }
        updateZombieTracker();
    }

    public void updateHUD() {
        HUDmanager hud = MyGame.getEngine().getHUDmanager();
        hud.setHUD2font(HUD_LARGE_FONT);
        hud.setHUD3font(HUD_LARGE_FONT);
        hud.setHUD4font(HUD_SMALL_FONT);
        hud.setHUD5font(HUD_SMALL_FONT);
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
            hud.setHUD2font(HUD_SMALL_FONT);
            hud.setHUD3font(HUD_SMALL_FONT);
            String[] lines = instructionLines();
            hud.setHUD2(lines[0], hudLight, 15, 40);
            hud.setHUD3(lines[1], hudLight, 15, 58);
            hud.setHUD4(lines[2], hudLight, 15, 76);
            hud.setHUD5(lines[3], hudLight, 15, 94);
            return;
        } else {
            String[] lines = buildGuideLines();
            hud.setHUD2(lines[0], hudLight, 15, 40);
            hud.setHUD3(lines[1], hudLight, 15, 65);
        }

        if (!game.state.fullMapMode) {
            Vector3f p = game.assets.avatar.getWorldLocation();
            String pos = String.format("YOU X %.1f  Y %.1f  Z %.1f", p.x, p.y, p.z);
            int hudX = game.overHudLeftPx() + 12;
            int hudY = game.overHudTopPx() + 18;
            String tracker = game.state.localPlayerZombie ? game.state.zombieTrackerText : "";
            hud.setHUD4(tracker, hudLight, hudX, hudY + 24);
            hud.setHUD5(pos, hudLight, hudX, hudY);
        } else {
            hud.setHUD4("", hudLight, 0, 0);
            hud.setHUD5("", hudLight, 0, 0);
        }
    }

    public void toggleHelp() {
        game.state.helpPage = game.state.helpPage == 1 ? 0 : 1;
        showEvent(game.state.helpPage == 1 ? "INSTRUCTIONS" : "INSTRUCTIONS OFF", 1.0);
    }

    private String[] buildGuideLines() {
        if (game.state.matchZombieWon) return new String[]{"ZOMBIES WIN", "ALL HUMANS WERE TAGGED BEFORE TIME RAN OUT"};
        if (game.state.matchHumanWon) return new String[]{"HUMANS WIN", "AT LEAST ONE HUMAN SURVIVED 300 SECONDS"};
        if (game.state.localPlayerZombie) {
            return new String[]{
                "ZOMBIE: TAG EVERY HUMAN BEFORE TIME RUNS OUT   I INSTRUCTIONS   G POV",
                "F INVISIBILITY   R EQUIP BABY ZOMBIE   LEFT CLICK THROW/ATTACK   BABIES " + game.state.babyZombieCharges
            };
        }
        if (game.state.buildMode) {
            String pieceName = game.state.buildPieceType == 0 ? "WALL" : "ROOF";
            String material = GameConstants.buildMaterialName(game.state.buildMaterialType);
            return new String[]{
                "BUILD MODE: LEFT CLICK PLACE   R ROTATE   Q WALL/ROOF   B MATERIAL   . UP   N DOWN",
                "PIECE " + pieceName + "   " + material + "   HEIGHT " + game.state.buildHeightLevel
                        + "   WOOD " + game.state.buildMaterials + " METAL " + game.state.metalBuildMaterials
                        + " GLASS " + game.state.glassBuildMaterials + "   E EXIT"
            };
        }
        return new String[]{
            "HUMAN: SURVIVE UNTIL THE TIMER HITS ZERO   I INSTRUCTIONS   G POV",
            "F FLASHLIGHT   C POTION   Q ROCK   R DASH   E BUILD   LEFT CLICK USE"
        };
    }

    private String buildRoleStatus() {
        if (game.state.localPlayerZombie) {
            String invis = game.state.invisCharges > 0 ? "READY" : "EMPTY";
            return "INVIS " + invis + "   BABIES " + game.state.babyZombieCharges + "   HEALTH --";
        }
        String dash = game.state.dashCharges > 0 ? "READY" : "EMPTY";
        String light = game.state.hasFlashlight ? String.format("   LIGHT %.0fs", game.state.flashlightBatterySeconds) : "";
        return "HEALTH " + game.state.health + "/" + game.state.maxHealth + "   DASH " + dash
                + "   ROCKS " + game.state.rockCharges + light
                + "   MAT W" + game.state.buildMaterials + " M" + game.state.metalBuildMaterials + " G" + game.state.glassBuildMaterials;
    }

    private String[] instructionLines() {
        if (game.state.localPlayerZombie) {
            return new String[]{
                "ZOMBIE CONTROLS: I close instructions | W/S move | A/D strafe | mouse turns | Shift run | Space jump",
                "CAMERA: Left/Right orbit | Up/Down tilt | PageUp zoom in | PageDown zoom out | G POV | Y shoulder | Tab mouse",
                "ZOMBIE TOOLS: F invisibility | R equip baby zombie | Left click attack/tag, break builds, or throw baby",
                "TRACK: nearest human coordinates update every 30 seconds above your own X/Y/Z position"
            };
        }
        return new String[]{
            "HUMAN CONTROLS: I close instructions | W/S move | A/D strafe | mouse turns | Shift run | Space jump",
            "CAMERA: Left/Right orbit | Up/Down tilt | PageUp zoom in | PageDown zoom out | G POV | Y shoulder | Tab mouse",
            "ITEMS: F flashlight | C potion | Q rock | R dash | Left click use equipped item or place while building",
            "BUILD: E build mode/exit | Q wall/roof | B wood/metal/glass | R rotate | . raise | N lower | M map"
        };
    }

    private void updateZombieTracker() {
        if (!game.state.localPlayerZombie || !game.state.zombieRoundActive || game.isMatchOver()) {
            game.state.zombieTrackerTimer = 0.0;
            game.state.zombieTrackerText = "";
            return;
        }

        game.state.zombieTrackerTimer -= game.state.elapsedTime;
        if (game.state.zombieTrackerTimer > 0.0 && !game.state.zombieTrackerText.isEmpty()) return;

        Vector3f closest = findClosestHumanPosition();
        game.state.zombieTrackerTimer = GameConstants.ZOMBIE_TRACKER_INTERVAL_SECONDS;
        if (closest == null) {
            game.state.zombieTrackerText = "NEAREST HUMAN UNKNOWN";
            return;
        }

        game.state.zombieTrackerText = String.format("NEAREST HUMAN X %.1f  Y %.1f  Z %.1f", closest.x, closest.y, closest.z);
        showEvent("NEAREST HUMAN LOCATION UPDATED", 1.2);
    }

    private Vector3f findClosestHumanPosition() {
        Vector3f self = game.assets.avatar.getWorldLocation();
        Vector3f closest = null;
        float closestDistSq = Float.MAX_VALUE;
        if (game.state.gm == null) return null;
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            java.util.UUID id = ghost.getID();
            if (game.state.remoteZombieStates.getOrDefault(id, false)) continue;
            if (game.state.remoteHealthStates.getOrDefault(id, game.state.maxHealth) <= 0) continue;
            Vector3f p = ghost.getWorldLocation();
            float dx = p.x - self.x;
            float dz = p.z - self.z;
            float distSq = dx * dx + dz * dz;
            if (distSq >= closestDistSq) continue;
            closest = new Vector3f(p);
            closestDistSq = distSq;
        }
        return closest;
    }
}
