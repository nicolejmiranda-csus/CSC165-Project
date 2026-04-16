package a3;

import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import a3.networking.GhostManager;
import tage.Camera;
import tage.Engine;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.NetworkDiscovery;

public class MyGame extends VariableFrameRateGame {
	private static Engine engine;

	final MyGameState state = new MyGameState();
	final MyGameAssets assets = new MyGameAssets();

	final MyGameLoaders loaders = new MyGameLoaders(this);
	final MyGameWorldBuilder worldBuilder = new MyGameWorldBuilder(this);
	final MyGameLighting lighting = new MyGameLighting(this);
	final MyGameInitializer initializer = new MyGameInitializer(this);
	final MyGameUpdater updater = new MyGameUpdater(this);
	final MyGameHUDSystem hudSystem = new MyGameHUDSystem(this);
	final MyGamePhotoSystem photoSystem = new MyGamePhotoSystem(this);
	final MyGameBuildSystem buildSystem = new MyGameBuildSystem(this);
	final MyGameCameraSystem cameraSystem = new MyGameCameraSystem(this);
	final MyGameMovementSystem movementSystem = new MyGameMovementSystem(this);
	final MyGameItemSystem itemSystem = new MyGameItemSystem(this);
	final MyGameVisualSystem visualSystem = new MyGameVisualSystem(this);
	final MyGameMouseLookSystem mouseLookSystem = new MyGameMouseLookSystem(this);
	final MyGameNetworkingSystem networking = new MyGameNetworkingSystem(this);
	final MyGameInputBinder inputBinder = new MyGameInputBinder(this);

	public MyGame() {
		super();
		state.isMultiplayer = false;
	}

	public MyGame(String serverAddress) {
		super();
		state.serverAddress = serverAddress;
		state.serverPort = 0;
		state.serverProtocol = ProtocolType.UDP;
		state.isMultiplayer = true;
	}

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		state.serverAddress = serverAddress;
		state.serverPort = serverPort;
		state.serverProtocol = protocol.toUpperCase().compareTo("TCP") == 0 ? ProtocolType.TCP : ProtocolType.UDP;
		state.isMultiplayer = true;
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
		return choice == null ? "playerModel1" : choice.toString();
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
			if (args.length >= 4)
				game.setSelectedAvatarType(args[3]);
			else
				game.setSelectedAvatarType(chooseAvatarPopup());
		} else {
			System.out.println("Usage:");
			System.out.println("Single-player: java a3.MyGame");
			System.out.println("Server       : java a3.server.NetworkingServer <port> <UDP|TCP>");
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
		loaders.loadShapes();
	}

	@Override
	public void loadTextures() {
		loaders.loadTextures();
	}

	@Override
	public void loadSkyBoxes() {
		loaders.loadSkyBoxes();
	}

	@Override
	public void buildObjects() {
		worldBuilder.buildObjects();
	}

	@Override
	public void initializeLights() {
		lighting.initializeLights();
	}

	@Override
	public void initializeGame() {
		initializer.initializeGame();
	}

	@Override
	public void update() {
		updater.update();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseLookSystem.mouseMoved(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseLookSystem.mouseMoved(e);
	}

	public void setPadMove(float v) {
		movementSystem.setPadMove(v);
	}

	public void setPadStrafe(float v) {
		movementSystem.setPadStrafe(v);
	}

	public void triggerRun() {
		movementSystem.triggerRun();
	}

	public void doMove(float input, float time) {
		movementSystem.doMove(input, time);
	}

	public void doStrafe(float dir, float time) {
		movementSystem.doStrafe(dir, time);
	}

	public void doYaw(float input, float time) {
		movementSystem.doYaw(input, time);
	}

	public void doJump() {
		movementSystem.doJump();
	}

	public void tryTakePhoto() {
		photoSystem.tryTakePhoto();
	}

	public void tryPlacePhotos() {
		photoSystem.tryPlacePhotos();
	}

	public void applyRemotePhoto(int idx) {
		photoSystem.applyRemotePhoto(idx);
	}

	public void applyRemotePlacePhotos() {
		photoSystem.applyRemotePlacePhotos();
	}

	public void toggleBuildMode() {
		buildSystem.toggleBuildMode();
	}

	public void raiseBuildHeight() {
		buildSystem.raiseBuildHeight();
	}

	public void lowerBuildHeight() {
		buildSystem.lowerBuildHeight();
	}

	public void rotateBuildWall() {
		buildSystem.rotateBuildWall();
	}

	public void switchBuildPiece() {
		buildSystem.switchBuildPiece();
	}

	public void placeBuildWall() {
		buildSystem.placeBuildWall();
	}

	public void removeBuildWall() {
		buildSystem.removeBuildWall();
	}

	public void applyRemoteBuild(int pieceType, int modeType, int roofDir, Vector3f p) {
		buildSystem.applyRemoteBuild(pieceType, modeType, roofDir, p);
	}

	public void applyRemoteRemoveBuild(int pieceType, int modeType, int roofDir, Vector3f p) {
		buildSystem.applyRemoteRemoveBuild(pieceType, modeType, roofDir, p);
	}

	public void setOverheadCamera(Camera cam) {
		cameraSystem.setOverheadCamera(cam);
	}

	public void panOverhead(float dx, float dz) {
		cameraSystem.panOverhead(dx, dz);
	}

	public void zoomOverhead(float dy) {
		cameraSystem.zoomOverhead(dy);
	}

	public void toggleFullMap() {
		cameraSystem.toggleFullMap();
	}

	public void toggleCameraMode() {
		cameraSystem.toggleCameraMode();
	}

	public void swapShoulderCamera() {
		cameraSystem.swapShoulderCamera();
	}

	public void doOrbit(float input, float time) {
		cameraSystem.doOrbit(input, time);
	}

	public void doElevate(float input, float time) {
		cameraSystem.doElevate(input, time);
	}

	public void doZoom(float input, float time) {
		cameraSystem.doZoom(input, time);
	}

	public void toggleMouseLook() {
		mouseLookSystem.toggleMouseLook();
	}

	public void toggleAxesVisibility() {
		visualSystem.toggleAxesVisibility();
	}

	public void toggleHelp() {
		hudSystem.toggleHelp();
	}

	public void equipFlashlight() {
		itemSystem.equipFlashlight();
	}

	public void equipPotion() {
		itemSystem.equipPotion();
	}

	public void unequipItem() {
		itemSystem.unequipItem();
	}

	public void usePotion() {
		itemSystem.usePotion();
	}

	public void useSky04() {
		visualSystem.useSky04();
	}

	public void useSky15Night() {
		visualSystem.useSky15Night();
	}

	public void toggleSkyboxOff() {
		visualSystem.toggleSkyboxOff();
	}

	public static Engine getEngine() {
		return engine;
	}

	public boolean isLost() {
		return state.lost;
	}

	public boolean isWon() {
		return state.won;
	}

	public float getPlayerYaw() {
		return state.playerYaw;
	}

	public Vector3f getPlayerPosition() {
		return assets.avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value) {
		networking.setIsConnected(value);
	}

	public String getSelectedAvatarType() {
		return state.selectedAvatarType;
	}

	public void setSelectedAvatarType(String type) {
		if (type != null)
			state.selectedAvatarType = type;
	}

	boolean isPlayerModel2Selected() {
		return "playerModel2".equals(state.selectedAvatarType);
	}

	public Matrix4f getCollectedHudIconScale() {
		return (new Matrix4f()).scaling(0.30f, 0.21f, 1f);
	}

	public ObjShape getGhostShape(String avatarType) {
		return "playerModel2".equals(avatarType) ? assets.playerModel2S : assets.playerModel1S;
	}

	public TextureImage getGhostTexture(String avatarType) {
		return "playerModel2".equals(avatarType) ? assets.playerModel2Tx : assets.playerModel1Tx;
	}

	public Matrix4f getGhostScale(String avatarType) {
		return (new Matrix4f()).scaling(1.0f);
	}

	public GhostManager getGhostManager() {
		if (state.gm == null)
			state.gm = new GhostManager(this);
		return state.gm;
	}

	int winW() {
		return engine.getRenderSystem().getGLCanvas().getWidth();
	}

	int winH() {
		return engine.getRenderSystem().getGLCanvas().getHeight();
	}

	int overHudLeftPx() {
		return java.lang.Math.round(GameConstants.OVER_LEFT * winW());
	}

	int overHudTopPx() {
		return java.lang.Math.round((1.0f - (GameConstants.OVER_BOTTOM + GameConstants.OVER_H)) * winH());
	}
}

