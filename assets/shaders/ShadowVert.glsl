#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec2 texCoord;

uniform mat4 m_matrix;
uniform mat4 lightVP_matrix;
uniform int heightMapped;

layout (binding = 2) uniform sampler2D height;

void main(void)
{
	float heightOffset = (heightMapped == 1) ? texture(height, texCoord).r : 0.0;
	vec4 localPos = vec4(vertPos.x, vertPos.y + heightOffset, vertPos.z, 1.0);
	gl_Position = lightVP_matrix * m_matrix * localPos;
}
