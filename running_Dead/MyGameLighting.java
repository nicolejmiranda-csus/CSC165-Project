package running_Dead;

import org.joml.Vector3f;
import tage.*;

public class MyGameLighting {
    private final MyGame game;
    private static final float SUN_RADIUS = 145.0f;
    private static final float SUN_MIN_Y = 8.0f;
    private static final float SUN_MAX_Y = 125.0f;
    private static final float PLAYER_NIGHT_LIGHT_LIFT = 2.65f;

    public MyGameLighting(MyGame game) {
        this.game = game;
    }

    public void initializeLights() {
        MyGameAssets a = game.assets;
        Light.setGlobalAmbient(0.42f, 0.42f, 0.40f);

        a.sunLight = new Light();
        a.sunLight.setAmbient(0.22f, 0.22f, 0.20f);
        a.sunLight.setDiffuse(1.00f, 0.94f, 0.82f);
        a.sunLight.setSpecular(1.00f, 0.95f, 0.82f);
        a.sunLight.setLocation(new Vector3f(70f, 115f, 55f));
        MyGame.getEngine().getSceneGraph().addLight(a.sunLight);

        a.playerNightLight = new Light();
        a.playerNightLight.setAmbient(0.0f, 0.0f, 0.0f);
        a.playerNightLight.setDiffuse(0.0f, 0.0f, 0.0f);
        a.playerNightLight.setSpecular(0.0f, 0.0f, 0.0f);
        a.playerNightLight.setConstantAttenuation(1.0f);
        a.playerNightLight.setLinearAttenuation(0.25f);
        a.playerNightLight.setQuadraticAttenuation(0.11f);
        a.playerNightLight.setLocation(new Vector3f(0f, -100f, 0f));
        a.playerNightLight.disable();
        MyGame.getEngine().getSceneGraph().addLight(a.playerNightLight);

        a.flashlightSpotlight = new Light();
        a.flashlightSpotlight.setType(Light.LightType.SPOTLIGHT);
        a.flashlightSpotlight.setAmbient(0.01f, 0.01f, 0.008f);
        a.flashlightSpotlight.setDiffuse(2.80f, 2.55f, 1.85f);
        a.flashlightSpotlight.setSpecular(2.20f, 2.05f, 1.65f);
        a.flashlightSpotlight.setCutoffAngle(30.0f);
        a.flashlightSpotlight.setOffAxisExponent(4.0f);
        a.flashlightSpotlight.setConstantAttenuation(0.45f);
        a.flashlightSpotlight.setLinearAttenuation(0.018f);
        a.flashlightSpotlight.setQuadraticAttenuation(0.0035f);
        a.flashlightSpotlight.setLocation(new Vector3f(0f, -100f, 0f));
        a.flashlightSpotlight.setDirection(new Vector3f(0f, 0f, -1f));
        a.flashlightSpotlight.disable();
        MyGame.getEngine().getSceneGraph().addLight(a.flashlightSpotlight);
        updateDayNight(0.0f);
    }

    public void updateDayNight(float dt) {
        if (!game.state.dayNightEnabled || game.assets.sunLight == null) return;

        float cycle = Math.max(1.0f, game.state.dayNightCycleSeconds);
        if (game.state.dayNightServerSynced && game.state.dayNightRoundEndEpochMs > 0L) {
            float roundSeconds = Math.max(cycle, (float) game.state.survivalRoundDuration);
            float secondsRemaining = Math.max(0.0f, (game.state.dayNightRoundEndEpochMs - System.currentTimeMillis()) / 1000.0f);
            float elapsed = clamp(roundSeconds - secondsRemaining, 0.0f, roundSeconds);
            game.state.dayNightTime = (game.state.dayNightRoundStartOffset + elapsed) % cycle;
        } else {
            game.state.dayNightTime = (game.state.dayNightTime + Math.max(0.0f, dt)) % cycle;
        }
        boolean night = game.state.dayNightTime >= game.state.daySlotSeconds;
        float slotTime = night ? game.state.dayNightTime - game.state.daySlotSeconds : game.state.dayNightTime;
        float slotLength = night ? game.state.nightSlotSeconds : game.state.daySlotSeconds;
        float slotProgress = clamp(slotTime / Math.max(1.0f, slotLength), 0.0f, 1.0f);

        float angle = (night ? 1.0f + slotProgress : slotProgress) * (float) Math.PI;
        float sunHeight = (float) Math.sin(angle);
        float dayEdge = Math.min(smoothstep(0.0f, 0.08f, slotProgress), smoothstep(0.0f, 0.08f, 1.0f - slotProgress));
        float daylight = night ? 0.0f : Math.max(0.38f, dayEdge);
        float twilight = 1.0f - daylight;
        float x = (float) Math.cos(angle) * SUN_RADIUS;
        float y = SUN_MIN_Y + Math.max(sunHeight, 0.0f) * (SUN_MAX_Y - SUN_MIN_Y);
        float z = (float) Math.sin(angle * 0.72f + 0.7f) * SUN_RADIUS * 0.55f;
        float visionLight = game.state.localPlayerZombie ? Math.max(daylight, 0.70f) : daylight;

        game.assets.sunLight.setLocation(new Vector3f(x, y, z));
        game.assets.sunLight.setAmbient(lerp(0.025f, 0.24f, daylight), lerp(0.03f, 0.23f, daylight), lerp(0.045f, 0.20f, daylight));
        game.assets.sunLight.setDiffuse(lerp(0.04f, 1.00f, daylight), lerp(0.05f, 0.94f, daylight), lerp(0.08f, 0.82f, daylight));
        game.assets.sunLight.setSpecular(lerp(0.03f, 1.00f, daylight), lerp(0.04f, 0.95f, daylight), lerp(0.08f, 0.82f, daylight));
        if (game.state.localPlayerZombie) {
            Light.setGlobalAmbient(lerp(0.12f, 0.42f, visionLight), lerp(0.14f, 0.43f, visionLight), lerp(0.18f, 0.46f, visionLight));
        } else if (night) {
            Light.setGlobalAmbient(0.11f, 0.12f, 0.16f);
        } else {
            Light.setGlobalAmbient(lerp(0.025f, 0.42f, visionLight), lerp(0.028f, 0.42f, visionLight), lerp(0.045f, 0.40f, visionLight));
        }

        updateAtmosphereFog(daylight, twilight, night);
        updatePlayerNightLight(night);
        updateCycleSkybox(night);
    }

    private void updatePlayerNightLight(boolean night) {
        if (game.assets.playerNightLight == null || game.assets.avatar == null) return;
        if (!night || game.state.localPlayerZombie || game.state.health <= 0) {
            game.assets.playerNightLight.disable();
            game.assets.playerNightLight.setLocation(new Vector3f(0f, -100f, 0f));
            return;
        }

        Vector3f pos = new Vector3f(game.assets.avatar.getWorldLocation()).add(0f, PLAYER_NIGHT_LIGHT_LIFT, 0f);
        game.assets.playerNightLight.setLocation(pos);
        game.assets.playerNightLight.setAmbient(0.018f, 0.016f, 0.012f);
        game.assets.playerNightLight.setDiffuse(0.95f, 0.84f, 0.58f);
        game.assets.playerNightLight.setSpecular(0.08f, 0.07f, 0.05f);
        game.assets.playerNightLight.enable();
    }

    public void randomizeRoundTimeSlot(java.util.UUID roundZombieId, long roundToken) {
        long seed = roundToken == 0L ? System.currentTimeMillis() : roundToken;
        if (roundZombieId != null) {
            seed ^= roundZombieId.getMostSignificantBits();
            seed = Long.rotateLeft(seed, 23) ^ roundZombieId.getLeastSignificantBits();
        }
        seed ^= 0x1652026D4A7L;
        seed ^= (seed >>> 33);
        float cycle = Math.max(1.0f, game.state.dayNightCycleSeconds);
        game.state.dayNightRoundStartOffset = (float) (Math.floorMod(seed, 100000L) / 100000.0 * cycle);
        game.state.dayNightRoundEndEpochMs = roundToken;
        game.state.dayNightServerSynced = roundToken > 0L;
        game.state.dayNightTime = game.state.dayNightRoundStartOffset;
        game.state.nightSkyActive = !(game.state.dayNightTime >= game.state.daySlotSeconds);
        updateDayNight(0.0f);
    }

    public void clearRoundTimeSync() {
        game.state.dayNightServerSynced = false;
        game.state.dayNightRoundEndEpochMs = 0L;
    }

    private void updateAtmosphereFog(float daylight, float twilight, boolean night) {
        RenderSystem renderSystem = MyGame.getEngine().getRenderSystem();
        if (renderSystem == null) return;

        if (night) {
            renderSystem.setFogColor(0.035f, 0.052f, 0.085f, 1.0f);
            renderSystem.setFogRange(42.0f, 185.0f);
        } else {
            float haze = clamp(0.25f + twilight * 0.45f, 0.20f, 0.70f);
            renderSystem.setFogColor(lerp(0.58f, 0.75f, daylight), lerp(0.66f, 0.84f, daylight), lerp(0.76f, 0.94f, daylight), 1.0f);
            renderSystem.setFogRange(lerp(95.0f, 125.0f, daylight), lerp(220.0f, 340.0f, daylight) - haze * 28.0f);
        }
    }

    private void updateCycleSkybox(boolean night) {
        if (game.assets.sky04 == 0 || game.assets.sky15Night == 0) return;
        if (night == game.state.nightSkyActive) return;
        game.state.nightSkyActive = night;
        MyGame.getEngine().getSceneGraph().setActiveSkyBoxTexture(night ? game.assets.sky15Night : game.assets.sky04);
        MyGame.getEngine().getSceneGraph().setSkyBoxEnabled(true);
        game.hudSystem.showEvent(night ? "NIGHT FALLS" : "DAY BREAKS", 1.0);
    }

    private float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0.0f, 1.0f);
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
