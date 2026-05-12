package tage.nodeControllers;

import tage.Engine;
import tage.GameObject;
import tage.NodeController;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;

/**
 * BobbingController causes target GameObjects to bob up and down using a sine wave on local Y.
 * This controller is used as the custom NodeController for A2.
 *
 * <p>The controller caches each target's initial local translation (its base position) the first time it is
 * applied, then continuously offsets the local Y component around that base. Multiple targets are supported by
 * tracking base position, time, and a phase offset per GameObject.</p>
 *
 * @author Givin Yang
 * @author Nicole Joshua Espinoza
 */
public class BobbingController extends NodeController
{
	/*
	A2 requirement map
	Custom node controller in tage.nodeControllers: BobbingController extends NodeController
	Controller behavior: sine-wave bob on local Y translation
	Multiple targets support: base position, time, and phase are tracked per GameObject
	*/

    // Base positions are stored so bobbing happens around the original placement.
    private final HashMap<GameObject, Vector3f> basePos = new HashMap<>();

    // Each target has its own timer so animation speed stays the same with more targets.
    private final HashMap<GameObject, Float> localTimeSec = new HashMap<>();

    // Phase offsets keep multiple targets from bobbing in sync.
    private final HashMap<GameObject, Float> phaseRad = new HashMap<>();

    private float amplitude = 0.6f;
    private float periodSec = 1.4f;

    // Reused matrix avoids allocations every frame.
    private final Matrix4f tmp = new Matrix4f();

    /**
     * Creates a bobbing controller with the specified amplitude and period.
     *
     * <p>Targets move along their local Y axis using a sine wave with the given period. The first time a target
     * is processed, its current local translation is stored as the base position the bobbing will oscillate
     * around.</p>
     */
    public BobbingController(Engine e, float amplitudeUnits, float periodSeconds)
    {
        super();
        amplitude = amplitudeUnits;
        periodSec = periodSeconds;
    }

    /**
     * Applies the bobbing animation to the specified GameObject.
     *
     * <p>This method is called by the engine/controller system. It uses {@link #getElapsedTime()} to advance a
     * per-object timer (in seconds), then computes a sine-wave Y offset using the configured period and amplitude.
     * The resulting translation is written back to the target via {@link GameObject#setLocalTranslation(Matrix4f)}.</p>
     */
    @Override
    public void apply(GameObject go)
    {
        // Elapsed time comes from NodeController timing and is converted to seconds for the sine wave.
        float elapsedMs = super.getElapsedTime();
        float elapsedSec = (elapsedMs / 1000.0f);

        // Per-object setup caches starting translation and assigns a unique phase offset.
        if (!basePos.containsKey(go))
        {
            Matrix4f t = go.getLocalTranslation();
            basePos.put(go, new Vector3f(t.m30(), t.m31(), t.m32()));

            float phaseStep = (float)(2.0 * Math.PI / 3.0);
            phaseRad.put(go, phaseStep * phaseRad.size());

            localTimeSec.put(go, 0.0f);
        }

        float tSec = localTimeSec.get(go) + elapsedSec;
        localTimeSec.put(go, tSec);

        Vector3f base = basePos.get(go);

        // y offset is computed with a sine wave using period to control speed and amplitude to control height.
        float omega = (float)(2.0 * Math.PI / periodSec);
        float yOff = amplitude * (float)Math.sin((omega * tSec) + phaseRad.get(go));

        tmp.identity().translation(base.x, base.y + yOff, base.z);
        go.setLocalTranslation(tmp);
    }

    /**
     * Resets the stored base translation for a target so bobbing continues around its new position.
     * This is useful after manual transforms such as placing a photo on the wall.
     */
    public void rebase(GameObject go)
    {
        Matrix4f t = go.getLocalTranslation();
        basePos.put(go, new Vector3f(t.m30(), t.m31(), t.m32()));
        localTimeSec.put(go, 0.0f);

        if (!phaseRad.containsKey(go)) phaseRad.put(go, 0.0f);
    }
}
