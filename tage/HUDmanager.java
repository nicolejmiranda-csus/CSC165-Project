package tage;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.gl2.GLUT;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.joml.*;

/**
 * Manages fixed HUD strings and optional world-positioned HUD labels, implemented as GLUT strings.
 * This class is instantiated automatically by the engine.
 * Note that this class utilizes deprecated OpenGL functionality.
 * <p>
 * The available fonts are:
 * <ul>
 * <li> GLUT.BITMAP_8_BY_13
 * <li> GLUT.BITMAP_9_BY_15
 * <li> GLUT.BITMAP_TIMES_ROMAN_10
 * <li> GLUT.BITMAP_TIMES_ROMAN_24
 * <li> GLUT.BITMAP_HELVETICA_10
 * <li> GLUT.BITMAP_HELVETICA_12
 * <li> GLUT.BITMAP_HELVETICA_18
 * </ul>
 * @author Scott Gordon
 * @author Givin Yang
 * @author Nicole Joshua Espinoza
 */

public class HUDmanager
{	private GLCanvas myCanvas;
	private GLUT glut = new GLUT();
	private Engine engine;

	private String HUD1string, HUD2string;
	private float[] HUD1color, HUD2color;
	private int HUD1font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD2font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD1x, HUD1y, HUD2x, HUD2y;

	//HUD3, HUD4, and HUD5
	private String HUD3string, HUD4string, HUD5string;
	private float[] HUD3color, HUD4color, HUD5color;
	private int HUD3font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD4font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD5font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD3x, HUD3y, HUD4x, HUD4y, HUD5x, HUD5y;
	private ArrayList<WorldHUDString> worldHUDStrings = new ArrayList<WorldHUDString>();

	// The constructor is called by the engine, and should not be called by the game application.
	// It initializes the two HUDs to empty strings.

	// It now initializes another two HUDs to empty strings.

	protected HUDmanager(Engine e)
	{	engine = e;
		HUD1string = "";
		HUD2string = "";
		HUD1color = new float[3];
		HUD2color = new float[3];

		//initialize HUD3, HUD4, and HUD5
		HUD3string = "";
		HUD4string = "";
		HUD5string = "";
		HUD3color = new float[3];
		HUD4color = new float[3];
		HUD5color = new float[3];
	}

	protected void setGLcanvas(GLCanvas g) { myCanvas = g; }

	protected void drawHUDs(Matrix4f viewMatrix, Matrix4f projectionMatrix, Viewport viewport, int canvasWidth, int canvasHeight)
	{	GL4 gl4 = (GL4) GLContext.getCurrentGL();
		GL4bc gl4bc = (GL4bc) gl4;

		gl4.glUseProgram(0);
		drawWorldHUDStrings(gl4bc, viewMatrix, projectionMatrix, viewport, canvasWidth, canvasHeight);

		gl4.glDisable(GL_DEPTH_TEST);
		gl4bc.glColor3f(HUD1color[0], HUD1color[1], HUD1color[2]);
		gl4bc.glWindowPos2d (HUD1x, HUD1y);
		glut.glutBitmapString(HUD1font, HUD1string);

		gl4bc.glColor3f(HUD2color[0], HUD2color[1], HUD2color[2]);
		gl4bc.glWindowPos2d (HUD2x, HUD2y);
		glut.glutBitmapString (HUD2font, HUD2string);

		//HUD3, HUD4, and HUD5
		gl4bc.glColor3f(HUD3color[0], HUD3color[1], HUD3color[2]);
		gl4bc.glWindowPos2d (HUD3x, HUD3y);
		glut.glutBitmapString (HUD3font, HUD3string);

		gl4bc.glColor3f(HUD4color[0], HUD4color[1], HUD4color[2]);
		gl4bc.glWindowPos2d (HUD4x, HUD4y);
		glut.glutBitmapString (HUD4font, HUD4string);

		gl4bc.glColor3f(HUD5color[0], HUD5color[1], HUD5color[2]);
		gl4bc.glWindowPos2d (HUD5x, HUD5y);
		glut.glutBitmapString (HUD5font, HUD5string);
		gl4.glEnable(GL_DEPTH_TEST);
	}

	private void drawWorldHUDStrings(GL4bc gl4bc, Matrix4f viewMatrix, Matrix4f projectionMatrix, Viewport viewport, int canvasWidth, int canvasHeight)
	{	if (viewMatrix == null || projectionMatrix == null || viewport == null || worldHUDStrings.isEmpty()) return;
		int vx = (int)(viewport.getRelativeLeft() * canvasWidth);
		int vy = (int)(viewport.getRelativeBottom() * canvasHeight);
		int vw = (int)(viewport.getRelativeWidth() * canvasWidth);
		int vh = (int)(viewport.getRelativeHeight() * canvasHeight);
		if (vw <= 0 || vh <= 0) return;

		Matrix4f viewProjection = new Matrix4f(projectionMatrix).mul(viewMatrix);
		gl4bc.glEnable(GL_DEPTH_TEST);
		gl4bc.glDepthFunc(GL_LEQUAL);
		gl4bc.glDepthMask(false);
		for (WorldHUDString label : worldHUDStrings)
		{	if (label == null || label.string == null || label.string.isEmpty() || label.location == null) continue;
			Vector4f clip = new Vector4f(label.location.x(), label.location.y(), label.location.z(), 1.0f);
			viewProjection.transform(clip);
			if (clip.w() <= 0.0001f) continue;
			float ndcX = clip.x() / clip.w();
			float ndcY = clip.y() / clip.w();
			float ndcZ = clip.z() / clip.w();
			if (ndcX < -1.0f || ndcX > 1.0f || ndcY < -1.0f || ndcY > 1.0f || ndcZ < -1.0f || ndcZ > 1.0f) continue;

			int x = vx + java.lang.Math.round((ndcX + 1.0f) * 0.5f * vw);
			int y = vy + java.lang.Math.round((ndcY + 1.0f) * 0.5f * vh);
			int width = glut.glutBitmapLength(label.font, label.string);
			double windowDepth = (ndcZ + 1.0) * 0.5;
			int sampleX = java.lang.Math.max(vx, java.lang.Math.min(vx + vw - 1, x));
			int sampleY = java.lang.Math.max(vy, java.lang.Math.min(vy + vh - 1, y));
			if (isWorldPointOccluded(gl4bc, sampleX, sampleY, windowDepth)) continue;
			gl4bc.glColor3f(label.color[0], label.color[1], label.color[2]);
			gl4bc.glWindowPos3d(x - width / 2, y, windowDepth);
			glut.glutBitmapString(label.font, label.string);
		}
		gl4bc.glDepthMask(true);
	}

	private boolean isWorldPointOccluded(GL4bc gl4bc, int x, int y, double labelDepth)
	{	FloatBuffer depth = FloatBuffer.allocate(1);
		gl4bc.glReadPixels(x, y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, depth);
		float sceneDepth = depth.get(0);
		return sceneDepth < labelDepth - 0.002;
	}

	/** sets HUD #1 to the specified text string, color, and location */
	public void setHUD1(String string, Vector3f color, int x, int y)
	{	HUD1string = string;
		HUD1color[0]=color.x(); HUD1color[1]=color.y(); HUD1color[2]=color.z();
		HUD1x = x;
		HUD1y = y;
	}

	/** sets HUD #2 to the specified text string, color, and location */
	public void setHUD2(String string, Vector3f color, int x, int y)
	{	HUD2string = string;
		HUD2color[0]=color.x(); HUD2color[1]=color.y(); HUD2color[2]=color.z();
		HUD2x = x;
		HUD2y = y;
	}

	//HUD3, HUD4, and HUD5
	/** sets HUD #3 to the specified text string, color, and location */
	public void setHUD3(String string, Vector3f color, int x, int y)
	{	HUD3string = string;
		HUD3color[0]=color.x(); HUD3color[1]=color.y(); HUD3color[2]=color.z();
		HUD3x = x;
		HUD3y = y;
	}

	/** sets HUD #4 to the specified text string, color, and location */
	public void setHUD4(String string, Vector3f color, int x, int y)
	{	HUD4string = string;
		HUD4color[0]=color.x(); HUD4color[1]=color.y(); HUD4color[2]=color.z();
		HUD4x = x;
		HUD4y = y;
	}

	/** sets HUD #5 to the specified text string, color, and location */
	public void setHUD5(String string, Vector3f color, int x, int y)
	{	HUD5string = string;
		HUD5color[0]=color.x(); HUD5color[1]=color.y(); HUD5color[2]=color.z();
		HUD5x = x;
		HUD5y = y;
	}

	/** removes all world-positioned HUD labels before the next frame is drawn */
	public void clearWorldHUDStrings()
	{	worldHUDStrings.clear();
	}

	/**
	 * Adds a HUD label that is projected from a 3D world location onto the main viewport.
	 * This is useful for floating names or markers above GameObjects; labels are depth-tested
	 * against the rendered scene so nearer world geometry can occlude them.
	 */
	public void addWorldHUDString(String string, Vector3f worldLocation, Vector3f color, int font)
	{	if (string == null || worldLocation == null || color == null) return;
		worldHUDStrings.add(new WorldHUDString(string, worldLocation, color, font));
	}

	/** sets HUD #1 font - available fonts are listed above. */
	public void setHUD1font(int font) { HUD1font = font; }

	/** sets HUD #2 font - available fonts are listed above. */
	public void setHUD2font(int font) { HUD2font = font; }

	//HUD3, HUD4, and HUD5
	/** sets HUD #3 font - available fonts are listed above. */
	public void setHUD3font(int font) { HUD3font = font; }

	/** sets HUD #4 font - available fonts are listed above. */
	public void setHUD4font(int font) { HUD4font = font; }

	/** sets HUD #5 font - available fonts are listed above. */
	public void setHUD5font(int font) { HUD5font = font; }

	private static class WorldHUDString
	{	private String string;
		private Vector3f location;
		private float[] color;
		private int font;

		private WorldHUDString(String s, Vector3f l, Vector3f c, int f)
		{	string = s;
			location = new Vector3f(l);
			color = new float[] { c.x(), c.y(), c.z() };
			font = f;
		}
	}
}
