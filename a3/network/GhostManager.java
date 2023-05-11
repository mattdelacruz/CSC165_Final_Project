package a3.network;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import a3.MyGame;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.physics.PhysicsObject;
import tage.shapes.AnimatedShape;

public class GhostManager {
    private MyGame game;
    private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

    public GhostManager(VariableFrameRateGame vfrg) {
        this.game = (MyGame) vfrg;
    }

    public void removeGhostAvatar(UUID ghostID) {
        GhostAvatar ghostAvatar = findAvatar(ghostID);
        if (ghostAvatar != null) {
            ghostAvatar.getRenderStates().disableRendering();
            MyGame.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
            ghostAvatars.remove(ghostAvatar);
        } else {
            System.out.println("tried to remove, but unable to find ghost in list");
        }
    }

    public void createGhostAvatar(UUID id, Vector3f pos) throws IOException {
        System.out.println("adding ghost with ID --> " + id);
        float mass = 1f;
        double[] tempTransform;
        float[] vals = new float[16];
        Matrix4f translation;
        PhysicsObject ghostBody;

        AnimatedShape s = game.getGhostShape();
        TextureImage t = game.getGhostTexture();
        GhostAvatar newAvatar = new GhostAvatar(id, s, t, pos);
        AnimatedShape ks = game.getGhostKatanaShape();
        TextureImage kt = game.getGhostKatanaTexture();
        GhostWeapon newKatana = new GhostWeapon(id, ks, kt, newAvatar);
        newAvatar.addWeapon(newKatana);

        translation = newAvatar.getLocalTranslation();
        tempTransform = game.toDoubleArray(translation.get(vals));

        ghostBody = game.getPhysicsManager().addCapsuleObject(mass, tempTransform, 1f, 5f);
        newAvatar.setPhysicsObject(ghostBody);

        game.getAnimationController().addTarget(newAvatar);
        game.addToPlayerQuadTree(newAvatar);
        ghostAvatars.add(newAvatar);
    }

    private GhostAvatar findAvatar(UUID ghostID) {
        GhostAvatar ghostAvatar;
        Iterator<GhostAvatar> it = ghostAvatars.iterator();

        while (it.hasNext()) {
            ghostAvatar = it.next();
            if (ghostAvatar.getID().equals(ghostID)) {
                return ghostAvatar;
            }
        }
        return null;
    }

    public void updateGhostAvatar(UUID ghostID, Vector3f pos) {
        GhostAvatar ghostAvatar = findAvatar(ghostID);
        if (ghostAvatar != null) {
            ghostAvatar.setPosition(pos);
        } else {
            System.out.println("Can't find ghost!!");
            
        }
    }

    public void updateGhostAvatarAnimation(UUID ghostID, String animation) {
        GhostAvatar ghostAvatar = findAvatar(ghostID);
        if (ghostAvatar != null) {
            if (animation.equals("IDLE")) {
                ghostAvatar.idle();
            } else {
                ghostAvatar.handleAnimationSwitch(animation, 1f);
            }
        } else {
            System.out.println("Can't find ghost!!");
        }
    }

    public void updateGhostAvatarYaw(UUID ghostID, float rotation) {
        GhostAvatar ghostAvatar = findAvatar(ghostID);
        if (ghostAvatar != null) {
            ghostAvatar.yaw(game.getFrameTime(), rotation);
        } else {
            System.out.println("Can't find ghost!!");
        }
    }

}
