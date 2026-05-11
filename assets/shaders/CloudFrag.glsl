#version 430

out vec4 fragColor;

in vec2 fragUv;

uniform float timeSeconds;
uniform float nightFactor;

float hash(vec2 p)
{
	return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p)
{
	vec2 i = floor(p);
	vec2 f = fract(p);
	vec2 u = f * f * (3.0 - 2.0 * f);
	float a = hash(i);
	float b = hash(i + vec2(1.0, 0.0));
	float c = hash(i + vec2(0.0, 1.0));
	float d = hash(i + vec2(1.0, 1.0));
	return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p)
{
	float value = 0.0;
	float amp = 0.55;
	for (int i = 0; i < 5; i++)
	{
		value += noise(p) * amp;
		p = p * 2.03 + vec2(17.7, 9.2);
		amp *= 0.52;
	}
	return value;
}

void main(void)
{
	float skyMask = smoothstep(0.47, 0.66, fragUv.y) * (1.0 - smoothstep(0.98, 1.04, fragUv.y));
	vec2 windUv = vec2(fragUv.x * 2.35 + timeSeconds * 0.018, fragUv.y * 3.2);
	float cloud = fbm(windUv);
	cloud += fbm(windUv * 1.85 + vec2(timeSeconds * 0.01, 4.0)) * 0.35;
	cloud = smoothstep(0.52, 0.84, cloud);

	float streak = smoothstep(0.0, 0.25, sin((fragUv.x + timeSeconds * 0.008) * 12.0 + fragUv.y * 3.0) * 0.5 + 0.5);
	float alpha = cloud * skyMask * mix(0.34, 0.18, nightFactor);
	alpha *= mix(0.86, 1.05, streak);

	vec3 dayColor = vec3(1.0, 0.98, 0.92);
	vec3 nightColor = vec3(0.20, 0.24, 0.34);
	vec3 color = mix(dayColor, nightColor, nightFactor);
	fragColor = vec4(color, alpha);
}
