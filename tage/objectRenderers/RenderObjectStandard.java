package tage.objectRenderers;
import java.nio.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;
import tage.*;

/**
* Includes a single method render() for rendering a Game Object.
* Considers the various render states that have been set.
* Boolean flags are sent to the shaders at integers.
* <p>
* Follows closely the method described in Chapters 4, 5, 7, 9, 10, and 14.
* of Computer Graphics Programming in OpenGL with Java.
* <p>
* Used by the engine, should not be used directly by the game application.
* @author Scott Gordon
*/
public class RenderObjectStandard
{	private GLCanvas myCanvas;
	private Engine engine;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private Matrix4f viewInvMat = new Matrix4f();
	private Vector3f cameraPos = new Vector3f();
	private int mLoc, vLoc, pLoc, nLoc, tLoc, lLoc, eLoc, fLoc, sLoc, cLoc, hLoc, oLoc, tfLoc, alphaLoc, flipLoc;
	private int lightVPLoc, cameraPosLoc, fogColorLoc, fogStartLoc, fogEndLoc, useFogLoc;
	private int useBumpLoc, useNormalMapLoc, mappingModeLoc, bumpStyleLoc, useTextureDetailBlendLoc, hasDetailTextureLoc;
	private int receivesShadowLoc, hasShadowMapLoc, shadowLightIndexLoc, surfaceScaleLoc, shadowStrengthLoc, textureDetailNearLoc, textureDetailFarLoc;
	private int sampLoc, cubeLoc, heightTexLoc, shadowTexLoc, normalTexLoc, detailTexLoc;
	private int globalAmbLoc,mambLoc,mdiffLoc,mspecLoc,mshiLoc;
	private int hasSolidColor, hasTex, thisTexture, defaultTexture, tiling, tilingOption, tileFactor, heightMapped, isTransparentLoc;
	private int isEnvMapped, hasLighting, isTransparent, activeSkyBoxTexture, heightMapTexture, normalMapTexture, detailTexture;
	private float alpha;

	/** for engine use only. */
	public RenderObjectStandard(Engine e) { engine = e; }

	/** for engine use only. */
	public void render(GameObject go, int renderingProgram, Matrix4f pMat, Matrix4f vMat) {	
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(renderingProgram);

		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
		tLoc = gl.glGetUniformLocation(renderingProgram, "has_texture");
		eLoc = gl.glGetUniformLocation(renderingProgram, "envMapped");
		oLoc = gl.glGetUniformLocation(renderingProgram, "hasLighting");
		sLoc = gl.glGetUniformLocation(renderingProgram, "solidColor");
		cLoc = gl.glGetUniformLocation(renderingProgram, "color");
		hLoc = gl.glGetUniformLocation(renderingProgram, "heightMapped");
		lLoc = gl.glGetUniformLocation(renderingProgram, "num_lights");
		fLoc = gl.glGetUniformLocation(renderingProgram, "fields_per_light");
		tfLoc = gl.glGetUniformLocation(renderingProgram, "tileCount");
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
		lightVPLoc = gl.glGetUniformLocation(renderingProgram, "lightVP_matrix");
		cameraPosLoc = gl.glGetUniformLocation(renderingProgram, "cameraPos");
		fogColorLoc = gl.glGetUniformLocation(renderingProgram, "fogColor");
		fogStartLoc = gl.glGetUniformLocation(renderingProgram, "fogStart");
		fogEndLoc = gl.glGetUniformLocation(renderingProgram, "fogEnd");
		useFogLoc = gl.glGetUniformLocation(renderingProgram, "useFog");
		useBumpLoc = gl.glGetUniformLocation(renderingProgram, "useBumpMap");
		useNormalMapLoc = gl.glGetUniformLocation(renderingProgram, "useNormalMap");
		mappingModeLoc = gl.glGetUniformLocation(renderingProgram, "mappingMode");
		bumpStyleLoc = gl.glGetUniformLocation(renderingProgram, "bumpStyle");
		useTextureDetailBlendLoc = gl.glGetUniformLocation(renderingProgram, "useTextureDetailBlend");
		hasDetailTextureLoc = gl.glGetUniformLocation(renderingProgram, "hasDetailTexture");
		receivesShadowLoc = gl.glGetUniformLocation(renderingProgram, "receivesShadow");
		hasShadowMapLoc = gl.glGetUniformLocation(renderingProgram, "hasShadowMap");
		shadowLightIndexLoc = gl.glGetUniformLocation(renderingProgram, "shadowLightIndex");
		surfaceScaleLoc = gl.glGetUniformLocation(renderingProgram, "surfaceScale");
		shadowStrengthLoc = gl.glGetUniformLocation(renderingProgram, "shadowStrength");
		textureDetailNearLoc = gl.glGetUniformLocation(renderingProgram, "textureDetailNear");
		textureDetailFarLoc = gl.glGetUniformLocation(renderingProgram, "textureDetailFar");
		sampLoc = gl.glGetUniformLocation(renderingProgram, "samp");
		cubeLoc = gl.glGetUniformLocation(renderingProgram, "t");
		heightTexLoc = gl.glGetUniformLocation(renderingProgram, "height");
		shadowTexLoc = gl.glGetUniformLocation(renderingProgram, "shadowTex");
		normalTexLoc = gl.glGetUniformLocation(renderingProgram, "normalTex");
		detailTexLoc = gl.glGetUniformLocation(renderingProgram, "detailTex");

		isTransparentLoc = gl.glGetUniformLocation(renderingProgram, "isTransparent");
		alphaLoc = gl.glGetUniformLocation(renderingProgram, "alpha");
		flipLoc = gl.glGetUniformLocation(renderingProgram, "flipNormal");
		
		mMat.identity();
		mMat.mul(go.getWorldTranslation());
		mMat.mul(go.getWorldRotation());
		mMat.mul(go.getRenderStates().getModelOrientationCorrection());
		mMat.mul(go.getWorldScale());

		if ((go.getRenderStates()).hasSolidColor()) {	
			hasSolidColor = 1;
			hasTex = 0;
		}
		else {	
			hasSolidColor = 0;
			hasTex = 1;
		}

		if ((go.getRenderStates()).isEnvironmentMapped())
			isEnvMapped=1;
		else
			isEnvMapped=0;

		if (go.isTerrain())
			heightMapped = 1;
		else
			heightMapped = 0;
		
		if (go.getRenderStates().hasLighting())
			hasLighting = 1;
		else
			hasLighting = 0;

		// Check if the object is transparent, if so we will need to enable the blend function in OpenGL
		if (go.getRenderStates().isTransparent())
			isTransparent = 1;
		else
			isTransparent = 0;
	
		alpha = go.getRenderStates().getOpacity();
		
		gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, (engine.getLightManager()).getLightSSBO());

		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(lightVPLoc, 1, false, engine.getRenderSystem().getLightVPMatrix().get(vals));
		viewInvMat.set(vMat).invert();
		viewInvMat.getTranslation(cameraPos);
		gl.glUniform3f(cameraPosLoc, cameraPos.x, cameraPos.y, cameraPos.z);
		float[] fogColor = engine.getRenderSystem().getFogColor();
		gl.glUniform4f(fogColorLoc, fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
		gl.glUniform1f(fogStartLoc, engine.getRenderSystem().getFogStart());
		gl.glUniform1f(fogEndLoc, engine.getRenderSystem().getFogEnd());
		if (go.getTextureImage() != null) hasTex=1; else hasTex=0;
		gl.glUniform1i(tLoc, hasTex);
		gl.glUniform1i(eLoc, isEnvMapped);
		gl.glUniform1i(oLoc, hasLighting);
		gl.glUniform1i(sLoc, hasSolidColor);
		gl.glUniform1i(isTransparentLoc, isTransparent);
		gl.glUniform3fv(cLoc, 1, ((go.getRenderStates()).getColor()).get(vals));
		gl.glUniform1i(hLoc, heightMapped);
		tileFactor = (go.getRenderStates()).getTileFactor();
		gl.glUniform1i(tfLoc, tileFactor);
		gl.glUniform1i(lLoc, (engine.getLightManager()).getNumLights());
		gl.glUniform1i(fLoc, (engine.getLightManager()).getFieldsPerLight());
		gl.glUniform1i(useFogLoc, go.getRenderStates().usesFog() ? 1 : 0);
		gl.glUniform1i(useBumpLoc, go.getRenderStates().usesBumpMapping() ? 1 : 0);
		gl.glUniform1i(useNormalMapLoc, (go.getRenderStates().usesNormalMapping() && go.getNormalMap() != null) ? 1 : 0);
		gl.glUniform1i(mappingModeLoc, go.getRenderStates().getTextureMappingMode());
		gl.glUniform1i(bumpStyleLoc, go.getRenderStates().getBumpStyle());
		gl.glUniform1i(useTextureDetailBlendLoc, go.getRenderStates().usesTextureDetailBlend() ? 1 : 0);
		gl.glUniform1i(hasDetailTextureLoc, (go.getDetailTextureImage() != null) ? 1 : 0);
		gl.glUniform1i(receivesShadowLoc, go.getRenderStates().receivesShadow() ? 1 : 0);
		gl.glUniform1i(hasShadowMapLoc, engine.getRenderSystem().hasShadowMap() ? 1 : 0);
		gl.glUniform1i(shadowLightIndexLoc, engine.getRenderSystem().getShadowLightIndex());
		gl.glUniform1f(surfaceScaleLoc, go.getRenderStates().getSurfaceScale());
		gl.glUniform1f(shadowStrengthLoc, engine.getRenderSystem().getShadowStrength());
		gl.glUniform1f(textureDetailNearLoc, engine.getRenderSystem().getTextureDetailNear());
		gl.glUniform1f(textureDetailFarLoc, engine.getRenderSystem().getTextureDetailFar());
		gl.glUniform1i(sampLoc, 0);
		gl.glUniform1i(cubeLoc, 1);
		gl.glUniform1i(heightTexLoc, 2);
		gl.glUniform1i(shadowTexLoc, 3);
		gl.glUniform1i(normalTexLoc, 4);
		gl.glUniform1i(detailTexLoc, 5);
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, Light.getGlobalAmbient(), 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, go.getShape().getMatAmb(), 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, go.getShape().getMatDif(), 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, go.getShape().getMatSpe(), 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, go.getShape().getMatShi());
		gl.glProgramUniform1f(renderingProgram, alphaLoc, alpha);

		gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getVertexBuffer());
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
	
		gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getTexCoordBuffer());
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getNormalBuffer());
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		if (hasTex==1)
			thisTexture = go.getTextureImage().getTexture();
		else
			thisTexture = engine.getRenderSystem().getDefaultTexture();

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, thisTexture);
		tiling = (go.getRenderStates()).getTiling();
		if (tiling != 0)
		{	if (tiling == 1) { tilingOption = GL_REPEAT; }
			else if (tiling == 2) { tilingOption = GL_MIRRORED_REPEAT; }
			else if (tiling == 3) { tilingOption = GL_CLAMP_TO_EDGE; }
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, tilingOption);
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, tilingOption);
		}

		activeSkyBoxTexture = (engine.getSceneGraph()).getActiveSkyBoxTexture();
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, activeSkyBoxTexture);

		heightMapTexture = go.getHeightMap().getTexture();
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_2D, heightMapTexture);

		normalMapTexture = (go.getNormalMap() != null) ? go.getNormalMap().getTexture() : engine.getRenderSystem().getDefaultTexture();
		gl.glActiveTexture(GL_TEXTURE4);
		gl.glBindTexture(GL_TEXTURE_2D, normalMapTexture);

		gl.glActiveTexture(GL_TEXTURE3);
		gl.glBindTexture(GL_TEXTURE_2D, engine.getRenderSystem().getShadowTexture());

		detailTexture = (go.getDetailTextureImage() != null) ? go.getDetailTextureImage().getTexture() : thisTexture;
		gl.glActiveTexture(GL_TEXTURE5);
		gl.glBindTexture(GL_TEXTURE_2D, detailTexture);

		if (go.getShape().isWindingOrderCCW())
			gl.glFrontFace(GL_CCW);
		else
			gl.glFrontFace(GL_CW);

		if ((go.getRenderStates()).isWireframe())
			gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		else
			gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		// two-pass transparency handler (modified from a single-pass version by Erik Colchado).
		// This version corresponds to the method described in the CSc-155 textbook.
		if (isTransparent == 1)
		{	gl.glEnable(GL_BLEND);
			gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			gl.glBlendEquation(GL_FUNC_ADD);
			gl.glDepthMask(false);
			gl.glDisable(GL_CULL_FACE);
			gl.glProgramUniform1f(renderingProgram, flipLoc, 1.0f);
			gl.glDrawArrays(GL_TRIANGLES, 0, go.getShape().getNumVertices());
			gl.glDepthMask(true);
			gl.glDisable(GL_BLEND);
		} else
		{	gl.glDisable(GL_BLEND);
			gl.glDisable(GL_CULL_FACE);
			gl.glDrawArrays(GL_TRIANGLES, 0, go.getShape().getNumVertices());
		}
	}
}
