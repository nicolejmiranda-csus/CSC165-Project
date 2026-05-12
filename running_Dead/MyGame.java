package running_Dead;

import java.awt.event.MouseEvent;
import java.util.UUID;
import javax.swing.JOptionPane;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import running_Dead.networking.GhostManager;
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.NetworkDiscovery;
import tage.shapes.AnimatedShape;

/**
 * Main Running_Dead game coordinator.
 * Most real work is delegated to named systems so the class fulfills requirements
 * and anyone can jump directly to movement, items, AI, building, lighting, or networking.
 * Connected to: Launched by the run scripts/java command; owns all MyGame* systems and is called by input actions.
 */
public class MyGame extends VariableFrameRateGame {
	/*
	Running_Dead requirement map
	Multiplayer discovery/client startup: main, MyGameNetworkingSystem, ProtocolClient, NetworkingServer
	Role-based gameplay: MyGameState, MyGameHUDSystem, MyGameItemSystem, GameServer state broadcasts
	Day/night lighting and flashlight visibility: MyGameLighting, MyGameItemSystem, StandardFrag.glsl
	Terrain-aware building and scenery placement: MyGameBuildSystem, MyGameWorldBuilder, MyGamePhysicsSystem
	AI threats: MyGameSmilingManSystem and MyGameMushroomMonSystem
	Team-only name labels: MyGameHUDSystem uses TAGE world HUD labels so humans see humans and zombies see zombies
	*/

	private static Engine engine;

	final MyGameState state = new MyGameState();
	final MyGameAssets assets = new MyGameAssets();

	final MyGameLoaders loaders = new MyGameLoaders(this);
	final MyGameWorldBuilder worldBuilder = new MyGameWorldBuilder(this);
	final MyGameLighting lighting = new MyGameLighting(this);
	final MyGameInitializer initializer = new MyGameInitializer(this);
	final MyGameUpdater updater = new MyGameUpdater(this);
	final MyGameHUDSystem hudSystem = new MyGameHUDSystem(this);
	final MyGameBuildSystem buildSystem = new MyGameBuildSystem(this);
	final MyGameCameraSystem cameraSystem = new MyGameCameraSystem(this);
	final MyGameMovementSystem movementSystem = new MyGameMovementSystem(this);
	final MyGameAnimationSystem animationSystem = new MyGameAnimationSystem(this);
	final MyGameSoundSystem soundSystem = new MyGameSoundSystem(this);
	final MyGameSmilingManSystem smilingManSystem = new MyGameSmilingManSystem(this);
	final MyGameItemSystem itemSystem = new MyGameItemSystem(this);
	final MyGameVisualSystem visualSystem = new MyGameVisualSystem(this);
	final MyGameMouseLookSystem mouseLookSystem = new MyGameMouseLookSystem(this);
	final MyGameMushroomMonSystem mushroomMonSystem = new MyGameMushroomMonSystem(this);
	final MyGameNetworkingSystem networking = new MyGameNetworkingSystem(this);
	final MyGamePhysicsSystem physicsSystem = new MyGamePhysicsSystem(this);
	final MyGameInputBinder inputBinder = new MyGameInputBinder(this);
	private final java.util.HashMap<UUID, ObjShape> assignedGhostShapes = new java.util.HashMap<>();

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

	private static String choosePlayerNamePopup() {
		String fallback = System.getProperty("user.name", "Player");
		while (true) {
			String value = JOptionPane.showInputDialog(
					null,
					"Enter your player name:",
					"Player Name",
					JOptionPane.QUESTION_MESSAGE);
			if (value == null) value = fallback;
			value = sanitizePlayerName(value);
			if (!value.isEmpty()) return value;
			JOptionPane.showMessageDialog(null, "Player name cannot be empty.", "Player Name", JOptionPane.WARNING_MESSAGE);
		}
	}

	private static String sanitizePlayerName(String value) {
		if (value == null) return "";
		String cleaned = value.replace(',', ' ').trim().replaceAll("\\s+", " ");
		if (cleaned.length() > 24) cleaned = cleaned.substring(0, 24).trim();
		return cleaned;
	}

	private static String joinedArgs(String[] args, int start) {
		if (args == null || start >= args.length) return "";
		StringBuilder builder = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			if (builder.length() > 0) builder.append(' ');
			builder.append(args[i]);
		}
		return builder.toString();
	}

	public static void main(String[] args) {
		MyGameNativePathSanitizer.relaunchWithSanitizedPathIfNeeded(MyGame.class.getName(), args);
		MyGameNativePathSanitizer.apply();
		MyGame game;

		if (args.length == 0) {
			game = new MyGame();
			game.setSelectedAvatarType(chooseAvatarPopup());
			game.setPlayerName(choosePlayerNamePopup());
		} else if (args.length >= 1 && NetworkDiscovery.usesAutoDiscovery(args[0])) {
			game = new MyGame(args[0]);
			game.setSelectedAvatarType(args.length >= 2 ? args[1] : chooseAvatarPopup());
			game.setPlayerName(args.length >= 3 ? joinedArgs(args, 2) : choosePlayerNamePopup());
		} else if (args.length >= 3) {
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
			game.setSelectedAvatarType(args.length >= 4 ? args[3] : chooseAvatarPopup());
			game.setPlayerName(args.length >= 5 ? joinedArgs(args, 4) : choosePlayerNamePopup());
		} else {
			System.out.println("Usage:");
			System.out.println("Single-player: java running_Dead.MyGame");
			System.out.println("Server       : java running_Dead.server.NetworkingServer <port> <UDP|TCP>");
			System.out.println("Auto client  : java running_Dead.MyGame AUTO [avatarType] [playerName]");
			System.out.println("Manual client: java running_Dead.MyGame <serverAddress> <serverPort> <UDP|TCP> [avatarType] [playerName]");
			System.out.println("Note         : NetworkingServer tracks round, pickup, role, health, build, and animation state.");
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
	public void loadSounds() {
		soundSystem.loadSounds();
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
	public void initializePhysicsObjects() {
		physicsSystem.initializePhysicsObjects();
	}

	@Override
	public void update() {
		updater.update();
	}

	@Override
	public float getScreenFlashOpacity() {
		if (state.blindTimer > 0.0) return Math.min(1.0f, (float) (state.blindTimer / 0.15));
		if (state.stareWarningTimer > 0.0) return Math.min(0.58f, (float) (state.stareWarningTimer / GameConstants.SMILING_MAN_STARE_WARNING_SECONDS) * 0.58f);
		return 0.0f;
	}

	@Override
	public Vector3f getScreenFlashColor() {
		if (state.blindTimer > 0.0) return new Vector3f(1.0f, 1.0f, 1.0f);
		if (state.stareWarningTimer > 0.0) return new Vector3f(0.0f, 0.0f, 0.0f);
		return new Vector3f(1.0f, 1.0f, 1.0f);
	}

	@Override
	public float getScreenFlashVignette() {
		return state.blindTimer <= 0.0 && state.stareWarningTimer > 0.0 ? 1.0f : 0.0f;
	}

	@Override
	public float getCloudNightFactor() {
		if (!state.dayNightEnabled) return 0.0f;
		return state.dayNightTime >= state.daySlotSeconds ? 1.0f : 0.0f;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseLookSystem.mouseMoved(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseLookSystem.mouseMoved(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			handlePrimaryAction();
		}
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

	public void cycleBuildMaterial() {
		buildSystem.cycleBuildMaterial();
	}

	public void placeBuildWall() {
		buildSystem.placeBuildWall();
	}

	public void removeBuildWall() {
		buildSystem.removeBuildWall();
	}

	public void useZombieTagAbility() {
		physicsSystem.useAbility();
	}

	public void throwZombieTagProjectile() {
		physicsSystem.throwProjectile();
	}

	public void attackBuildPiece() {
		physicsSystem.attackBuildPiece();
	}

	public void handlePrimaryAction() {
		if (state.buildMode) {
			buildSystem.placeBuildWall();
			return;
		}
		if (state.localPlayerZombie) {
			if (state.equippedItem == GameConstants.ITEM_BABY_ZOMBIE) physicsSystem.throwProjectile();
			else physicsSystem.attackAsZombie();
			return;
		}
		if (state.equippedItem == GameConstants.ITEM_FLASHLIGHT) itemSystem.toggleEquippedFlashlight();
		else if (state.equippedItem == GameConstants.ITEM_POTION) itemSystem.usePotion();
		else if (state.equippedItem == GameConstants.ITEM_ROCK) physicsSystem.throwProjectile();
		else hudSystem.showEvent("NO ITEM EQUIPPED", 0.8);
	}

	public void handleFAction() {
		if (state.localPlayerZombie) physicsSystem.useAbility();
		else itemSystem.equipFlashlight();
	}

	public void handleRAction() {
		if (state.buildMode) buildSystem.rotateBuildWall();
		else if (state.localPlayerZombie) itemSystem.equipBabyZombie();
		else physicsSystem.useAbility();
	}

	public void handleQAction() {
		if (state.buildMode) buildSystem.switchBuildPiece();
		else if (!state.localPlayerZombie) itemSystem.equipRock();
	}

	public void togglePhysicsVisualization() {
		physicsSystem.togglePhysicsVisualization();
	}

	public void toggleZombieRoleForTesting() {
		physicsSystem.toggleLocalRoleForTesting();
	}

	public void applyRemoteBuild(int pieceType, int modeType, int roofDir, int materialType, Vector3f p) {
		buildSystem.applyRemoteBuild(pieceType, modeType, roofDir, materialType, p);
	}

	public void applyRemoteRemoveBuild(int pieceType, int modeType, int roofDir, int materialType, Vector3f p) {
		buildSystem.applyRemoteRemoveBuild(pieceType, modeType, roofDir, materialType, p);
	}

	public void applyRemoteRole(java.util.UUID id, boolean zombie) {
		physicsSystem.applyRemoteRole(id, zombie);
	}

	public void applyRemoteTag(java.util.UUID sourceId, java.util.UUID targetId) {
		physicsSystem.applyRemoteTag(sourceId, targetId);
	}

	public void applyHumanLastChanceEscape(java.util.UUID id) {
		physicsSystem.applyHumanLastChanceEscape(id);
		smilingManSystem.onHumanLastChanceEscape(id);
	}

	public void applyRemoteAbility(java.util.UUID sourceId, String ability, boolean active) {
		physicsSystem.applyRemoteAbility(sourceId, ability, active);
	}

	public void applyRemoteProjectile(java.util.UUID sourceId, int projectileType, Vector3f pos, Vector3f velocity) {
		physicsSystem.spawnRemoteProjectile(sourceId, projectileType, pos, velocity);
	}

	public void applyRemoteSlow(java.util.UUID sourceId, java.util.UUID targetId, float seconds) {
		physicsSystem.applyRemoteSlow(sourceId, targetId, seconds);
	}

	public void applyRemoteBlind(java.util.UUID sourceId, java.util.UUID targetId, float seconds) {
		itemSystem.applyRemoteBlind(sourceId, targetId, seconds);
	}

	public void applyServerPickupState(int pickupId, int pickupType, boolean active, float x, float z, long respawnEpochMs) {
		physicsSystem.applyServerPickupState(pickupId, pickupType, active, x, z, respawnEpochMs);
	}

	public void applyServerPickupSpawn(int pickupId, int pickupType, float x, float z) {
		physicsSystem.applyServerPickupSpawn(pickupId, pickupType, x, z);
	}

	public void applyServerPickupHide(int pickupId, long respawnEpochMs) {
		physicsSystem.applyServerPickupHide(pickupId, respawnEpochMs);
	}

	public void applyServerPickupGrant(int pickupId, int pickupType) {
		physicsSystem.applyServerPickupGrant(pickupId, pickupType);
	}

	public void applyServerRoundWaiting() {
		physicsSystem.applyServerRoundWaiting();
	}

	public void applyServerRoundCountdown(long endEpochMs) {
		physicsSystem.applyServerRoundCountdown(endEpochMs);
	}

	public void applyServerRoundStart(java.util.UUID zombieId, long endEpochMs) {
		physicsSystem.applyServerRoundStart(zombieId, endEpochMs);
	}

	public void applyServerRoundEnd(String winner) {
		physicsSystem.applyServerRoundEnd(winner);
	}

	public void applyRemoteHealth(java.util.UUID id, int health) {
		physicsSystem.applyRemoteHealth(id, health);
	}

	public void applyRemoteSmilingManState(java.util.UUID sourceId, boolean spawned, Vector3f position, float yaw, String animationName) {
		smilingManSystem.applyRemoteState(sourceId, spawned, position, yaw, animationName);
	}

	public void applyRemoteSmilingManState(int index, java.util.UUID sourceId, boolean spawned, Vector3f position, float yaw, String animationName) {
		smilingManSystem.applyRemoteState(index, sourceId, spawned, position, yaw, animationName);
	}

	public void applyRemoteSmilingManBlind(java.util.UUID sourceId) {
		smilingManSystem.applyRemoteBlind(sourceId);
	}

	public void applyRemoteSmilingManBlind(int index, java.util.UUID sourceId) {
		smilingManSystem.applyRemoteBlind(index, sourceId);
	}

	public void applyMushroomMonExplosion(java.util.UUID targetId) { mushroomMonSystem.applyExplosion(targetId); }
	public void applyRemoteMushroomMonState(java.util.UUID sourceId, boolean spawned, Vector3f position, float yaw, String animationName) { mushroomMonSystem.applyRemoteState(sourceId, spawned, position, yaw, animationName); }

	public void applyRemoteAnimation(java.util.UUID id, String animationName) {
		physicsSystem.applyRemoteAnimation(id, animationName);
	}

	public void applyRemoteSound(java.util.UUID sourceId, String soundType, Vector3f location) {
		soundSystem.playRemoteSound(soundType, location);
	}

	public void removeRemotePhysicsFor(java.util.UUID id) {
		physicsSystem.removeRemotePlayerBody(id);
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
		return state.matchZombieWon;
	}

	public boolean isWon() {
		return state.matchHumanWon;
	}

	public boolean isMatchOver() {
		return state.matchHumanWon || state.matchZombieWon;
	}

	public boolean isLocalPlayerZombie() {
		return state.localPlayerZombie;
	}

	public boolean isLocalHumanTarget() {
		return !state.localPlayerZombie && state.health > 0;
	}

	public float getPlayerYaw() {
		return state.playerYaw;
	}

	public Vector3f getPlayerPosition() {
		return assets.avatar.getWorldLocation();
	}

	public UUID getLocalPlayerId() {
		if (state.protClient != null) return state.protClient.getID();
		return state.offlineLocalId;
	}

	public Vector3f avatarForward() {
		return (new Vector3f(assets.avatar.getWorldForwardVector())).normalize();
	}

	public void setIsConnected(boolean value) {
		networking.setIsConnected(value);
	}

	public String getSelectedAvatarType() {
		return state.selectedAvatarType;
	}

	public String getPlayerName() {
		return state.playerName;
	}

	public void setSelectedAvatarType(String type) {
		if ("playerModel2".equals(type)) state.selectedAvatarType = "playerModel2";
		else state.selectedAvatarType = "playerModel1";
	}

	public void setPlayerName(String name) {
		String cleaned = sanitizePlayerName(name);
		state.playerName = cleaned.isEmpty() ? "Player" : cleaned;
	}

	public void applyRemotePlayerName(UUID id, String name) {
		if (id == null) return;
		String cleaned = sanitizePlayerName(name);
		state.remotePlayerNames.put(id, cleaned.isEmpty() ? "Player" : cleaned);
	}

	public void announceRemotePlayerJoined(UUID id, String name) {
		applyRemotePlayerName(id, name);
		String joinedName = state.remotePlayerNames.getOrDefault(id, "Player");
		hudSystem.showEvent(joinedName + " has joined the lobby", 2.2);
		soundSystem.playLobbyJoin();
	}

	public void announceLocalPlayerJoined() {
		hudSystem.showEvent(getPlayerName() + " has joined the lobby", 2.2);
		soundSystem.playLobbyJoin();
	}

	boolean isPlayerModel1Selected() {
		return "playerModel1".equals(state.selectedAvatarType);
	}

	boolean isPlayerModel2Selected() {
		return "playerModel2".equals(state.selectedAvatarType);
	}

	boolean canUseAnimatedPlayerModel1() {
		return assets.playerModel1AnimatedS != null;
	}

	boolean canUseAnimatedPlayerModel2() {
		return assets.playerModel2AnimatedS != null;
	}

	public ObjShape getGhostShape(String avatarType) {
		return "playerModel2".equals(avatarType) ? assets.playerModel2S : assets.playerModel1S;
	}

	public ObjShape createGhostShape(String avatarType) {
		return getGhostShape(avatarType);
	}

	public ObjShape createGhostShape(UUID id, String avatarType) {
		if ("playerModel2".equals(avatarType)) {
			if (assets.playerModel2GhostAnimatedS != null) {
				for (AnimatedShape shape : assets.playerModel2GhostAnimatedS) {
					if (assignedGhostShapes.containsValue(shape)) continue;
					if (id != null) assignedGhostShapes.put(id, shape);
					return shape;
				}
			}
   		 	return assets.playerModel2S; // fallback if pool exhausted
		}
		if (id != null && assignedGhostShapes.containsKey(id)) return assignedGhostShapes.get(id);
		if (assets.playerModel1GhostAnimatedS != null) {
			for (AnimatedShape shape : assets.playerModel1GhostAnimatedS) {
				if (assignedGhostShapes.containsValue(shape)) continue;
				if (id != null) assignedGhostShapes.put(id, shape);
				return shape;
			}
		}
		System.out.println("animated ghost shape pool exhausted; using static player model");
		return assets.playerModel1S;
	}

	public void releaseGhostShape(UUID id) {
		if (id != null) assignedGhostShapes.remove(id);
	}

	public TextureImage getGhostTexture(String avatarType) {
		return "playerModel2".equals(avatarType) ? assets.playerModel2Tx : assets.playerModel1Tx;
	}

	public TextureImage getGhostDetailTexture(String avatarType) {
		return "playerModel2".equals(avatarType) ? assets.playerModel2FarTx : assets.playerModel1FarTx;
	}

	public Matrix4f getGhostScale(String avatarType) {
		return (new Matrix4f()).scaling(1.0f);
	}

	public ObjShape getRemoteHeldItemShape(int itemType) {
		if (itemType == GameConstants.ITEM_FLASHLIGHT) return assets.flashlightS;
		if (itemType == GameConstants.ITEM_POTION) return assets.healthPotionS;
		if (itemType == GameConstants.ITEM_ROCK) return assets.rock2S;
		if (itemType == GameConstants.ITEM_BABY_ZOMBIE) return assets.babyZombieS;
		return null;
	}

	public TextureImage getRemoteHeldItemTexture(int itemType) {
		if (itemType == GameConstants.ITEM_FLASHLIGHT) return assets.flashlightTx;
		if (itemType == GameConstants.ITEM_POTION) return assets.healthPotionTx;
		if (itemType == GameConstants.ITEM_ROCK) return assets.rockTx;
		if (itemType == GameConstants.ITEM_BABY_ZOMBIE) return assets.babyZombieTx;
		return null;
	}

	public float getRemoteHeldItemScale(String avatarType, int itemType) {
		boolean model2 = "playerModel2".equals(avatarType);
		if (itemType == GameConstants.ITEM_FLASHLIGHT) return model2 ? 0.18f : 0.23f;
		if (itemType == GameConstants.ITEM_POTION) return model2 ? 0.12f : 0.10f;
		if (itemType == GameConstants.ITEM_ROCK) return model2 ? 0.18f : 0.20f;
		if (itemType == GameConstants.ITEM_BABY_ZOMBIE) return model2 ? 0.13f : 0.15f;
		return 1.0f;
	}

	public Vector3f getRemoteHeldItemOffset(String avatarType, int itemType) {
		boolean model2 = "playerModel2".equals(avatarType);
		if (itemType == GameConstants.ITEM_FLASHLIGHT)
			return new Vector3f(model2 ? -1.18f : -0.95f, model2 ? 1.14f : 1.34f, model2 ? 0.08f : 0.10f);
		if (itemType == GameConstants.ITEM_POTION)
			return new Vector3f(model2 ? -1.14f : -0.92f, model2 ? 1.12f : 1.32f, model2 ? 0.10f : 0.12f);
		return new Vector3f(model2 ? -1.12f : -0.92f, model2 ? 1.12f : 1.32f, model2 ? 0.12f : 0.14f);
	}

	public Matrix4f getRemoteHeldItemRotation(int itemType) {
		if (itemType == GameConstants.ITEM_ROCK) return new Matrix4f().rotationY((float) Math.toRadians(30.0f));
		if (itemType == GameConstants.ITEM_BABY_ZOMBIE) return new Matrix4f().rotationY((float) Math.toRadians(180.0f));
		return new Matrix4f().identity();
	}

	public void styleRemoteHeldItem(GameObject item, int itemType) {
		if (item == null) return;
		if (itemType == GameConstants.ITEM_FLASHLIGHT) {
			item.getRenderStates().hasLighting(true);
			item.getRenderStates().isEnvironmentMapped(false);
			item.getRenderStates().setHasSolidColor(true);
			item.getRenderStates().setColor(new Vector3f(0.018f, 0.017f, 0.015f));
			item.getRenderStates().setTextureDetailBlend(false);
			item.getRenderStates().setNormalMapping(false);
			item.getRenderStates().castsShadow(true);
		} else if (itemType == GameConstants.ITEM_POTION) {
			physicsSystem.stylePotionGlass(item, 0.42f);
		} else if (itemType == GameConstants.ITEM_ROCK) {
			item.getRenderStates().hasLighting(true);
			item.getRenderStates().setBumpMapping(true);
			item.setNormalMap(assets.rockNormalTx);
			item.getRenderStates().setNormalMapping(true);
			item.getRenderStates().setTextureMappingMode(1);
			item.getRenderStates().setTextureDetailBlend(true);
			item.getRenderStates().setSurfaceScale(0.9f);
			item.setDetailTextureImage(assets.rockFarTx);
		} else if (itemType == GameConstants.ITEM_BABY_ZOMBIE) {
			item.getRenderStates().hasLighting(true);
			item.setDetailTextureImage(assets.babyZombieFarTx);
			item.getRenderStates().setTextureDetailBlend(true);
		}
	}

	public GhostManager getGhostManager() {
		if (state.gm == null)
			state.gm = new GhostManager(this);
		return state.gm;
	}

	public String getRemoteAnimationState(java.util.UUID id) {
		return state.remoteAnimationStates.getOrDefault(id, "IDLE");
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

