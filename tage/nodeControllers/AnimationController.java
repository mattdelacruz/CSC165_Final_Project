package tage.nodeControllers;

import javax.swing.event.SwingPropertyChangeSupport;

import org.joml.Vector3f;

import a3.MyGame;
import a3.npcs.Enemy;
import tage.*;

public class AnimationController extends NodeController {
    public AnimationController() {
        super();
    }

    @Override
    public void apply(GameObject go) {
        if (go instanceof AnimatedGameObject) {
            ((AnimatedGameObject) go).updateAnimation();
        }
    }
}
