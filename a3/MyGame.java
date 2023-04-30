package a3;

import tage.*;
import tage.input.InputManager;
import tage.networking.IGameConnection.ProtocolType;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;
import tage.shapes.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import a3.controls.PlayerControlMap;
import a3.controls.PlayerControls;
import a3.network.GhostManager;
import a3.network.ProtocolClient;
import a3.npcs.Enemy;
import a3.player.Player;
import a3.quadtree.*;

public class MyGame extends VariableFrameRateGame {
	private static Engine engine;
	private static MyGame game;
	private static PlayerControlMap playerControlMaps; // do not delete!!!
	private static ScriptManager scriptManager;

	private InputManager inputManager;
	private TargetCamera targetCamera;
	private OverheadCamera overheadCamera;
	private PlayerControls controls;

	private int serverPort;
	private double lastFrameTime, currFrameTime, elapsTime;
	private float lastHeightLoc, currHeightLoc;

	private String serverAddress;

	private float frameTime = 0;
	private float currentRotation = 0;
	private float[] vals = new float[16];

	private GameObject terrain;

	private GhostManager ghostManager;
	private static PhysicsManager physicsManager;

	private Enemy enemy;
	private Player player;
	private AnimatedGameObject katana;
	private ProtocolType serverProtocol;
	private ProtocolClient protocolClient;
	private boolean isClientConnected = false;
	private boolean hasRotated = false;
	private boolean hasMovedNorthWest = false;
	private boolean hasMovedNorthEast = false;
	private boolean hasMovedSouthWest = false;
	private boolean hasMovedSouthEast = false;
	private boolean hasMovedWest = false;
	private boolean hasMovedEast = false;
	private boolean hasMovedSouth = false;

	private ObjShape playerS, enemyS, ghostS, terrS;
	private TextureImage playerTx, enemyTx, terrMap, ghostTx, terrTx, katanaTx;
	private AnimatedShape playerShape, katanaShape, enemyShape;

	private ArrayList<Enemy> enemyList = new ArrayList<Enemy>();
	private Map<Integer, Enemy> enemyMap = new HashMap<Integer, Enemy>();
	private Set<String> activeKeys = new HashSet<>();
	private Set<String> activeOrientation = new HashSet<>();
	private static QuadTree quadTree;
	private com.bulletphysics.dynamics.RigidBody object1, object2;

	public MyGame(String serverAddress, int serverPort, String protocol) {
		super();
		if (serverAddress != null && serverPort != 0 && protocol != null) {
			ghostManager = new GhostManager(this);
			this.serverAddress = serverAddress;
			this.serverPort = serverPort;
			if (protocol.toUpperCase().compareTo("UDP") == 0) {
				this.serverProtocol = ProtocolType.UDP;
			} else {
				System.err.println("PROTOCOL NOT SUPPORTED, EXITING...");
				System.exit(-1);
			}
		}
	}

	public MyGame() {
		super();
	}

	public static void main(String[] args) {
		if (args.length != 0) {
			System.out.println("setting up network...");
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		} else {
			game = new MyGame();
		}
		engine = new Engine(getGameInstance());
		scriptManager = new ScriptManager();
		scriptManager.loadScript("assets/scripts/LoadInitValues.js");
		physicsManager = new PhysicsManager();

		int playAreaSize = (int) scriptManager.getValue("PLAY_AREA_SIZE");
		quadTree = new QuadTree(
				new QuadTreePoint((float) playAreaSize, (float) playAreaSize),
				new QuadTreePoint((float) playAreaSize, (float) -playAreaSize));
		getGameInstance().initializeSystem();
		getGameInstance().game_loop();
	}

	private void setupNetworking() {
		isClientConnected = false;
		try {
			protocolClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (protocolClient == null) {
			System.out.println("missing protocol host");
		} else {
			System.out.println("sending join message...");
			protocolClient.sendJoinMessage();
		}
	}

	protected void processNetworking(float elapsTime) {
		if (protocolClient != null) {
			protocolClient.processPackets();
		}
	}

	@Override
	public void loadShapes() {
		ghostS = new Sphere();
		terrS = new TerrainPlane(150);
		initializePlayerAnimations();
		initializeEnemyAnimations();
	}

	@Override
	public void loadTextures() {
		playerTx = new TextureImage((String) scriptManager.getValue("PLAYER_TEXTURE"));
		ghostTx = new TextureImage((String) scriptManager.getValue("GHOST_TEXTURE"));
		enemyTx = new TextureImage((String) scriptManager.getValue("ENEMY_TEXTURE"));
		terrMap = new TextureImage((String) scriptManager.getValue("TERRAIN_MAP"));
		terrTx = new TextureImage((String) scriptManager.getValue("TERRAIN_TEXTURE"));
		katanaTx = new TextureImage((String) scriptManager.getValue("KATANA_TEXTURE"));
	}

	@Override
	public void loadSkyBoxes() {
		engine.getSceneGraph().setSkyBoxEnabled(true);
		engine.getSceneGraph().setActiveSkyBoxTexture(
				engine.getSceneGraph().loadCubeMap((String) scriptManager.getValue("SKYBOX_NAME")));
	}

	@Override
	public void buildObjects() {
		buildTerrainMap();
		buildPlayer();
		buildEnemy();
		buildEnemyQuadTree();
	}

	@Override
	public void initializeLights() {
		(engine.getSceneGraph()).addLight((Light) scriptManager.getValue("light"));
	}

	@Override
	public void initializeGame() {
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		lastHeightLoc = 0;
		elapsTime = 0.0;

		(engine.getRenderSystem()).setWindowDimensions(
				(int) scriptManager.getValue("WINDOW_WIDTH"),
				(int) scriptManager.getValue("WINDOW_HEIGHT"));

		initializeControls();
		updateFrameTime();
		initializeCameras();
		setupNetworking();
		initializePhysicsObjects();
		controls = new PlayerControls();
	}

	private void initializePhysicsObjects() {
	}

	private void initializePlayerAnimations() {
		playerShape = new AnimatedShape(
				(String) scriptManager.getValue("PLAYER_RKM"),
				(String) scriptManager.getValue("PLAYER_RKS"));
		playerShape.loadAnimation(
				"RUN", (String) scriptManager.getValue("PLAYER_RUN_RKA"));
		playerShape.loadAnimation(
				"IDLE", (String) scriptManager.getValue("PLAYER_IDLE_RKA"));
		playerShape.loadAnimation(
				"ATTACK1", (String) scriptManager.getValue("PLAYER_ATTACK_1_RKA"));
		playerShape.loadAnimation(
				"GUARD", (String) scriptManager.getValue("PLAYER_GUARD_RKA"));

		katanaShape = new AnimatedShape(
				(String) scriptManager.getValue("KATANA_RKM"),
				(String) scriptManager.getValue("KATANA_RKS"));
		katanaShape.loadAnimation(
				"RUN", (String) scriptManager.getValue("KATANA_RUN_RKA"));
		katanaShape.loadAnimation(
				"IDLE",
				(String) scriptManager.getValue("KATANA_IDLE_RKA"));
		katanaShape.loadAnimation(
				"ATTACK1",
				(String) scriptManager.getValue("KATANA_ATTACK_1_RKA"));
		katanaShape.loadAnimation(
				"GUARD",
				(String) scriptManager.getValue("KATANA_GUARD_RKA"));
	}

	private void initializeEnemyAnimations() {
		enemyShape = new AnimatedShape(
				(String) scriptManager.getValue("ENEMY_RKM"),
				(String) scriptManager.getValue("ENEMY_RKS"));
		enemyShape.loadAnimation(
				"RUN",
				(String) scriptManager.getValue("ENEMY_RUN_RKA"));
		enemyShape.loadAnimation(
				"IDLE",
				(String) scriptManager.getValue("ENEMY_IDLE_RKA"));
	}

	private void initializeCameras() {
		buildTargetCamera();
	}

	private void initializeControls() {
		inputManager = engine.getInputManager();
		playerControlMaps = new PlayerControlMap(inputManager);
	}

	@Override
	public void update() {
		updateFrameTime();
		updateMovementControls();
		player.updateAnimation();
		checkObjectMovement(player);
		// System.out.printf(
		// "player x: %.2f, player y: %.2f, player z: %.2f\nkatana x: %.2f, katana y:
		// %.2f, katana z: %.2f\n",
		// player.getLocalLocation().x(), player.getLocalLocation().y(),
		// player.getLocalLocation().z(),
		// katana.getWorldLocation().x(),
		// katana.getWorldLocation().y(),
		// katana.getWorldLocation().z());

		if (player.isLocked()) {
			targetCamera.targetTo();
		}
		checkForCollisions();

		targetCamera.setLookAtTarget(player.getLocalLocation());
		if (player.isMoving()) {
			updatePlayerTerrainLocation();
		}

		updatePhysicsObjectLocation(player.getPhysicsObject(), player.getLocalTranslation());

		updatePhysicsObjectLocation(katana.getPhysicsObject(), katana.getWorldTranslation());

		for (int i = 0; i < enemyList.size(); i++) {
			updatePhysicsObjectLocation(enemyList.get(i).getPhysicsObject(), enemyList.get(i).getLocalTranslation());
		}

		inputManager.update((float) elapsTime);
		physicsManager.update((float) elapsTime);
		processNetworking((float) elapsTime);
	}

	private void updatePhysicsObjectLocation(PhysicsObject po, Matrix4f localTranslation) {
		Matrix4f translation = new Matrix4f();
		double[] tempTransform;
		translation = new Matrix4f(localTranslation);
		tempTransform = toDoubleArray(translation.get(vals));
		po.setTransform(tempTransform);
	}

	private void matchGameObjectwithPhysicsObject(GameObject go, PhysicsObject po) {
		Matrix4f translation = new Matrix4f().set(toFloatArray(po.getTransform()));
		go.setLocalTranslation(translation);
	}

	private void checkObjectMovement(AnimatedGameObject obj) {
		if (obj.getLocalLocation().x() == obj.getLastLocation(0)
				&& obj.getLocalLocation().z() == obj.getLastLocation(2)) {
			obj.setIsMoving(false);
		} else {
			obj.setIsMoving(true);
		}

		if (!obj.isMoving()) {
			if (obj instanceof Player) {
				if (((Player) obj).getStanceState().isNormal()) {
					((Player) obj).idle();
				} else if (((Player) obj).getStanceState().isAttacking()) {
					if (((Player) obj).getAnimationShape().isAnimPlaying()) {
						obj.setLastLocation(
								new float[] { obj.getLocalLocation().x(), obj.getLocalLocation().y(),
										obj.getLocalLocation().z() });
						return;
					} else {
						((Player) obj).idle();
					}
				}
			} else {
				obj.idle();
			}
		}

		obj.setLastLocation(
				new float[] { obj.getLocalLocation().x(), obj.getLocalLocation().y(), obj.getLocalLocation().z() });

	}

	private void updatePlayerTerrainLocation() {
		Vector3f currLoc = player.getLocalLocation();
		currHeightLoc = terrain.getHeight(currLoc.x, currLoc.z);

		float targetHeight = currHeightLoc + player.getLocalScale().m00();
		double playerHeightSpeed = (double) scriptManager.getValue("PLAYER_HEIGHT_SPEED");
		float alpha = frameTime * (float) playerHeightSpeed;
		float newHeight = lerp(lastHeightLoc, targetHeight, alpha);

		player.setLocalLocation(new Vector3f(currLoc.x(), newHeight, currLoc.z()));
		lastHeightLoc = newHeight;
		targetCamera.updateCameraLocation(frameTime);
	}

	private void updateFrameTime() {
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		frameTime = (float) (currFrameTime - lastFrameTime) / 1000.0f;
		elapsTime += frameTime;
	}

	private void buildTerrainMap() {
		float up[] = { 0, 1, 0 };
		double[] tempTransform;
		PhysicsObject planeP;
		Matrix4f translation;

		terrain = new GameObject(GameObject.root(), terrS, terrTx);
		terrain.setLocalLocation(new Vector3f(0, 0, 0));
		int playAreaSize = (int) scriptManager.getValue("PLAY_AREA_SIZE");
		terrain.setLocalScale((new Matrix4f().scaling((float) playAreaSize)));
		terrain.setHeightMap(terrMap);
		terrain.getRenderStates().setTiling(1);

		translation = new Matrix4f(terrain.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		planeP = physicsManager.addStaticPlaneObject(
				physicsManager.nextUID(), tempTransform, up, 0.0f);
		terrain.setPhysicsObject(planeP);
	}

	private void buildPlayer() {
		float mass = 0.1f;
		double[] tempTransform;
		float[] size = { 5f, 5f, 5f };
		Matrix4f translation;
		PhysicsObject playerBody, katanaBody;

		player = new Player(GameObject.root(), playerShape, playerTx);
		katana = new AnimatedGameObject(player, katanaShape, katanaTx);
		player.addWeapon(katana);
		player.idle();
		katana.propagateTranslation(true);

		translation = new Matrix4f(player.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		playerBody = physicsManager.addBoxObject(physicsManager.nextUID(), mass,
				tempTransform, size);
		player.setPhysicsObject(playerBody);

		size = new float[] { 1f, 1f, 1f };
		translation = new Matrix4f(player.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		katanaBody = physicsManager.addBoxObject(physicsManager.nextUID(), mass, tempTransform, size);
		katana.setPhysicsObject(katanaBody);
	}

	private void buildEnemy() {
		float mass = 0.1f;
		double[] tempTransform;
		float[] size = new float[] { 0.5f, 0.5f, 0.5f };
		Matrix4f translation;
		PhysicsObject enemyObject;

		for (int i = 0; i < (int) scriptManager.getValue("ENEMY_AMOUNT"); i++) {
			enemy = new Enemy(GameObject.root(), enemyShape, enemyTx, i);
			enemyList.add(enemy);
			translation = new Matrix4f(enemy.getLocalTranslation());
			tempTransform = toDoubleArray(translation.get(vals));
			enemyObject = physicsManager.addBoxObject(physicsManager.nextUID(),
					mass, tempTransform, size);
			enemyMap.put(enemyObject.getUID(), enemy);
			enemy.setPhysicsObject(enemyObject);
		}
	}

	private void checkForCollisions() {
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld = ((JBulletPhysicsEngine) physicsManager.getPhysicsEngine()).getDynamicsWorld();

		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i = 0; i < manifoldCount; i++) {
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody1();

			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

			for (int j = 0; j < manifold.getNumContacts(); j++) {
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0f) {
					if (enemyMap.get(obj2.getUID()) != null) {
						if (obj2 == enemyMap.get(obj2.getUID()).getPhysicsObject() &&
								obj1.getUID() == katana.getPhysicsObject().getUID()
								&& player.getStanceState().isAttacking()) {

							System.out.println("hit!");
						} else if (obj2 == enemyMap.get(obj2.getUID()).getPhysicsObject() &&
								obj1.getUID() == player.getPhysicsObject().getUID()) {
							System.out.println("collided with enemy...");
							// obj2.applyForce(1, 0, 0, 0, 0, 0);
							matchGameObjectwithPhysicsObject(player, obj1);
							targetCamera.updateCameraLocation(getFrameTime());
						}
					}
				}
			}
		}
	}

	private void updateMovementControls() {
		boolean movingNorth = isKeyPressed("W");
		boolean movingWest = isKeyPressed("A");
		boolean movingSouth = isKeyPressed("S");
		boolean movingEast = isKeyPressed("D");

		if (movingNorth == false && movingWest == false && movingEast == false && movingSouth == false) {
			hasMovedNorthWest = false;
			hasMovedSouthEast = false;
			hasMovedNorthEast = false;
			hasMovedSouthWest = false;
			hasMovedEast = false;
			hasMovedWest = false;
			hasMovedSouth = false;
		}

		if (movingNorth && movingWest) {
			if (!hasMovedNorthWest) {
				if (!hasRotated) {
					currentRotation += 45f;
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = true;
				hasMovedSouthEast = false;
				hasMovedNorthEast = false;
				hasMovedSouthWest = false;
				hasMovedEast = false;
				hasMovedWest = false;
				hasMovedSouth = false;
			}
			getControls().moveNorth(getFrameTime());

		} else if (movingNorth && movingEast) {
			if (!hasMovedNorthEast) {
				if (!hasRotated) {
					currentRotation += -45f;
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = false;
				hasMovedSouthEast = false;
				hasMovedNorthEast = true;
				hasMovedSouthWest = false;
				hasMovedEast = false;
				hasMovedWest = false;
				hasMovedSouth = false;
			}
			getControls().moveNorth(getFrameTime());

		} else if (movingSouth && movingWest) {
			if (!hasMovedSouthWest) {
				if (!hasRotated) {
					currentRotation += 135f;
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = false;
				hasMovedSouthEast = false;
				hasMovedNorthEast = false;
				hasMovedSouthWest = true;
				hasMovedEast = false;
				hasMovedWest = false;
				hasMovedSouth = false;
			}
			getControls().moveNorth(getFrameTime());
		} else if (movingSouth && movingEast) {
			if (!hasMovedSouthEast) {
				if (!hasRotated) {
					currentRotation += -135f;
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = false;
				hasMovedSouthEast = true;
				hasMovedNorthEast = false;
				hasMovedSouthWest = false;
				hasMovedEast = false;
				hasMovedWest = false;
				hasMovedSouth = false;
			}
			getControls().moveNorth(getFrameTime());
		} else if (movingNorth) {
			hasMovedNorthWest = false;
			hasMovedNorthEast = false;
			getControls().moveNorth(getFrameTime());
		} else if (movingSouth) {
			if (!hasMovedSouth) {
				if (!hasRotated) {
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = false;
				hasMovedSouthEast = false;
				hasMovedNorthEast = false;
				hasMovedSouthWest = false;
				hasMovedEast = false;
				hasMovedWest = false;
				hasMovedSouth = true;
			}
			if (!player.isLocked()) {
				getControls().moveNorth(-getFrameTime());
			} else {
				getControls().moveNorth(-getFrameTime());
			}
		} else if (movingWest) {
			if (!hasMovedWest) {
				if (!hasRotated) {
					currentRotation += 90f;
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = false;
				hasMovedSouthEast = false;
				hasMovedNorthEast = false;
				hasMovedSouthWest = false;
				hasMovedEast = false;
				hasMovedWest = true;
				hasMovedSouth = false;
			}
			if (!player.isLocked()) {
				getControls().moveNorth(getFrameTime());
			} else {
				getControls().moveEast(-getFrameTime());
			}

		} else if (movingEast) {
			if (!hasMovedEast) {
				if (!hasRotated) {
					currentRotation += -90f;
					player.yaw(getFrameTime(), currentRotation);
					hasRotated = true;
				}
				hasMovedNorthWest = false;
				hasMovedSouthEast = false;
				hasMovedNorthEast = false;
				hasMovedSouthWest = false;
				hasMovedEast = true;
				hasMovedWest = false;
				hasMovedSouth = false;
			}
			if (!player.isLocked()) {
				getControls().moveNorth(getFrameTime());
			} else {
				getControls().moveEast(getFrameTime());
			}

		}
		hasRotated = false;

		if (currentRotation >= 360f) {
			currentRotation -= 360f;
		} else if (currentRotation <= -360f) {
			currentRotation += 360f;
		}
	}

	private void buildEnemyQuadTree() {
		for (Enemy e : enemyList) {
			quadTree.insert(new QuadTreeNode(new QuadTreePoint(e.getLocalLocation().z, e.getLocalLocation().x), e));
		}
	}

	private void buildTargetCamera() {
		targetCamera = new TargetCamera(getPlayer());
		targetCamera.setLocation((Vector3f) scriptManager.getValue("INITIAL_CAMERA_POS"));
		engine.getRenderSystem().getViewport("MAIN").setCamera(targetCamera);
		targetCamera.setLookAtTarget(player.getLocalLocation());
		targetCamera.setLocation(targetCamera.getLocation().mul(new Vector3f(1, 1, -1)));
		targetCamera.updateCameraAngles(frameTime);
	}

	public static MyGame getGameInstance() {
		return game;
	}

	public static Engine getEngineInstance() {
		if (engine == null)
			engine = new Engine(getGameInstance());
		return engine;
	}

	public GameObject findTarget() {
		QuadTreePoint playerPos = new QuadTreePoint(player.getLocalLocation().z,
				player.getLocalLocation().x());
		QuadTreeNode target;
		target = quadTree.findNearby(playerPos, -1, null);

		if (target != null) {
			return (GameObject) target.getData();
		}
		return null;
	}

	private float[] toFloatArray(double[] arr) {
		if (arr == null)
			return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float) arr[i];
		}
		return ret;
	}

	private double[] toDoubleArray(float[] arr) {
		if (arr == null) {
			return null;
		}
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double) arr[i];
		}
		return ret;
	}

	private float lerp(float start, float end, float alpha) {
		return start + alpha * (end - start);
	}

	public float getFrameTime() {
		return frameTime;
	}

	public PlayerControls getControls() {
		return controls;
	}

	public TargetCamera getTargetCamera() {
		return targetCamera;
	}

	public Camera getOverheadCamera() {
		return overheadCamera;
	}

	public Player getPlayer() {
		return player;
	}

	public Enemy getEnemy() {
		return enemy;
	}

	public void setIsConnected(boolean b) {
		isClientConnected = b;
	}

	public boolean isConnected() {
		return isClientConnected;
	}

	public GhostManager getGhostManager() {
		return ghostManager;
	}

	public ObjShape getGhostShape() {
		return ghostS;
	}

	public TextureImage getGhostTexture() {
		return ghostTx;
	}

	public ProtocolClient getProtocolClient() {
		return protocolClient;
	}

	public ScriptManager getScriptManager() {
		return scriptManager;
	}

	public void addKeyToActiveKeys(String key) {
		activeKeys.add(key);
	}

	public void removeKeyFromActiveKeys(String key) {
		activeKeys.remove(key);
	}

	public boolean isKeyPressed(String key) {
		return activeKeys.contains(key);
	}

	public void addOrientationToActiveOrientation(String orientation) {
		activeOrientation.add(orientation);
	}

	public void removeKeyFromActiveOrientation(String orientation) {
		activeOrientation.remove(orientation);
	}

	public boolean isCurrentOrientation(String orientation) {
		return activeOrientation.contains(orientation);

	}
}
