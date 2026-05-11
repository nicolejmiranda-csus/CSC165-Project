#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 vertNormal;

out vec2 tc;
out vec3 varyingNormal;
out vec3 varyingVertPos;
out vec3 vVertPos;
out vec4 shadowCoord;

struct Light
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
	float constantAttenuation;
	float linearAttenuation;
	float quadraticAttenuation;
	float range;
	vec3 direction;
	float cutoffAngle;
	float offAxisExponent;
	float type;
	float enabled;
};
struct Material
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	float shininess;
};

Light light;

uniform vec4 globalAmbient;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform mat4 lightVP_matrix;
uniform int envMapped;
uniform int has_texture;
uniform int tileCount;
uniform int heightMapped;
uniform int hasLighting;
uniform int isTransparent;
uniform float alpha;
uniform float flipNormal;
uniform int solidColor;
uniform vec3 color;
uniform int num_lights;
uniform int fields_per_light;

layout (std430, binding=0) buffer lightBuffer { float lightArray[]; };
layout (binding = 0) uniform sampler2D samp;
layout (binding = 1) uniform samplerCube t;
layout (binding = 2) uniform sampler2D height;

void main(void)
{
	// Most of the time this height offset is 0.
	// If this is a terrain plane, and has a height map, then this will do the height mapping.
	float heightOffset = (heightMapped == 1) ? texture(height, texCoord).r : 0.0;
	vec4 p = vec4(vertPos.x, vertPos.y + heightOffset, vertPos.z, 1.0);
	vec4 worldPos = m_matrix * p;

	vVertPos = (v_matrix * worldPos).xyz;
	varyingVertPos = worldPos.xyz;
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	shadowCoord = lightVP_matrix * worldPos;

	// Compute the texture coordinates depending on the specified tileFactor
	tc = texCoord;
	tc = tc * tileCount;
	
	if (flipNormal < 0) varyingNormal = -varyingNormal;
	
	gl_Position = p_matrix * v_matrix * worldPos;
}
