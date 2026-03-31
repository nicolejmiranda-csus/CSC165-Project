package tage.shapes;

import tage.*;
import org.joml.*;

/**
 * ManualQuad is a manually-defined quad (rectangle) mesh built from two triangles.
 *
 * <p>The quad is centered at the origin in the XY plane and faces the +Z direction. Vertex data
 * is provided as indexed arrays (positions, texture coordinates, and normals), then expanded into
 * a 6-vertex triangle list via the index order.</p>
 */
public class ManualQuad extends ManualObject
{
    // Four corners of a single flat quad
    private Vector3f[] vertices = new Vector3f[4];

    // UVs cover the whole texture once from 0 to 1
    private Vector2f[] texcoords = new Vector2f[4];

    // Flat lighting uses the same normal for every corner
    private Vector3f[] normals = new Vector3f[4];

    // Two triangles that cover the quad using the four corners
    private int[] indices = new int[]
            {
                    0, 1, 2,
                    0, 2, 3
            };

    /**
     * Constructs a unit quad centered at the origin, facing +Z, with standard 0..1 UV mapping.
     *
     * <p>Material properties are initialized using the engine's gold material presets.</p>
     */
    public ManualQuad()
    {
        super();

        // Corner positions are centered at the origin and face toward positive Z
        vertices[0] = new Vector3f(-1f, -1f, 0f);
        vertices[1] = new Vector3f( 1f, -1f, 0f);
        vertices[2] = new Vector3f( 1f,  1f, 0f);
        vertices[3] = new Vector3f(-1f,  1f, 0f);

        // UV mapping matches the corners so the texture is not stretched oddly
        texcoords[0] = new Vector2f(0f, 0f);
        texcoords[1] = new Vector2f(1f, 0f);
        texcoords[2] = new Vector2f(1f, 1f);
        texcoords[3] = new Vector2f(0f, 1f);

        // Normal points out of the front face
        for (int i = 0; i < 4; i++)
            normals[i] = new Vector3f(0f, 0f, 1f);

        // Triangle list expands to six vertices because each triangle has three vertices
        setNumVertices(6);

        // Indexed data is expanded into the final vertex stream using the index order
        setVerticesIndexed(indices, vertices);
        setTexCoordsIndexed(indices, texcoords);
        setNormalsIndexed(indices, normals);

        // Gold material preset gives consistent lighting results
        setMatAmb(Utils.goldAmbient());
        setMatDif(Utils.goldDiffuse());
        setMatSpe(Utils.goldSpecular());
        setMatShi(Utils.goldShininess());
    }
}