package running_Dead;

import org.joml.Vector3f;
import running_Dead.networking.GhostAvatar;
import com.jogamp.opengl.util.gl2.GLUT;
import tage.HUDmanager;

/**
 * Builds all HUD text, role hints, event messages, and team-only floating player names.
 * Floating names intentionally filter by role so HUD labels do not reveal the enemy team.
 * Connected to: Owned by MyGame; called by MyGameUpdater and uses TAGE HUDmanager/world HUD labels.
 */
public class MyGameHUDSystem {
    private final MyGame game;
    private final Vector3f hudLight = new Vector3f(0.95f, 0.94f, 0.72f);
    private final Vector3f humanNameColor = new Vector3f(0.72f, 0.92f, 1.0f);
    private final Vector3f zombieNameColor = new Vector3f(0.62f, 1.0f, 0.54f);
    private static final int HUD_SMALL_FONT = GLUT.BITMAP_9_BY_15;
    private static final int HUD_LARGE_FONT = GLUT.BITMAP_TIMES_ROMAN_24;
    private static final int HUD_NAME_FONT = GLUT.BITMAP_HELVETICA_18;

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
        updateFloatingPlayerNames(hud);
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

        if (game.state.helpPage != GameConstants.HELP_OFF) {
            hud.setHUD2font(HUD_SMALL_FONT);
            hud.setHUD3font(HUD_SMALL_FONT);
            String[] lines = instructionLines(game.state.helpPage == GameConstants.HELP_XBOX);
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

    private void updateFloatingPlayerNames(HUDmanager hud) {
        hud.clearWorldHUDStrings();
        if (!game.state.isMultiplayer || game.state.gm == null || game.state.fullMapMode || game.assets.avatar == null) return;
        Vector3f localPos = game.assets.avatar.getWorldLocation();
        for (GhostAvatar ghost : game.getGhostManager().getGhostAvatarsSnapshot()) {
            java.util.UUID id = ghost.getID();
            boolean remoteZombie = game.state.remoteZombieStates.getOrDefault(id, false);
            // Name labels are a team communication aid, not an enemy reveal mechanic.
            if (remoteZombie != game.state.localPlayerZombie) continue;
            Vector3f ghostPos = ghost.getWorldLocation();
            if (localPos.distance(ghostPos) > GameConstants.NAME_LABEL_MAX_DISTANCE) continue;
            if (!remoteZombie && game.worldBuilder.isInsideTableCover(ghostPos)) continue;
            String name = game.state.remotePlayerNames.getOrDefault(id, ghost.getPlayerName());
            if (name == null || name.isBlank()) continue;
            Vector3f labelPos = new Vector3f(ghostPos).add(0f, 2.45f, 0f);
            hud.addWorldHUDString(name, labelPos, remoteZombie ? zombieNameColor : humanNameColor, HUD_NAME_FONT);
        }
    }

    public void toggleHelp() {
        if (game.state.helpPage == GameConstants.HELP_OFF) {
            game.state.helpPage = game.state.lastInputDevice == GameConstants.INPUT_DEVICE_XBOX
                    ? GameConstants.HELP_XBOX : GameConstants.HELP_KEYBOARD;
        } else if (game.state.helpPage == GameConstants.HELP_KEYBOARD) {
            game.state.helpPage = GameConstants.HELP_XBOX;
        } else {
            game.state.helpPage = GameConstants.HELP_OFF;
        }

        if (game.state.helpPage == GameConstants.HELP_KEYBOARD) showEvent("KEYBOARD INSTRUCTIONS", 1.0);
        else if (game.state.helpPage == GameConstants.HELP_XBOX) showEvent("XBOX INSTRUCTIONS", 1.0);
        else showEvent("INSTRUCTIONS OFF", 1.0);
    }

    private String[] buildGuideLines() {
        if (game.state.matchZombieWon) return new String[]{"ZOMBIES WIN", "ALL HUMANS WERE TAGGED BEFORE TIME RAN OUT"};
        if (game.state.matchHumanWon) return new String[]{"HUMANS WIN", "AT LEAST ONE HUMAN SURVIVED 300 SECONDS"};
        boolean xbox = game.state.lastInputDevice == GameConstants.INPUT_DEVICE_XBOX;
        if (game.state.localPlayerZombie) {
            if (xbox) {
                return new String[]{
                    "ZOMBIE: TAG EVERY HUMAN BEFORE TIME RUNS OUT   START HELP   R3 POV",
                    "X/D-PAD UP INVIS   RB/D-PAD RIGHT BABY ZOMBIE   RT THROW/ATTACK   BABIES " + game.state.babyZombieCharges
                };
            }
            return new String[]{
                "ZOMBIE: TAG EVERY HUMAN BEFORE TIME RUNS OUT   I INSTRUCTIONS   G POV",
                "F INVISIBILITY   R EQUIP BABY ZOMBIE   LEFT CLICK THROW/ATTACK   BABIES " + game.state.babyZombieCharges
            };
        }
        if (game.state.buildMode) {
            String pieceName = game.state.buildPieceType == 0 ? "WALL" : "ROOF";
            String material = GameConstants.buildMaterialName(game.state.buildMaterialType);
            if (xbox) {
                return new String[]{
                    "BUILD MODE: RT PLACE   RB ROTATE   LB WALL/ROOF   Y MATERIAL   D-PAD UP/DOWN HEIGHT",
                    "PIECE " + pieceName + "   " + material + "   HEIGHT " + game.state.buildHeightLevel
                            + "   WOOD " + game.state.buildMaterials + " METAL " + game.state.metalBuildMaterials
                            + " GLASS " + game.state.glassBuildMaterials + "   B EXIT"
                };
            }
            return new String[]{
                "BUILD MODE: LEFT CLICK PLACE   R ROTATE   Q WALL/ROOF   B MATERIAL   . UP   N DOWN",
                "PIECE " + pieceName + "   " + material + "   HEIGHT " + game.state.buildHeightLevel
                        + "   WOOD " + game.state.buildMaterials + " METAL " + game.state.metalBuildMaterials
                + " GLASS " + game.state.glassBuildMaterials + "   E EXIT"
            };
        }
        if (xbox) {
            return new String[]{
                "HUMAN: SURVIVE UNTIL THE TIMER HITS ZERO   START HELP   R3 POV",
                "X/D-PAD UP FLASHLIGHT   Y POTION   LB/D-PAD LEFT ROCK   RB/D-PAD RIGHT DASH   RT USE"
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

    private String[] instructionLines(boolean xbox) {
        if (game.state.localPlayerZombie) {
            if (xbox) {
                return new String[]{
                    "XBOX ZOMBIE: left stick move | right stick turn/tilt | L3 run | A jump | Select map | Start close",
                    "XBOX CAMERA: R3 POV | full map uses D-pad pan and LT/RT zoom",
                    "XBOX ZOMBIE TOOLS: X or D-pad up invisibility | RB or D-pad right equip baby zombie | RT attack/throw",
                    "TRACK: nearest human coordinates update every 30 seconds above your own X/Y/Z position"
                };
            }
            return new String[]{
                "KEYBOARD ZOMBIE: W/S move | A/D strafe | mouse turns | Shift run | Space jump | I cycles help",
                "KEYBOARD CAMERA: Left/Right orbit | Up/Down tilt | PageUp zoom in | PageDown zoom out | G POV | M map",
                "ZOMBIE TOOLS: F invisibility | R equip baby zombie | Left click attack/tag, break builds, or throw baby",
                "TRACK: nearest human coordinates update every 30 seconds above your own X/Y/Z position"
            };
        }
        if (xbox) {
            return new String[]{
                "XBOX HUMAN: left stick move | right stick turn/tilt | L3 run | A jump | Select map | Start close",
                "XBOX CAMERA: R3 POV | full map uses D-pad pan and LT/RT zoom",
                "XBOX ITEMS: X or D-pad up flashlight | Y potion | LB or D-pad left rock | RB or D-pad right dash | RT use",
                "XBOX BUILD: B build/exit | LB wall/roof | RB rotate | Y material | D-pad up/down height | RT place"
            };
        }
        return new String[]{
            "KEYBOARD HUMAN: W/S move | A/D strafe | mouse turns | Shift run | Space jump | I cycles help",
            "KEYBOARD CAMERA: Left/Right orbit | Up/Down tilt | PageUp zoom in | PageDown zoom out | G POV | M map",
            "ITEMS: F flashlight | C potion | Q rock | R dash | Left click use equipped item or place while building",
            "BUILD: E build mode/exit | Q wall/roof | B wood/metal/glass | R rotate | . raise | N lower"
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
