package a3.player.movement;

public class PlayerGuardMovement implements PlayerMovement {

    @Override
    public float getSpeed() {
        return 0.5f;
    }

    @Override
    public String getAnimation() {
        return "GUARD_WALK";
    }

}
