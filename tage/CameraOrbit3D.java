package tage;

import org.joml.Vector3f;

/**
 * CameraOrbit3D implements a 3rd-person orbit camera around a target GameObject (avatar).
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Orbit (azimuth) around the avatar without changing avatar heading</li>
 *   <li>Adjust elevation angle</li>
 *   <li>Zoom in/out (radius)</li>
 *   <li>When the avatar moves/turns, the camera preserves its relative position/orientation to the avatar</li>
 * </ul>
 *
 * <p>Typical usage (in your game):</p>
 * <pre>
 * Camera cam = engine.getRenderSystem().getViewport("MAIN").getCamera();
 * orbitCam = new CameraOrbit3D(cam, avatar);
 * orbitCam.setRadius(4.0f);
 * orbitCam.setElevationDeg(15.0f);
 * orbitCam.setAzimuthDeg(180.0f); // start behind avatar
 * orbitCam.update();
 * </pre>
 */
public class CameraOrbit3D
{
    /*
    A2 requirement map
    Orbit without changing avatar heading: orbit changes azimuth only, target rotation is never touched
    Elevation adjust: elevate with clamps
    Zoom in and out: zoom with clamps
    Camera stays relative to avatar during movement and turning: update builds offset using target world basis vectors
    */

    private final Camera cam;
    private final GameObject target;

    // Orbit state is stored as angles and a radius so inputs can adjust camera movement smoothly.
    private float azimuthDeg = 180.0f;
    private float elevationDeg = 15.0f;
    private float radius = 4.0f;

    // Clamps keep camera movement reasonable and prevent flipping into unstable angles.
    private float minRadius = 1.5f;
    private float maxRadius = 25.0f;
    private float minElevationDeg = -10.0f;
    private float maxElevationDeg = 80.0f;

    // Look target is offset upward so the camera aims at the avatar body instead of the feet.
    private final Vector3f targetOffset = new Vector3f(0f, 1.0f, 0f);

    /**
     * Creates an orbit camera controller for the specified camera and target.
     */
    public CameraOrbit3D(Camera camera, GameObject targetAvatar)
    {
        this.cam = camera;
        this.target = targetAvatar;
    }

    /**
     * Adjusts the orbit azimuth around the target.
     * This does not modify the target rotation.
     */
    public void orbit(float deltaAzimuthDeg)
    {
        azimuthDeg += deltaAzimuthDeg;

        // Wrap keeps angles from growing forever while still allowing continuous orbiting.
        if (azimuthDeg > 360f) azimuthDeg -= 360f;
        if (azimuthDeg < 0f)   azimuthDeg += 360f;
    }

    /**
     * Adjusts the elevation angle within the configured clamp range.
     */
    public void elevate(float deltaElevationDeg)
    {
        elevationDeg += deltaElevationDeg;
        if (elevationDeg < minElevationDeg) elevationDeg = minElevationDeg;
        if (elevationDeg > maxElevationDeg) elevationDeg = maxElevationDeg;
    }

    /**
     * Adjusts the camera radius within the configured clamp range.
     */
    public void zoom(float deltaRadius)
    {
        radius += deltaRadius;
        if (radius < minRadius) radius = minRadius;
        if (radius > maxRadius) radius = maxRadius;
    }

    /**
     * Sets the orbit azimuth angle.
     */
    public void setAzimuthDeg(float deg) { azimuthDeg = deg; }

    /**
     * Sets the orbit elevation angle.
     */
    public void setElevationDeg(float deg) { elevationDeg = deg; }

    /**
     * Sets the orbit radius.
     */
    public void setRadius(float r) { radius = r; }

    /**
     * Gets the current orbit azimuth angle.
     */
    public float getAzimuthDeg() { return azimuthDeg; }

    /**
     * Gets the current orbit elevation angle.
     */
    public float getElevationDeg() { return elevationDeg; }

    /**
     * Gets the current orbit radius.
     */
    public float getRadius() { return radius; }

    /**
     * Sets the minimum and maximum allowed radius values and clamps the current radius into that range.
     */
    public void setRadiusClamp(float minR, float maxR)
    {
        minRadius = minR;
        maxRadius = maxR;

        if (radius < minRadius) radius = minRadius;
        if (radius > maxRadius) radius = maxRadius;
    }

    /**
     * Sets the minimum and maximum allowed elevation values in degrees and clamps the current elevation into that range.
     */
    public void setElevationClamp(float minDeg, float maxDeg)
    {
        minElevationDeg = minDeg;
        maxElevationDeg = maxDeg;

        if (elevationDeg < minElevationDeg) elevationDeg = minElevationDeg;
        if (elevationDeg > maxElevationDeg) elevationDeg = maxElevationDeg;
    }

    /**
     * Recomputes camera location and orientation. Call once per frame after avatar movement and rotation.
     */
    public void update()
    {
        // Target world position uses an offset so camera aim stays centered on the avatar.
        Vector3f tLoc = new Vector3f(target.getWorldLocation()).add(targetOffset);

        // Basis vectors come from the avatar world rotation so turning the avatar carries the camera around it.
        Vector3f fwd = new Vector3f(target.getWorldForwardVector()).normalize();
        Vector3f rt  = new Vector3f(target.getWorldRightVector()).normalize();
        Vector3f up  = new Vector3f(0f, 1f, 0f);

        // Angles are converted to radians for trig, then used as spherical coordinates around the target.
        float az = (float) Math.toRadians(azimuthDeg);
        float el = (float) Math.toRadians(elevationDeg);

        float x = (float)(radius * Math.sin(az) * Math.cos(el));
        float y = (float)(radius * Math.sin(el));
        float z = (float)(radius * Math.cos(az) * Math.cos(el));

        // Offset is rebuilt in world space so camera stays in the same relative orbit around the avatar.
        Vector3f offsetWorld = new Vector3f(rt).mul(x)
                .add(new Vector3f(up).mul(y))
                .add(new Vector3f(fwd).mul(z));

        Vector3f cLoc = new Vector3f(tLoc).add(offsetWorld);
        cam.setLocation(cLoc);

        // UVN vectors are built from a look at direction so the camera always points at the avatar.
        Vector3f n = new Vector3f(tLoc).sub(cLoc).normalize();
        Vector3f u;

        // When looking nearly straight up or down, cross products can get unstable, so target right is used.
        if (Math.abs(n.dot(up)) > 0.98f)
            u = new Vector3f(rt);
        else
            u = new Vector3f(n).cross(up).normalize();

        Vector3f v = new Vector3f(u).cross(n).normalize();

        cam.setU(u);
        cam.setV(v);
        cam.setN(n);
    }
}