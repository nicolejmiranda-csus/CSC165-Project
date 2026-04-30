package tage.shapes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.util.ArrayList;
import org.joml.*;
import tage.*;

/**
 * Supports importing OBJ model files into the game.
 * <p>
 * There are tight restrictions on which OBJ files are supported.
 * They must have the following characteristics:
 * <ul>
 * <li> vertices must be triangulated since this importer does not support quads
 * <li> texture coordinates must be present so the model must be UV unwrapped
 * <li> normal vectors must be present
 * <li> must be a single object not a composite of multiple objects
 * <li> only v vt vn and f tags are read and all other tags are ignored
 * <li> all f tags must be of the form f xxx/xxx/xxx xxx/xxx/xxx xxx/xxx/xxx
 * <li> associated material file is ignored so use the material accessor functions instead
 * </ul>
 * <p>
 * If you have a model that fails one of the above restrictions, you may need to load it into
 * a tool such as Blender, and export it as an OBJ file that meets all of the above.
 * <p>
 * This tool is described in Chapter 6 of Computer Graphics Programming in OpenGL with Java.
 * @author Scott Gordon
 */
public class ImportedModel extends ObjShape
{
	private Vector3f[] verticesV;
	private Vector2f[] texCoordsV;
	private Vector3f[] normalsV;
	private int numVerts;

	/** Use this constructor to read in an OBJ file with the specified file name, from the models folder. */
	public ImportedModel(String filename)
	{	super();
		importFrom(filename, "assets/models/");
	}

	/** Use this constructor to read in an OBJ file with the specified file name, from the specified folder. */
	public ImportedModel(String filename, String location)
	{	super();
		importFrom(filename, location);
	}

	// Importer described in the CSC 155 textbook
	private void importFrom(String filename, String location)
	{	ModelImporter modelImporter = new ModelImporter();
		try
		{	modelImporter.parseOBJ(location + filename);
			numVerts      = modelImporter.getNumVertices();
			super.setNumVertices(numVerts);
			float[] verts = modelImporter.getVertices();
			float[] tcs   = modelImporter.getTextureCoordinates();
			float[] norm  = modelImporter.getNormals();

			verticesV  = new Vector3f[numVerts];
			texCoordsV = new Vector2f[numVerts];
			normalsV   = new Vector3f[numVerts];

			for(int i=0; i<verticesV.length; i++)
			{	verticesV[i] = new Vector3f();
				verticesV[i].set(verts[i*3], verts[i*3+1], verts[i*3+2]);
				texCoordsV[i] = new Vector2f();
				texCoordsV[i].set(tcs[i*2], tcs[i*2+1]);
				normalsV[i] = new Vector3f();
				normalsV[i].set(norm[i*3], norm[i*3+1], norm[i*3+2]);
			}
		} catch (IOException e)
		{ e.printStackTrace();
		}
		setNumVertices(this.getNumVertices());
		setVertices(this.getVerticesVector());
		setTexCoords(this.getTexCoordsVector());
		setNormals(this.getNormalsVector());
		setWindingOrderCCW(true);
	}

	// Methods below are for engine use only
	protected Vector3f[] getVerticesVector() { return verticesV; }
	protected Vector2f[] getTexCoordsVector() { return texCoordsV; }
	protected Vector3f[] getNormalsVector() { return normalsV; }

	private class ModelImporter
	{	// Values read from the OBJ file
		private ArrayList<Float> vertVals = new ArrayList<Float>();
		private ArrayList<Float> triangleVerts = new ArrayList<Float>();
		private ArrayList<Float> textureCoords = new ArrayList<Float>();

		// Values stored as final vertex attributes
		private ArrayList<Float> stVals = new ArrayList<Float>();
		private ArrayList<Float> normals = new ArrayList<Float>();
		private ArrayList<Float> normVals = new ArrayList<Float>();

		protected void parseOBJ(String filename) throws IOException
		{	InputStream input = new FileInputStream(new File(filename));
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String line;
			while ((line = br.readLine()) != null)
			{	line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				if(line.startsWith("v ")) // Vertex position v case
			{	for(String s : (line.substring(2).trim()).split("\\s+"))
			{	vertVals.add(Float.valueOf(s));
			}	}
			else if(line.startsWith("vt")) // Texture coordinates vt case
			{	for(String s : (line.substring(3).trim()).split("\\s+"))
			{	stVals.add(Float.valueOf(s));
			}	}
			else if(line.startsWith("vn")) // Vertex normals vn case
			{	for(String s : (line.substring(3).trim()).split("\\s+"))
			{	normVals.add(Float.valueOf(s));
			}	}
			else if(line.startsWith("f")) // Triangle faces f case
			{	for(String s : (line.substring(2).trim()).split("\\s+"))
			{	String[] faceRefs = s.split("/", -1);
				String v = faceRefs.length > 0 ? faceRefs[0] : "";
				String vt = faceRefs.length > 1 ? faceRefs[1] : "";
				String vn = faceRefs.length > 2 ? faceRefs[2] : "";

				// OBJ indices are one based so subtract one before indexing arrays
				int vertRef = (resolveObjIndex(v, vertVals.size()/3)-1)*3;

				triangleVerts.add(vertVals.get(vertRef));
				triangleVerts.add(vertVals.get((vertRef)+1));
				triangleVerts.add(vertVals.get((vertRef)+2));

				if (vt.isEmpty() || stVals.isEmpty()) {
					textureCoords.add(generatedU(vertRef));
					textureCoords.add(generatedV(vertRef));
				} else {
					int tcRef = (resolveObjIndex(vt, stVals.size()/2)-1)*2;
					textureCoords.add(stVals.get(tcRef));
					textureCoords.add(stVals.get(tcRef+1));
				}

				if (vn.isEmpty() || normVals.isEmpty()) {
					normals.add(0.0f);
					normals.add(1.0f);
					normals.add(0.0f);
				} else {
					int normRef = (resolveObjIndex(vn, normVals.size()/3)-1)*3;
					normals.add(normVals.get(normRef));
					normals.add(normVals.get(normRef+1));
					normals.add(normVals.get(normRef+2));
				}
			}	}	}
			input.close();
		}

		private int resolveObjIndex(String value, int elementCount)
		{	int index = Integer.valueOf(value);
			return index < 0 ? elementCount + index + 1 : index;
		}

		private float generatedU(int vertRef)
		{	return vertVals.get(vertRef) * 0.25f;
		}

		private float generatedV(int vertRef)
		{	return vertVals.get(vertRef + 2) * 0.25f;
		}

		protected int getNumVertices() { return (triangleVerts.size()/3); }

		protected float[] getVertices()
		{	float[] p = new float[triangleVerts.size()];
			for(int i = 0; i < triangleVerts.size(); i++)
			{	p[i] = triangleVerts.get(i);
			}
			return p;
		}

		protected float[] getTextureCoordinates()
		{	float[] t = new float[(textureCoords.size())];
			for(int i = 0; i < textureCoords.size(); i++)
			{	t[i] = textureCoords.get(i);
			}
			return t;
		}

		protected float[] getNormals()
		{	float[] n = new float[(normals.size())];
			for(int i = 0; i < normals.size(); i++)
			{	n[i] = normals.get(i);
			}
			return n;
		}
	}
}
