#version 430

out vec4 fragColor;

in vec2 fragUv;

uniform float opacity;
uniform vec3 overlayColor;
uniform float vignette;

void main(void)
{
	float alpha = opacity;
	if (vignette > 0.5)
	{
		vec2 centered = fragUv * 2.0 - 1.0;
		float edgeDarkness = smoothstep(0.32, 1.05, length(centered));
		alpha *= edgeDarkness;
	}
	fragColor = vec4(overlayColor, alpha);
}
