package a3;

import java.awt.Cursor;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import com.jogamp.opengl.awt.GLCanvas;

import a3.networking.GhostManager;
import a3.networking.ProtocolClient;
import tage.CameraOrbit3D;
import tage.GameObject;
import tage.input.InputManager;
import tage.networking.IGameConnection.ProtocolType;

public class MyGameState {
    InputManager im;
    GhostManager gm;

    double lastFrameTime, currFrameTime, elapsedTime;

    final int maxHealth = 100;
    int health = 100;
    final int potionHealAmount = 35;

    final float itemPickupRange = 1.25f;
    double damageCooldown = 0.0;
    final double damageCooldownTime = 1.0;

    boolean hasFlashlight = false;
    boolean hasPotion = false;
    boolean flashlightOn = false;
    float flashlightBatterySeconds = GameConstants.FLASHLIGHT_BATTERY_SECONDS;
    boolean potionUsed = false;
    int equippedItem = GameConstants.ITEM_NONE;

    int helpPage = 0;
    String eventMsg = "";
    double eventHold = 0.0;
    double stareWarningTimer = 0.0;
    double zombieTrackerTimer = 0.0;
    String zombieTrackerText = "";

    float runHoldTime = 0.0f;

    float overX = 0f, overZ = 0f, overY = 35f;
    float padMove = 0.0f, padStrafe = 0.0f;

    boolean healthPotionSnappedToTerrain = false;
    boolean flashlightSnappedToTerrain = false;
    final float flashlightLift = 0.10f;
    final float potionLift = 0.18f;
    final float flashlightBeamForwardOffset = 0.18f;
    final float flashlightBeamLift = 0.03f;

    boolean onGround = true;
    float yVel = 0.0f;

    CameraOrbit3D orbitCam;
    float orbitAzimuthDeg = 180.0f;
    float orbitElevationDeg = 15.0f;
    float orbitRadius = 4.5f;
    float minOrbitElevationDeg = -10.0f;
    float maxOrbitElevationDeg = 80.0f;
    boolean fullMapMode = false;
    int cameraMode = 0;
    float shoulderOffsetX = 1.25f;
    float shoulderOffsetY = 0.75f;

    boolean axesVisible = true;

    boolean buildMode = false;
    int buildHeightLevel = 0;
    final float buildDistance = 3.0f;
    final float buildGrid = 2.0f;
    final float buildSnap = 0.5f;
    final float buildWallHalfW = 1.0f;
    final float buildWallHalfH = 1.0f;
    int buildModeType = 0;
    int buildPieceType = 0;
    int buildRoofDir = 0;
    int buildMaterialType = GameConstants.BUILD_MATERIAL_WOOD;
    final ArrayList<GameObject> placedWalls = new ArrayList<>();
    final HashMap<String, GameObject> wallMap = new HashMap<>();

    boolean physicsVisualizationEnabled = false;
    boolean physicsReady = false;

    boolean zombieRoundActive = false;
    boolean zombieIntermissionStarted = false;
    double zombieIntermissionTime = 30.0;
    double zombieIntermissionRemaining = 30.0;
    double survivalRoundDuration = 300.0;
    double survivalTimeRemaining = 300.0;
    double postRoundRestartTimer = 0.0;
    boolean matchHumanWon = false;
    boolean matchZombieWon = false;
    boolean localPlayerZombie = false;
    UUID roundStartingZombieId;
    final UUID offlineLocalId = UUID.nameUUIDFromBytes("offline-local-player".getBytes());
    final HashMap<UUID, Boolean> remoteZombieStates = new HashMap<>();
    final HashMap<UUID, Boolean> remoteInvisibleStates = new HashMap<>();
    final HashMap<UUID, Integer> remoteHealthStates = new HashMap<>();
    final HashMap<UUID, String> remoteAnimationStates = new HashMap<>();

    int dashCharges = 0;
    int rockCharges = 0;
    final int maxRockCharges = 3;
    int babyZombieCharges = 0;
    final int maxBabyZombieCharges = 2;
    int invisCharges = 0;
    int buildMaterials = 0;
    int metalBuildMaterials = 0;
    int glassBuildMaterials = 0;
    double invisTimer = 0.0;
    double slowTimer = 0.0;
    double blindTimer = 0.0;
    double blindCooldownTimer = 0.0;
    double projectileCooldown = 0.0;
    double buildAttackCooldown = 0.0;
    final double invisDuration = 5.0;
    final double slowDuration = 3.0;
    final double projectileCooldownTime = 0.75;
    final double buildAttackCooldownTime = 0.55;
    final float humanWalkSpeed = 4.2f;
    final float humanRunSpeed = 8.0f;
    final float zombieWalkSpeed = 4.65f;
    final float zombieRunSpeed = 8.8f;
    final float slowedSpeedMultiplier = 0.45f;
    final float dashDistance = 7.0f;
    final HashMap<UUID, Double> flashlightBlindCooldowns = new HashMap<>();

    Robot robot;
    boolean mouseModeInitiated = false;
    boolean isRecentering = false;
    boolean mouseLookEnabled = true;
    Cursor defaultCursor;
    Cursor invisibleCursor;
    float curMouseX, curMouseY;
    float prevMouseX, prevMouseY;
    float centerX, centerY;
    GLCanvas canvas;
    final float mouseYawSpeed = 0.0025f;
    final float mouseElevSpeed = 0.15f;

    String serverAddress;
    int serverPort;
    ProtocolType serverProtocol;
    ProtocolClient protClient;
    boolean isClientConnected = false;
    float playerYaw = 180.0f;
    boolean byeMessageSent = false;
    boolean isMultiplayer = false;
    String selectedAvatarType = "playerModel1";
}
