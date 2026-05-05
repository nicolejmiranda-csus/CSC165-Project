
package tage.shapes;

import tage.*;
import org.joml.*;

import java.util.ArrayList;
import java.util.*;
import java.nio.*;
import java.io.*;

/**
 * AnimatedShape contains functions for loading animated models, building animated shapes and
 * their skeletons, and applying animations to the models at runtime.
 * It is based on the skeletal mesh classes and functions by Luis Gutierrez (2017) for RAGE.
 * <p>
 * Animations can be looped or ping-ponged, depending on the ENUM setting.
 * The methods appropriate for the game application to call are:
 * <ul>
 * <li> AnimatedShape() - the constructor
 * <li> loadAnimation() - should be called during loadShapes()
 * <li> playAnimation() - to initiate an animation when desired
 * <li> updateAnimation() - should be called by the game application in update()
 * </ul>
 * <p>
 * Note that the only animated models supported are those exported using the Blender RAGE exporters.
 * @author Luis Gutierrez.
 * Adapted for TAGE by Scott Gordon.
 */
public final class AnimatedShape extends ObjShape
{
	public static final int MAX_SKIN_BONES = 256;

	/** Specifies what happens when reaching the end of an animation. */
	public enum EndType
	{	NONE,    // Default (equivalent in function to STOP)
		STOP,    // Stops the animation, reverts to root pose
		PAUSE,   // Freezes the animation at the last frame
		LOOP,    // Restarts the animation from the first frame
		PINGPONG // Plays the animation in reverse from the current frame
	}
	
	private int vertCount;
	private int triCount;
	private int boneCount;

	// mesh data
	private List<Float> vertexPositionsList = new ArrayList<>();
	private List<Float> vertexTexCoordsList = new ArrayList<>();
	private List<Float> vertexBoneWeightsList = new ArrayList<>();
	private List<Float> vertexBoneIndicesList = new ArrayList<>();
	private List<Float> vertexNormalsList = new ArrayList<>();
	private List<Integer> vertexIndicesList = new ArrayList<>();

	// skeleton data
	private AnimatedSkeleton skel = new AnimatedSkeleton();
	private List<String> boneNamesList = new ArrayList<>();
	private List<Float> boneLengthsList = new ArrayList<>();
	private List<Float> boneRestRotationsList = new ArrayList<>();
	private List<Float> boneRestLocationsList = new ArrayList<>();
	private List<Integer> boneParentsList = new ArrayList<>();
	private List<Float> boneInverseBindMatricesList = new ArrayList<>();
	private tage.rml.Matrix4[] boneInverseBindMatrices;
	private tage.rml.Matrix4[] boneBindMatrices;
	private boolean hasMatrixBindPose = false;

	// animation data
	private HashMap<String, Animation> animationsList = new HashMap<>();
	private Animation curAnimation = null;
	private int curAnimFrame = -1;
	private float curLerpedAnimFrame = -1;
	private float curAnimSpeed = 1.0f;  // negative is backwards
	private EndType curAnimEndtype = EndType.NONE;
	private int curAnimEndTypeTotal = -1;  // how many times to loop (0 for forever)
	private int curAnimEndTypeCount = 0;   // how many time have we looped
	private boolean curAnimPaused = false;
	private boolean warnedLegacyRigifyAnimation = false;

	// Current Skeleton Pose Skinning Matrices.
	// This array holds a list of 4x4 matrices.
	// These matrices are multiplied by the vertices to yield their skinned model-space locations.
	// This instance of this list of matrices is updated every frame.
	// The IT version is inverse-transpose, for modifying the normal vectors.

	private tage.rml.Matrix4[] curSkinMatrices = new tage.rml.Matrix4[MAX_SKIN_BONES];
	private tage.rml.Matrix3[] curSkinMatricesIT = new tage.rml.Matrix3[MAX_SKIN_BONES];

	/** Specifies filenames for the model (with extension "rkm") and the skeleton (with extension "rks"). */
	public AnimatedShape(String meshPath, String skelPath)
	{
		// Defaulting the skin matrices to identity
		for (int i = 0; i < curSkinMatrices.length; i++)
			curSkinMatrices[i] = tage.rml.Matrix4f.createIdentityMatrix();
		for (int i = 0; i < curSkinMatricesIT.length; i++)
			curSkinMatricesIT[i] = tage.rml.Matrix3f.createIdentityMatrix();
	
		// --------- FIRST, READ IN MESH ----------

		String line;
		vertCount = 0;
		triCount = 0;
		boneCount = 0;
		try
		{	InputStream input = new FileInputStream(new File("assets/animations/" + meshPath));
			BufferedReader br = new BufferedReader(new InputStreamReader(input));

			// Read the header data
			if ((line = br.readLine()) != null)
			{	// Split this tab-delimited line into an array
				String[] header = line.split("\t");
				vertCount = Integer.parseInt(header[0]);
				triCount = Integer.parseInt(header[1]);
				boneCount = Integer.parseInt(header[2]);
			}

			// Read the vertices
			for (int i = 0; i < vertCount && ((line = br.readLine()) != null); i++)
			{	String[] vert = line.split("\t");

				// First 3 floats are positions
				vertexPositionsList.add(Float.parseFloat(vert[0]));
				vertexPositionsList.add(Float.parseFloat(vert[1]));
				vertexPositionsList.add(Float.parseFloat(vert[2]));

				// Next 2 floats are texture coordinates
				vertexTexCoordsList.add(Float.parseFloat(vert[3]));
				vertexTexCoordsList.add(Float.parseFloat(vert[4]));

				// Next 6 floats are bone weights and bone indices for 3 bones
				vertexBoneWeightsList.add(Float.parseFloat(vert[5]));
				vertexBoneIndicesList.add(Float.parseFloat(vert[6]));

				vertexBoneWeightsList.add(Float.parseFloat(vert[7]));
				vertexBoneIndicesList.add(Float.parseFloat(vert[8]));

				vertexBoneWeightsList.add(Float.parseFloat(vert[9]));
				vertexBoneIndicesList.add(Float.parseFloat(vert[10]));

				// Next 3 floats are the normal vector
				vertexNormalsList.add(Float.parseFloat(vert[11]));
				vertexNormalsList.add(Float.parseFloat(vert[12]));
				vertexNormalsList.add(Float.parseFloat(vert[13]));

				// Next 3 floats are the tangent vector
				//RAGE does not currently support vertex tangent vectors
				// Next 3 floats are the binormal vector
				//RAGE does not currently support vertex binormal vectors
			}

			// Read the triangles
			for (int i = 0; i < triCount && ((line = br.readLine()) != null); i++)
			{	String[] tri_verts = line.split("\t");

				// Each line contains 3 floats
				vertexIndicesList.add(Integer.parseInt(tri_verts[0]));
				vertexIndicesList.add(Integer.parseInt(tri_verts[1]));
				vertexIndicesList.add(Integer.parseInt(tri_verts[2]));
			}
		}
		catch (IOException e)
		{	throw new RuntimeException(e);
		}

		// transfer ArrayLists to arrays of Vector3f

		Vector3f[] verticesV = new Vector3f[triCount*3];
		Vector2f[] texCoordsV = new Vector2f[triCount*3];
		Vector3f[] normalsV = new Vector3f[triCount*3];
		Vector3f[] boneWeightsV = new Vector3f[triCount*3];
		Vector3f[] boneIndicesV = new Vector3f[triCount*3];

		for (int i = 0; i < triCount; i++)
		{
			verticesV[i*3+0] = new Vector3f(
				vertexPositionsList.get(vertexIndicesList.get(i*3+0)*3+0),
				vertexPositionsList.get(vertexIndicesList.get(i*3+0)*3+1),
				vertexPositionsList.get(vertexIndicesList.get(i*3+0)*3+2));
			verticesV[i*3+1] = new Vector3f(
				vertexPositionsList.get(vertexIndicesList.get(i*3+1)*3+0),
				vertexPositionsList.get(vertexIndicesList.get(i*3+1)*3+1),
				vertexPositionsList.get(vertexIndicesList.get(i*3+1)*3+2));
			verticesV[i*3+2] = new Vector3f(
				vertexPositionsList.get(vertexIndicesList.get(i*3+2)*3+0),
				vertexPositionsList.get(vertexIndicesList.get(i*3+2)*3+1),
				vertexPositionsList.get(vertexIndicesList.get(i*3+2)*3+2));

			texCoordsV[i*3+0] = new Vector2f(
				vertexTexCoordsList.get(vertexIndicesList.get(i*3+0)*2+0),
				vertexTexCoordsList.get(vertexIndicesList.get(i*3+0)*2+1));
			texCoordsV[i*3+1] = new Vector2f(
				vertexTexCoordsList.get(vertexIndicesList.get(i*3+1)*2+0),
				vertexTexCoordsList.get(vertexIndicesList.get(i*3+1)*2+1));
			texCoordsV[i*3+2] = new Vector2f(
				vertexTexCoordsList.get(vertexIndicesList.get(i*3+2)*2+0),
				vertexTexCoordsList.get(vertexIndicesList.get(i*3+2)*2+1));

			normalsV[i*3+0] = new Vector3f(
				vertexNormalsList.get(vertexIndicesList.get(i*3+0)*3+0),
				vertexNormalsList.get(vertexIndicesList.get(i*3+0)*3+1),
				vertexNormalsList.get(vertexIndicesList.get(i*3+0)*3+2));
			normalsV[i*3+1] = new Vector3f(
				vertexNormalsList.get(vertexIndicesList.get(i*3+1)*3+0),
				vertexNormalsList.get(vertexIndicesList.get(i*3+1)*3+1),
				vertexNormalsList.get(vertexIndicesList.get(i*3+1)*3+2));
			normalsV[i*3+2] = new Vector3f(
				vertexNormalsList.get(vertexIndicesList.get(i*3+2)*3+0),
				vertexNormalsList.get(vertexIndicesList.get(i*3+2)*3+1),
				vertexNormalsList.get(vertexIndicesList.get(i*3+2)*3+2));

			boneWeightsV[i*3+0] = new Vector3f(
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+0)*3+0),
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+0)*3+1),
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+0)*3+2));
			boneWeightsV[i*3+1] = new Vector3f(
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+1)*3+0),
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+1)*3+1),
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+1)*3+2));
			boneWeightsV[i*3+2] = new Vector3f(
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+2)*3+0),
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+2)*3+1),
				vertexBoneWeightsList.get(vertexIndicesList.get(i*3+2)*3+2));

			boneIndicesV[i*3+0] = new Vector3f(
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+0)*3+0),
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+0)*3+1),
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+0)*3+2));
			boneIndicesV[i*3+1] = new Vector3f(
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+1)*3+0),
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+1)*3+1),
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+1)*3+2));
			boneIndicesV[i*3+2] = new Vector3f(
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+2)*3+0),
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+2)*3+1),
				vertexBoneIndicesList.get(vertexIndicesList.get(i*3+2)*3+2));
		}

		setNumVertices(triCount*3);
		setVertices(verticesV);
		setTexCoords(texCoordsV);
		setNormals(normalsV);
		setBoneWeights(boneWeightsV);
		setBoneIndices(boneIndicesV);
		setWindingOrderCCW(true);
		setAnimated(true);

		// --------------- NEXT, READ IN SKELETON -----------------

		try
		{	InputStream input = new FileInputStream(new File("assets/animations/" + skelPath));
			BufferedReader br = new BufferedReader(new InputStreamReader(input));

			if ((line = br.readLine()) != null)
			{	String[] header = line.split("\t");
				boneCount = Integer.parseInt(header[0]);
			}

			// Read each bone
			int matrixBindPoseBoneCount = 0;
			for (int i = 0; i < boneCount && ((line = br.readLine()) != null); i++)
			{	String[] bone = line.split("\t");

				// First string is the bone's name
				boneNamesList.add(bone[0]);

				// Next float is the bone's length
				boneLengthsList.add(Float.parseFloat(bone[1]));

				// Next 3 floats is the bone's rest location
				boneRestLocationsList.add(Float.parseFloat(bone[2]));
				boneRestLocationsList.add(Float.parseFloat(bone[3]));
				boneRestLocationsList.add(Float.parseFloat(bone[4]));

				// Next 4 floats is the bone's rest rotation
				boneRestRotationsList.add(Float.parseFloat(bone[5]));
				boneRestRotationsList.add(Float.parseFloat(bone[6]));
				boneRestRotationsList.add(Float.parseFloat(bone[7]));
				boneRestRotationsList.add(Float.parseFloat(bone[8]));

				// Last int is the bone's parent's index
				boneParentsList.add(Integer.parseInt(bone[9]));

				// Optional MATRIX4 exporter extension:
				// columns 10..25 store this bone's inverse bind matrix in model space.
				if (bone.length >= 26)
				{	for (int j = 10; j < 26; j++)
						boneInverseBindMatricesList.add(Float.parseFloat(bone[j]));
					matrixBindPoseBoneCount++;
				}
			}
			hasMatrixBindPose = matrixBindPoseBoneCount == boneCount;
			if (hasMatrixBindPose)
			{	boneInverseBindMatrices = new tage.rml.Matrix4[boneCount];
				boneBindMatrices = new tage.rml.Matrix4[boneCount];
				for (int i = 0; i < boneCount; i++)
				{	float[] values = new float[16];
					for (int j = 0; j < 16; j++)
						values[j] = boneInverseBindMatricesList.get(i * 16 + j);
					boneInverseBindMatrices[i] = tage.rml.Matrix4f.createFrom(values);
					boneBindMatrices[i] = boneInverseBindMatrices[i].inverse();
				}
			}
		}
		catch (IOException e)
		{	throw new RuntimeException(e);
		}

		repairRigifyCompactParents();

		skel.setBoneCount(boneCount);
		skel.setBoneNames(toStringArray(boneNamesList));
		skel.setBoneLengthsBuffer(toFloatBuffer(boneLengthsList));
		skel.setBoneRestLocationsBuffer(toFloatBuffer(boneRestLocationsList));
		skel.setBoneRestRotationsBuffer(toFloatBuffer(boneRestRotationsList));
		skel.setBoneParentsBuffer(toIntBuffer(boneParentsList));
		safeReset();
	}

	// ------------- READ IN AN ANIMATION --------------

	/** Specifies a string name for an animation, and the file containing the animation (with extension "rka"). */
	public void loadAnimation(String animationName, String animationPath)
	{	int frameCount = 0;
		int animationBoneCount = 0;
		int skeletonBoneCount = skel.getBoneCount();
		List<List<Float>> framesList = new ArrayList<List<Float>>();
		boolean matrixFrames = false;
		int valuesPerBone = 10;
		String line;
		Animation anim = new Animation();
		try
		{	InputStream input = new FileInputStream(new File("assets/animations/" + animationPath));
			BufferedReader br = new BufferedReader(new InputStreamReader(input));

			if ((line = br.readLine()) != null)
			{	String[] header = line.split("\t");
				animationBoneCount = Integer.parseInt(header[0]);
				frameCount = Integer.parseInt(header[1]);
				matrixFrames = header.length >= 3 && header[2].equalsIgnoreCase("MATRIX4");
				valuesPerBone = matrixFrames ? 16 : 10;
				if (animationBoneCount != skeletonBoneCount)
				{	System.out.println("WARNING: skipped animation \"" + animationName + "\" from file \""
						+ animationPath + "\" because it has " + animationBoneCount
						+ " bones but the loaded skeleton has " + skeletonBoneCount + ".");
					return;
				}
				if (matrixFrames && !hasMatrixBindPose)
				{	System.out.println("WARNING: skipped MATRIX4 animation \"" + animationName + "\" from file \""
						+ animationPath + "\" because the loaded skeleton does not include inverse bind matrices.");
					return;
				}
				if (!matrixFrames && isRigifyLikeSkeleton())
				{	if (!warnedLegacyRigifyAnimation)
					{	warnedLegacyRigifyAnimation = true;
						System.out.println("WARNING: skipped legacy Rigify animation data for skeleton with "
							+ skeletonBoneCount + " bones. Re-export the .rks with inverse bind matrices and "
							+ "the .rka files as MATRIX4 animations.");
					}
					return;
				}
				boneCount = animationBoneCount;
			}
			// Iterate through each frame
			for (int i = 0; i < frameCount; i++)
			{	framesList.add( new ArrayList<Float>() );

				// Iterate through each bone
				for (int j = 0; j < animationBoneCount && ((line = br.readLine()) != null); j++)
				{	String[] boneTransform = line.split("\t");

					// In the animation file, each line is a single bone's frame's transform.
					// Legacy animations use 10 values. MATRIX4 animations use 16 matrix values.
					for (int k = 0; k < valuesPerBone; k++)
						framesList.get(i).add(Float.parseFloat(boneTransform[k]));
				}
			}
		}
		catch (IOException e)
		{	throw new RuntimeException(e);
		}

		anim.setBoneCount(animationBoneCount);
		anim.setFrameCount(frameCount);
		anim.setMatrixFrames(matrixFrames);
		for (List<Float> frame : framesList) { anim.appendFrame(toFloatBuffer(frame)); }

		animationsList.put(animationName, anim);

		framesList.clear();
	}

	// ---------- ANIMATION ACCESSORS ---------------

	/** Returns a reference to the animation with the specified name. */
	public Animation getAnimation(String s) { return animationsList.get(s); }

	/** Returns the number of animations ("rka" files) that have been loaded for this shape. */
	public int getNumberOfAnimations() { return animationsList.size(); }

	protected HashMap<String, Animation> getAllAnimations() { return animationsList; }

	// ---------- PERFORM ANIMATION ----------------

	/** The game application should call this function at each frame (i.e., in update()) for each animated shape. */
	public void updateAnimation()
	{	update();
		updateCurrentPoseMatrices();
	}

	// This method calculates the skinning matrices for the current animation pose.

	private void updateCurrentPoseMatrices()
	{	if (curAnimation != null && curAnimation.usesMatrixFrames() && hasMatrixBindPose)
		{	int poseBoneCount = java.lang.Math.min(boneCount, MAX_SKIN_BONES);
			if (isRigifyLikeSkeleton())
				updateMatrixPoseSkinningWithoutRigifyStretch(poseBoneCount);
			else
				updateMatrixPoseSkinning(poseBoneCount);
			return;
		}

		int poseBoneCount = java.lang.Math.min(boneCount, MAX_SKIN_BONES);
		for (int i = 0; i < poseBoneCount; i++)
		{	tage.rml.Matrix4 mat;

			// 1) get inverse of bone's local-space to model space
			mat = getBoneLocal2ModelSpaceTransform(i).inverse();

			int curBone = i;
			while (curBone != -1)
			{
				// 2) transforming based on bone's current frame transform
				mat = getBoneCurLocalTransform(curBone).mult(mat);

				// 3) transforming based on its position relative to its parent
				mat = getBoneRestTransformRel2Parent(curBone).mult(mat);

				curBone = skel.getBoneParentIndex(curBone);
			}
			curSkinMatrices[i] = mat;
			curSkinMatricesIT[i] = curSkinMatrices[i].inverse().transpose().toMatrix3();
		}
	}

	private void updateMatrixPoseSkinning(int poseBoneCount)
	{	for (int i = 0; i < poseBoneCount; i++)
		{	tage.rml.Matrix4 poseMatrix = curAnimation.getFrameBoneMatrix(curAnimFrame, i);
			curSkinMatrices[i] = poseMatrix.mult(boneInverseBindMatrices[i]);
			curSkinMatricesIT[i] = curSkinMatrices[i].inverse().transpose().toMatrix3();
		}
	}

	private void updateMatrixPoseSkinningWithoutRigifyStretch(int poseBoneCount)
	{	tage.rml.Matrix4[] noStretchPoseMatrices = new tage.rml.Matrix4[poseBoneCount];

		for (int i = 0; i < poseBoneCount; i++)
		{	tage.rml.Matrix4 poseMatrix = removeMatrixScaleAndShear(curAnimation.getFrameBoneMatrix(curAnimFrame, i));
			int parentIndex = skel.getBoneParentIndex(i);
			if (parentIndex >= 0 && parentIndex < poseBoneCount && noStretchPoseMatrices[parentIndex] != null
					&& boneBindMatrices != null && parentIndex < boneInverseBindMatrices.length && i < boneBindMatrices.length)
			{	poseMatrix = rebuildPoseTranslationFromRestParent(
					poseMatrix,
					noStretchPoseMatrices[parentIndex],
					boneInverseBindMatrices[parentIndex],
					boneBindMatrices[i]);
			}

			noStretchPoseMatrices[i] = poseMatrix;
			curSkinMatrices[i] = poseMatrix.mult(boneInverseBindMatrices[i]);
			curSkinMatricesIT[i] = curSkinMatrices[i].inverse().transpose().toMatrix3();
		}
	}

	private tage.rml.Matrix4 rebuildPoseTranslationFromRestParent(
			tage.rml.Matrix4 poseMatrix,
			tage.rml.Matrix4 parentPoseMatrix,
			tage.rml.Matrix4 parentInverseBindMatrix,
			tage.rml.Matrix4 bindMatrix)
	{	tage.rml.Matrix4 restRelativeToParent = parentInverseBindMatrix.mult(bindMatrix);
		tage.rml.Vector4 restOffset = tage.rml.Vector4f.createFrom(
			restRelativeToParent.value(0, 3),
			restRelativeToParent.value(1, 3),
			restRelativeToParent.value(2, 3),
			1.0f);
		tage.rml.Vector4 posePosition = parentPoseMatrix.mult(restOffset);
		return replaceMatrixTranslation(poseMatrix, posePosition.x(), posePosition.y(), posePosition.z());
	}

	//====================================================
	//          Useful Transformations Matrix Maths
	//====================================================

	// Returns the ith bone's local space to model space (when all bones are in rest pose)

	protected tage.rml.Matrix4 getBoneLocal2ModelSpaceTransform(int i)
	{	tage.rml.Matrix4 mat = tage.rml.Matrix4f.createIdentityMatrix();

		int curBone = i;
		// Iterate until we reach root bone.
		while (curBone != -1)
		{	mat = getBoneRestTransformRel2Parent(curBone).mult(mat);
			// Move on to curBone's parent.
			curBone = skel.getBoneParentIndex(curBone);
		}
		return mat;
	}

	// Returns the ith bone's rest transform relative to its parent-space

	protected tage.rml.Matrix4 getBoneRestTransformRel2Parent(int i)
	{	tage.rml.Matrix4 mat = tage.rml.Matrix4f.createIdentityMatrix();

		// 1) First apply ith bone's rest rotation
		tage.rml.Quaternion restRot = skel.getBoneRestRot(i);
		mat = tage.rml.Matrix4f.createRotationFrom(restRot.angle(),getQuatAxis(restRot)).mult(mat);

		// 2) Then apply ith bone's rest location
		tage.rml.Vector3 restLoc = skel.getBoneRestLoc(i);
		mat = tage.rml.Matrix4f.createTranslationFrom(restLoc).mult(mat);

		// 3) Then apply parent bone's length in vertical direction (is the y-axis correct?)
		// Get the ith bone's parent's index
		int parentIndex = skel.getBoneParentIndex(i);
		//If parent index is -1, this bone is the root bone
		if (parentIndex == -1)
			return mat;

		float parentBoneLength = skel.getBoneLength(parentIndex);

		// For the bone's local space, y is up along the bone
		// mat = Matrix4f.createTranslationFrom(0,0,-parentBoneLength).mult(mat);
		mat = tage.rml.Matrix4f.createTranslationFrom(0,parentBoneLength,0).mult(mat);

		return mat;
	}

	// Returns the ith bone's current local transform for the current animation frame

	protected tage.rml.Matrix4 getBoneCurLocalTransform(int i)
	{	if (curAnimation == null)
			return tage.rml.Matrix4f.createIdentityMatrix();

		//TODO: use curLerpedAnimFrame to interpolate these three variables

		tage.rml.Vector3 scale = curAnimation.getFrameBoneScl(curAnimFrame,i);
		tage.rml.Quaternion rot = curAnimation.getFrameBoneRot(curAnimFrame, i);
		tage.rml.Vector3 loc = curAnimation.getFrameBoneLoc(curAnimFrame, i);

		tage.rml.Matrix4 mat;
		// 1) Apply scale 1st
		mat = tage.rml.Matrix4f.createScalingFrom(scale);
		// 2) Apply rotation 2nd
		mat = tage.rml.Matrix4f.createRotationFrom(rot.angle(),getQuatAxis(rot)).mult(mat);
		// 3) Apply translation 3rd
		mat = tage.rml.Matrix4f.createTranslationFrom(loc).mult(mat);
		return mat;
	}

	private tage.rml.Matrix4 removeMatrixScaleAndShear(tage.rml.Matrix4 matrix)
	{	float x0 = matrix.value(0, 0);
		float x1 = matrix.value(1, 0);
		float x2 = matrix.value(2, 0);
		float y0 = matrix.value(0, 1);
		float y1 = matrix.value(1, 1);
		float y2 = matrix.value(2, 1);
		float originalZ0 = matrix.value(0, 2);
		float originalZ1 = matrix.value(1, 2);
		float originalZ2 = matrix.value(2, 2);

		float xLen = vectorLength(x0, x1, x2);
		if (xLen < 0.00001f)
		{	x0 = 1.0f; x1 = 0.0f; x2 = 0.0f;
		}
		else
		{	x0 /= xLen; x1 /= xLen; x2 /= xLen;
		}

		float xyDot = dot(x0, x1, x2, y0, y1, y2);
		y0 -= x0 * xyDot;
		y1 -= x1 * xyDot;
		y2 -= x2 * xyDot;

		float yLen = vectorLength(y0, y1, y2);
		if (yLen < 0.00001f)
		{	if (java.lang.Math.abs(x0) < 0.9f)
			{	y0 = 1.0f; y1 = 0.0f; y2 = 0.0f;
			}
			else
			{	y0 = 0.0f; y1 = 1.0f; y2 = 0.0f;
			}
			xyDot = dot(x0, x1, x2, y0, y1, y2);
			y0 -= x0 * xyDot;
			y1 -= x1 * xyDot;
			y2 -= x2 * xyDot;
			yLen = vectorLength(y0, y1, y2);
		}
		y0 /= yLen; y1 /= yLen; y2 /= yLen;

		float z0 = x1 * y2 - x2 * y1;
		float z1 = x2 * y0 - x0 * y2;
		float z2 = x0 * y1 - x1 * y0;
		float zLen = vectorLength(z0, z1, z2);
		if (zLen < 0.00001f)
		{	z0 = originalZ0; z1 = originalZ1; z2 = originalZ2;
			zLen = vectorLength(z0, z1, z2);
		}
		if (zLen < 0.00001f)
		{	z0 = 0.0f; z1 = 0.0f; z2 = 1.0f;
		}
		else
		{	z0 /= zLen; z1 /= zLen; z2 /= zLen;
		}

		if (dot(z0, z1, z2, originalZ0, originalZ1, originalZ2) < 0.0f)
		{	z0 = -z0; z1 = -z1; z2 = -z2;
		}

		y0 = z1 * x2 - z2 * x1;
		y1 = z2 * x0 - z0 * x2;
		y2 = z0 * x1 - z1 * x0;

		float[] values =
		{	x0, x1, x2, 0.0f,
			y0, y1, y2, 0.0f,
			z0, z1, z2, 0.0f,
			matrix.value(0, 3), matrix.value(1, 3), matrix.value(2, 3), matrix.value(3, 3)
		};
		return tage.rml.Matrix4f.createFrom(values);
	}

	private tage.rml.Matrix4 replaceMatrixTranslation(tage.rml.Matrix4 matrix, float x, float y, float z)
	{	float[] values =
		{	matrix.value(0, 0), matrix.value(1, 0), matrix.value(2, 0), 0.0f,
			matrix.value(0, 1), matrix.value(1, 1), matrix.value(2, 1), 0.0f,
			matrix.value(0, 2), matrix.value(1, 2), matrix.value(2, 2), 0.0f,
			x, y, z, 1.0f
		};
		return tage.rml.Matrix4f.createFrom(values);
	}

	private float vectorLength(float x, float y, float z)
	{	return (float) java.lang.Math.sqrt(x * x + y * y + z * z);
	}

	private float dot(float ax, float ay, float az, float bx, float by, float bz)
	{	return ax * bx + ay * by + az * bz;
	}

	// Returns the Quaternion's axis, if is not the identity quaternion, else (1,0,0)
	// This is safer than calling q.axis(), as q.axis() ALWAYS attempts to normalize the axis vector.
	// However, if the Quaternion is the identity quaternion (1,0,0,0), it will attempt to normalize a zero-vector
	// Throwing an arithmetic exception.
	// If an arithmetic exception is thrown, I can return an arbitrary axis (namely, (1,0,0))
	// because an identity Quaternion is no rotation about any axis
	// ... and no rotation about (1,0,0) is identical to no rotation about any axis.

	protected tage.rml.Vector3 getQuatAxis(tage.rml.Quaternion q)
	{
		tage.rml.Vector3 axis;
		try
		{	axis = q.axis();
		}
		catch(ArithmeticException e)
		{	axis = tage.rml.Vector3f.createFrom(1,0,0);
		}
		return axis;
	}

	//====================================================
	//              Animation Update Logic
	//====================================================

	private void update()
	{	if (curAnimation != null && !curAnimPaused && curAnimSpeed != 0.0f)
		{	curLerpedAnimFrame += curAnimSpeed;
			curAnimFrame = java.lang.Math.round(curLerpedAnimFrame);

			// Check if the animation is over
			if (curAnimFrame >= curAnimation.getFrameCount() || curAnimFrame < 0)
			{	handleAnimationEnd();
			}
		}
	}

	private void handleAnimationEnd()
	{	curAnimEndTypeCount++;
		// Check if we have exceeded the number of times to perform the end type
		// 0 is loop forever

		if (curAnimEndTypeTotal != 0)
		{	if (curAnimEndTypeCount > curAnimEndTypeTotal)
			{	stopAnimation();
				return;
			}
		}
		switch(curAnimEndtype)
		{
			case NONE:

			// Completely stop the animation
			case STOP:
				stopAnimation();
				break;

			// Freeze model at the last frame
			case PAUSE:
				if (curAnimSpeed > 0.0)
				{	curAnimFrame = curAnimation.getFrameCount() - 1;
					curLerpedAnimFrame = curAnimation.getFrameCount() - 1;
				}
				else if (curAnimSpeed < 0.0)
				{	curAnimFrame = 0;
					curLerpedAnimFrame = 0;
				}
				curAnimSpeed = 0.0f;
				break;

			// Restart the animation at the opposite frame
			case LOOP:
				if (curAnimSpeed > 0.0)
				{	curAnimFrame = 0;
					curLerpedAnimFrame = 0;
				}
				else if (curAnimSpeed < 0.0)
				{	curAnimFrame = curAnimation.getFrameCount() - 1;
					curLerpedAnimFrame = curAnimation.getFrameCount() - 1;
				}
				break;

			// Play the animation backwards
			case PINGPONG:
				if (curAnimSpeed > 0.0)
				{	curAnimFrame = curAnimation.getFrameCount() - 2;
					curLerpedAnimFrame = curAnimation.getFrameCount() - 1;
					curAnimSpeed *= -1f;
				}
				else if (curAnimSpeed < 0.0)
				{	curAnimFrame = 1;
					curLerpedAnimFrame = 1;
					curAnimSpeed *= -1f;
				}
				break;
		}
	}

	// animName: the name of the animation to play
	// animSpeed: the speed at which to play the animation
	//         animSpeed = 1.0 for regular speed
	//         0 < animSpeed < 1.0 for slow motion
	//         animSpeed < 0 for backwards, etc...
	// endtype: What to do when the animation is over
	//        NONE/STOP: stops the animation, revert to default pose
	//        PAUSE: freezes the pose at the last frame of the animation
	//        LOOP: starts the animation from the beginning endTypeCount number of times
	//        PINGPONG: plays the animation backwards back and forth endTypeCount number of times

	/**
	* Play the specified animation at the specified speed, EndType, and number of times.
	* <br>
	* speed == 1.0 for regular, larger for faster, fractional for slower, and negative for backwards.
	* <br>
	* endType can have one of the following values:
	* <ul>
	* <li> NONE/STOP: stops the animation, revert to default pose
	* <li> PAUSE: freezes the pose at the last frame of the animation
	* <li> LOOP: starts the animation from the beginning endTypeCount number of times
	* <li> PINGPONG: plays the animation backwards back and forth endTypeCount number of times
	* </ul>
	*/
	public void playAnimation(String animName, float animSpeed, EndType endType, int endTypeCount)
	{
		Animation anim = animationsList.get(animName);

		// If the animation is not found, return
		if (anim == null) return;

		curAnimation = anim;
		curLerpedAnimFrame = 0;
		curAnimFrame = 0;
		curAnimEndTypeTotal = endTypeCount;
		curAnimEndTypeCount = 0;
		curLerpedAnimFrame = 0;
		curAnimSpeed = animSpeed;
		curAnimEndtype = endType;

		// If speed is negative, play the animation in reverse
		if (curAnimSpeed < 0)
		{	curAnimFrame = anim.getFrameCount() - 1;
			curLerpedAnimFrame = anim.getFrameCount() - 1;
		}

		// Play the anim
		curAnimPaused = false;
	}

	/** freezes a running animation at the last frame displayed */
	public void pauseAnimation() { curAnimPaused = true; }

	/** stops a running or paused animation, returning the object to the default pose */
	public void stopAnimation()
	{	curAnimation = null;
		curAnimEndtype = EndType.NONE;
		curAnimFrame = -1;
		curLerpedAnimFrame = -1;
		curAnimPaused = false;
		curAnimSpeed = 1.0f;
		curAnimEndTypeCount = 0;
		curAnimEndTypeTotal = 0;
	}

	// ---------- OTHER UTILITY FUNCTIONS ----------------

	private static String[] toStringArray(List<String> list)
	{	String[] values = new String[list.size()];
		for (int i = 0; i < values.length; i++)
			values[i] = list.get(i);
		return values;
	}

	private static FloatBuffer toFloatBuffer(List<Float> list)
	{	float[] values = new float[list.size()];
		for (int i = 0; i < values.length; i++)
			values[i] = list.get(i);
		return directFloatBuffer(values);
	}
	private static FloatBuffer directFloatBuffer(float[] values)
	{	return (FloatBuffer) directFloatBuffer(values.length).put(values).rewind();
	}
	private static FloatBuffer directFloatBuffer(int capacity)
	{	return directByteBuffer(capacity * Float.BYTES).asFloatBuffer();
	}

	private static IntBuffer toIntBuffer(List<Integer> list)
	{	int[] values = new int[list.size()];
		for (int i = 0; i < values.length; i++)
			values[i] = list.get(i);
		return (IntBuffer) directIntBuffer(values);
	}
	private static IntBuffer directIntBuffer(int[] values)
	{	return (IntBuffer) directIntBuffer(values.length).put(values).rewind();
	}
	private static IntBuffer directIntBuffer(int capacity)
	{	return directByteBuffer(capacity * Integer.BYTES).asIntBuffer();
	}

	private static ByteBuffer directByteBuffer(int capacity)
	{	return (ByteBuffer) ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
	}
	private static ByteBuffer directByteBuffer(byte[] values)
	{	return (ByteBuffer) directByteBuffer(values.length).put(values).rewind();
	}

	private void safeReset()
	{	boneNamesList.clear();
		boneLengthsList.clear();
		boneRestRotationsList.clear();
		boneRestLocationsList.clear();
		boneParentsList.clear();
		boneInverseBindMatricesList.clear();
	}

	private void repairRigifyCompactParents()
	{	if (!isRigifyLikeBoneNamesList())
			return;

		HashMap<String, Integer> boneIndices = new HashMap<>();
		for (int i = 0; i < boneNamesList.size(); i++)
			boneIndices.put(boneNamesList.get(i), i);

		String rootName = boneIndices.containsKey("root") ? "root" : null;
		String firstSpineName = boneIndices.containsKey("DEF-spine") ? "DEF-spine" : rootName;
		String lastSpineName = firstSpineName;
		for (String name : boneNamesList)
		{	if (name.equals("DEF-spine") || name.startsWith("DEF-spine."))
				lastSpineName = name;
		}

		setRigifyParentIfPresent(boneIndices, "DEF-spine", rootName);
		for (String side : new String[] {"L", "R"})
		{	setRigifyParentIfPresent(boneIndices, "DEF-pelvis." + side, firstSpineName, rootName);
			setRigifyParentIfPresent(boneIndices, "DEF-thigh." + side, "DEF-pelvis." + side, firstSpineName, rootName);
			setRigifyParentIfPresent(boneIndices, "DEF-thigh." + side + ".001", "DEF-thigh." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-shin." + side, "DEF-thigh." + side + ".001", "DEF-thigh." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-shin." + side + ".001", "DEF-shin." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-foot." + side, "DEF-shin." + side + ".001", "DEF-shin." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-toe." + side, "DEF-foot." + side);

			setRigifyParentIfPresent(boneIndices, "DEF-shoulder." + side, lastSpineName, firstSpineName, rootName);
			setRigifyParentIfPresent(boneIndices, "DEF-upper_arm." + side, "DEF-shoulder." + side, lastSpineName, firstSpineName, rootName);
			setRigifyParentIfPresent(boneIndices, "DEF-upper_arm." + side + ".001", "DEF-upper_arm." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-forearm." + side, "DEF-upper_arm." + side + ".001", "DEF-upper_arm." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-forearm." + side + ".001", "DEF-forearm." + side);
			setRigifyParentIfPresent(boneIndices, "DEF-hand." + side, "DEF-forearm." + side + ".001", "DEF-forearm." + side);
		}
	}

	private void setRigifyParentIfPresent(HashMap<String, Integer> boneIndices, String childName, String... parentNames)
	{	Integer childIndex = boneIndices.get(childName);
		if (childIndex == null)
			return;

		for (String parentName : parentNames)
		{	if (parentName == null)
				continue;
			Integer parentIndex = boneIndices.get(parentName);
			if (parentIndex != null && parentIndex < childIndex)
			{	boneParentsList.set(childIndex, parentIndex);
				return;
			}
		}
	}

	private boolean isRigifyLikeBoneNamesList()
	{	int rigifyNames = 0;
		for (String name : boneNamesList)
		{	if (name == null) continue;
			if (name.startsWith("DEF-") || name.startsWith("ORG-") || name.startsWith("MCH-")
					|| name.startsWith("VIS_") || name.contains("_ik") || name.contains("_fk")
					|| name.startsWith("tweak_"))
				rigifyNames++;
		}
		return rigifyNames >= 8;
	}

	private boolean isRigifyLikeSkeleton()
	{	String[] names = skel.getBoneNames();
		if (names == null) return false;
		int rigifyNames = 0;
		for (String name : names)
		{	if (name == null) continue;
			if (name.startsWith("DEF-") || name.startsWith("ORG-") || name.startsWith("MCH-")
					|| name.startsWith("VIS_") || name.contains("_ik") || name.contains("_fk")
					|| name.startsWith("tweak_"))
				rigifyNames++;
		}
		return rigifyNames >= 8;
	}

	// ------------- ACCESSORS -----------------

	/** for engine use only. */
	public tage.rml.Matrix4[] getPoseSkinMatrices() { return curSkinMatrices; }
	/** for engine use only. */
	public tage.rml.Matrix3[] getPoseSkinMatricesIT() { return curSkinMatricesIT; }
	/** for engine use only. */
	public int getBoneCount() { return boneCount; }
}
