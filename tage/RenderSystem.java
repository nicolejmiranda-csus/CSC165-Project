package tage;

import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import java.util.*;
import java.awt.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;
import tage.shapes.*;
import tage.objectRenderers.*;

/**
 * Manages the OpenGL setup and rendering for each frame.
 * Closely follows the method described in Computer Graphics Programming in
 * OpenGL with Java.
 * <p>
 * The only methods that the game application is likely to utilize are:
 * <ul>
 * <li>setTitle() to set the title in the bar at the top of the render window
 * <li>addViewport() if setting up multiple viewports
 * <li>getViewport() mainly to get that viewport's camera
 * </ul>
 * <p>
 * This class includes the init() and display() methods used by the JOGL
 * animator.
 * 
 * @author Scott Gordon
 */
public class RenderSystem extends JFrame implements GLEventListener {
	private GLCanvas myCanvas;
	private Engine engine;
	private RenderQueue rq;
	private RenderObjectStandard objectRendererStandard;
	private RenderObjectSkyBox objectRendererSkyBox;
	private RenderObjectLine objectRendererLine;
	private RenderObjectAnimation objectRendererAnimation;

	private float fov = 60.0f;
	private float nearClip = 0.1f;
	private float farClip = 1000.0f;

	private int renderingProgram, skyboxProgram, lineProgram;
	private int heightProgram, skelProgram;
	private int shadowProgram, shadowSkeletalProgram;
	private int screenFlashProgram, cloudProgram;
	private int[] vao = new int[1];
	private int[] vbo = new int[3];
	private int[] shadowFramebuffer = new int[1];
	private int[] shadowTexture = new int[1];
	private int[] shadowSkinSSBO = new int[1];
	private int[] screenFlashVao = new int[1];
	private int[] screenFlashVbo = new int[1];

	private int defaultSkyBox;
	private static final int SHADOW_MAP_SIZE = 2048;
	private static final float SHADOW_ORTHO_HALF_EXTENT = 230.0f;
	private static final float SHADOW_LIGHT_DISTANCE = 260.0f;
	private static final float SHADOW_NEAR_PLANE = 1.0f;
	private static final float SHADOW_FAR_PLANE = 650.0f;
	private final float[] fogColor = { 0.10f, 0.13f, 0.16f, 1.0f };
	private float fogStart = 120.0f;
	private float fogEnd = 330.0f;
	private float textureDetailNear = 55.0f;
	private float textureDetailFar = 300.0f;
	private float shadowStrength = 0.62f;
	private int shadowLightIndex = 0;
	private boolean shadowMapReady = false;

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private FloatBuffer shadowSkinVals = Buffers.newDirectFloatBuffer(AnimatedShape.MAX_SKIN_BONES * 16);
	private Matrix4f pMat = new Matrix4f(); // perspective matrix
	private Matrix4f vMat = new Matrix4f(); // view matrix
	private Matrix4f shadowLightVMat = new Matrix4f();
	private Matrix4f shadowLightPMat = new Matrix4f();
	private Matrix4f shadowLightVPMat = new Matrix4f();
	private Matrix4f shadowModelMat = new Matrix4f();
	private Vector3f shadowLightPos = new Vector3f();
	private Vector3f shadowTarget = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f shadowUp = new Vector3f(0.0f, 1.0f, 0.0f);
	private int xLoc, zLoc;
	private float aspect;
	private int defaultTexture;
	private String defaultTitle = "default title", title;
	private int screenSizeX, screenSizeY;
	private long cloudStartMillis = System.currentTimeMillis();

	private ArrayList<TextureImage> textures = new ArrayList<TextureImage>();
	private ArrayList<ObjShape> shapes = new ArrayList<ObjShape>();
	private LinkedHashMap<String, Viewport> viewportList = new LinkedHashMap<String, Viewport>();

	private int canvasWidth, canvasHeight;
	private boolean isInFullScreenMode = false;
	GraphicsEnvironment ge;
	GraphicsDevice gd;

	GLCapabilities capabilities;

	private int buffer[] = new int[1];
	private float res[] = new float[1];

	protected RenderSystem(Engine e) {
		engine = e;

		ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gd = ge.getDefaultScreenDevice();
		DisplaySettingsDialog dsd = new DisplaySettingsDialog(gd);
		dsd.showIt();

		DisplayMode dm = dsd.getSelectedDisplayMode();
		this.setSize(dm.getWidth(), dm.getHeight());
		if (dsd.isFullScreenModeSelected())
			tryFullScreenMode(gd, dm);
	}

	/**
	 * The game application can use this to set the window dimensions if in windowed
	 * mode.
	 */
	public void setWindowDimensions(int ssX, int ssY) {
		title = defaultTitle;
		screenSizeX = ssX;
		screenSizeY = ssY;
		if (!isInFullScreenMode)
			setSize(screenSizeX, screenSizeY);
	}

	/** gets a reference to the current OpenGL canvas used by the engine */
	public GLCanvas getGLCanvas() {
		return myCanvas;
	}

	/** sets the title at the top of the window if in windowed mode */
	public void setTitle(String t) {
		title = t;
	}

	private void tryFullScreenMode(GraphicsDevice gd, DisplayMode dispMode) {
		isInFullScreenMode = false;
		if (gd.isFullScreenSupported()) {
			this.setUndecorated(true);
			this.setResizable(false);
			this.setIgnoreRepaint(true); // AWT repaint events ignored for active rendering
			gd.setFullScreenWindow(this);
			if (gd.isDisplayChangeSupported()) {
				try {
					gd.setDisplayMode(dispMode);
					this.setSize(dispMode.getWidth(), dispMode.getHeight());
					isInFullScreenMode = true;
				} catch (IllegalArgumentException e) {
					System.out.println(e.getLocalizedMessage());
					this.setUndecorated(false);
					this.setResizable(true);
				}
			} else {
				System.out.println("FSEM not supported");
			}
		} else {
			this.setUndecorated(false);
			this.setResizable(true);
			this.setSize(dispMode.getWidth(), dispMode.getHeight());
			this.setLocationRelativeTo(null);
		}
	}

	/** incomplete - see comments in the code. */
	public void toggleFullScreenMode() { // Note that toggling out of fullscreen mode is incomplete.
											// It basically just makes the window smaller - it cannot be resized or
											// minimized.
											// The only way to completely exit fullscreen mode is to first dispose the
											// frame.
											// But that causes a new frame to be created, resulting in another call to
											// JOGL init().
											// So although this doesn't fully return to windowed mode, at least you can
											// access other windows.
											// And toggling back to fullscreen mode, if initially was in fullscreen
											// mode, should work.

		if (isInFullScreenMode) {
			gd.setFullScreenWindow(null);
			this.setSize(screenSizeX, screenSizeY);
			isInFullScreenMode = false;
		} else {
			gd.setFullScreenWindow(this);
			this.setExtendedState(JFrame.MAXIMIZED_BOTH);
			isInFullScreenMode = true;
		}
	}

	/** Adds a viewport at (bottom, left) with dimensions width and height. */
	public Viewport addViewport(String name, float bottom, float left, float width, float height) {
		Viewport vp = new Viewport(name, engine, bottom, left, width, height);
		viewportList.put(name, vp);
		return vp;
	}

	/** gets a reference to the viewport with the specified name. */
	public Viewport getViewport(String name) {
		return viewportList.get(name);
	}

	// Creates the OpenGL profile, canvas, and starts the animator which serves as
	// the game loop
	protected void startOpenGLGameLoop() {
		setTitle(title);

		// configure OpenGL
		GLProfile profile = GLProfile.getDefault();
		capabilities = new GLCapabilities(profile);
		capabilities.setSampleBuffers(true);
		capabilities.setNumSamples(4);

		// create the actual GLCanvas, then start the animator
		myCanvas = new GLCanvas(capabilities);
		myCanvas.addGLEventListener(this);
		this.getContentPane().add(myCanvas, BorderLayout.CENTER);
		this.setVisible(true);
		(engine.getHUDmanager()).setGLcanvas(myCanvas);

		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	/**
	 * Displays the current frame - for Engine use only.
	 * This method is called automatically by the JOGL Animator, once per frame.
	 * It renders every object in the scene, considering all factors such as lights,
	 * etc.
	 * The game application should NOT call this function directly.
	 */
	public void display(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		(engine.getGame()).update();
		(engine.getSceneGraph()).applyNodeControllers();

		LightManager lm = engine.getLightManager();
		if (lm.getHasChanged())
			lm.loadLightArraySSBO();
		else
			lm.updateSSBO();

		canvasWidth = myCanvas.getWidth();
		canvasHeight = myCanvas.getHeight();

		rq = new RenderQueue((engine.getSceneGraph()).getRoot());
		Vector<GameObject> q = rq.createStandardQueue();
		if (engine.willRenderGraphicsObjects()) {
			updateShadowMatrices();
			renderShadowMap(q);
		}

		Matrix4f hudVMat = null;
		Matrix4f hudPMat = null;
		Viewport hudViewport = null;
		Viewport mainViewport = viewportList.get("MAIN");
		for (Viewport vp : viewportList.values()) {
			if (!vp.isEnabled())
				continue;

			vMat = vp.getCamera().getViewMatrix();

			aspect = ((float) myCanvas.getWidth() * vp.getRelativeWidth())
					/ ((float) myCanvas.getHeight() * vp.getRelativeHeight());
			pMat.setPerspective((float) Math.toRadians(fov), aspect, nearClip, farClip);

			constructViewport(vp);
			if (hudViewport == null || vp == mainViewport) {
				hudVMat = new Matrix4f(vMat);
				hudPMat = new Matrix4f(pMat);
				hudViewport = vp;
			}

			if ((engine.getSceneGraph()).isSkyboxEnabled()) {
				objectRendererSkyBox.render((engine.getSceneGraph()).getSkyBoxObject(), skyboxProgram, pMat, vMat);
				drawCloudLayer(gl);
			}

			// render the graphics objects unless this has been disabled
			if (engine.willRenderGraphicsObjects()) {
				for (int i = 0; i < q.size(); i++) {
					GameObject go = q.get(i);
					gl.glDisable(GL_BLEND); // Prevents transparency bleed
					if ((go.getRenderStates()).renderingEnabled()) {
						if ((go.getShape()).getPrimitiveType() < 3) {
							objectRendererLine.render(go, lineProgram, pMat, vMat);
						} else if (go.getShape() instanceof AnimatedShape) {
							objectRendererAnimation.render(go, skelProgram, pMat, vMat);
						} else {
							objectRendererStandard.render(go, renderingProgram, pMat, vMat);
							// if hidden faces are rendered, render a second time with opposite winding
							// order
							if ((go.getRenderStates()).willRenderHiddenFaces()) {
								(go.getShape()).toggleWindingOrder();
								objectRendererStandard.render(go, renderingProgram, pMat, vMat);
								(go.getShape()).toggleWindingOrder();
							}
						}
					}
				}
			}

			// render the physics world if this is enabled, in wireframe
			if (engine.willRenderPhysicsObjects()) {
				Vector<GameObject> physicsQueue = (engine.getSceneGraph()).getPhysicsRenderables();
				gl.glClear(GL_DEPTH_BUFFER_BIT);
				for (int i = 0; i < physicsQueue.size(); i++) {
					GameObject go = physicsQueue.get(i);

					// set translation
					Vector3f loc = go.getPhysicsObject().getLocation();
					Matrix4f locMat = new Matrix4f();
					locMat.set(3, 0, loc.x);
					locMat.set(3, 1, loc.y);
					locMat.set(3, 2, loc.z);
					go.setLocalTranslation(locMat);

					// set rotation
					Quaternionf rot = go.getPhysicsObject().getRotation();
					Matrix4f rotMat = new Matrix4f();
					rot.get(rotMat);
					go.setLocalRotation(rotMat);

					objectRendererStandard.render(go, renderingProgram, pMat, vMat);
				}
			}
		}

		(engine.getHUDmanager()).drawHUDs(hudVMat, hudPMat, hudViewport, canvasWidth, canvasHeight);
		drawScreenFlashOverlay(gl, engine.getGame().getScreenFlashOpacity());
	}

	private void drawScreenFlashOverlay(GL4 gl, float opacity) {
		if (opacity <= 0.001f || screenFlashProgram == 0) return;
		float alpha = Math.max(0.0f, Math.min(1.0f, opacity));

		gl.glUseProgram(screenFlashProgram);
		int opacityLoc = gl.glGetUniformLocation(screenFlashProgram, "opacity");
		int colorLoc = gl.glGetUniformLocation(screenFlashProgram, "overlayColor");
		int vignetteLoc = gl.glGetUniformLocation(screenFlashProgram, "vignette");
		gl.glUniform1f(opacityLoc, alpha);
		Vector3f color = engine.getGame().getScreenFlashColor();
		gl.glUniform3f(colorLoc, color.x, color.y, color.z);
		gl.glUniform1f(vignetteLoc, engine.getGame().getScreenFlashVignette());

		gl.glDisable(GL_DEPTH_TEST);
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBindVertexArray(screenFlashVao[0]);
		gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		gl.glBindVertexArray(0);
		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_DEPTH_TEST);
	}

	private void drawCloudLayer(GL4 gl) {
		if (cloudProgram == 0 || screenFlashVao[0] == 0) return;
		float time = (System.currentTimeMillis() - cloudStartMillis) / 1000.0f;
		gl.glUseProgram(cloudProgram);
		int timeLoc = gl.glGetUniformLocation(cloudProgram, "timeSeconds");
		int nightLoc = gl.glGetUniformLocation(cloudProgram, "nightFactor");
		gl.glUniform1f(timeLoc, time);
		gl.glUniform1f(nightLoc, Math.max(0.0f, Math.min(1.0f, engine.getGame().getCloudNightFactor())));

		gl.glDisable(GL_DEPTH_TEST);
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBindVertexArray(screenFlashVao[0]);
		gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		gl.glBindVertexArray(0);
		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_DEPTH_TEST);
	}

	private void constructViewport(Viewport vp) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glEnable(GL_SCISSOR_TEST);
		gl.glScissor((int) (vp.getRelativeLeft() * canvasWidth),
				(int) (vp.getRelativeBottom() * canvasHeight),
				(int) vp.getActualWidth(),
				(int) vp.getActualHeight());
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GL_SCISSOR_TEST);

		if (vp.getHasBorder()) {
			int borderWidth = vp.getBorderWidth();
			float[] color = vp.getBorderColor();
			gl.glEnable(GL_SCISSOR_TEST);
			gl.glScissor((int) (vp.getRelativeLeft() * canvasWidth),
					(int) (vp.getRelativeBottom() * canvasHeight),
					(int) vp.getActualWidth(),
					(int) vp.getActualHeight());
			gl.glClearColor(color[0], color[1], color[2], 1.0f);
			gl.glClear(GL_COLOR_BUFFER_BIT);
			gl.glScissor((int) (vp.getRelativeLeft() * canvasWidth) + borderWidth,
					(int) (vp.getRelativeBottom() * canvasHeight) + borderWidth,
					(int) vp.getActualWidth() - borderWidth * 2,
					(int) vp.getActualHeight() - borderWidth * 2);
			gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			gl.glClear(GL_COLOR_BUFFER_BIT);
			gl.glDisable(GL_SCISSOR_TEST);

			gl.glViewport((int) (vp.getRelativeLeft() * canvasWidth) + borderWidth,
					(int) (vp.getRelativeBottom() * canvasHeight) + borderWidth,
					(int) (vp.getRelativeWidth() * canvasWidth) - borderWidth * 2,
					(int) (vp.getRelativeHeight() * canvasHeight) - borderWidth * 2);
		} else {
			gl.glViewport((int) (vp.getRelativeLeft() * canvasWidth),
					(int) (vp.getRelativeBottom() * canvasHeight),
					(int) (vp.getRelativeWidth() * canvasWidth),
					(int) (vp.getRelativeHeight() * canvasHeight));
		}
	}

	/**
	 * Initializes the elements needed for rendering - for Engine use only.
	 * This method is called one time, automatically, by the JOGL Animator.
	 * The game application should NOT call this function directly.
	 * Note that the order of operations in this function is significant.
	 */
	public void init(GLAutoDrawable drawable) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		System.out.println("OpenGL init() called");

		gl.glEnable(GL_MULTISAMPLE);

		renderingProgram = Utils.createShaderProgram("assets/shaders/StandardVert.glsl",
				"assets/shaders/StandardFrag.glsl");

		skyboxProgram = Utils.createShaderProgram("assets/shaders/skyboxVert.glsl",
				"assets/shaders/skyboxFrag.glsl");

		lineProgram = Utils.createShaderProgram("assets/shaders/LineVert.glsl",
				"assets/shaders/LineFrag.glsl");

		skelProgram = Utils.createShaderProgram("assets/shaders/skeletalVert.glsl",
				"assets/shaders/StandardFrag.glsl");

		shadowProgram = Utils.createShaderProgram("assets/shaders/ShadowVert.glsl",
				"assets/shaders/ShadowFrag.glsl");

		shadowSkeletalProgram = Utils.createShaderProgram("assets/shaders/ShadowSkeletalVert.glsl",
				"assets/shaders/ShadowFrag.glsl");

		screenFlashProgram = Utils.createShaderProgram("assets/shaders/ScreenFlashVert.glsl",
				"assets/shaders/ScreenFlashFrag.glsl");

		cloudProgram = Utils.createShaderProgram("assets/shaders/ScreenFlashVert.glsl",
				"assets/shaders/CloudFrag.glsl");

		objectRendererStandard = new RenderObjectStandard(engine);
		objectRendererSkyBox = new RenderObjectSkyBox(engine);
		objectRendererLine = new RenderObjectLine(engine);
		objectRendererAnimation = new RenderObjectAnimation(engine);

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(fov), aspect, nearClip, farClip);

		System.out.println("loading skyboxes");
		defaultTexture = Utils.loadTexture("assets/defaultAssets/checkerboardSmall.JPG");
		defaultSkyBox = Utils.loadCubeMap("assets/defaultAssets/lakeIslands");
		setupShadowMap();

		loadTexturesIntoOpenGL();
		(engine.getGame()).loadSkyBoxes();

		// prepare buffer for extracting height from height map
		heightProgram = Utils.createShaderProgram("assets/shaders/heightCompute.glsl");
		gl.glGenBuffers(1, buffer, 0);
		gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer[0]);
		FloatBuffer resBuf = Buffers.newDirectFloatBuffer(res.length);
		gl.glBufferData(GL_SHADER_STORAGE_BUFFER, resBuf.limit() * 4, null, GL_STATIC_READ);

		System.out.println("initializing physics objects");
		(engine.getGame()).initializePhysicsObjects();

		loadVBOs();
		loadScreenFlashVBO();

		engine.setGLstarted();
		(engine.getLightManager()).loadLightsSSBOinitial();
	}

	protected int getDefaultSkyBox() {
		return defaultSkyBox;
	}

	/** for engine use only. */
	public int getDefaultTexture() {
		return defaultTexture;
	}

	/** for engine use only. */
	public Matrix4f getLightVPMatrix() {
		return shadowLightVPMat;
	}

	/** for engine use only. */
	public int getShadowTexture() {
		return shadowTexture[0];
	}

	/** for engine use only. */
	public boolean hasShadowMap() {
		return shadowMapReady;
	}

	/** for engine use only. */
	public int getShadowLightIndex() {
		return shadowLightIndex;
	}

	/** for engine use only. */
	public float getShadowStrength() {
		return shadowStrength;
	}

	/** for engine use only. */
	public float[] getFogColor() {
		return fogColor;
	}

	/** for engine use only. */
	public float getFogStart() {
		return fogStart;
	}

	/** for engine use only. */
	public float getFogEnd() {
		return fogEnd;
	}

	/** sets renderer distance fog color */
	public void setFogColor(float r, float g, float b, float a) {
		fogColor[0] = r;
		fogColor[1] = g;
		fogColor[2] = b;
		fogColor[3] = a;
	}

	/** sets renderer distance fog start/end distances */
	public void setFogRange(float start, float end) {
		fogStart = Math.max(0.0f, start);
		fogEnd = Math.max(fogStart + 1.0f, end);
	}

	/** for engine use only. */
	public float getTextureDetailNear() {
		return textureDetailNear;
	}

	/** for engine use only. */
	public float getTextureDetailFar() {
		return textureDetailFar;
	}

	// ----------------------- SHAPES SECTION ----------------------

	protected void addShape(ObjShape s) {
		shapes.add(s);
	}

	// loads the vertices, tex coords, and normals into three VBOs - for engine use.
	// */
	private void loadVBOs() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		for (ObjShape shape : shapes) {
			gl.glGenBuffers(3, vbo, 0);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
			FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(shape.getVertices());
			gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
			FloatBuffer texBuf = Buffers.newDirectFloatBuffer(shape.getTexCoords());
			gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
			FloatBuffer norBuf = Buffers.newDirectFloatBuffer(shape.getNormals());
			gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

			shape.setVertexBuffer(vbo[0]);
			shape.setTexCoordBuffer(vbo[1]);
			shape.setNormalBuffer(vbo[2]);

			if (shape instanceof AnimatedShape) {
				gl.glGenBuffers(2, vbo, 0);

				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
				FloatBuffer boneBuf = Buffers.newDirectFloatBuffer(shape.getBoneWeights());
				gl.glBufferData(GL_ARRAY_BUFFER, boneBuf.limit() * 4, boneBuf, GL_STATIC_DRAW);

				gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
				FloatBuffer binBuf = Buffers.newDirectFloatBuffer(shape.getBoneIndices());
				gl.glBufferData(GL_ARRAY_BUFFER, binBuf.limit() * 4, binBuf, GL_STATIC_DRAW);

				shape.setBoneWeightBuffer(vbo[0]);
				shape.setBoneIndicesBuffer(vbo[1]);
			}
		}

		// load skybox into vbo
		gl.glGenBuffers(1, vbo, 0);
		GameObject go = (engine.getSceneGraph()).getSkyBoxObject();
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(go.getShape().getVertices());
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);
		go.getShape().setVertexBuffer(vbo[0]);
	}

	private void loadScreenFlashVBO() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		float[] quad = {
				-1.0f, -1.0f,
				 1.0f, -1.0f,
				-1.0f,  1.0f,
				 1.0f,  1.0f
		};
		gl.glGenVertexArrays(1, screenFlashVao, 0);
		gl.glGenBuffers(1, screenFlashVbo, 0);
		gl.glBindVertexArray(screenFlashVao[0]);
		gl.glBindBuffer(GL_ARRAY_BUFFER, screenFlashVbo[0]);
		FloatBuffer quadBuf = Buffers.newDirectFloatBuffer(quad);
		gl.glBufferData(GL_ARRAY_BUFFER, quadBuf.limit() * 4, quadBuf, GL_STATIC_DRAW);
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glBindVertexArray(0);
	}

	private void setupShadowMap() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glGenTextures(1, shadowTexture, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTexture[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0,
				GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
		gl.glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor, 0);

		gl.glGenFramebuffers(1, shadowFramebuffer, 0);
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebuffer[0]);
		gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTexture[0], 0);
		gl.glDrawBuffer(GL_NONE);
		gl.glReadBuffer(GL_NONE);
		shadowMapReady = gl.glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
		if (!shadowMapReady)
			System.out.println("shadow framebuffer is incomplete");
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	private void updateShadowMatrices() {
		LightManager lm = engine.getLightManager();
		if (lm.getNumLights() == 0 || shadowFramebuffer[0] == 0 || shadowTexture[0] == 0) {
			shadowMapReady = false;
			return;
		}

		Light shadowLight = null;
		for (int i = 0; i < lm.getNumLights(); i++) {
			Light light = lm.getLight(i);
			if (light.isEnabled()) {
				shadowLight = light;
				shadowLightIndex = i;
				break;
			}
		}
		if (shadowLight == null) {
			shadowMapReady = false;
			return;
		}

		shadowMapReady = true;
		Vector3f lightSource = shadowLight.getLocationVector();
		Vector3f lightDirection = new Vector3f(lightSource);
		if (lightDirection.lengthSquared() < 0.0001f)
			lightDirection.set(0.45f, 0.85f, 0.35f);
		lightDirection.normalize();
		shadowTarget.set(getShadowFocusPoint());
		shadowTarget.y = 0.0f;
		shadowLightPos.set(shadowTarget).add(lightDirection.mul(SHADOW_LIGHT_DISTANCE));

		Vector3f lightDir = new Vector3f(shadowTarget).sub(shadowLightPos).normalize();
		if (Math.abs(lightDir.dot(0.0f, 1.0f, 0.0f)) > 0.95f)
			shadowUp.set(0.0f, 0.0f, 1.0f);
		else
			shadowUp.set(0.0f, 1.0f, 0.0f);

		shadowLightVMat.identity().lookAt(shadowLightPos, shadowTarget, shadowUp);
		shadowLightPMat.identity().ortho(-SHADOW_ORTHO_HALF_EXTENT, SHADOW_ORTHO_HALF_EXTENT,
				-SHADOW_ORTHO_HALF_EXTENT, SHADOW_ORTHO_HALF_EXTENT,
				SHADOW_NEAR_PLANE, SHADOW_FAR_PLANE);
		shadowLightVPMat.set(shadowLightPMat).mul(shadowLightVMat);
	}

	private Vector3f getShadowFocusPoint() {
		Viewport mainViewport = viewportList.get("MAIN");
		if (mainViewport != null && mainViewport.isEnabled())
			return mainViewport.getCamera().getLocation();
		for (Viewport vp : viewportList.values()) {
			if (vp != null && vp.isEnabled())
				return vp.getCamera().getLocation();
		}
		return new Vector3f(0.0f, 0.0f, 0.0f);
	}

	private void renderShadowMap(Vector<GameObject> queue) {
		if (!shadowMapReady || queue == null)
			return;

		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebuffer[0]);
		gl.glDrawBuffer(GL_NONE);
		gl.glReadBuffer(GL_NONE);
		gl.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glColorMask(false, false, false, false);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDisable(GL_BLEND);
		gl.glDisable(GL_CULL_FACE);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1.5f, 3.0f);

		for (int i = 0; i < queue.size(); i++) {
			GameObject go = queue.get(i);
			if (go == null || go.getShape() == null)
				continue;
			if (!go.getRenderStates().renderingEnabled() || !go.getRenderStates().castsShadow())
				continue;
			if (go.getRenderStates().isTransparent())
				continue;
			if (go.getShape().getPrimitiveType() < 3)
				continue;
			renderShadowObject(go);
		}

		gl.glDisable(GL_POLYGON_OFFSET_FILL);
		gl.glColorMask(true, true, true, true);
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glDrawBuffer(GL_BACK);
		gl.glReadBuffer(GL_BACK);
		gl.glViewport(0, 0, canvasWidth, canvasHeight);
	}

	private void renderShadowObject(GameObject go) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		boolean animated = go.getShape() instanceof AnimatedShape;
		int program = animated ? shadowSkeletalProgram : shadowProgram;
		gl.glUseProgram(program);

		shadowModelMat.identity();
		shadowModelMat.mul(go.getWorldTranslation());
		shadowModelMat.mul(go.getWorldRotation());
		shadowModelMat.mul(go.getRenderStates().getModelOrientationCorrection());
		shadowModelMat.mul(go.getWorldScale());

		int mLoc = gl.glGetUniformLocation(program, "m_matrix");
		int lightVPLoc = gl.glGetUniformLocation(program, "lightVP_matrix");
		gl.glUniformMatrix4fv(mLoc, 1, false, shadowModelMat.get(vals));
		gl.glUniformMatrix4fv(lightVPLoc, 1, false, shadowLightVPMat.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getVertexBuffer());
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		if (animated) {
			uploadShadowSkinMatrices((AnimatedShape) go.getShape());
			gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getBoneIndicesBuffer());
			gl.glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(3);

			gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getBoneWeightBuffer());
			gl.glVertexAttribPointer(4, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(4);
		} else {
			int heightMapped = go.isTerrain() ? 1 : 0;
			int hLoc = gl.glGetUniformLocation(program, "heightMapped");
			int heightTexLoc = gl.glGetUniformLocation(program, "height");
			gl.glUniform1i(hLoc, heightMapped);
			gl.glUniform1i(heightTexLoc, 2);
			gl.glBindBuffer(GL_ARRAY_BUFFER, go.getShape().getTexCoordBuffer());
			gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(1);
			gl.glActiveTexture(GL_TEXTURE2);
			gl.glBindTexture(GL_TEXTURE_2D, go.getHeightMap().getTexture());
		}

		if (go.getShape().isWindingOrderCCW())
			gl.glFrontFace(GL_CCW);
		else
			gl.glFrontFace(GL_CW);

		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		gl.glDrawArrays(GL_TRIANGLES, 0, go.getShape().getNumVertices());
	}

	private void uploadShadowSkinMatrices(AnimatedShape shape) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		tage.rml.Matrix4[] skinMats = shape.getPoseSkinMatrices();
		int boneCount = shape.getBoneCount();
		int uploadCount = java.lang.Math.min(boneCount, AnimatedShape.MAX_SKIN_BONES);
		uploadCount = java.lang.Math.min(uploadCount, skinMats.length);

		if (shadowSkinSSBO[0] == 0)
			gl.glGenBuffers(1, shadowSkinSSBO, 0);

		shadowSkinVals.clear();
		for (int i = 0; i < uploadCount; i++)
			shadowSkinVals.put(skinMats[i].toFloatArray());
		if (uploadCount == 0)
			shadowSkinVals.put(new float[] {
					1.0f, 0.0f, 0.0f, 0.0f,
					0.0f, 1.0f, 0.0f, 0.0f,
					0.0f, 0.0f, 1.0f, 0.0f,
					0.0f, 0.0f, 0.0f, 1.0f
			});
		shadowSkinVals.flip();

		gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, shadowSkinSSBO[0]);
		gl.glBufferData(GL_SHADER_STORAGE_BUFFER, shadowSkinVals.limit() * 4, shadowSkinVals, GL_DYNAMIC_DRAW);
		gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, shadowSkinSSBO[0]);
		gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
	}

	// ------------------ TEXTURE SECTION ---------------------

	protected void addTexture(TextureImage t) {
		textures.add(t);
	}

	private void loadTexturesIntoOpenGL() {
		int thisTexture;
		for (int i = 0; i < textures.size(); i++) {
			TextureImage t = textures.get(i);
			thisTexture = Utils.loadTexture(t.getTextureFile());
			t.setTexture(thisTexture);
		}
		engine.getSceneGraph().setActiveSkyBoxTexture(defaultSkyBox);
	}

	/**
	 * Get height map height at the specified texture coordinate (x,z).
	 * This is accomplished with the help of a small shader program.
	 */
	public float getHeightAt(int texture, float x, float z) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(heightProgram);
		gl.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, buffer[0]);

		xLoc = gl.glGetUniformLocation(heightProgram, "x");
		zLoc = gl.glGetUniformLocation(heightProgram, "z");
		gl.glUniform1f(xLoc, x);
		gl.glUniform1f(zLoc, z);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, texture);

		gl.glDispatchCompute(1, 1, 1);
		gl.glFinish();

		gl.glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer[0]);
		FloatBuffer resBuf = Buffers.newDirectFloatBuffer(res.length);
		gl.glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, resBuf.limit() * 4, resBuf);

		float res = resBuf.get();
		return res;
	}

	// ---------------------------------------------------------
	/** for engine use, called by JOGL. */
	public void dispose(GLAutoDrawable drawable) {
	}

	/** for engine use, called by JOGL. */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(fov), aspect, nearClip, farClip);
	}
}
