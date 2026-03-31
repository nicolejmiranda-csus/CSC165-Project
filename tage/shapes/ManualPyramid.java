package tage.shapes;

import tage.*;
import org.joml.*;

/**
 * ManualPyramid is a manually-defined pyramid mesh (square base + 4 triangular sides).
 * It is centered at the origin with its base on the Y=0 plane and apex at Y=1.
 *
 * <p>Uses indexed source vertices but expands to a triangle-list stream using {@code set*Indexed()},
 * following the same pattern as {@link ManualQuad}.</p>
 */
public class ManualPyramid extends ManualObject
{
    // Vertex data is laid out as a triangle list
    // Each face gets its own vertices so flat normals and UVs stay clean
    // Six triangles means eighteen vertices
    private Vector3f[] vertices = new Vector3f[18];
    private Vector2f[] texcoords = new Vector2f[18];
    private Vector3f[] normals = new Vector3f[18];

    // Indices match the triangle list layout
    // They are still provided so setIndexed functions expand data in order
    private int[] indices = new int[] {
            0, 1, 2,     // Front
            3, 4, 5,     // Right
            6, 7, 8,     // Back
            9,10,11,     // Left
            12,13,14,    // Base tri 1
            15,16,17     // Base tri 2
    };

    /**
     * Constructs a pyramid mesh and initializes positions, texture coordinates, normals, and material properties.
     *
     * <p>The pyramid is built as 6 triangles (4 sides + 2 for the base). Vertices are duplicated per face so each
     * face can use a flat normal and consistent UV mapping.</p>
     */
    public ManualPyramid()
    {
        super();

        // Base corners and apex used to build the faces
        Vector3f bl = new Vector3f(-1f, 0f,  1f);
        Vector3f br = new Vector3f( 1f, 0f,  1f);
        Vector3f tr = new Vector3f( 1f, 0f, -1f);
        Vector3f tl = new Vector3f(-1f, 0f, -1f);
        Vector3f ap = new Vector3f( 0f, 1f,  0f);

        // Side faces are four triangles that meet at the apex
        vertices[0] = new Vector3f(bl);
        vertices[1] = new Vector3f(br);
        vertices[2] = new Vector3f(ap);

        vertices[3] = new Vector3f(br);
        vertices[4] = new Vector3f(tr);
        vertices[5] = new Vector3f(ap);

        vertices[6] = new Vector3f(tr);
        vertices[7] = new Vector3f(tl);
        vertices[8] = new Vector3f(ap);

        vertices[9]  = new Vector3f(tl);
        vertices[10] = new Vector3f(bl);
        vertices[11] = new Vector3f(ap);

        // Base is two triangles
        // Winding is chosen so the base normal points down
        vertices[12] = new Vector3f(tl);
        vertices[13] = new Vector3f(tr);
        vertices[14] = new Vector3f(br);

        vertices[15] = new Vector3f(tl);
        vertices[16] = new Vector3f(br);
        vertices[17] = new Vector3f(bl);

        // Side UV mapping uses a simple triangle layout
        // Base uses a square style mapping
        for (int f = 0; f < 4; f++)
        {
            int base = f * 3;
            texcoords[base]     = new Vector2f(0f, 0f);
            texcoords[base + 1] = new Vector2f(1f, 0f);
            texcoords[base + 2] = new Vector2f(0.5f, 1f);
        }

        texcoords[12] = new Vector2f(0f, 1f);
        texcoords[13] = new Vector2f(1f, 1f);
        texcoords[14] = new Vector2f(1f, 0f);

        texcoords[15] = new Vector2f(0f, 1f);
        texcoords[16] = new Vector2f(1f, 0f);
        texcoords[17] = new Vector2f(0f, 0f);

        // Normals are flat per face
        // Side normals are approximated and base normal points down
        float s = 0.70710678f;

        normals[0] = new Vector3f(0f, s, s);
        normals[1] = new Vector3f(0f, s, s);
        normals[2] = new Vector3f(0f, s, s);

        normals[3] = new Vector3f(s, s, 0f);
        normals[4] = new Vector3f(s, s, 0f);
        normals[5] = new Vector3f(s, s, 0f);

        normals[6] = new Vector3f(0f, s, -s);
        normals[7] = new Vector3f(0f, s, -s);
        normals[8] = new Vector3f(0f, s, -s);

        normals[9]  = new Vector3f(-s, s, 0f);
        normals[10] = new Vector3f(-s, s, 0f);
        normals[11] = new Vector3f(-s, s, 0f);

        for (int i = 12; i < 18; i++)
            normals[i] = new Vector3f(0f, -1f, 0f);

        // TAGE triangle list has eighteen vertices for six triangles
        setNumVertices(18);

        // Indexed calls expand the vertex data into the final stream in triangle order
        setVerticesIndexed(indices, vertices);
        setTexCoordsIndexed(indices, texcoords);
        setNormalsIndexed(indices, normals);

        // Gold material preset for consistent lighting results
        setMatAmb(Utils.goldAmbient());
        setMatDif(Utils.goldDiffuse());
        setMatSpe(Utils.goldSpecular());
        setMatShi(Utils.goldShininess());
    }
}