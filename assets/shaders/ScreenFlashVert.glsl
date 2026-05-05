#version 430

layout (location = 0) in vec2 position;

out vec2 fragUv;

void main(void)
{
	fragUv = position * 0.5 + 0.5;
	gl_Position = vec4(position, 0.0, 1.0);
}
