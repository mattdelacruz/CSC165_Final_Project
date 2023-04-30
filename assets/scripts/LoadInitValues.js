var JavaPackages = new JavaImporter(
    Packages.tage.Light,
    Packages.org.joml.Vector3f
);

with (JavaPackages) {
    var light = new Light();
    Light.setGlobalAmbient(0.5, 0.5, 0.5);
	light.setLocation(new Vector3f(5.0, 0.0, 2.0));

    var WINDOW_WIDTH = 500;
	var WINDOW_HEIGHT = 500;
	var ENEMY_AMOUNT = 4;
	var PLAY_AREA_SIZE = 300;
	var INITIAL_CAMERA_POS = new Vector3f(0, 0, 5);
	var SKYBOX_NAME = "fluffyClouds";
	var PLAYER_TEXTURE = "player-texture.png";
	var GHOST_TEXTURE = "neptune.jpg";
	var ENEMY_TEXTURE = "knight-texture.png";
	var TERRAIN_MAP = "terrain-map.png";
	var TERRAIN_TEXTURE = "moon-craters.jpg";
	var KATANA_TEXTURE = "katana-texture.png";
	var PLAYER_RKM = "player-animations/player.rkm";
	var PLAYER_RKS = "player-animations/player.rks";
	var PLAYER_RUN_RKA = "player-animations/player-run.rka";
	var PLAYER_IDLE_RKA = "player-animations/player-idle.rka";
	var PLAYER_ATTACK_1_RKA = "player-animations/player-attack-1.rka";
	var PLAYER_GUARD_RKA = "player-animations/player-guard.rka";

	// Katana animation file paths
	var KATANA_RKM = "player-animations/weapon-animations/katana.rkm";
	var KATANA_RKS = "player-animations/weapon-animations/katana.rks";
	var KATANA_RUN_RKA = "player-animations/weapon-animations/katana-run.rka";
	var KATANA_IDLE_RKA = "player-animations/weapon-animations/katana-idle.rka";
	var KATANA_ATTACK_1_RKA = "player-animations/weapon-animations/katana-attack-1.rka";
	var KATANA_GUARD_RKA = "player-animations/weapon-animations/katana-guard.rka";
	// Enemy animation file paths
	var ENEMY_RKM = "enemy-animations/knight-enemy.rkm";
	var ENEMY_RKS = "enemy-animations/knight-enemy.rks";
	var ENEMY_RUN_RKA = "enemy-animations/knight-enemy-run.rka";
	var ENEMY_IDLE_RKA = "enemy-animations/knight-enemy-idle.rka";
	var PLAYER_HEIGHT_SPEED = 0.5;
}



var xPlayerPos = 70;
var yPlayerPos = 0;
var zPlayerPos = 124;
