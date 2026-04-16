package a3;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import tage.*;
import tage.nodeControllers.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier.*;
import org.joml.*;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;

import com.jogamp.opengl.awt.GLCanvas;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.NetworkDiscovery;

public class MyGame extends VariableFrameRateGame {
	/*
	 * A2 requirement map
	 * Orbit camera controller: initializeGame, doOrbit, doElevate, doZoom,
	 * CameraOrbit3D in tage
	 * Second viewport overhead: initializeGame, setOverheadCamera, panOverhead,
	 * zoomOverhead
	 * HUD for both viewports: updateHUD, overHudLeftPx, overHudTopPx
	 * Ground plane and movement limits: buildObjects ground plane, doMove,
	 * doStrafe, updateJump
	 * Axes toggle: toggleAxesVisibility
	 * Node controllers: initializeGame, tryTakePhoto, RotationController and
	 * BobbingController
	 * Hierarchical photos: buildObjects hudIcons parented to dolphin,
	 * placePhotosOnWall reparent to root
	 * Global yaw: doYaw uses globalYaw in GameObject
	 */

	private static Engine engine;

	// Home sizing values are reused for building the walls and for photo placement
	// on the back wall.
	private static final float HOME_W = 8f;
	private static final float HOME_H = 4f;
	private static final float HOME_D = 12f;
	private static final float HOME_FLOOR_Y = 0.05f;
	private static final float HOME_SCALE = 0.65f;

	// Overhead viewport placement in normalized screen space.
	private static final float OVER_LEFT = 0.72f;
	private static final float OVER_BOTTOM = 0.72f;
	private static final float OVER_W = 0.26f;
	private static final float OVER_H = 0.26f;

	private InputManager im;
	private GhostManager gm;

	private double lastFrameTime, currFrameTime, elapsedTime;

	private boolean lost = false;
	private boolean won = false;
	private int score = 0;

	private final int maxHealth = 100;
	private int health = 100;

	private final int pyramidDamage = 25;
	private final int potionHealAmount = 35;

	private final float itemPickupRange = 1.25f;
	private double damageCooldown = 0.0;
	private final double damageCooldownTime = 1.0;

	private boolean hasFlashlight = false;
	private boolean hasPotion = false;
	private boolean potionUsed = false;

	private static final int ITEM_NONE = 0;
	private static final int ITEM_FLASHLIGHT = 1;
	private static final int ITEM_POTION = 2;
	private int equippedItem = ITEM_NONE;

	// Help overlay page
	// 0 means off
	// 1 shows keyboard controls
	// 2 shows gamepad controls
	private int helpPage = 0;

	private String eventMsg = "";
	private double eventHold = 0.0;

	private final float walkSpeed = 4.0f;
	private final float runSpeed = 8.0f;
	private float runHoldTime = 0.0f;
	private final float homeRange = 6.0f;

	// A2 overhead camera controls pan using x and z, and zoom using y.
	private float overX = 0f, overZ = 0f, overY = 35f;

	// Gamepad axis actions store values here, then movement runs once per frame.
	private float padMove = 0.0f;
	private float padStrafe = 0.0f;

	private boolean healthPotionSnappedToTerrain = false;
	private boolean flashlightSnappedToTerrain = false;
	private final float flashlightLift = 0.08f;
	private final float potionLift = 0.12f;
	private Light flashlightSpotlight;
	private final float flashlightBeamForwardOffset = 0.18f;
	private final float flashlightBeamLift = 0.03f;

	public void setPadMove(float v) {
		padMove = v;
	}

	public void setPadStrafe(float v) {
		padStrafe = v;
	}

	public void triggerRun() {
		runHoldTime = 0.15f;
	}

	private float getCurrentMoveSpeed() {
		if (runHoldTime > 0.0f)
			return runSpeed;
		return walkSpeed;
	}

	private boolean onGround = true;
	private float yVel = 0.0f;

	private final GameObject[] hudIcons = new GameObject[3];
	private final boolean[] iconCollected = new boolean[3];

	private ObjShape dolS, quadS, linxS, linyS, linzS;
	private ObjShape pyrS;
	private TerrainPlane terrainShape;
	private ObjShape playerModel2S, flashlightS, tableS, healthPotionS;

	// Player Models
	private ObjShape playerModel1S;
	private TextureImage playerModel1Tx;
	private TextureImage playerModel2Tx;
	private TextureImage healthPotionTx;

	// Player Avatar
	private GameObject avatar;

	// Extra A2 textures
	private TextureImage grassTx;
	private TextureImage dolTx;
	private TextureImage homeTx;

	// Extra A2 objects
	private GameObject healthPotion;
	private final TextureImage[] pyramidTx = new TextureImage[3];
	private TextureImage heightMaptx; 
	private TextureImage flashlightTx;
	private TextureImage tableTx;

	private GameObject dol;
	private final GameObject[] pyramids = new GameObject[3];
	private GameObject terrain;
	private GameObject flashlight;
	private GameObject table;

	private final Light[] lightPyramid = new Light[3];

	// A2 orbit camera controller lives in tage as CameraOrbit3D.
	private CameraOrbit3D orbitCam;
	private float orbitAzimuthDeg = 180.0f;
	private float orbitElevationDeg = 15.0f;
	private float orbitRadius = 4.5f;

	private float minOrbitElevationDeg = -10.0f;
	private float maxOrbitElevationDeg = 80.0f;
	private boolean fullMapMode = false;

	// A3 camera mode builds on the existing orbit camera instead of replacing it.
	// 0 = normal behind
	// 1 = right shoulder
	// 2 = left shoulder
	// 3 = first person
	private int cameraMode = 0;

	// Shoulder camera is only translated in camera-local x and y so the original
	// orbit direction stays unchanged.
	private float shoulderOffsetX = 1.25f;
	private float shoulderOffsetY = 0.75f;

	// Node controllers: built-in rotation and custom bobbing controller.
	private RotationController photoSpin;
	private BobbingController activatedPyramidBob;

	private GameObject axisX, axisY, axisZ;
	private boolean axesVisible = true;

	// Skybox handles for switching outdoor world backgrounds.
	private int sky04;
	private int sky15Night;

	// A3 building: simple wall placement uses existing quad geometry and home
	// texture.
	private boolean buildMode = false;
	private int buildHeightLevel = 0;
	private final float buildDistance = 3.0f;
	private final float buildGrid = 2.0f;
	private final float buildSnap = 0.5f;
	private final float buildWallHalfW = 1.0f;
	private final float buildWallHalfH = 1.0f;
	private GameObject buildPreview;

	// A3 building mode type: determines wall orientation and positioning
	// 0 = wall along X axis
	// 1 = wall along Z axis
	// 2 = floor piece
	// 3 = ceiling piece
	private int buildModeType = 0;

	// A3 build piece type: determines what piece is being built
	// 0 = wall
	// 1 = roof
	private int buildPieceType = 0;

	// A3 roof direction: which slope direction the roof uses (only for roofs)
	// 0 = slope up toward +X
	// 1 = slope up toward -X
	// 2 = slope up toward +Z
	// 3 = slope up toward -Z
	private int buildRoofDir = 0;

	// A3 placed wall storage: key is snapped grid position plus orientation.
	private final ArrayList<GameObject> placedWalls = new ArrayList<>();
	private final HashMap<String, GameObject> wallMap = new HashMap<>();

	// Mouse-look mode uses Java Robot recentering so the mouse never reaches
	// the screen edge while controlling camera orientation.
	private Robot robot;
	private boolean mouseModeInitiated = false;
	private boolean isRecentering = false;
	private boolean mouseLookEnabled = true;

	private Cursor defaultCursor;
	private Cursor invisibleCursor;

	private float curMouseX, curMouseY;
	private float prevMouseX, prevMouseY;
	private float centerX, centerY;

	private GLCanvas canvas;

	// Mouse sensitivity values tune horizontal avatar turn and vertical camera
	// elevation.
	private final float mouseYawSpeed = 0.0025f;
	private final float mouseElevSpeed = 0.15f;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	private float playerYaw = 180.0f;
	private boolean byeMessageSent = false;

	private boolean isMultiplayer = false;

	// Avatar selection at startup is sent to remote clients so their ghost uses
	// the proper model and texture.
	private String selectedAvatar = "playerModel1";

	public MyGame() {
		super();
		isMultiplayer = false;
	}

	public MyGame(String serverAddress) {
		super();
		this.serverAddress = serverAddress;
		this.serverPort = 0;
		this.serverProtocol = ProtocolType.UDP;
		isMultiplayer = true;
	}

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		isMultiplayer = true;

		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	private static String chooseAvatarPopup() {
		Object[] options = { "playerModel1", "playerModel2" };

		Object choice = JOptionPane.showInputDialog(
				null,
				"Choose your character:",
				"Avatar Selection",
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]);

		if (choice == null)
			return "playerModel1";

		return choice.toString();
	}

	public static void main(String[] args) {
		MyGame game;

		if (args.length == 0) {
			game = new MyGame();
			game.setSelectedAvatarType(chooseAvatarPopup());
		} else if (args.length >= 1 && NetworkDiscovery.usesAutoDiscovery(args[0])) {
			game = new MyGame(args[0]);

			if (args.length >= 2)
				game.setSelectedAvatarType(args[1]);
			else
				game.setSelectedAvatarType(chooseAvatarPopup());
		} else if (args.length >= 3) {
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);

			// Optional 4th argument chooses avatar type at startup.
			if (args.length >= 4)
				game.setSelectedAvatarType(args[3]);
			else
				game.setSelectedAvatarType(chooseAvatarPopup());
		} else {
			System.out.println("Usage:");
			System.out.println("Single-player: java a3.MyGame");
			System.out.println("Server       : java Server.NetworkingServer <port> <UDP|TCP>");
			System.out.println("Auto client  : java a3.MyGame AUTO [avatarType]");
			System.out.println("Manual client: java a3.MyGame <serverAddress> <serverPort> <UDP|TCP> [avatarType]");
			System.out.println("Note         : NetworkingServer relays packets only, so 2 players need 2 MyGame clients.");
			System.out.println("Auto client  : searches the LAN for a running NetworkingServer.");
			return;
		}

		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	@Override
	public void loadShapes() {
		dolS = new ImportedModel("dolphinHighPoly.obj");

		// Player character models
		playerModel1S = new ImportedModel("boy_character_textured.obj");
		playerModel2S = new ImportedModel("playerModel.obj");

		// Custom models
		flashlightS = new ImportedModel("flashlight.obj");
		tableS = new ImportedModel("table.obj");
		healthPotionS = new ImportedModel("healthpotion1.obj");

		quadS = new ManualQuad();
		pyrS = new ManualPyramid();
		terrainShape = new TerrainPlane(1000);

		linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(3f, 0f, 0f));
		linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 3f, 0f));
		linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -3f));
	}

	@Override
	public void loadTextures() {
		dolTx = new TextureImage("Dolphin_HighPolyUV.jpg");

		// Player character texture
		playerModel1Tx = new TextureImage("boy_textured.png");
		playerModel2Tx = new TextureImage("playerModel.png");

		// A2 pyramid textures: each pyramid uses a different texture.
		pyramidTx[0] = new TextureImage("3d-geometric-texture-copper_512x512.jpg");
		pyramidTx[1] = new TextureImage("pyramid2_512x512.jpg");
		pyramidTx[2] = new TextureImage("pyramidbyme_512x512.jpg");

		grassTx = new TextureImage("grass.png");
		homeTx = new TextureImage("brick1.jpg");

		// height map
		heightMaptx = new TextureImage("heightmap.png");

		// custom model textures
		flashlightTx = new TextureImage("flashlightTx.png");
		tableTx = new TextureImage("tableTx.png");
		healthPotionTx = new TextureImage("health_potion.png");
	}

	@Override
	public void loadSkyBoxes() {
		// Skybox requirement: load both cubemaps and enable one as the initial
		// world background.
		sky04 = engine.getSceneGraph().loadCubeMap("sky04_cubemap");
		sky15Night = engine.getSceneGraph().loadCubeMap("sky15_night");

		engine.getSceneGraph().setActiveSkyBoxTexture(sky04);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects() {
		buildLocalAvatar();
		buildInteractiveProps();
		buildPyramids();
		buildWorldDecor();
		buildTerrain();
		buildHealthPotion();
		buildAxes();
		buildHome();
		buildHudIcons(); 
		buildBuildPreview();
	}

	private void buildLocalAvatar() {
		ObjShape avatarShape = (selectedAvatar.compareTo("playerModel2") == 0) ? playerModel2S : playerModel1S;
		TextureImage avatarTexture = (selectedAvatar.compareTo("playerModel2") == 0) ? playerModel2Tx : playerModel1Tx;

		if (avatarShape == null)
			throw new RuntimeException("avatarShape is null for selectedAvatar = " + selectedAvatar);
		avatar = new GameObject(GameObject.root(), avatarShape, avatarTexture);
		avatar.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 6f));
		avatar.setLocalScale((new Matrix4f()).scaling(1.0f));
		avatar.setLocalRotation((new Matrix4f()).rotationY((float) java.lang.Math.toRadians(180f)));
		avatar.getRenderStates().hasLighting(true);
		avatar.getRenderStates().setModelOrientationCorrection((new Matrix4f()).identity());
		playerYaw = 180.0f;
	}

	private void buildInteractiveProps() {
		flashlight = new GameObject(GameObject.root(), flashlightS, flashlightTx);
		flashlight.setLocalTranslation((new Matrix4f()).translation(0f, 0.5f, 6f));
		flashlight.setLocalScale((new Matrix4f()).scaling(0.18f));
		flashlight.setLocalRotation((new Matrix4f()).rotationY((float) java.lang.Math.toRadians(180f)));

		table = new GameObject(GameObject.root(), tableS, tableTx);
		table.setLocalTranslation((new Matrix4f()).translation(5f, 0f, 8f));
		table.setLocalScale((new Matrix4f()).scaling(0.4f));
	}

	private void buildPyramids() {
		for (int i = 0; i < 3; i++) {
			pyramids[i] = new GameObject(GameObject.root(), pyrS, pyramidTx[i]);
			pyramids[i].setLocalScale((new Matrix4f()).scaling(5f));
			pyramids[i].getRenderStates().hasLighting(true);
		}

		pyramids[0].setLocalTranslation((new Matrix4f()).translation(-24f, 0f, -10f));
		pyramids[1].setLocalTranslation((new Matrix4f()).translation(34f, 0f, 0f));
		pyramids[2].setLocalTranslation((new Matrix4f()).translation(18f, 0f, 10f));
	}

	private void buildWorldDecor() {
		dol = new GameObject(GameObject.root(), dolS, dolTx);
		dol.setLocalTranslation((new Matrix4f()).translation(-8f, 0f, -12f));
		dol.setLocalScale((new Matrix4f()).scaling(1.0f));
		dol.setLocalRotation((new Matrix4f()).rotationY((float) java.lang.Math.toRadians(45f)));
		dol.getRenderStates().hasLighting(true);
	}

	private void buildTerrain() {
		terrain = new GameObject(GameObject.root(), terrainShape, grassTx);
		terrain.setLocalTranslation(new Matrix4f().translation(0, 0, 0));
		terrain.setLocalScale(new Matrix4f().scaling(200.0f, 20f, 200.0f));
		terrain.setHeightMap(heightMaptx);
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);
	}

	private void buildHealthPotion() {
		healthPotion = new GameObject(GameObject.root(), healthPotionS, healthPotionTx);
		healthPotion.setLocalTranslation((new Matrix4f()).translation(3f, 0.15f, -3f));
		healthPotion.setLocalScale((new Matrix4f()).scaling(0.15f));
		healthPotion.setLocalRotation((new Matrix4f()).identity());
		healthPotion.getRenderStates().hasLighting(true);
		healthPotion.getRenderStates().isTransparent(true);
		healthPotion.getRenderStates().setOpacity(0.4f);
	}

	private void buildAxes() {
		axisX = new GameObject(GameObject.root(), linxS);
		axisY = new GameObject(GameObject.root(), linyS);
		axisZ = new GameObject(GameObject.root(), linzS);
		axisX.getRenderStates().setColor(new Vector3f(1f, 0f, 0f));
		axisY.getRenderStates().setColor(new Vector3f(0f, 1f, 0f));
		axisZ.getRenderStates().setColor(new Vector3f(0f, 0f, 1f));

		Matrix4f lift = new Matrix4f().translation(0f, 0.1f, 0f);
		axisX.setLocalTranslation(lift);
		axisY.setLocalTranslation(lift);
		axisZ.setLocalTranslation(lift);
	}

	private void buildHome() {
		float s = HOME_SCALE;
		float w = HOME_W * s;
		float h = HOME_H * s;
		float d = HOME_D * s;
		float floorY = HOME_FLOOR_Y;
		float hw = w / 2f;
		float hh = h / 2f;
		float hd = d / 2f;

		buildHomeQuad(
				(new Matrix4f()).rotationX((float) java.lang.Math.toRadians(-90f)),
				(new Matrix4f()).scaling(hw, hd, 1f),
				(new Matrix4f()).translation(0f, floorY, 0f));
		buildHomeQuad(
				null,
				(new Matrix4f()).scaling(hw, hh, 1f),
				(new Matrix4f()).translation(0f, floorY + hh, -hd));
		buildHomeQuad(
				(new Matrix4f()).rotationY((float) java.lang.Math.toRadians(90f)),
				(new Matrix4f()).scaling(hd, hh, 1f),
				(new Matrix4f()).translation(-hw, floorY + hh, 0f));
		buildHomeQuad(
				(new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90f)),
				(new Matrix4f()).scaling(hd, hh, 1f),
				(new Matrix4f()).translation(hw, floorY + hh, 0f));

		float doorW = 2.6f * s;
		float sideW = (w - doorW) / 2f;
		float hSideW = sideW / 2f;
		Matrix4f frontRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(180f));

		buildHomeQuad(
				frontRotation,
				(new Matrix4f()).scaling(hSideW, hh, 1f),
				(new Matrix4f()).translation(-(doorW / 2f + hSideW), floorY + hh, hd));
		buildHomeQuad(
				frontRotation,
				(new Matrix4f()).scaling(hSideW, hh, 1f),
				(new Matrix4f()).translation((doorW / 2f + hSideW), floorY + hh, hd));

		buildHomeRoof(w, d, floorY + h);
	}

	private GameObject buildHomeQuad(Matrix4f rotation, Matrix4f scale, Matrix4f translation) {
		GameObject quad = new GameObject(GameObject.root(), quadS, homeTx);
		if (rotation != null)
			quad.setLocalRotation(rotation);
		quad.setLocalScale(scale);
		quad.setLocalTranslation(translation);
		quad.getRenderStates().hasLighting(true);
		return quad;
	}

	private void buildHomeRoof(float w, float d, float wallTopY) {
		float halfW = w / 2f;
		float halfD = d / 2f;
		float roofAngle = (float) java.lang.Math.toRadians(45f);
		float roofRise = (float) java.lang.Math.tan(roofAngle) * halfW;
		float slantLen = halfW / (float) java.lang.Math.cos(roofAngle);
		float roofHalfSlant = slantLen / 2f;

		GameObject roofLeft = new GameObject(GameObject.root(), quadS, homeTx);
		roofLeft.getRenderStates().hasLighting(true);
		roofLeft.setLocalScale((new Matrix4f()).scaling(roofHalfSlant, halfD, 1f));
		roofLeft.setLocalRotation((new Matrix4f()).rotationZ(roofAngle).rotateX((float) java.lang.Math.toRadians(90f)));
		roofLeft.setLocalTranslation((new Matrix4f()).translation(-halfW / 2f, wallTopY + roofRise / 2f, 0f));

		GameObject roofRight = new GameObject(GameObject.root(), quadS, homeTx);
		roofRight.getRenderStates().hasLighting(true);
		roofRight.setLocalScale((new Matrix4f()).scaling(roofHalfSlant, halfD, 1f));
		roofRight.setLocalRotation(
				(new Matrix4f()).rotationZ(-roofAngle).rotateX((float) java.lang.Math.toRadians(90f)));
		roofRight.setLocalTranslation((new Matrix4f()).translation(halfW / 2f, wallTopY + roofRise / 2f, 0f));
	}

	private void buildHudIcons() {
		for (int i = 0; i < 3; i++) {
			hudIcons[i] = new GameObject(avatar, quadS, pyramidTx[i]);
			hudIcons[i].getRenderStates().hasLighting(false);
			hudIcons[i].setLocalScale((new Matrix4f()).scaling(0f));

			float xOff = 0.55f - (i * 0.55f);
			hudIcons[i].setLocalTranslation((new Matrix4f()).translation(xOff, 1.1f, -1.0f));
			hudIcons[i].setLocalRotation((new Matrix4f()).identity());
			iconCollected[i] = false;
		}
	}

	private void buildBuildPreview() {
		buildPreview = new GameObject(GameObject.root(), quadS, homeTx);
		buildPreview.setLocalScale((new Matrix4f()).scaling(buildWallHalfW, buildWallHalfH, 1f));
		buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
		buildPreview.getRenderStates().hasLighting(true);
	}

	@Override
	public void initializeLights() {
		Light.setGlobalAmbient(0.35f, 0.35f, 0.35f);

		for (int i = 0; i < 3; i++) {
			lightPyramid[i] = new Light();
			Vector3f p = pyramids[i].getWorldLocation();
			lightPyramid[i].setLocation(new Vector3f(p.x, p.y + 8f, p.z));
			engine.getSceneGraph().addLight(lightPyramid[i]);
		}

		Light lightHome = new Light();
		lightHome.setLocation(new Vector3f(0f, 8f, 0f));
		engine.getSceneGraph().addLight(lightHome);

		flashlightSpotlight = new Light();
		flashlightSpotlight.setType(Light.LightType.SPOTLIGHT);
		flashlightSpotlight.setAmbient(0.0f, 0.0f, 0.0f);
		flashlightSpotlight.setDiffuse(0.95f, 0.92f, 0.82f);
		flashlightSpotlight.setSpecular(1.0f, 0.97f, 0.88f);
		flashlightSpotlight.setCutoffAngle(18.0f);
		flashlightSpotlight.setOffAxisExponent(10.0f);
		flashlightSpotlight.setLinearAttenuation(0.08f);
		flashlightSpotlight.setQuadraticAttenuation(0.03f);
		flashlightSpotlight.setLocation(new Vector3f(0f, -100f, 0f));
		flashlightSpotlight.setDirection(new Vector3f(0f, 0f, -1f));
		flashlightSpotlight.disable();
		engine.getSceneGraph().addLight(flashlightSpotlight);
	}

	@Override
	public void initializeGame() {
		initializeFrameTiming();
		configureWindow();
		setupCameras();
		setupNodeControllers();
		setupInputs();
		setupNetworkingIfNeeded();
		installShutdownHandlers();
	}

	private void initializeFrameTiming() {
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsedTime = 0.0;
	}

	private void configureWindow() {
		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);
	}

	private void setupCameras() {
		Camera mainCam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		orbitCam = new CameraOrbit3D(mainCam, avatar);
		orbitCam.setRadius(orbitRadius);
		orbitCam.setElevationDeg(orbitElevationDeg);
		orbitCam.setAzimuthDeg(orbitAzimuthDeg);
		orbitCam.update();
		applyCameraModeOffset();

		// A2 second viewport: overhead view of the same world with pan and zoom
		// controls.
		engine.getRenderSystem().addViewport("OVER", OVER_BOTTOM, OVER_LEFT, OVER_W, OVER_H);
		Camera overCam = engine.getRenderSystem().getViewport("OVER").getCamera();
		setOverheadCamera(overCam);
	}

	private void setupNodeControllers() {
		photoSpin = new RotationController(engine, new Vector3f(0, 1, 0), 0.002f);
		engine.getSceneGraph().addNodeController(photoSpin);
		photoSpin.enable();

		activatedPyramidBob = new BobbingController(engine, 0.35f, 1.2f);
		engine.getSceneGraph().addNodeController(activatedPyramidBob);
		activatedPyramidBob.enable();
	}

	private void setupInputs() {
		im = engine.getInputManager();
		setupKeyboardInputs();
		setupGamepadInputs();
	}

	private void setupKeyboardInputs() {
		im.associateActionWithAllKeyboards(Key.W, new MoveAction(this, 1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.S, new MoveAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Key.LEFT, new YawAction(this, 1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.RIGHT, new YawAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Key.A, new StrafeAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.D, new StrafeAction(this, 1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Key.LSHIFT, new RunAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Key.SPACE, new JumpAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllKeyboards(Key.H, new UsePotionAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.F1, new ToggleHelpAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// A2 win action: photos move to the home wall using ENTER.
		im.associateActionWithAllKeyboards(Key.RETURN, new PlacePhotosAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// A2 photo action: pyramid activates after being photographed.
		im.associateActionWithAllKeyboards(Key.P, new TakePhotoAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// A2 orbit camera inputs.
		im.associateActionWithAllKeyboards(Key.Q, new OrbitAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.E, new OrbitAction(this, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.UP, new ElevateAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.DOWN, new ElevateAction(this, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.Z, new ZoomAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.X, new ZoomAction(this, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// A2 overhead camera inputs.
		im.associateActionWithAllKeyboards(Key.I, new OverPanAction(this, 0f, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.K, new OverPanAction(this, 0f, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.J, new OverPanAction(this, -1f, 0f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.L, new OverPanAction(this, +1f, 0f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Key.U, new OverZoomAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Key.O, new OverZoomAction(this, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// A2 axes toggle.
		im.associateActionWithAllKeyboards(Key.T, new ToggleAxesAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key._4, new EquipFlashlightAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key._5, new EquipPotionAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key._6, new UnequipItemAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// Skybox switching.
		im.associateActionWithAllKeyboards(Key._1, new SkyboxAction(this, 1),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key._2, new SkyboxAction(this, 2),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key._3, new SkyboxAction(this, 3),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// A3 building controls: player can toggle build mode, place walls, remove
		// walls, and raise or lower the build height.
		im.associateActionWithAllKeyboards(Key.B, new ToggleBuildAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.V, new PlaceBuildWallAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.C, new RemoveBuildWallAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.R, new RotateBuildWallAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.F, new SwitchBuildPieceAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.TAB, new ToggleMouseLookAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.PERIOD, new RaiseBuildHeightAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.M, new ToggleFullMapAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.N, new LowerBuildHeightAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// A3 camera controls: G cycles camera mode and Y swaps shoulder side.
		im.associateActionWithAllKeyboards(Key.G, new ToggleCameraModeAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.Y, new SwapShoulderAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	private void setupGamepadInputs() {
		im.associateActionWithAllGamepads(Axis.Y, new MoveAxisAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Axis.X, new StrafeAxisAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(Axis.RX, new YawAxisAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Axis.RY, new ElevateAxisAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(Button._2, new TakePhotoAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllGamepads(Button._0, new PlacePhotosAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllGamepads(Button._4, new OrbitAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Button._5, new OrbitAction(this, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(Button._7, new ZoomAction(this, -1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Button._6, new ZoomAction(this, +1f),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(Axis.POV, new PovOverPanAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Axis.Z, new OverZoomAxisAction(this),
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(Button._1, new ToggleAxesAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllGamepads(Button._3, new JumpAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllGamepads(Button._9, new ToggleHelpAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	private void setupNetworkingIfNeeded() {
		if (isMultiplayer)
			setupNetworking();
	}

	private void installShutdownHandlers() {
		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			sendByeAndClose();
		}));

		try {
			JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(engine.getRenderSystem().getGLCanvas());
			if (frame != null) {
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						sendByeAndClose();
					}
				});
			}
		} catch (Exception e) {
			System.out.println("unable to attach window close handler");
		}
	}

	public float getPlayerYaw() {
		return playerYaw;
	}


	@Override
	public void update() {
		ensureMouseModeInitialized();
		updateFrameTiming();
		updateTransientTimers();
		processPlayerInput();
		syncPlayerToTerrain();
		updateCameraLimits();
		checkCrash();
		updateMainCamera();
		updateWorldState();
		processNetworking((float) elapsedTime);
		updateHUD();
		updateRunState((float) elapsedTime);
	}

	private void ensureMouseModeInitialized() {
		if (!mouseModeInitiated)
			initMouseMode();
	}

	private void updateFrameTiming() {
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsedTime = (currFrameTime - lastFrameTime) / 1000.0;
	}

	private void updateTransientTimers() {
		if (damageCooldown > 0.0)
			damageCooldown -= elapsedTime;

		if (eventHold > 0.0) {
			eventHold -= elapsedTime;
			if (eventHold <= 0.0)
				eventMsg = "";
		}
	}

	private void processPlayerInput() {
		padMove = 0.0f;
		padStrafe = 0.0f;

		im.update((float) elapsedTime);

		// Gamepad movement runs once per frame so diagonal speed stays consistent.
		applyPadMovement((float) elapsedTime);
		updateJump((float) elapsedTime);
	}

	private void syncPlayerToTerrain() {
		Vector3f loc = avatar.getWorldLocation();
		float groundY = getGroundHeight(loc.x(), loc.z());

		if (onGround) {
			boolean yChanged = java.lang.Math.abs(loc.y - groundY) > 0.001f;
			avatar.setLocalLocation(new Vector3f(loc.x(), groundY, loc.z()));

			if (yChanged)
				sendPlayerTransform();
		}
	}

	private void updateMainCamera() {
		Camera mainCam = engine.getRenderSystem().getViewport("MAIN").getCamera();

		if (fullMapMode) {
			Vector3f p = avatar.getWorldLocation();

			mainCam.setLocation(new Vector3f(p.x, 60f, p.z));
			mainCam.setN(new Vector3f(0f, -1f, 0f));
			mainCam.setU(new Vector3f(1f, 0f, 0f));
			mainCam.setV(new Vector3f(0f, 0f, -1f));
		} else {
			if (orbitCam != null) {
				orbitCam.update();
				applyCameraModeOffset();
			}
		}
	}

	private void updateWorldState() {
		updateCarriedFlashlightPose();
		updateCarriedPotionPose();
		updateBuildPreview();
		updateFlashlightHeight();
		updateHealthPotionHeight();
		checkItemPickups();
		updateFlashlightSpotlight();
	}

	private void updateRunState(float dt) {
		runHoldTime -= dt;
		if (runHoldTime < 0.0f)
			runHoldTime = 0.0f;
	}

	private void updateCameraLimits() {
		if (selectedAvatar.compareTo("playerModel2") == 0) {
			// restrict downward look more to avoid clipping into head
			minOrbitElevationDeg = -10.0f;
			maxOrbitElevationDeg = 76.0f;
		} else {
			// avatar can look more freely
			minOrbitElevationDeg = -10.0f;
			maxOrbitElevationDeg = 80.0f;
		}
	}

	private void showEvent(String msg, double seconds) {
		eventMsg = msg;
		eventHold = seconds;
	}

	// HUD1 is always score and event text.
	// HUD2 and HUD3 are either the game guide or the help overlay.
	// HUD4 is always the overhead viewport text showing player world position.
	private void updateHUD() {
		HUDmanager hud = engine.getHUDmanager();

		String top = "HEALTH " + health + "/" + maxHealth + "   |   COLLECTED " + score + "/3";
		if (!eventMsg.isEmpty())
			top = top + "   |   " + eventMsg;
		hud.setHUD1(top, new Vector3f(0.75f, 0.75f, 0.35f), 15, 15);

		if (helpPage == 1) {
			// Help page for keyboard controls
			String line1 = "KEY W S MOVE   A D STRAFE   LEFT RIGHT TURN   SHIFT RUN   SPACE JUMP   P PHOTO   ENTER PLACE";
			String line2 = "T AXES G CAMERA Y SWAP SHOULDER Q E ORBIT UP DOWN ELEVATE Z X ZOOM I K J L PAN OVER U O OVER ZOOM";
			hud.setHUD2(line1, new Vector3f(0, 0, 0), 15, 40);
			hud.setHUD3(line2, new Vector3f(0, 0, 0), 15, 65);
		} else if (helpPage == 2) {
			// Help page for gamepad controls
			String line1 = "PAD LS MOVE STRAFE   RS X TURN   Y JUMP   X PHOTO   A PLACE   B AXES   PAD LB RB ORBIT";
			String line2 = "RS Y ELEVATE   BACK ZOOM IN   START ZOOM OUT   DPAD PAN OVER   LT OVER IN   RT OVER OUT";
			hud.setHUD2(line1, new Vector3f(0, 0, 0), 15, 40);
			hud.setHUD3(line2, new Vector3f(0, 0, 0), 15, 65);
		} else {
			// Normal guide does not list controls
			String[] lines = buildGuideLines();
			hud.setHUD2(lines[0], new Vector3f(0, 0, 0), 15, 40);
			hud.setHUD3(lines[1], new Vector3f(0, 0, 0), 15, 65);
		}

		// Overhead viewport HUD shows player world position
		if (!fullMapMode) {
			Vector3f p = avatar.getWorldLocation();
			String pos = String.format("POS (%.1f, %.1f, %.1f)", p.x, p.y, p.z);
			int x = overHudLeftPx() + 12;
			int y = overHudTopPx() + 18;
			hud.setHUD4(pos, new Vector3f(0, 0, 0), x, y);
		} else {
			hud.setHUD4("", new Vector3f(0, 0, 0), 0, 0);
		}
	}

	private String[] buildGuideLines() {
		if (isLost()) {
			return new String[] {
					"CRASHED!",
					"YOU LOSE"
			};
		}

		if (isWon()) {
			return new String[] {
					"PHOTOS PLACED!",
					"YOU WIN"
			};
		}

		if (buildMode) {
			String pieceName = (buildPieceType == 0) ? "WALL" : "ROOF";

			return new String[] {
					"BUILD MODE: V PLACE   C REMOVE   R ROTATE   F SWITCH TYPE   TAB LOCK/UNLOCK   . UP   N DOWN",
					"PIECE " + pieceName + "   HEIGHT " + buildHeightLevel + "   PRESS B TO EXIT BUILD MODE"
			};
		}

		if (score == 3) {
			if (distanceToHome() <= homeRange) {
				return new String[] {
						"HOME REACHED: PLACE PHOTOS",
						"PRESS ENTER OR A ON CONTROLLER"
				};
			}
			return new String[] {
					"ALL PHOTOS TAKEN: RETURN HOME",
					"FLY HOME THEN PRESS ENTER OR A ON CONTROLLER"
			};
		}

		int near = closestPyramidWithinRange();
		if (near >= 0) {
			return new String[] {
					"PYRAMID IN RANGE: TAKE PHOTO",
					"PRESS P OR X ON CONTROLLER"
			};
		}

		return new String[] {
				"FIND A PYRAMID AND GET CLOSER",
				"PRESS B TO ENTER BUILD MODE TAB LOCK/UNLOCK MOUSE G CAMERA Y SWAP"
		};
	}

	private Vector3f dolphinForward() {
		return (new Vector3f(avatar.getWorldForwardVector())).normalize();
	}

	private float distanceToHome() {
		return avatar.getWorldLocation().distance(new Vector3f(0f, 0f, 0f));
	}

	// Collision uses pyramid scale plus a buffer so contact feels fair.
	private static final float PYR_COLLIDER_MULT = 1.35f;
	private static final float PYR_COLLIDER_PAD = 0.6f;

	private float pyramidCollisionRadius(int i) {
		float s = pyramids[i].getLocalScale().m00();
		return (s * PYR_COLLIDER_MULT) + PYR_COLLIDER_PAD;
	}

	private void checkCrash() {
		if (isLost() || isWon())
			return;

		if (damageCooldown > 0.0)
			return;

		Vector3f dLoc = avatar.getWorldLocation();

		for (int i = 0; i < 3; i++) {
			float dist = dLoc.distance(pyramids[i].getWorldLocation());
			if (dist < pyramidCollisionRadius(i)) {
				damagePlayer(pyramidDamage);
				damageCooldown = damageCooldownTime;

				showEvent("TOOK DAMAGE -" + pyramidDamage, 1.0);

				if (health <= 0) {
					lost = true;
					showEvent("OUT OF HEALTH", 2.0);
				}
				return;
			}
		}
	}

	private float pyramidRadius(int i) {
		return pyramids[i].getLocalScale().m00();
	}

	private int closestPyramidWithinRange() {
		Vector3f dLoc = avatar.getWorldLocation();
		int best = -1;
		float bestDist = Float.MAX_VALUE;

		for (int i = 0; i < 3; i++) {
			if (iconCollected[i])
				continue;

			float dist = dLoc.distance(pyramids[i].getWorldLocation());

			float photoRange = 7.0f;
			float ok = pyramidRadius(i) + photoRange;

			if (dist < ok && dist < bestDist) {
				bestDist = dist;
				best = i;
			}
		}
		return best;
	}

	// A2 photo action: taking a photo reveals the icon, increments score, and
	// activates pyramid movement.
	public void tryTakePhoto() {
		if (isLost())
			return;
		if (isWon())
			return;

		int idx = closestPyramidWithinRange();
		if (idx == -1) {
			showEvent("NOT CLOSE ENOUGH", 1.0);
			return;
		}

		if (iconCollected[idx]) {
			showEvent("ALREADY PHOTOGRAPHED", 1.0);
			return;
		}

		iconCollected[idx] = true;
		score++;
		showEvent("PICTURE TAKEN", 1.0);

		// HUD photos are parented to the avatar, so compensate for avatar scale so
		// both avatar types show the same world-size photo while it is attached.
		hudIcons[idx].setLocalScale(getCollectedHudIconScale());

		photoSpin.addTarget(pyramids[idx]);
		activatedPyramidBob.addTarget(hudIcons[idx]);

		if (protClient != null && isClientConnected)
			protClient.sendPhotoMessage(idx);

		if (score == 3)
			showEvent("ALL PHOTOS TAKEN  RETURN HOME", 2.0);
	}

	// A2 win action: ENTER places photos on the wall when close enough to home.
	public void tryPlacePhotos() {
		if (isLost())
			return;
		if (isWon())
			return;

		if (score < 3) {
			showEvent("GET ALL 3 PHOTOS FIRST", 1.5);
			return;
		}
		if (distanceToHome() > homeRange) {
			showEvent("RETURN HOME TO PLACE PHOTOS", 1.5);
			return;
		}

		placePhotosOnWall();
		won = true;

		if (protClient != null && isClientConnected)
			protClient.sendPlacePhotosMessage();

		activatedPyramidBob.disable();

		showEvent("PHOTOS PLACED", 2.0);
	}

	public void applyRemoteBuild(int pieceType, int modeType, int roofDir, Vector3f p) {
		String key = (pieceType == 0) ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType)
				: String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
		if (wallMap.containsKey(key))
			return;

		GameObject piece = new GameObject(GameObject.root(), quadS, homeTx);
		if (pieceType == 0)
			piece.setLocalScale((new Matrix4f()).scaling(buildWallHalfW, buildWallHalfH, 1f));
		else
			piece.setLocalScale((new Matrix4f()).scaling(1.414f, buildWallHalfH, 1f));

		// Temporarily swap build state to reuse existing rotation logic
		int oldPiece = buildPieceType, oldMode = buildModeType, oldRoof = buildRoofDir;
		buildPieceType = pieceType;
		buildModeType = modeType;
		buildRoofDir = roofDir;

		piece.setLocalRotation(getBuildPieceRotation());
		piece.setLocalTranslation((new Matrix4f()).translation(p));
		piece.getRenderStates().hasLighting(true);

		buildPieceType = oldPiece;
		buildModeType = oldMode;
		buildRoofDir = oldRoof;

		placedWalls.add(piece);
		wallMap.put(key, piece);
	}

	public void applyRemoteRemoveBuild(int pieceType, int modeType, int roofDir, Vector3f p) {
		String key = (pieceType == 0) ? String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, modeType)
				: String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, roofDir);
		GameObject piece = wallMap.get(key);
		if (piece != null) {
			piece.getRenderStates().disableRendering();
			placedWalls.remove(piece);
			wallMap.remove(key);
		}
	}

	public void applyRemotePhoto(int idx) {
		if (idx < 0 || idx >= 3 || iconCollected[idx])
			return;

		iconCollected[idx] = true;
		score++;

		// HUD photos are parented to the avatar, so compensate for avatar scale so
		// both avatar types show the same world-size photo while it is attached.
		hudIcons[idx].setLocalScale(getCollectedHudIconScale());

		photoSpin.addTarget(pyramids[idx]);
		activatedPyramidBob.addTarget(hudIcons[idx]);

		showEvent("REMOTE PLAYER TOOK PHOTO", 1.5);
	}

	public void applyRemotePlacePhotos() {
		if (won)
			return;
		placePhotosOnWall();
		won = true;
		activatedPyramidBob.disable();
		showEvent("REMOTE PLAYER PLACED PHOTOS", 2.0);
	}

	private void placePhotosOnWall() {
		float s = HOME_SCALE;
		float w = HOME_W * s;
		float h = HOME_H * s;
		float d = HOME_D * s;

		float spacing = w * 0.30f;
		float[] x = new float[] { -spacing, 0.0f, spacing };

		float wallZ = -(d / 2f) + 0.02f;
		float wallY = HOME_FLOOR_Y + (h / 2f);

		for (int i = 0; i < 3; i++) {
			if (!iconCollected[i])
				continue;

			// Reparenting switches the photo from avatar space to world space.
			hudIcons[i].setParent(GameObject.root());

			hudIcons[i].getRenderStates().hasLighting(true);
			hudIcons[i].setLocalScale((new Matrix4f()).scaling(1.0f, 0.7f, 1f));
			hudIcons[i].setLocalTranslation((new Matrix4f()).translation(x[i], wallY, wallZ));
			hudIcons[i].setLocalRotation((new Matrix4f()).identity());
		}
	}

	// A2 overhead camera: UVN set to look straight down, then movement adjusts only
	// the camera location.
	private void setOverheadCamera(Camera cam) {
		cam.setLocation(new Vector3f(overX, overY, overZ));
		cam.setN(new Vector3f(0f, -1f, 0f));
		cam.setU(new Vector3f(1f, 0f, 0f));
		cam.setV(new Vector3f(0f, 0f, -1f));
	}

	public void panOverhead(float dx, float dz) {
		overX += dx;
		overZ += dz;
		setOverheadCamera(engine.getRenderSystem().getViewport("OVER").getCamera());
	}

	public void zoomOverhead(float dy) {
		overY += dy;

		if (overY < 10f)
			overY = 10f;
		if (overY > 120f)
			overY = 120f;

		setOverheadCamera(engine.getRenderSystem().getViewport("OVER").getCamera());
	}

	public void toggleFullMap() {
		fullMapMode = !fullMapMode;

		if (fullMapMode) {
			engine.getRenderSystem().getViewport("OVER").setEnabled(false);
			showEvent("MAP OPEN", 1.0);
		} else {
			engine.getRenderSystem().getViewport("OVER").setEnabled(true);
			showEvent("MAP CLOSED", 1.0);
		}
	}

	// A2 axes toggle: enableRendering and disableRendering keeps the scenegraph
	// objects intact.
	public void toggleAxesVisibility() {
		axesVisible = !axesVisible;

		if (axesVisible) {
			axisX.getRenderStates().enableRendering();
			axisY.getRenderStates().enableRendering();
			axisZ.getRenderStates().enableRendering();
			showEvent("AXES ON", 0.8);
		} else {
			axisX.getRenderStates().disableRendering();
			axisY.getRenderStates().disableRendering();
			axisZ.getRenderStates().disableRendering();
			showEvent("AXES OFF", 0.8);
		}
	}

	// Cycles the help overlay pages
	// Pressing help repeatedly rotates through off keyboard gamepad
	public void toggleHelp() {
		helpPage++;
		if (helpPage > 2)
			helpPage = 0;

		if (helpPage == 1)
			showEvent("HELP KEYBOARD", 1.0);
		else if (helpPage == 2)
			showEvent("HELP GAMEPAD", 1.0);
		else
			showEvent("HELP OFF", 1.0);
	}

	// A3 building mode toggle: preview is shown only while build mode is active.
	public void toggleBuildMode() {
		if (isLost())
			return;
		if (isWon())
			return;

		buildMode = !buildMode;

		if (buildMode)
			showEvent("BUILD MODE ON", 1.0);
		else
			showEvent("BUILD MODE OFF", 1.0);
	}

	// A3 building height: lets the player stack walls above ground level.
	public void raiseBuildHeight() {
		if (!buildMode)
			return;

		buildHeightLevel++;
		showEvent("BUILD HEIGHT " + buildHeightLevel, 0.8);
	}

	public void lowerBuildHeight() {
		if (!buildMode)
			return;

		buildHeightLevel--;
		if (buildHeightLevel < 0)
			buildHeightLevel = 0;

		showEvent("BUILD HEIGHT " + buildHeightLevel, 0.8);
	}

	// A3 building rotation: rotates walls through 4 modes, or toggles roof slopes
	public void rotateBuildWall() {
		if (!buildMode)
			return;

		if (buildPieceType == 0) {
			// wall rotation modes: cycle through 0-3
			buildModeType++;
			if (buildModeType > 3)
				buildModeType = 0;

			switch (buildModeType) {
				case 0:
					showEvent("BUILD FRONT WALL", 0.8);
					break;
				case 1:
					showEvent("BUILD SIDE WALL", 0.8);
					break;
				case 2:
					showEvent("BUILD FLOOR", 0.8);
					break;
				case 3:
					showEvent("BUILD CEILING", 0.8);
					break;
			}
		} else {
			// roof: rotate through 4 directions
			buildRoofDir++;
			if (buildRoofDir > 3)
				buildRoofDir = 0;

			switch (buildRoofDir) {
				case 0:
					showEvent("ROOF +X", 0.8);
					break;
				case 1:
					showEvent("ROOF -X", 0.8);
					break;
				case 2:
					showEvent("ROOF +Z", 0.8);
					break;
				case 3:
					showEvent("ROOF -Z", 0.8);
					break;
			}
		}
	}

	// Method to switch between wall and roof piece types
	public void switchBuildPiece() {
		buildPieceType++;
		if (buildPieceType > 1)
			buildPieceType = 0;

		switch (buildPieceType) {
			case 0:
				showEvent("BUILD PIECE: WALL", 0.8);
				break;
			case 1:
				showEvent("BUILD PIECE: ROOF", 0.8);
				break;
		}
	}

	// Helper method to get the rotation matrix based on piece type and orientation
	private Matrix4f getBuildPieceRotation() {
		if (buildPieceType == 0) {
			// wall pieces
			switch (buildModeType) {
				case 0:
					return new Matrix4f().rotationY(0f);
				case 1:
					return new Matrix4f().rotationY((float) java.lang.Math.toRadians(90.0f));
				case 2:
					return new Matrix4f().rotationX((float) java.lang.Math.toRadians(-90.0f));
				case 3:
					return new Matrix4f().rotationX((float) java.lang.Math.toRadians(90.0f));
				default:
					return new Matrix4f().identity();
			}
		} else {
			// roof pieces: slanted roof in X/Z with Y rotation for Z ridges
			float roofAngle = (float) java.lang.Math.toRadians(45.0f);

			switch (buildRoofDir) {
				case 0: // slopes across X +X ridge
					return new Matrix4f()
							.rotationZ(roofAngle)
							.rotateX((float) java.lang.Math.toRadians(90.0f));
				case 1: // slopes across X -X ridge
					return new Matrix4f()
							.rotationZ(-roofAngle)
							.rotateX((float) java.lang.Math.toRadians(90.0f));
				case 2: // slopes across Z +Z ridge
					return new Matrix4f()
							.rotationY((float) java.lang.Math.toRadians(90.0f))
							.rotateZ(roofAngle)
							.rotateX((float) java.lang.Math.toRadians(90.0f));
				case 3: // slopes across Z -Z ridge
					return new Matrix4f()
							.rotationY((float) java.lang.Math.toRadians(90.0f))
							.rotateZ(-roofAngle)
							.rotateX((float) java.lang.Math.toRadians(90.0f));
				default:
					return new Matrix4f().identity();
			}
		}
	}

	// A3 build preview: wall/roof segment snaps to a grid in front of the player
	// and
	// rotates to match the manual build rotation set by the player.
	private void updateBuildPreview() {
		if (!buildMode || isLost() || isWon()) {
			buildPreview.setLocalTranslation((new Matrix4f()).translation(0f, -1000f, 0f));
			return;
		}

		Vector3f p = getBuildPiecePosition();

		if (buildPieceType == 0)
			buildPreview.setLocalScale((new Matrix4f()).scaling(buildWallHalfW, buildWallHalfH, 1f));
		else
			buildPreview.setLocalScale((new Matrix4f()).scaling(1.414f, buildWallHalfH, 1f));
		buildPreview.setLocalRotation(getBuildPieceRotation());
		buildPreview.setLocalTranslation((new Matrix4f()).translation(p));
	}

	// A3 build placement: creates a new wall/roof quad at the snapped preview
	// position
	// if that grid cell is empty.
	public void placeBuildWall() {
		if (!buildMode || isLost() || isWon())
			return;

		Vector3f p = getBuildPiecePosition();
		String key = buildPieceKey(p);

		if (wallMap.containsKey(key)) {
			showEvent("PIECE ALREADY THERE", 1.0);
			return;
		}

		GameObject piece = new GameObject(GameObject.root(), quadS, homeTx);
		if (buildPieceType == 0)
			piece.setLocalScale((new Matrix4f()).scaling(buildWallHalfW, buildWallHalfH, 1f));
		else
			piece.setLocalScale((new Matrix4f()).scaling(1.414f, buildWallHalfH, 1f));
		piece.setLocalRotation(getBuildPieceRotation());
		piece.setLocalTranslation((new Matrix4f()).translation(p));
		piece.getRenderStates().hasLighting(true);

		placedWalls.add(piece);
		wallMap.put(key, piece);

		if (protClient != null && isClientConnected)
			protClient.sendBuildMessage(buildPieceType, buildModeType, buildRoofDir, p);

		if (buildPieceType == 0)
			showEvent("WALL PLACED", 0.8);
		else
			showEvent("ROOF PLACED", 0.8);
	}

	// A3 build removal: removes the piece at the current snapped preview position.
	// Rendering is disabled so the object no longer appears in the scene.
	public void removeBuildWall() {
		if (!buildMode || isLost() || isWon())
			return;

		Vector3f p = getBuildPiecePosition();
		String key = buildPieceKey(p);

		GameObject piece = wallMap.get(key);
		if (piece == null) {
			showEvent("NO PIECE THERE", 0.8);
			return;
		}

		piece.getRenderStates().disableRendering();
		placedWalls.remove(piece);
		wallMap.remove(key);

		if (protClient != null && isClientConnected)
			protClient.sendRemoveBuildMessage(buildPieceType, buildModeType, buildRoofDir, p);

		showEvent("PIECE REMOVED", 0.8);
	}

	private float snap(float v, float step) {
		return java.lang.Math.round(v / step) * step;
	}

	private float snapToCellCenter(float v, float step) {
		return (java.lang.Math.round(v / step - 0.5f) + 0.5f) * step;
	}

	private Vector3f getBuildPiecePosition() {
		Vector3f pos = avatar.getWorldLocation();
		Vector3f fwd = dolphinForward();

		fwd.y = 0f;
		if (fwd.lengthSquared() < 0.0001f)
			fwd.set(0f, 0f, -1f);
		fwd.normalize();

		Vector3f t = new Vector3f(pos).add(new Vector3f(fwd).mul(buildDistance));

		float x;
		float y;
		float z;

		if (buildPieceType == 0) {
			// WALL / FLOOR / CEILING
			float wallOffset = 0.25f;
			switch (buildModeType) {
				case 0: // wall along X axis
					x = snapToCellCenter(t.x, buildSnap);
					z = snap(t.z, buildSnap) + (fwd.z >= 0 ? wallOffset : -wallOffset);
					y = buildHeightLevel * buildGrid + buildWallHalfH;
					break;

				case 1: // wall along Z axis
					x = snap(t.x, buildSnap) + (fwd.x >= 0 ? wallOffset : -wallOffset);
					z = snapToCellCenter(t.z, buildSnap);
					y = buildHeightLevel * buildGrid + buildWallHalfH;
					break;

				case 2: // floor
					x = snapToCellCenter(t.x, buildSnap);
					z = snapToCellCenter(t.z, buildSnap);
					y = buildHeightLevel * buildGrid;
					break;

				case 3: // ceiling
					x = snapToCellCenter(t.x, buildSnap);
					z = snapToCellCenter(t.z, buildSnap);
					y = buildHeightLevel * buildGrid + buildGrid;
					break;

				default:
					x = snapToCellCenter(t.x, buildSnap);
					z = snapToCellCenter(t.z, buildSnap);
					y = buildHeightLevel * buildGrid + buildWallHalfH;
					break;
			}
		} else {
			// ROOF PIECES
			float roofInset = buildGrid * 0.5f;
			float roofLift = buildGrid * 0.5f;

			x = snapToCellCenter(t.x, buildSnap);
			z = snapToCellCenter(t.z, buildSnap);
			y = buildHeightLevel * buildGrid + buildGrid + roofLift;

			switch (buildRoofDir) {
				case 0: // right
					x += roofInset;
					break;
				case 1: // left
					x -= roofInset;
					break;
				case 2:
					z += roofInset;
					break;
				case 3:
					z -= roofInset;
					break;
			}
		}

		return new Vector3f(x, y, z);
	}

	private String buildPieceKey(Vector3f p) {
		if (buildPieceType == 0) {
			return String.format("%.2f,%.2f,%.2f,W,%d", p.x, p.y, p.z, buildModeType);
		} else {
			return String.format("%.2f,%.2f,%.2f,R,%d", p.x, p.y, p.z, buildRoofDir);
		}
	}

	// Applies a simple translation to the main camera after the orbit camera has
	// already set its normal behind-the-player orientation.
	private void applyCameraModeOffset() {
		if (orbitCam == null)
			return;

		Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();

		if (cameraMode == 0)
			return;

		if (cameraMode == 1 || cameraMode == 2) {
			Vector3f loc = cam.getLocation();
			Vector3f u = new Vector3f(cam.getU()).normalize();
			Vector3f v = new Vector3f(cam.getV()).normalize();

			float side = shoulderOffsetX;
			if (cameraMode == 2)
				side = -side;

			Vector3f newLoc = new Vector3f(loc)
					.add(new Vector3f(u).mul(side))
					.add(new Vector3f(v).mul(shoulderOffsetY));

			cam.setLocation(newLoc);
			return;
		}

		if (cameraMode == 3) {
			Vector3f playerLoc = avatar.getWorldLocation();

			// Use the avatar's forward direction, not the orbit camera's current N.
			Vector3f forward = new Vector3f(dolphinForward()).normalize();

			// Convert orbit elevation into a first-person pitch.
			// 15 degrees is your starting third-person elevation, so make that the
			// "look straight ahead" reference for first person.
			float pitchDeg = 15.0f - orbitElevationDeg;
			float pitchRad = (float) java.lang.Math.toRadians(pitchDeg);

			float horiz = (float) java.lang.Math.cos(pitchRad);
			forward.x *= horiz;
			forward.z *= horiz;
			forward.y = (float) java.lang.Math.sin(pitchRad);
			forward.normalize();

			// Put camera at eye level and slightly in front of the avatar so it is not
			// inside the head/body mesh.
			Vector3f newLoc = new Vector3f(playerLoc)
					.add(0f, getFirstPersonEyeHeight(), 0f)
					.add(new Vector3f(forward).mul(getFirstPersonForwardOffset()));

			// Build camera basis vectors
			Vector3f worldUp = new Vector3f(0f, 1f, 0f);
			Vector3f u = new Vector3f(forward).cross(worldUp).normalize();
			Vector3f v = new Vector3f(u).cross(forward).normalize();

			cam.setLocation(newLoc);
			cam.setN(forward);
			cam.setU(u);
			cam.setV(v);
			return;
		}
	}

	private float getFirstPersonEyeHeight() {
		if (selectedAvatar.compareTo("playerModel2") == 0)
			return 1.65f;
		return 1.80f;
	}

	private float getFirstPersonForwardOffset() {
		if (selectedAvatar.compareTo("playerModel2") == 0)
			return 0.32f;
		return 0.35f;
	}

	// Cycles camera mode and updates orbit parameters.
	public void toggleCameraMode() {
		cameraMode++;
		if (cameraMode > 3)
			cameraMode = 0;

		switch (cameraMode) {
			case 0:
				orbitRadius = 4.5f;
				showEvent("CAMERA BEHIND", 1.0);
				break;
			case 1:
				orbitRadius = 4.5f;
				showEvent("CAMERA RIGHT SHOULDER", 1.0);
				break;
			case 2:
				orbitRadius = 4.5f;
				showEvent("CAMERA LEFT SHOULDER", 1.0);
				break;
			case 3:
				orbitRadius = 0.0f;
				showEvent("CAMERA FIRST PERSON", 1.0);
				break;
		}
		orbitCam.setRadius(orbitRadius);
		orbitCam.update();
		updateCameraLimits();
		applyCameraModeOffset();
	}

	// Swaps the shoulder camera side.
	public void swapShoulderCamera() {
		if (cameraMode == 1) {
			cameraMode = 2;
			showEvent("CAMERA LEFT SHOULDER", 1.0);
		} else if (cameraMode == 2) {
			cameraMode = 1;
			showEvent("CAMERA RIGHT SHOULDER", 1.0);
		} else {
			return;
		}
		applyCameraModeOffset();
	}

	// A2 orbit controls: orbit, elevation, and zoom update camera state without
	// changing dolphin heading.
	public void doOrbit(float input, float time) {
		if (orbitCam == null)
			return;
		float orbitSpeed = 90.0f;
		orbitAzimuthDeg += input * orbitSpeed * time;
		orbitCam.setAzimuthDeg(orbitAzimuthDeg);
		orbitCam.update();
		applyCameraModeOffset();
	}

	public void doElevate(float input, float time) {
		if (isLost() || isWon())
			return;

		if (orbitCam == null)
			return;

		float elevSpeed = 70.0f;
		orbitElevationDeg += input * elevSpeed * time;

		if (orbitElevationDeg < minOrbitElevationDeg)
			orbitElevationDeg = minOrbitElevationDeg;
		if (orbitElevationDeg > maxOrbitElevationDeg)
			orbitElevationDeg = maxOrbitElevationDeg;

		orbitCam.setElevationDeg(orbitElevationDeg);
		orbitCam.update();
		applyCameraModeOffset();
	}

	public void doZoom(float input, float time) {
		if (orbitCam == null)
			return;
		float zoomSpeed = 6.0f;
		orbitRadius += input * zoomSpeed * time;

		if (orbitRadius < 0.05f)
			orbitRadius = 0.05f;
		if (orbitRadius > 12.0f)
			orbitRadius = 12.0f;

		orbitCam.setRadius(orbitRadius);
		orbitCam.update();
		applyCameraModeOffset();
	}

	private float getGroundHeight(float x, float z) {
		return terrain.getHeight(x, z);
	}

	// A2 ground movement: x and z movement is allowed, y stays on terrain unless
	// jump is active.
	public void doMove(float input, float time) {
		if (isLost())
			return;
		if (isWon())
			return;

		float dist = getCurrentMoveSpeed() * input * time;

		Vector3f pos = avatar.getWorldLocation();
		Vector3f fwd = dolphinForward();

		Vector3f newPos = new Vector3f(pos).add(new Vector3f(fwd).mul(dist));

		newPos.y = onGround ? getGroundHeight(newPos.x, newPos.z) : pos.y;

		avatar.setLocalLocation(newPos);

		sendPlayerTransform();
	}

	public void doStrafe(float dir, float time) {
		if (isLost())
			return;
		if (isWon())
			return;

		float dist = getCurrentMoveSpeed() * dir * time;

		Vector3f pos = avatar.getWorldLocation();
		Vector3f rt = new Vector3f(avatar.getWorldRightVector()).normalize();

		Vector3f right = new Vector3f(rt.x, 0f, rt.z);
		if (right.lengthSquared() < 0.0001f)
			right.set(1f, 0f, 0f);
		right.normalize();

		Vector3f newPos = new Vector3f(pos).add(right.mul(dist));

		newPos.y = onGround ? getGroundHeight(newPos.x, newPos.z) : pos.y;

		avatar.setLocalLocation(newPos);

		sendPlayerTransform();
	}

	// A2 global yaw: uses globalYaw so turning is around world y, not local y.
	public void doYaw(float input, float time) {
		if (isLost())
			return;
		if (isWon())
			return;

		float yawSpeed = 1.6f;
		float ang = yawSpeed * input * time;

		avatar.globalYaw(ang);
		playerYaw += (float) java.lang.Math.toDegrees(ang);

		sendPlayerTransform();
	}

	public void doPitch() {
	}

	public void doJump() {
		if (isLost())
			return;
		if (isWon())
			return;

		if (!onGround)
			return;

		onGround = false;

		float jumpSpeed = 7.5f;
		yVel = jumpSpeed;

		sendPlayerTransform();
	}

	private void applyPadMovement(float dt) {
		float f = padMove;
		float s = padStrafe;

		float mag = (float) java.lang.Math.sqrt((f * f) + (s * s));
		if (mag < 0.0001f)
			return;

		if (mag > 1.0f) {
			f /= mag;
			s /= mag;
		}

		doMove(f, dt);
		doStrafe(s, dt);
	}

	private void updateJump(float dt) {
		if (onGround)
			return;

		float gravity = -18.0f;
		yVel += gravity * dt;

		Vector3f pos = avatar.getWorldLocation();
		float groundY = getGroundHeight(pos.x, pos.z);
		float newY = pos.y + yVel * dt;

		if (newY <= groundY) {
			newY = groundY;
			yVel = 0.0f;
			onGround = true;
		}

		avatar.setLocalLocation(new Vector3f(pos.x, newY, pos.z));

		sendPlayerTransform();
	}

	private void updateHealthPotionHeight() {
		if (healthPotion == null || terrain == null)
			return;

		if (healthPotionSnappedToTerrain || hasPotion || potionUsed)
			return;

		Vector3f p = healthPotion.getWorldLocation();
		float groundY = terrain.getHeight(p.x, p.z);

		healthPotion.setLocalTranslation(
				(new Matrix4f()).translation(p.x, groundY + potionLift, p.z));
		healthPotionSnappedToTerrain = true;
	}

	private void updateFlashlightHeight() {
		if (flashlight == null || terrain == null)
			return;

		if (flashlightSnappedToTerrain || hasFlashlight)
			return;

		Vector3f p = flashlight.getWorldLocation();
		float groundY = terrain.getHeight(p.x, p.z);

		flashlight.setLocalTranslation(
				(new Matrix4f()).translation(p.x, groundY + flashlightLift, p.z));
		flashlightSnappedToTerrain = true;
	}

	private void updateCarriedFlashlightPose() {
		if (flashlight == null || !hasFlashlight)
			return;

		if (flashlight.getParent() != avatar) {
			flashlight.setParent(avatar);
			flashlight.propagateRotation(true);
			flashlight.applyParentRotationToPosition(true);
			flashlight.propagateScale(false);
		}

		flashlight.setLocalTranslation((new Matrix4f()).translation(getFlashlightCarryLocalOffset()));
		flashlight.setLocalScale((new Matrix4f()).scaling(getFlashlightCarryScale()));
		flashlight.setLocalRotation(getFlashlightCarryRotation());
	}

	private Matrix4f getFlashlightCarryRotation() {
		return (new Matrix4f()).identity();
	}

	private Vector3f getFlashlightCarryLocalOffset() {
		return new Vector3f(
				getFlashlightCarrySideOffset(),
				getFlashlightCarryHeight(),
				getFlashlightCarryForwardOffset());
	}

	private float getFlashlightCarryScale() {
		if (selectedAvatar.compareTo("player") == 0)
			return 0.14f;
		return 0.18f;
	}

	private float getFlashlightCarrySideOffset() {
		if (selectedAvatar.compareTo("player") == 0)
			return -1.18f;
		return -0.95f;
	}

	private float getFlashlightCarryForwardOffset() {
		if (selectedAvatar.compareTo("player") == 0)
			return 0.08f;
		return 0.10f;
	}

	private float getFlashlightCarryHeight() {
		if (selectedAvatar.compareTo("player") == 0)
			return 1.14f;
		return 1.34f;
	}

	private float getPotionCarryScale() {
		if (selectedAvatar.compareTo("player") == 0)
			return 0.12f;
		return 0.10f;
	}

	private void updateCarriedPotionPose() {
		if (healthPotion == null || !hasPotion || potionUsed)
			return;

		if (healthPotion.getParent() != avatar) {
			healthPotion.setParent(avatar);
			healthPotion.propagateRotation(true);
			healthPotion.applyParentRotationToPosition(true);
			healthPotion.propagateScale(false);
		}

		healthPotion.setLocalTranslation((new Matrix4f()).translation(getPotionCarryLocalOffset()));
		healthPotion.setLocalScale((new Matrix4f()).scaling(getPotionCarryScale()));
		healthPotion.setLocalRotation(getPotionCarryRotation());
	}

	private Vector3f getPotionCarryLocalOffset() {
		return new Vector3f(
				getPotionCarrySideOffset(),
				getPotionCarryHeight(),
				getPotionCarryForwardOffset());
	}

	private float getPotionCarrySideOffset() {
		if (selectedAvatar.compareTo("player") == 0)
			return -1.14f;
		return -0.92f;
	}

	private float getPotionCarryForwardOffset() {
		if (selectedAvatar.compareTo("player") == 0)
			return 0.10f;
		return 0.12f;
	}

	private float getPotionCarryHeight() {
		if (selectedAvatar.compareTo("player") == 0)
			return 1.12f;
		return 1.32f;
	}

	private Matrix4f getPotionCarryRotation() {
		return (new Matrix4f()).identity();
	}

	private void updateFlashlightSpotlight() {
		if (flashlightSpotlight == null)
			return;

		if (!hasFlashlight || equippedItem != ITEM_FLASHLIGHT) {
			flashlightSpotlight.disable();
			return;
		}

		Vector3f forward = new Vector3f(dolphinForward());
		forward.y = 0f;
		if (forward.lengthSquared() < 0.0001f)
			forward.set(0f, 0f, -1f);
		forward.normalize();
		Vector3f lightPos = new Vector3f(flashlight.getWorldLocation())
				.add(new Vector3f(forward).mul(flashlightBeamForwardOffset))
				.add(0f, flashlightBeamLift, 0f);

		flashlightSpotlight.setLocation(lightPos);
		flashlightSpotlight.setDirection(forward);
		flashlightSpotlight.enable();
	}

	private void healPlayer(int amount) {
		health += amount;
		if (health > maxHealth)
			health = maxHealth;
	}

	private void damagePlayer(int amount) {
		health -= amount;
		if (health < 0)
			health = 0;
	}

	private void updateEquippedItemVisibility() {
		if (flashlight != null && hasFlashlight) {
			if (equippedItem == ITEM_FLASHLIGHT)
				flashlight.getRenderStates().enableRendering();
			else
				flashlight.getRenderStates().disableRendering();
		}

		if (healthPotion != null) {
			if (hasPotion && !potionUsed) {
				if (equippedItem == ITEM_POTION)
					healthPotion.getRenderStates().enableRendering();
				else
					healthPotion.getRenderStates().disableRendering();
			} else if (potionUsed) {
				healthPotion.getRenderStates().disableRendering();
			}
		}

		updateFlashlightSpotlight();
	}

	public void equipFlashlight() {
		if (!hasFlashlight) {
			showEvent("NO FLASHLIGHT", 0.8);
			return;
		}

		equippedItem = ITEM_FLASHLIGHT;
		updateEquippedItemVisibility();
		showEvent("FLASHLIGHT EQUIPPED", 0.8);
	}

	public void equipPotion() {
		if (!hasPotion || potionUsed) {
			showEvent("NO POTION", 0.8);
			return;
		}

		equippedItem = ITEM_POTION;
		updateEquippedItemVisibility();
		showEvent("POTION EQUIPPED", 0.8);
	}

	public void unequipItem() {
		if (equippedItem == ITEM_NONE) {
			showEvent("NO ITEM EQUIPPED", 0.8);
			return;
		}

		equippedItem = ITEM_NONE;
		updateEquippedItemVisibility();
		showEvent("ITEM UNEQUIPPED", 0.8);
	}

	private void tryPickupFlashlight() {
		if (flashlight == null || hasFlashlight)
			return;

		float dist = avatar.getWorldLocation().distance(flashlight.getWorldLocation());
		if (dist > itemPickupRange)
			return;

		hasFlashlight = true;
		updateCarriedFlashlightPose();
		if (equippedItem == ITEM_NONE)
			equippedItem = ITEM_FLASHLIGHT;
		updateEquippedItemVisibility();

		showEvent("FLASHLIGHT PICKED UP", 1.0);
	}

	private void tryPickupPotion() {
		if (healthPotion == null || hasPotion || potionUsed)
			return;

		float dist = avatar.getWorldLocation().distance(healthPotion.getWorldLocation());
		if (dist > itemPickupRange)
			return;

		hasPotion = true;

		updateCarriedPotionPose();
		healthPotion.getRenderStates().isTransparent(false);
		healthPotion.getRenderStates().setOpacity(1.0f);
		if (equippedItem == ITEM_NONE)
			equippedItem = ITEM_POTION;
		updateEquippedItemVisibility();

		showEvent("POTION PICKED UP  PRESS H TO HEAL", 1.2);
	}

	private void checkItemPickups() {
		tryPickupFlashlight();
		tryPickupPotion();
	}

	public void usePotion() {
		if (!hasPotion) {
			showEvent("NO POTION", 0.8);
			return;
		}

		if (health >= maxHealth) {
			showEvent("HEALTH FULL", 0.8);
			return;
		}

		healPlayer(potionHealAmount);

		hasPotion = false;
		potionUsed = true;
		if (equippedItem == ITEM_POTION) {
			if (hasFlashlight)
				equippedItem = ITEM_FLASHLIGHT;
			else
				equippedItem = ITEM_NONE;
		}

		healthPotion.getRenderStates().disableRendering();
		updateEquippedItemVisibility();

		showEvent("HEALTH RESTORED +" + potionHealAmount, 1.2);
	}

	public void useSky04() {
		engine.getSceneGraph().setActiveSkyBoxTexture(sky04);
		engine.getSceneGraph().setSkyBoxEnabled(true);
		showEvent("SKYBOX sky04", 1.0);
	}

	public void useSky15Night() {
		engine.getSceneGraph().setActiveSkyBoxTexture(sky15Night);
		engine.getSceneGraph().setSkyBoxEnabled(true);
		showEvent("SKYBOX sky15_night", 1.0);
	}

	public void toggleSkyboxOff() {
		engine.getSceneGraph().setSkyBoxEnabled(false);
		showEvent("SKYBOX OFF", 1.0);
	}

	public Engine getEngine() {
		return engine;
	}

	public boolean isLost() {
		return lost;
	}

	public boolean isWon() {
		return won;
	}

	private int winW() {
		return engine.getRenderSystem().getGLCanvas().getWidth();
	}

	private int winH() {
		return engine.getRenderSystem().getGLCanvas().getHeight();
	}

	private int overHudLeftPx() {
		return java.lang.Math.round(OVER_LEFT * winW());
	}

	// HUD top position is derived from viewport placement so the text stays inside
	// the overhead viewport.
	private int overHudTopPx() {
		return java.lang.Math.round((1.0f - (OVER_BOTTOM + OVER_H)) * winH());
	}

	// Mouse-look initialization sets up Robot recentering, stores the center of
	// the main viewport, registers the canvas listener, and hides the cursor.
	private void initMouseMode() {
		mouseModeInitiated = true;
		RenderSystem rs = engine.getRenderSystem();

		isRecentering = false;

		try {
			robot = new Robot();
		} catch (AWTException ex) {
			throw new RuntimeException("Couldn't create Robot!");
		}

		canvas = rs.getGLCanvas();
		canvas.addMouseMotionListener(this);

		Toolkit tk = Toolkit.getDefaultToolkit();
		defaultCursor = Cursor.getDefaultCursor();
		invisibleCursor = tk.createCustomCursor(tk.getImage(""), new Point(), "InvisibleCursor");
		canvas.setCursor(invisibleCursor);

		updateMouseCenterOnScreen();
		recenterMouse();

		prevMouseX = centerX;
		prevMouseY = centerY;

		canvas.requestFocusInWindow();
	}

	// Robot places the mouse back in the center after each move, which prevents
	// the cursor from hitting the screen edge.
	private void recenterMouse() {
		if (!mouseLookEnabled)
			return;

		updateMouseCenterOnScreen();
		isRecentering = true;
		robot.mouseMove((int) centerX, (int) centerY);
	}

	private void updateMouseCenterOnScreen() {
		if (canvas == null)
			return;

		Point canvasLoc = canvas.getLocationOnScreen();
		centerX = canvasLoc.x + canvas.getWidth() / 2.0f;
		centerY = canvasLoc.y + canvas.getHeight() / 2.0f;
	}

	public void toggleMouseLook() {
		if (!mouseModeInitiated || canvas == null)
			return;

		mouseLookEnabled = !mouseLookEnabled;

		if (mouseLookEnabled) {
			canvas.setCursor(invisibleCursor);
			recenterMouse();
			prevMouseX = centerX;
			prevMouseY = centerY;
			canvas.requestFocusInWindow();
			showEvent("MOUSE LOCKED", 1.0);
		} else {
			isRecentering = false;
			canvas.setCursor(defaultCursor);
			showEvent("MOUSE UNLOCKED", 1.0);
		}
	}

	// Horizontal mouse motion rotates the avatar using global yaw so the avatar and
	// the targeted orbit camera both follow the mouse direction.
	private void yawMouse(float mouseDeltaX) {
		if (isLost() || isWon())
			return;

		if (java.lang.Math.abs(mouseDeltaX) < 0.001f)
			return;

		float yawAmt = mouseDeltaX * mouseYawSpeed;

		// Ground-based character turning should use global yaw.
		avatar.globalYaw(yawAmt);

		// Keep the tracked yaw value in sync so remote clients receive the same
		// heading.
		playerYaw += (float) java.lang.Math.toDegrees(yawAmt);

		if (orbitCam != null) {
			orbitCam.update();
			applyCameraModeOffset();
		}

		sendPlayerTransform();
	}

	// Vertical mouse motion adjusts orbit camera elevation, which is the natural
	// pitch control for a targeted third-person camera.
	// First-person keeps the same mouse look feel through the orbit camera.
	private void pitchMouse(float mouseDeltaY) {
		if (isLost() || isWon())
			return;

		if (orbitCam == null)
			return;

		if (java.lang.Math.abs(mouseDeltaY) < 0.001f)
			return;

		float elevAmt = -mouseDeltaY * mouseElevSpeed;
		orbitElevationDeg += elevAmt;

		if (orbitElevationDeg < minOrbitElevationDeg)
			orbitElevationDeg = minOrbitElevationDeg;
		if (orbitElevationDeg > maxOrbitElevationDeg)
			orbitElevationDeg = maxOrbitElevationDeg;

		orbitCam.setElevationDeg(orbitElevationDeg);
		orbitCam.update();
		applyCameraModeOffset();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!mouseModeInitiated)
			return;

		if (!mouseLookEnabled)
			return;

		// If the Robot just moved the mouse to the center, ignore that event.
		if (isRecentering &&
				centerX == e.getXOnScreen() &&
				centerY == e.getYOnScreen()) {
			isRecentering = false;
			return;
		}

		// Process actual player mouse movement.
		curMouseX = e.getXOnScreen();
		curMouseY = e.getYOnScreen();

		float mouseDeltaX = prevMouseX - curMouseX;
		float mouseDeltaY = prevMouseY - curMouseY;

		yawMouse(mouseDeltaX);
		pitchMouse(mouseDeltaY);

		prevMouseX = curMouseX;
		prevMouseY = curMouseY;

		// Recenter after every user move.
		recenterMouse();

		prevMouseX = centerX;
		prevMouseY = centerY;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	public ObjShape getGhostShape(String avatarType) {
		if (avatarType.compareTo("playerModel2") == 0)
			return playerModel2S;

		return playerModel1S;
	}

	public TextureImage getGhostTexture(String avatarType) {
		if (avatarType.compareTo("playerModel2") == 0)
			return playerModel2Tx;

		return playerModel1Tx;
	}

	public Matrix4f getGhostScale(String avatarType) {
		return (new Matrix4f()).scaling(1.0f);
	}

	public GhostManager getGhostManager() {
		if (gm == null)
			gm = new GhostManager(this);
		return gm;
	}

	public String getSelectedAvatarType() {
		return selectedAvatar;
	}

	public void setSelectedAvatarType(String type) {
		if (type != null)
			selectedAvatar = type;
	}

	private Matrix4f getCollectedHudIconScale() {
		return (new Matrix4f()).scaling(0.30f, 0.21f, 1f);
	}

	private void setupNetworking() {
		isClientConnected = false;
		try {
			InetAddress remoteAddress = resolveServerAddress();
			printMultiplayerStartupHints(remoteAddress);
			protClient = new ProtocolClient(remoteAddress, serverPort, serverProtocol, this);
		} catch (SocketTimeoutException e) {
			System.out.println("auto-discovery failed --> no NetworkingServer replied on UDP port "
					+ NetworkDiscovery.DEFAULT_DISCOVERY_PORT);
			System.out.println("auto-discovery failed --> make sure the server is running and that your network allows local device discovery");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (protClient == null) {
			System.out.println("missing protocol host");
		} else { // Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}

	private InetAddress resolveServerAddress() throws IOException {
		if (!NetworkDiscovery.usesAutoDiscovery(serverAddress))
			return InetAddress.getByName(serverAddress);

		System.out.println("auto-discovery --> searching for a NetworkingServer on UDP port "
				+ NetworkDiscovery.DEFAULT_DISCOVERY_PORT);
		NetworkDiscovery.DiscoveredServer discoveredServer = NetworkDiscovery.discoverServer(
				NetworkDiscovery.DEFAULT_DISCOVERY_PORT,
				NetworkDiscovery.DEFAULT_DISCOVERY_TIMEOUT_MS);

		serverAddress = discoveredServer.getAddress().getHostAddress();
		serverPort = discoveredServer.getGamePort();
		serverProtocol = discoveredServer.getProtocolType();

		System.out.println("auto-discovery --> found server at "
				+ serverAddress + ":" + serverPort + " using " + serverProtocol);

		return discoveredServer.getAddress();
	}

	private void printMultiplayerStartupHints(InetAddress remoteAddress) {
		System.out.println("multiplayer target --> "
				+ serverAddress + " (" + remoteAddress.getHostAddress() + "):" + serverPort
				+ " using " + serverProtocol);
		System.out.println("multiplayer note --> NetworkingServer is a relay only; launch two separate MyGame clients for two players");

		if (remoteAddress.isAnyLocalAddress() || remoteAddress.isLoopbackAddress()) {
			System.out.println("multiplayer note --> " + serverAddress + " only reaches this same computer");
			System.out.println("multiplayer note --> remote machines must use the server computer's LAN IPv4 address instead of localhost");
		}
	}

	private void sendPlayerTransform() {
		if (protClient != null && isClientConnected)
			protClient.sendMoveMessage(avatar.getWorldLocation(), playerYaw);
	}

	// Sends a BYE message once before the client closes so other players can
	// remove this ghost from their worlds.
	private void sendByeAndClose() {
		if (byeMessageSent)
			return;

		byeMessageSent = true;

		if (protClient != null && isClientConnected) {
			System.out.println("sending bye message to protocol host");
			protClient.sendByeMessage();
		}
	}

	protected void processNetworking(float elapsTime) { // Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() {
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value) {
		this.isClientConnected = value;
	}

	private class RunAction extends AbstractInputAction {
		private MyGame game;

		public RunAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.triggerRun();
		}
	}

	private class ToggleCameraModeAction extends AbstractInputAction {
		private MyGame game;

		public ToggleCameraModeAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.toggleCameraMode();
		}
	}

	private class SwapShoulderAction extends AbstractInputAction {
		private MyGame game;

		public SwapShoulderAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.swapShoulderCamera();
		}
	}

	private class ToggleFullMapAction extends AbstractInputAction {
		private MyGame game;

		public ToggleFullMapAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.toggleFullMap();
		}
	}

	private class UsePotionAction extends AbstractInputAction {
		private MyGame game;

		public UsePotionAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.usePotion();
		}
	}

	private class EquipFlashlightAction extends AbstractInputAction {
		private MyGame game;

		public EquipFlashlightAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.equipFlashlight();
		}
	}

	private class EquipPotionAction extends AbstractInputAction {
		private MyGame game;

		public EquipPotionAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.equipPotion();
		}
	}

	private class UnequipItemAction extends AbstractInputAction {
		private MyGame game;

		public UnequipItemAction(MyGame g) {
			game = g;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			game.unequipItem();
		}
	}
}
