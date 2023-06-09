package a3.player;

import javax.swing.Action;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import a3.MyGame;
import a3.managers.ScriptManager;
import a3.managers.SoundManager;
import a3.player.movement.PlayerGuardMovement;
import a3.player.movement.PlayerMovement;
import a3.player.movement.PlayerRunMovement;
import a3.player.stances.PlayerAttackStance;
import a3.player.stances.PlayerDeadStance;
import a3.player.stances.PlayerGuardStance;
import a3.player.stances.PlayerNormalStance;
import a3.player.stances.PlayerStanceState;
import tage.ActiveEntityObject;
import tage.AnimatedGameObject;
import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.audio.SoundType;
import tage.shapes.AnimatedShape;

public class Player extends ActiveEntityObject {
    private PlayerWeapon weapon;
    public int currFrame = 0;
    private boolean isLocked = false;
    private boolean canMove = true;
    private boolean isContact = false;
    private PlayerStanceState stanceState;
    private PlayerMovement movementState;
    private PlayerAttackStance attackStance = new PlayerAttackStance();
    private PlayerGuardStance guardStance = new PlayerGuardStance();
    private PlayerNormalStance normalStance = new PlayerNormalStance();
    private PlayerRunMovement runMovement = new PlayerRunMovement();
    private PlayerGuardMovement guardMovement = new PlayerGuardMovement();
    private float currentRotation = 0;

    private Vector3f validLocation;

    public Player(GameObject p, ObjShape s, TextureImage t) {
        super(p, s, t, 100);
        initializeSounds();
        setLocalScale(new Matrix4f().scaling(.2f));
        setLocalLocation((Vector3f) getScriptManager().getValue("PLAYER_START_POS"));
        setStanceState(normalStance);
        setMovementState(runMovement);
        initializeSounds();
    }

    private void initializeSounds() {
        int backgroundMusicRange = (int) getScriptManager().getValue("PLAY_AREA_SIZE");

        this.getSoundManager().addSound(
                "STEP", (String) getScriptManager().getValue("STEP"), 5, false, (float) backgroundMusicRange, 0, 0,
                getLocalLocation(), SoundType.SOUND_EFFECT);
        this.getSoundManager().addSound(
                "KATANA_WHIFF", (String) getScriptManager().getValue("KATANA_WHIFF_SOUND"), 10, false,
                (float) backgroundMusicRange, 0, 0, getLocalLocation(), SoundType.SOUND_EFFECT);
        this.getSoundManager().addSound(
                "KATANA_HIT", (String) getScriptManager().getValue("KATANA_HIT_SOUND"), 10, false,
                (float) backgroundMusicRange, 0, 0, getLocalLocation(), SoundType.SOUND_EFFECT);

    }

    public void moveNorth(Vector3f vec, float frameTime) {
        if (stanceState == normalStance) {
            canMove = true;
        }

        if (stanceState == guardStance) {
            movementState = guardMovement;
        }
        if (canMove) {
            super.move(vec, (frameTime * getStanceState().getMoveValue() * getMovementState().getSpeed()));
            if (!this.getSoundManager().isPlaying("STEP")) {
                this.getSoundManager().playSound("STEP");
            }
            handleAnimationSwitch(getMovementState().getAnimation(), 1f);
            if (MyGame.getGameInstance().getProtocolClient() != null) {
                MyGame.getGameInstance().getProtocolClient().sendMoveMessage(getWorldLocation());
                MyGame.getGameInstance().getProtocolClient().sendAnimationMessage(getMovementState().getAnimation());
            }
        }
    }

    public void moveSouth(Vector3f vec, float frameTime) {
        if (stanceState == normalStance) {
            canMove = true;
        }

        if (stanceState == guardStance) {
            movementState = guardMovement;
        }
        if (canMove) {
            super.move(vec, (frameTime * getStanceState().getMoveValue() * (getMovementState().getSpeed() * .5f)));
            if (!this.getSoundManager().isPlaying("STEP")) {
                this.getSoundManager().playSound("STEP");
            }
            handleAnimationSwitch("BACKWARDS_RUN", 1f);
            if (MyGame.getGameInstance().getProtocolClient() != null) {
                MyGame.getGameInstance().getProtocolClient().sendMoveMessage(getWorldLocation());
                MyGame.getGameInstance().getProtocolClient().sendAnimationMessage("BACKWARDS_RUN");
            }
        }
    }

    public void moveEast(Vector3f vec, float frameTime) {
        if (stanceState == normalStance) {
            canMove = true;
        }

        if (stanceState == guardStance) {
            movementState = guardMovement;
        }
        if (canMove) {
            super.move(
                    vec, (frameTime * (getStanceState().getMoveValue() / 1.5f) * getMovementState().getSpeed()));
            if (!this.getSoundManager().isPlaying("STEP")) {
                this.getSoundManager().playSound("STEP");
            }
            if (getStanceState().isGuarding()) {
                handleAnimationSwitch("GUARD_STRAFE", 1f);
                if (MyGame.getGameInstance().getProtocolClient() != null) {
                    MyGame.getGameInstance().getProtocolClient().sendMoveMessage(getWorldLocation());
                    MyGame.getGameInstance().getProtocolClient().sendAnimationMessage("GUARD_STRAFE");
                }
            } else {
                handleAnimationSwitch("STRAFE", 1f);
                if (MyGame.getGameInstance().getProtocolClient() != null) {
                    MyGame.getGameInstance().getProtocolClient().sendMoveMessage(getWorldLocation());
                    MyGame.getGameInstance().getProtocolClient().sendAnimationMessage("STRAFE");
                }
            }
        }
    }

    /* to be called by keyboard/gamepad events only */
    public void attack() {
        setLastValidLocation(getLocalLocation());

        if (isContact) {
            // play bloody sound
            if (this.getSoundManager().isPlaying("KATANA_WHIFF")) {
                this.getSoundManager().stopSound("KATANA_WHIFF");
            }
            if (this.getSoundManager().isPlaying("KATANA_HIT")) {
                this.getSoundManager().stopSound("KATANA_HIT");
            }
            this.getSoundManager().playSound("KATANA_HIT");
        } else {
            if (this.getSoundManager().isPlaying("KATANA_WHIFF")) {
                this.getSoundManager().stopSound("KATANA_WHIFF");
            }
            if (this.getSoundManager().isPlaying("KATANA_HIT")) {
                this.getSoundManager().stopSound("KATANA_HIT");
            }
            this.getSoundManager().playSound("KATANA_WHIFF");
        }

        if (isMoving()) {
            canMove = false;
        }

        if (!getStanceState().isAttacking()) {
            setStanceState(attackStance);
        }
        handleAnimationSwitch(getStanceState().getAnimation(), 1f);
        if (MyGame.getGameInstance().getProtocolClient() != null) {
            MyGame.getGameInstance().getProtocolClient().sendAnimationMessage(getStanceState().getAnimation());
        }

        if (!getAnimationShape().isAnimPlaying()) {
            setStanceState(normalStance);
        }
    }

    /* to be called by keyboard/gamepad events only */
    public void guard() {
        setStanceState(guardStance);
        handleAnimationSwitch(getStanceState().getAnimation(), 1f);
        if (MyGame.getGameInstance().getProtocolClient() != null) {
            MyGame.getGameInstance().getProtocolClient().sendAnimationMessage(getStanceState().getAnimation());
        }
    }

    /* to be called by keyboard/gamepad events only */
    public void unGuard() {
        setStanceState(normalStance);
        setMovementState(runMovement);
    }

    /* to be called by keyboard/gamepad events only */
    public void run() {
        setMovementState(runMovement);
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        weapon.updateAnimation();
    }

    @Override
    public void handleAnimationSwitch(String animation, float speed) {
        super.handleAnimationSwitch(animation, speed);
        if (weapon.getAnimationShape().getAnimation(animation) != null) {
            weapon.handleAnimationSwitch(animation, speed);
        }
    }

    @Override
    public void idle() {
        super.idle();
        setStanceState(normalStance);
        weapon.idle();
        if (MyGame.getGameInstance().getProtocolClient() != null) {
            MyGame.getGameInstance().getProtocolClient().sendAnimationMessage("IDLE");
        }
    }

    public void setLock(boolean lock) {
        isLocked = lock;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setStanceState(PlayerStanceState stanceState) {
        this.stanceState = stanceState;
    }

    public void setMovementState(PlayerMovement moveState) {
        this.movementState = moveState;
    }

    public PlayerMovement getMovementState() {
        return this.movementState;
    }

    public PlayerStanceState getStanceState() {
        return this.stanceState;
    }

    public void addWeapon(PlayerWeapon weapon) {
        this.weapon = weapon;
    }

    public PlayerWeapon getWeapon() {
        return this.weapon;
    }

    public void setLastValidLocation(Vector3f newLocation) {
        validLocation = newLocation;
    }

    public Vector3f getLastValidLocation() {
        return validLocation;
    }

    public void checkIfDead() {
        if (getHealth() <= 0) {
            setStanceState(new PlayerDeadStance());
        }
    }

    public void makeContact(boolean contact) {
        isContact = contact;
    }

    public float getCurrentRotation() {
        return currentRotation;
    }

    public void addCurrentRotation(float r) {
        currentRotation += r;
    }

}
