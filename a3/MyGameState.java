package a3;

import java.awt.Cursor;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.HashMap;

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

    boolean lost = false;
    boolean won = false;
    int score = 0;

    final int maxHealth = 100;
    int health = 100;
    final int pyramidDamage = 25;
    final int potionHealAmount = 35;

    final float itemPickupRange = 1.25f;
    double damageCooldown = 0.0;
    final double damageCooldownTime = 1.0;

    boolean hasFlashlight = false;
    boolean hasPotion = false;
    boolean potionUsed = false;
    int equippedItem = GameConstants.ITEM_NONE;

    int helpPage = 0;
    String eventMsg = "";
    double eventHold = 0.0;

    final float walkSpeed = 4.0f;
    final float runSpeed = 8.0f;
    float runHoldTime = 0.0f;
    final float homeRange = 6.0f;

    float overX = 0f, overZ = 0f, overY = 35f;
    float padMove = 0.0f, padStrafe = 0.0f;

    boolean healthPotionSnappedToTerrain = false;
    boolean flashlightSnappedToTerrain = false;
    final float flashlightLift = 0.08f;
    final float potionLift = 0.12f;
    final float flashlightBeamForwardOffset = 0.18f;
    final float flashlightBeamLift = 0.03f;

    boolean onGround = true;
    float yVel = 0.0f;
    final boolean[] iconCollected = new boolean[3];

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
    final ArrayList<GameObject> placedWalls = new ArrayList<>();
    final HashMap<String, GameObject> wallMap = new HashMap<>();

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
    String selectedAvatar = "playerModel1";
}
