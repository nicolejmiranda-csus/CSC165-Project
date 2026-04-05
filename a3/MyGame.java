package a3;

import java.util.ArrayList;
import java.util.HashMap;

import org.joml.Math;
import tage.*;
import tage.nodeControllers.*;
import tage.shapes.*;
import tage.input.*;

import net.java.games.input.Component.Identifier.*;
import net.java.games.input.Controller;
import org.joml.*;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.ImageIcon;

import com.jogamp.opengl.awt.GLCanvas;

public class MyGame extends VariableFrameRateGame implements MouseMotionListener {
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

	private double lastFrameTime, currFrameTime, elapsedTime;

	private boolean lost = false;
	private boolean won = false;
	private int score = 0;

	// Help overlay page
	// 0 means off
	// 1 shows keyboard controls
	// 2 shows gamepad controls
	private int helpPage = 0;

	private String eventMsg = "";
	private double eventHold = 0.0;

	private final float moveSpeed = 4.0f;
	private final float homeRange = 6.0f;

	// A2 overhead camera controls pan using x and z, and zoom using y.
	private float overX = 0f, overZ = 0f, overY = 35f;

	// Gamepad axis actions store values here, then movement runs once per frame.
	private float padMove = 0.0f;
	private float padStrafe = 0.0f;

	public void setPadMove(float v) {
		padMove = v;
	}

	public void setPadStrafe(float v) {
		padStrafe = v;
	}

	private boolean onGround = true;
	private float yVel = 0.0f;

	private final GameObject[] hudIcons = new GameObject[3];
	private final boolean[] iconCollected = new boolean[3];

	private ObjShape dolS, quadS, linxS, linyS, linzS;
	private ObjShape pyrS, planeS;
	private TerrainPlane terrainShape;
	private ObjShape playerS, flashlightS, tableS;

	// Player character
	private ObjShape boyS;
	private TextureImage boyTx;
	private GameObject boy;

	private TextureImage dolTx;
	private TextureImage homeTx;
	private TextureImage grassTx;
	private final TextureImage[] pyramidTx = new TextureImage[3];
	private TextureImage heightMaptx; 
	private TextureImage flashlightTx;
	private TextureImage playerModelTx;
	private TextureImage tableTx;

	private GameObject dol;
	private final GameObject[] pyramids = new GameObject[3];
	private GameObject terrain;
	private GameObject flashlight;
	private GameObject player;
	private GameObject table;

	private final Light[] lightPyramid = new Light[3];

	// A2 orbit camera controller lives in tage as CameraOrbit3D.
	private CameraOrbit3D orbitCam;

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

	public MyGame() {
		super();
	}

	public static void main(String[] args) {
		MyGame game = new MyGame();
		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	@Override
	public void loadShapes() {
		dolS = new ImportedModel("dolphinHighPoly.obj");

		// Player character models
		boyS = new ImportedModel("boy_character.obj");
		playerS = new ImportedModel("playerModel.obj");

		// Custom models
		flashlightS = new ImportedModel("flashlight.obj");
		tableS = new ImportedModel("table.obj");

		quadS = new ManualQuad();
		pyrS = new ManualPyramid();
		planeS = new Plane();
		terrainShape = new TerrainPlane(1000);

		linxS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(3f, 0f, 0f));
		linyS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 3f, 0f));
		linzS = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, -3f));
	}

	@Override
	public void loadTextures() {
		dolTx = new TextureImage("Dolphin_HighPolyUV.jpg");

		// Player character texture
		boyTx = new TextureImage("boy_character.jpg");
		playerModelTx = new TextureImage("playerModel.png");

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
		Matrix4f initialTranslation, initialScale, initialRotation;

		// A3 avatar: boy character replaces dolphin as the player avatar.
		boy = new GameObject(GameObject.root(), boyS, boyTx);
		initialTranslation = (new Matrix4f()).translation(0f, 0f, 6f);
		initialScale = (new Matrix4f()).scaling(1.0f); // adjust if needed
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(180f));
		boy.setLocalTranslation(initialTranslation);
		boy.setLocalScale(initialScale);
		boy.setLocalRotation(initialRotation);
		boy.getRenderStates().hasLighting(true);

		// Imported model correction: rotate the mesh upright without changing the
		// player object's movement/camera orientation.
		boy.getRenderStates().setModelOrientationCorrection(
				(new Matrix4f()).rotationX((float) java.lang.Math.toRadians(-90f)));

		// Player
		player = new GameObject(GameObject.root(), playerS, playerModelTx);
		player.setLocalTranslation((new Matrix4f()).translation(5f, 0f, 6f));
		player.setLocalScale((new Matrix4f()).scaling(0.25f));
		player.setLocalRotation((new Matrix4f()).rotationY((float)java.lang.Math.toRadians(180f)));

		// Flashlight object
		flashlight = new GameObject(GameObject.root(), flashlightS, flashlightTx);
		flashlight.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 6f));
		flashlight.setLocalScale((new Matrix4f()).scaling(0.5f));
		flashlight.setLocalRotation((new Matrix4f()).rotationY((float)java.lang.Math.toRadians(180f)));

		// Table object
		table = new GameObject(GameObject.root(), tableS, tableTx);
		table.setLocalTranslation((new Matrix4f()).translation(5f, 0f, 8f));
		table.setLocalScale((new Matrix4f()).scaling(0.4f));

		// A2 pyramids: placed on y=0 to match the ground plane world.
		for (int i = 0; i < 3; i++) {
			pyramids[i] = new GameObject(GameObject.root(), pyrS, pyramidTx[i]);
			pyramids[i].setLocalScale((new Matrix4f()).scaling(5f));
			pyramids[i].getRenderStates().hasLighting(true);
		}
		pyramids[0].setLocalTranslation((new Matrix4f()).translation(-24f, 0f, -10f));
		pyramids[1].setLocalTranslation((new Matrix4f()).translation(34f, 0f, 0f));
		pyramids[2].setLocalTranslation((new Matrix4f()).translation(18f, 0f, 10f));

		// Terrain plane: hilly terrain with grass texture.
		terrain = new GameObject(GameObject.root(), terrainShape, grassTx);
		terrain.setLocalTranslation(new Matrix4f().translation(0, 0, 0));
		terrain.setLocalScale(new Matrix4f().scaling(200.0f, 20f, 200.0f));
		terrain.setHeightMap(heightMaptx);

		// Set tilling for terrain texture
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);

		// A2 axes: kept from A1, with a toggle in toggleAxesVisibility.
		axisX = new GameObject(GameObject.root(), linxS);
		axisY = new GameObject(GameObject.root(), linyS);
		axisZ = new GameObject(GameObject.root(), linzS);
		(axisX.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
		(axisY.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
		(axisZ.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));
		Matrix4f lift = new Matrix4f().translation(0f, 0.1f, 0f);
		axisX.setLocalTranslation(lift);
		axisY.setLocalTranslation(lift);
		axisZ.setLocalTranslation(lift);

		// Home build: quads scaled and rotated so wall placement math stays aligned.
		float s = HOME_SCALE;
		float w = HOME_W * s, h = HOME_H * s, d = HOME_D * s;
		float floorY = HOME_FLOOR_Y;

		float hw = w / 2f;
		float hh = h / 2f;
		float hd = d / 2f;

		GameObject homeFloor = new GameObject(GameObject.root(), quadS, homeTx);
		initialRotation = (new Matrix4f()).rotationX((float) java.lang.Math.toRadians(-90f));
		initialScale = (new Matrix4f()).scaling(hw, hd, 1f);
		initialTranslation = (new Matrix4f()).translation(0f, floorY, 0f);
		homeFloor.setLocalRotation(initialRotation);
		homeFloor.setLocalScale(initialScale);
		homeFloor.setLocalTranslation(initialTranslation);
		homeFloor.getRenderStates().hasLighting(true);

		GameObject homeWallBack = new GameObject(GameObject.root(), quadS, homeTx);
		initialScale = (new Matrix4f()).scaling(hw, hh, 1f);
		initialTranslation = (new Matrix4f()).translation(0f, floorY + hh, -hd);
		homeWallBack.setLocalScale(initialScale);
		homeWallBack.setLocalTranslation(initialTranslation);
		homeWallBack.getRenderStates().hasLighting(true);

		GameObject homeWallLeft = new GameObject(GameObject.root(), quadS, homeTx);
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(90f));
		initialScale = (new Matrix4f()).scaling(hd, hh, 1f);
		initialTranslation = (new Matrix4f()).translation(-hw, floorY + hh, 0f);
		homeWallLeft.setLocalRotation(initialRotation);
		homeWallLeft.setLocalScale(initialScale);
		homeWallLeft.setLocalTranslation(initialTranslation);
		homeWallLeft.getRenderStates().hasLighting(true);

		GameObject homeWallRight = new GameObject(GameObject.root(), quadS, homeTx);
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(-90f));
		initialScale = (new Matrix4f()).scaling(hd, hh, 1f);
		initialTranslation = (new Matrix4f()).translation(hw, floorY + hh, 0f);
		homeWallRight.setLocalRotation(initialRotation);
		homeWallRight.setLocalScale(initialScale);
		homeWallRight.setLocalTranslation(initialTranslation);
		homeWallRight.getRenderStates().hasLighting(true);

		float doorW = 2.6f * s;
		float sideW = (w - doorW) / 2f;
		float hSideW = sideW / 2f;

		GameObject frontLeft = new GameObject(GameObject.root(), quadS, homeTx);
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(180f));
		initialScale = (new Matrix4f()).scaling(hSideW, hh, 1f);
		initialTranslation = (new Matrix4f()).translation(-(doorW / 2f + hSideW), floorY + hh, hd);
		frontLeft.setLocalRotation(initialRotation);
		frontLeft.setLocalScale(initialScale);
		frontLeft.setLocalTranslation(initialTranslation);
		frontLeft.getRenderStates().hasLighting(true);

		GameObject frontRight = new GameObject(GameObject.root(), quadS, homeTx);
		initialRotation = (new Matrix4f()).rotationY((float) java.lang.Math.toRadians(180f));
		initialScale = (new Matrix4f()).scaling(hSideW, hh, 1f);
		initialTranslation = (new Matrix4f()).translation((doorW / 2f + hSideW), floorY + hh, hd);
		frontRight.setLocalRotation(initialRotation);
		frontRight.setLocalScale(initialScale);
		frontRight.setLocalTranslation(initialTranslation);
		frontRight.getRenderStates().hasLighting(true);

		float halfW = w / 2f;
		float halfD = d / 2f;
		float wallTopY = floorY + h;

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

		// A2 hierarchical objects: photos are children of the dolphin until they get
		// placed on the home wall.
		for (int i = 0; i < 3; i++) {
			hudIcons[i] = new GameObject(boy, quadS, pyramidTx[i]);
			hudIcons[i].getRenderStates().hasLighting(false);

			hudIcons[i].setLocalScale((new Matrix4f()).scaling(0f));

			float xOff = 0.55f - (i * 0.55f);
			hudIcons[i].setLocalTranslation((new Matrix4f()).translation(xOff, 1.1f, -1.0f));
			hudIcons[i].setLocalRotation((new Matrix4f()).identity());

			iconCollected[i] = false;
		}

		// A3 building preview: a reusable wall segment is moved in front of the player
		// while build mode is on.
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
	}

	@Override
	public void initializeGame() {
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsedTime = 0.0;

		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);

		// A2 orbit camera controller: main viewport camera orbits around the dolphin.
		Camera mainCam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		orbitCam = new CameraOrbit3D(mainCam, boy);
		orbitCam.setRadius(4.5f);
		orbitCam.setElevationDeg(15f);
		orbitCam.setAzimuthDeg(180f);
		orbitCam.update();

		// A2 second viewport: overhead view of the same world with pan and zoom
		// controls.
		engine.getRenderSystem().addViewport("OVER", OVER_BOTTOM, OVER_LEFT, OVER_W, OVER_H);
		Camera overCam = engine.getRenderSystem().getViewport("OVER").getCamera();
		setOverheadCamera(overCam);

		// A2 node controllers: photographed pyramid rotates, collected photo bobs while
		// attached to dolphin.
		photoSpin = new RotationController(engine, new Vector3f(0, 1, 0), 0.002f);
		engine.getSceneGraph().addNodeController(photoSpin);
		photoSpin.enable();

		activatedPyramidBob = new BobbingController(engine, 0.35f, 1.2f);
		engine.getSceneGraph().addNodeController(activatedPyramidBob);
		activatedPyramidBob.enable();

		im = engine.getInputManager();

		// A2 dolphin controls: movement stays on ground, jump is temporary.
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

		im.associateActionWithAllKeyboards(Key.LSHIFT, new JumpAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// A2 win action: photos move to the home wall using SPACE.
		im.associateActionWithAllKeyboards(Key.SPACE, new PlacePhotosAction(this),
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

		im.associateActionWithAllKeyboards(Key.H, new ToggleHelpAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllGamepads(Button._9, new ToggleHelpAction(this),
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
		im.associateActionWithAllKeyboards(Key.M, new RaiseBuildHeightAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Key.N, new LowerBuildHeightAction(this),
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	@Override
	public void update() {
		if (!mouseModeInitiated)
			initMouseMode();

		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsedTime = (currFrameTime - lastFrameTime) / 1000.0;

		if (eventHold > 0.0) {
			eventHold -= elapsedTime;
			if (eventHold <= 0.0)
				eventMsg = "";
		}

		padMove = 0.0f;
		padStrafe = 0.0f;

		im.update((float) elapsedTime);

		// Gamepad movement runs once per frame so diagonal speed stays consistent.
		applyPadMovement((float) elapsedTime);

		updateJump((float) elapsedTime);

		// update altitude of player based on height map
		Vector3f loc = boy.getWorldLocation();
		float height = terrain.getHeight(loc.x(), loc.z());
		boy.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		checkCrash();

		if (orbitCam != null)
			orbitCam.update();

		updateBuildPreview();

		updateHUD();
	}

	private void showEvent(String msg, double seconds) {
		eventMsg = msg;
		eventHold = seconds;
	}

	// HUD1 is always score and event text
	// HUD2 and HUD3 are either the game guide or the help overlay
	// HUD4 is always the overhead viewport text showing dolphin world position
	private void updateHUD() {
		HUDmanager hud = engine.getHUDmanager();

		String top = "COLLECTED " + score + "/3";
		if (!eventMsg.isEmpty())
			top = top + "   |   " + eventMsg;
		hud.setHUD1(top, new Vector3f(0.75f, 0.75f, 0.35f), 15, 15);

		if (helpPage == 1) {
			// Help page for keyboard controls
			String line1 = "KEY W S MOVE   A D TURN   LEFT RIGHT STRAFE   SHIFT JUMP   KEY P PHOTO   SPACE PLACE";
			String line2 = "T AXES   Q E ORBIT   UP DOWN ELEVATE   Z X ZOOM   I K J L PAN OVER   U O OVER ZOOM";
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

		// Overhead viewport HUD shows dolphin world position
		Vector3f p = boy.getWorldLocation();
		String pos = String.format("POS (%.1f, %.1f, %.1f)", p.x, p.y, p.z);
		int pad = 60; // padding from the screen edge
		int approxCharW = 8; // rough pixels per character
		int x = winW() - pad - (pos.length() * approxCharW);
		if (x < pad)
			x = pad; // keep it on screen if the window is small

		hud.setHUD4(pos, new Vector3f(0, 0, 0), x, 15);
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
					"BUILD MODE: V PLACE   C REMOVE   R ROTATE   F SWITCH TYPE   TAB LOCK/UNLOCK   M UP   N DOWN",
					"PIECE " + pieceName + "   HEIGHT " + buildHeightLevel + "   PRESS B TO EXIT BUILD MODE"
			};
		}

		if (score == 3) {
			if (distanceToHome() <= homeRange) {
				return new String[] {
						"HOME REACHED: PLACE PHOTOS",
						"PRESS SPACE OR A ON CONTROLLER"
				};
			}
			return new String[] {
					"ALL PHOTOS TAKEN: RETURN HOME",
					"FLY HOME THEN PRESS SPACE OR A ON CONTROLLER"
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
				"PRESS B TO ENTER BUILD MODE   TAB LOCK/UNLOCK MOUSE"
		};
	}

	private Vector3f dolphinForward() {
		return (new Vector3f(boy.getWorldForwardVector())).normalize();
	}

	private float distanceToHome() {
		return boy.getWorldLocation().distance(new Vector3f(0f, 0f, 0f));
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

		Vector3f dLoc = boy.getWorldLocation();

		for (int i = 0; i < 3; i++) {
			float dist = dLoc.distance(pyramids[i].getWorldLocation());
			if (dist < pyramidCollisionRadius(i)) {
				lost = true;
				showEvent("CRASH", 2.0);
				return;
			}
		}
	}

	private float pyramidRadius(int i) {
		return pyramids[i].getLocalScale().m00();
	}

	private int closestPyramidWithinRange() {
		Vector3f dLoc = boy.getWorldLocation();
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

		hudIcons[idx].setLocalScale((new Matrix4f()).scaling(0.30f, 0.21f, 1f));

		photoSpin.addTarget(pyramids[idx]);
		activatedPyramidBob.addTarget(hudIcons[idx]);

		if (score == 3)
			showEvent("ALL PHOTOS TAKEN  RETURN HOME", 2.0);
	}

	// A2 win action: SPACE places photos on the wall when close enough to home.
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

		activatedPyramidBob.disable();

		showEvent("PHOTOS PLACED", 2.0);
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

		showEvent("PIECE REMOVED", 0.8);
	}

	private float snap(float v, float step) {
		return Math.round(v / step) * step;
	}

	private float snapToCellCenter(float v, float step) {
		return (Math.round(v / step - 0.5f) + 0.5f) * step;
	}

	private Vector3f getBuildPiecePosition() {
		Vector3f pos = boy.getWorldLocation();
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

	// A2 orbit controls: orbit, elevation, and zoom update camera state without
	// changing dolphin heading.
	public void doOrbit(float input, float time) {
		if (orbitCam == null)
			return;
		float orbitSpeed = 90.0f;
		orbitCam.orbit(input * orbitSpeed * time);
		orbitCam.update();
	}

	public void doElevate(float input, float time) {
		if (orbitCam == null)
			return;
		float elevSpeed = 70.0f;
		orbitCam.elevate(input * elevSpeed * time);
		orbitCam.update();
	}

	public void doZoom(float input, float time) {
		if (orbitCam == null)
			return;
		float zoomSpeed = 6.0f;
		orbitCam.zoom(input * zoomSpeed * time);
		orbitCam.update();
	}

	// A2 ground movement: x and z movement is allowed, y stays at 0 unless jump is
	// active.
	public void doMove(float input, float time) {
		if (isLost())
			return;
		if (isWon())
			return;

		float dist = moveSpeed * input * time;

		Vector3f pos = boy.getWorldLocation();
		Vector3f fwd = dolphinForward();

		Vector3f newPos = new Vector3f(pos).add(new Vector3f(fwd).mul(dist));

		newPos.y = onGround ? 0.0f : pos.y;

		boy.setLocalLocation(newPos);
	}

	public void doStrafe(float dir, float time) {
		if (isLost())
			return;
		if (isWon())
			return;

		float dist = moveSpeed * dir * time;

		Vector3f pos = boy.getWorldLocation();
		Vector3f rt = new Vector3f(boy.getWorldRightVector()).normalize();

		Vector3f right = new Vector3f(rt.x, 0f, rt.z);
		if (right.lengthSquared() < 0.0001f)
			right.set(1f, 0f, 0f);
		right.normalize();

		Vector3f newPos = new Vector3f(pos).add(right.mul(dist));

		newPos.y = onGround ? 0.0f : pos.y;

		boy.setLocalLocation(newPos);
	}

	// A2 global yaw: uses globalYaw so turning is around world y, not local y.
	public void doYaw(float input, float time) {
		if (isLost())
			return;
		if (isWon())
			return;

		float yawSpeed = 1.6f;
		float ang = yawSpeed * input * time;

		boy.globalYaw(ang);
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

		Vector3f pos = boy.getWorldLocation();
		float newY = pos.y + yVel * dt;

		if (newY <= 0.0f) {
			newY = 0.0f;
			yVel = 0.0f;
			onGround = true;
		}

		boy.setLocalLocation(new Vector3f(pos.x, newY, pos.z));
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
		return Math.round(OVER_LEFT * winW());
	}

	// HUD top position is derived from viewport placement so the text stays inside
	// the overhead viewport.
	private int overHudTopPx() {
		return Math.round((1.0f - (OVER_BOTTOM + OVER_H)) * winH());
	}

	// Mouse-look initialization sets up Robot recentering, stores the center of
	// the main viewport, registers the canvas listener, and hides the cursor.
	private void initMouseMode() {
		mouseModeInitiated = true;

		RenderSystem rs = engine.getRenderSystem();
		Viewport vw = rs.getViewport("MAIN");

		float left = vw.getActualLeft();
		float bottom = vw.getActualBottom();
		float width = vw.getActualWidth();
		float height = vw.getActualHeight();

		centerX = (int) (left + width / 2.0f);
		centerY = (int) (bottom - height / 2.0f);

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

		recenterMouse();

		prevMouseX = centerX;
		prevMouseY = centerY;

		canvas.requestFocus();
	}

	// Robot places the mouse back in the center after each move, which prevents
	// the cursor from hitting the screen edge.
	private void recenterMouse() {
		if (!mouseLookEnabled)
			return;

		RenderSystem rs = engine.getRenderSystem();
		Viewport vw = rs.getViewport("MAIN");

		float left = vw.getActualLeft();
		float bottom = vw.getActualBottom();
		float width = vw.getActualWidth();
		float height = vw.getActualHeight();

		centerX = (int) (left + width / 2.0f);
		centerY = (int) (bottom - height / 2.0f);

		isRecentering = true;
		robot.mouseMove((int) centerX, (int) centerY);
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
			canvas.requestFocus();
			showEvent("MOUSE LOCKED", 1.0);
		} else {
			isRecentering = false;
			canvas.setCursor(defaultCursor);
			showEvent("MOUSE UNLOCKED", 1.0);
		}
	}

	// Horizontal mouse motion rotates the avatar using global yaw so the boy and
	// the targeted orbit camera both follow the mouse direction.
	private void yawMouse(float mouseDeltaX) {
		if (isLost() || isWon())
			return;

		if (java.lang.Math.abs(mouseDeltaX) < 0.001f)
			return;

		float yawAmt = mouseDeltaX * mouseYawSpeed;

		// Ground-based character turning should use global yaw.
		boy.globalYaw(yawAmt);

		if (orbitCam != null)
			orbitCam.update();
	}

	// Vertical mouse motion adjusts orbit camera elevation, which is the natural
	// pitch control for a targeted third-person camera.
	private void pitchMouse(float mouseDeltaY) {
		if (orbitCam == null)
			return;

		if (java.lang.Math.abs(mouseDeltaY) < 0.001f)
			return;

		float elevAmt = -mouseDeltaY * mouseElevSpeed;
		orbitCam.elevate(elevAmt);
		orbitCam.update();
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
}