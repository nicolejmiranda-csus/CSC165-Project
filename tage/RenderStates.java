package tage;

import org.joml.*;

/**
 * Holds all render states for a given object.
 * Each game object should have an instantiated RenderStates object associated
 * with it.
 * <ul>
 * <li>enable rendering this object (or not)
 * <li>render this object with or without lighting
 * <li>render this object with or without depth testing
 * <li>render this object with transparency
 * <li>opacity, which is the degree of transparency (if transparent)
 * <li>utilize OpenGL texture tiling - options are: 0=none, 1=repeat,
 * 2=mirroredRepeat, 3=clampToEdge
 * <li>set tile factor -- determines the number of tiles along each dimension,
 * if tiling is turned on
 * <li>set primitive -- 1=point, 2=line, 3=triangle (default)
 * <li>render this object with a solid, specified color
 * <li>render this object in wireframe
 * <li>render hidden faces (if need to view from the inside)
 * <li>enable environment mapping (to make a "chrome-like" object)
 * <li>adjust for an incorrectly-aligned OBJ or RKM model
 * </ul>
 * 
 * @author Scott Gordon
 * @author Erik Colchado (initial version of transparency)
 */
public class RenderStates {
	private boolean enableRendering = true;
	private boolean hasLighting = true;
	private boolean hasDepthTesting = true;
	private boolean isTransparent = false;
	private float opacity = 0.5f; // values are 0 (fully transparent) to 1 (not transparent)
	private int tiling = 0; // 0=none, 1=repeat, 2=mirroredRepeat, 3=clampToEdge
	private int tileFactor = 1;
	private int primitive = 3; // 1=point, 2=line, 3=triangle
	private boolean solidColor = false;
	private Vector3f color = new Vector3f(1f, 0f, 0f);
	private boolean wireframe = false;
	private boolean renderHiddenFaces = false;
	private boolean isEnvironmentMapped = false;
	private boolean castsShadow = true;
	private boolean receivesShadow = true;
	private boolean usesBumpMapping = false;
	private boolean usesNormalMapping = false;
	private boolean usesFog = true;
	private boolean usesTextureDetailBlend = false;
	private int textureMappingMode = 0; // 0=mesh UVs, 1=world-space box projection
	private int bumpStyle = 0; // 0=stone/rock, 1=grass/ground
	private float surfaceScale = 1.0f;
	private Matrix4f modelOrientationCorrection = new Matrix4f();

	// ---------------- ACCESSORS ---------------------

	/** enables rendering this object */
	public void enableRendering() {
		enableRendering = true;
	}

	/** disables rendering this object */
	public void disableRendering() {
		enableRendering = false;
	}

	/** sets whether or not this object responds to lighting */
	public void hasLighting(boolean h) {
		hasLighting = h;
	}

	/** sets whether or not this object participates in depth testing */
	public void hasDepthTesting(boolean h) {
		hasDepthTesting = h;
	}

	/** sets whether or not this object is transparent */
	public void isTransparent(boolean i) {
		isTransparent = i;
	}

	/**
	 * sets opacity (alpha) of an object (0.0f to 1.0f), has no effect if
	 * isTransparent is false
	 */
	public void setOpacity(float f) {
		opacity = f;
	}

	/** sets whether or not this object is environment mapped (simulates chrome) */
	public void isEnvironmentMapped(boolean i) {
		isEnvironmentMapped = i;
	}

	/** sets whether this object casts into the shadow map */
	public void castsShadow(boolean c) {
		castsShadow = c;
	}

	/** sets whether this object receives shadows from the shadow map */
	public void receivesShadow(boolean r) {
		receivesShadow = r;
	}

	/** enables procedural bump mapping for rough stone/terrain-like surfaces */
	public void setBumpMapping(boolean b) {
		usesBumpMapping = b;
	}

	/** enables image normal mapping when the GameObject has a normal-map texture */
	public void setNormalMapping(boolean n) {
		usesNormalMapping = n;
	}

	/** sets whether this object blends into the renderer's distance fog */
	public void setFogEnabled(boolean f) {
		usesFog = f;
	}

	/** sets whether this object blends primary and far/detail textures by distance */
	public void setTextureDetailBlend(boolean b) {
		usesTextureDetailBlend = b;
	}

	/** sets the surface mapping mode: 0=mesh UVs, 1=world-space box projection */
	public void setTextureMappingMode(int m) {
		textureMappingMode = m;
	}

	/** sets the procedural bump style: 0=stone/rock, 1=grass/ground */
	public void setBumpStyle(int b) {
		bumpStyle = b;
	}

	/** sets the scale used by normal/bump procedural surface effects */
	public void setSurfaceScale(float s) {
		surfaceScale = s;
	}

	/** sets whether or not this object is rendered in wireframe mode */
	public void setWireframe(boolean w) {
		wireframe = w;
	}

	/**
	 * specifies how to texture when texcoords are outside [0 to 1] -- 0=none,
	 * 1=repeat, 2=mirroredRepeat, 3=clampToEdge
	 */
	public void setTiling(int t) {
		tiling = t;
	}

	/** sets number of tile occurrances if tiling is turned on */
	public void setTileFactor(int f) {
		tileFactor = f;
	}

	/** 1=point, 2=line, 3=triangle (default) -- set by the engine. */
	public void setPrimitive(int p) {
		primitive = p;
	}

	/** most useful for lines and wireframe. */
	public void setHasSolidColor(boolean sc) {
		solidColor = sc;
	}

	/** the color to use when solidColor also set. */
	public void setColor(Vector3f c) {
		color = new Vector3f(c);
	}

	/** useful if camera can go inside the object. */
	public void setRenderHiddenFaces(boolean r) {
		renderHiddenFaces = r;
	}

	/** apply a rotation without including it in the local or world transforms */
	public void setModelOrientationCorrection(Matrix4f r) {
		modelOrientationCorrection = new Matrix4f(r);
	}

	/** returns a boolean that is true if rendering is enabled for this object */
	public boolean renderingEnabled() {
		return enableRendering;
	}

	/** returns a boolean that is true if this object responds to lighting */
	public boolean hasLighting() {
		return hasLighting;
	}

	/**
	 * returns a boolean that is true if depth testing is enabled for this object -
	 * mostly for skyboxes
	 */
	public boolean hasDepthTesting() {
		return hasDepthTesting;
	}

	/** returns a boolean that is true if this object is transparent */
	public boolean isTransparent() {
		return isTransparent;
	}

	/**
	 * returns a boolean that is true if this object is environment mapped
	 * (simulated chrome)
	 */
	public boolean isEnvironmentMapped() {
		return isEnvironmentMapped;
	}

	/** returns true if this object should be rendered into the shadow map */
	public boolean castsShadow() {
		return castsShadow;
	}

	/** returns true if this object should receive sampled shadow-map shadows */
	public boolean receivesShadow() {
		return receivesShadow;
	}

	/** returns true if this object uses procedural bump mapping */
	public boolean usesBumpMapping() {
		return usesBumpMapping;
	}

	/** returns true if this object uses image normal mapping */
	public boolean usesNormalMapping() {
		return usesNormalMapping;
	}

	/** returns true if this object uses distance fog */
	public boolean usesFog() {
		return usesFog;
	}

	/** returns true if this object blends primary and far/detail textures by distance */
	public boolean usesTextureDetailBlend() {
		return usesTextureDetailBlend;
	}

	/** returns the texture mapping mode: 0=mesh UVs, 1=world-space box projection */
	public int getTextureMappingMode() {
		return textureMappingMode;
	}

	/** returns the procedural bump style: 0=stone/rock, 1=grass/ground */
	public int getBumpStyle() {
		return bumpStyle;
	}

	/** returns the scale used by normal/bump procedural surface effects */
	public float getSurfaceScale() {
		return surfaceScale;
	}

	/**
	 * returns an int specifying the texture behavior when texcoords exceed the
	 * range [0 to 1] -- 0=none, 1=repeat, 2=mirroredRepeat, 3=clampToEdge
	 */
	public int getTiling() {
		return (tiling);
	}

	/** returns the current number of tile occurrances if tiling is turned on */
	public int getTileFactor() {
		return (tileFactor);
	}

	/**
	 * returns an int specifying the primitives type for this object -- 1=point,
	 * 2=line, 3=triangle
	 */
	public int getPrimitive() {
		return (primitive);
	}

	/** returns a float of the object's opacity - relevant only if transparent. */
	public float getOpacity() {
		return opacity;
	}

	/**
	 * returns a boolean that is true if this object is rendered with a solid
	 * specified color
	 */
	public boolean hasSolidColor() {
		return solidColor;
	}

	/**
	 * returns a reference to the Vector3f that contains RGB values for this
	 * object's color if it is solid color
	 */
	public Vector3f getColor() {
		return color;
	}

	/**
	 * returns a boolean that is true if the object has been specified to render in
	 * wireframe
	 */
	public boolean isWireframe() {
		return wireframe;
	}

	/**
	 * returns a boolen that is true if the object has been specified to render
	 * hidden faces
	 */
	public boolean willRenderHiddenFaces() {
		return renderHiddenFaces;
	}

	/**
	 * returns a copy of the matrix that contains the model orientation correction,
	 * if one has been specified
	 */
	public Matrix4f getModelOrientationCorrection() {
		return new Matrix4f(modelOrientationCorrection);
	}

	/**
	 * Returns a clone of this RenderStates object.
	 * Could be useful if an application wants to bounce back and forth between
	 * various render states,
	 * and wants to therefore be able to save the current RenderState before
	 * temporarily switching to another.
	 */
	public RenderStates makeCopy() {
		RenderStates temp = new RenderStates();
		if (this.enableRendering)
			temp.enableRendering();
		else
			temp.disableRendering();
		temp.hasLighting(this.hasLighting);
		temp.hasDepthTesting(this.hasDepthTesting);
		temp.isTransparent(this.isTransparent);
		temp.setOpacity(this.opacity);
		temp.setTiling(this.tiling);
		temp.setTileFactor(this.tileFactor);
		temp.setPrimitive(this.primitive);
		temp.setHasSolidColor(this.solidColor);
		temp.setColor(new Vector3f(this.color.x, this.color.y, this.color.z));
		temp.setWireframe(this.wireframe);
		temp.setRenderHiddenFaces(this.renderHiddenFaces);
		temp.isEnvironmentMapped(this.isEnvironmentMapped);
		temp.castsShadow(this.castsShadow);
		temp.receivesShadow(this.receivesShadow);
		temp.setBumpMapping(this.usesBumpMapping);
		temp.setNormalMapping(this.usesNormalMapping);
		temp.setFogEnabled(this.usesFog);
		temp.setTextureDetailBlend(this.usesTextureDetailBlend);
		temp.setTextureMappingMode(this.textureMappingMode);
		temp.setBumpStyle(this.bumpStyle);
		temp.setSurfaceScale(this.surfaceScale);
		temp.setModelOrientationCorrection(this.getModelOrientationCorrection());
		return temp;
	}
}
