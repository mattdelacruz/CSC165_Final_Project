package myGame.controls;

import myGame.MyGame;
import myGame.network.ProtocolClient;
import myGame.npcs.Enemy;
import myGame.player.Player;
import tage.GameObject;
import tage.TargetCamera;

public class PlayerControls implements PlayerControlFunctions {
	private Player player;
	private TargetCamera cam;
	private MyGame game;
	private ProtocolClient protocolClient;

	public PlayerControls() {
		game = MyGame.getGameInstance();
		player = game.getPlayer();
		cam = game.getTargetCamera();
		if (game.isConnected()) {
			System.out.println("game is connected...");
			protocolClient = game.getProtocolClient();
		} else {
			System.out.println("game is not connected");
		}
	}

	@Override
	public void turnLeft(float frameTime) {
		player.move(player.getLocalRightVector(), -frameTime);
		cam.updateCameraLocation();
		if (protocolClient != null) {
			System.out.println("sending move message");
			protocolClient.sendMoveMessage(player.getWorldLocation());
		}
	}

	@Override
	public void turnRight(float frameTime) {
		player.move(player.getLocalRightVector(), frameTime);
		cam.updateCameraLocation();
		if (protocolClient != null) {
			System.out.println("sending move message");
			protocolClient.sendMoveMessage(player.getWorldLocation());
		}
	}

	@Override
	public void moveForward(float frameTime) {
		player.move(player.getLocalForwardVector(), frameTime);
		cam.updateCameraLocation();
		if (protocolClient != null) {
			System.out.println("sending move message");
			protocolClient.sendMoveMessage(player.getWorldLocation());
		}
	}

	@Override
	public void moveBackward(float frameTime) {
		player.move(player.getLocalForwardVector(), -frameTime);
		cam.updateCameraLocation();
		if (protocolClient != null) {
			System.out.println("sending move message");
			protocolClient.sendMoveMessage(player.getWorldLocation());
		}
	}

	@Override
	public void turnCameraLeft(float frameTime) {
		cam.move(-frameTime, cam.getU());
		cam.updateCameraAngles(frameTime);
	}

	@Override
	public void turnCameraRight(float frameTime) {
		cam.move(frameTime, cam.getU());
		cam.updateCameraAngles(frameTime);
	}

	@Override
	public void rotateUp(float frameTime) {
		return;
	}

	@Override
	public void rotateDown(float frameTime) {
		return;
	}

	@Override
	public void target() {
		GameObject target = MyGame.getGameInstance().findTarget();
		if (target != null && target instanceof Enemy) {
			player.getCamera().setTarget((Enemy) target);
			player.setLock(true);
		}
		return;
	}

	@Override
	public boolean isControlPlayer() {
		return true;
	}
}