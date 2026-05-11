#version 430 core

layout (location = 0) in vec3 vertex_position;
layout (location = 3) in vec3 vertex_bone_indices;
layout (location = 4) in vec3 vertex_bone_weights;

uniform mat4 m_matrix;
uniform mat4 lightVP_matrix;

layout (std430, binding=1) readonly buffer skinMatrixBuffer
{	mat4 skin_matrices[];
};

void main(void)
{
	vec4 basePos = vec4(vertex_position, 1.0);
	int index;

	index = int(vertex_bone_indices.x);
	vec4 bone1 = skin_matrices[index] * basePos;
	index = int(vertex_bone_indices.y);
	vec4 bone2 = skin_matrices[index] * basePos;
	index = int(vertex_bone_indices.z);
	vec4 bone3 = skin_matrices[index] * basePos;

	vec4 skinnedPos = bone1 * vertex_bone_weights.x
			+ bone2 * vertex_bone_weights.y
			+ bone3 * vertex_bone_weights.z;
	float totalWeight = vertex_bone_weights.x + vertex_bone_weights.y + vertex_bone_weights.z;
	vec4 finalPos = mix(basePos, skinnedPos, totalWeight);

	gl_Position = lightVP_matrix * m_matrix * finalPos;
}
