package a3;

import org.joml.Vector3f;
import tage.*;

public class MyGameLighting {
    private final MyGame game;

    public MyGameLighting(MyGame game) {
        this.game = game;
    }

    public void initializeLights() {
        MyGameAssets a = game.assets;
        Light.setGlobalAmbient(0.35f, 0.35f, 0.35f);

        for (int i = 0; i < 3; i++) {
            a.lightPyramid[i] = new Light();
            Vector3f p = a.pyramids[i].getWorldLocation();
            a.lightPyramid[i].setLocation(new Vector3f(p.x, p.y + 8f, p.z));
            MyGame.getEngine().getSceneGraph().addLight(a.lightPyramid[i]);
        }

        Light lightHome = new Light();
        lightHome.setLocation(new Vector3f(0f, 8f, 0f));
        MyGame.getEngine().getSceneGraph().addLight(lightHome);

        a.flashlightSpotlight = new Light();
        a.flashlightSpotlight.setType(Light.LightType.SPOTLIGHT);
        a.flashlightSpotlight.setAmbient(0.0f, 0.0f, 0.0f);
        a.flashlightSpotlight.setDiffuse(0.95f, 0.92f, 0.82f);
        a.flashlightSpotlight.setSpecular(1.0f, 0.97f, 0.88f);
        a.flashlightSpotlight.setCutoffAngle(18.0f);
        a.flashlightSpotlight.setOffAxisExponent(10.0f);
        a.flashlightSpotlight.setLinearAttenuation(0.08f);
        a.flashlightSpotlight.setQuadraticAttenuation(0.03f);
        a.flashlightSpotlight.setLocation(new Vector3f(0f, -100f, 0f));
        a.flashlightSpotlight.setDirection(new Vector3f(0f, 0f, -1f));
        a.flashlightSpotlight.disable();
        MyGame.getEngine().getSceneGraph().addLight(a.flashlightSpotlight);
    }
}
