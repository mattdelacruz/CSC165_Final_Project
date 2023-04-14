package tage;

import org.joml.Vector3f;

/**
 * A CameraOrbit3D is a special type of Camera that orbits around a specified
 * target.
 * A CameraOrbit3D instance calculates the position of the Camera by using
 * spherical coordinates to determine the location of the Camera about a
 * spherical system.
 * <p>
 * Methods include adjustment of the altitude of the Camera, adjusting the
 * distance of the Camera, and adjusting the azimuth of the Camera all about the
 * specified target
 * </p>
 * 
 * @author Matthew Dela Cruz
 */

public class CameraOrbit3D extends Camera {
    private static final float MAX_CAMERA_DIST = 5.0f;
    private static final float LOCKED_DISTANCE = 20.0f;
    private static final float MAX_ZOOM_DIST = 20.0f;
    private static final float INITIAL_ALTITUDE_ANGLE = 85;
    private static final float PITCH_SPEED = 2.0f;
    private static final float PHI_SPEED = 0.5f;
    private static final float THETA_SPEED = 0.5f;

    private GameObject origin;
    private Vector3f lookAtTarget;
    private float phi, theta, deltaPitch, pitchAngle, x_dist, y_dist, z_dist, current_dist, xy_dist, newX, newY, newZ;

    public CameraOrbit3D(GameObject target) {
        origin = target;
        pitchAngle = (float) Math.toRadians(INITIAL_ALTITUDE_ANGLE);
        current_dist = (float) getLocation().distance(origin.getWorldLocation());
        lookAtTarget = origin.getLocalLocation();
    }

    public void updateCameraLocation() {
        x_dist = getLocation().z - origin.getLocalLocation().z;
        y_dist = getLocation().x - origin.getLocalLocation().x;
        z_dist = getLocation().y - origin.getLocalLocation().y;
        xy_dist = (float) (Math.sqrt(Math.pow(x_dist, 2) + Math.pow(y_dist, 2)));

        findNewCameraLocation();
    }

    public void updateCameraAngles(float frameTime) {
        x_dist = getLocation().z - origin.getLocalLocation().z;
        y_dist = getLocation().x - origin.getLocalLocation().x;
        z_dist = getLocation().y - origin.getLocalLocation().y;
        xy_dist = (float) (Math.sqrt(Math.pow(x_dist, 2) + Math.pow(y_dist, 2)));

        adjustTheta(frameTime);
        adjustPhi(frameTime);
        adjustPitchAngle(frameTime);
        findNewCameraLocation();
    }

    private void adjustTheta(float frameTime) {
        // azimuth

        float deltaTheta = frameTime * THETA_SPEED;
        theta += deltaTheta;

        if (z_dist != 0) {
            theta = (float) Math.acos((z_dist / current_dist));
        } else {
            theta = (float) (Math.PI / 2);
        }

        if (z_dist == 0 && current_dist == 0) {
            theta = 0;
        } else if (z_dist == 0) {
            theta = (float) Math.PI / 2;
        } else if (current_dist == 0) {
            theta = (float) Math.PI;
        } else {
            theta = (float) Math.acos(z_dist / current_dist);
        }
    }

    private void adjustPhi(float frameTime) {
        // altitude
        phi = (float) Math.acos(x_dist / xy_dist);

        float deltaPhi = frameTime * PHI_SPEED;
        phi += deltaPhi;

        if (x_dist > 0) {
            phi = (float) Math.atan(y_dist / x_dist);
        } else if (x_dist < 0 && y_dist >= 0) {
            phi = (float) (Math.atan(y_dist / x_dist) + Math.PI);
        } else if (x_dist < 0 && y_dist < 0) {
            phi = (float) (Math.atan(y_dist / x_dist) - Math.PI);
        } else if (x_dist == 0 && y_dist > 0) {
            phi += (float) (Math.PI / 2);
        } else if (x_dist == 0 && y_dist < 0) {
            phi -= (float) (Math.PI / 2);
        }
    }

    private void adjustPitchAngle(float frameTime) {
        pitchAngle += deltaPitch;

        if (pitchAngle >= Math.PI / 2) {
            pitchAngle = (float) Math.PI / 2;
        } else if (pitchAngle >= Math.PI) {
            pitchAngle = (float) Math.PI;
        } else if (pitchAngle < 0) {
            pitchAngle = 0;
        }
    }

    private void findNewCameraLocation() {
        lookAt(lookAtTarget);

        newZ = (float) ((Math.sin(theta) * Math.cos(phi)) *
                MAX_CAMERA_DIST);
        newX = (float) ((Math.sin(theta) * Math.sin(phi)) *
                MAX_CAMERA_DIST);
        newY = (float) (Math.cos(pitchAngle) *
                LOCKED_DISTANCE);
        setLocation(new Vector3f(newX, newY, newZ).add(origin.getLocalLocation()));
        current_dist = Math.min(Math.max(LOCKED_DISTANCE, MAX_CAMERA_DIST), MAX_ZOOM_DIST);
    }

    public void setLookAtTarget(Vector3f t) {
        lookAtTarget = t;
    }
}