#version 430

in vec2 tc;
in vec3 varyingNormal;
in vec3 varyingVertPos;
in vec3 vVertPos;
in vec4 shadowCoord;

out vec4 fragColor;

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

uniform vec3 cameraPos;
uniform vec4 fogColor;
uniform float fogStart;
uniform float fogEnd;
uniform int useFog;
uniform int useBumpMap;
uniform int useNormalMap;
uniform int mappingMode;
uniform int bumpStyle;
uniform int useTextureDetailBlend;
uniform int hasDetailTexture;
uniform int receivesShadow;
uniform int hasShadowMap;
uniform int shadowLightIndex;
uniform float surfaceScale;
uniform float shadowStrength;
uniform float textureDetailNear;
uniform float textureDetailFar;

layout (std430, binding=0) buffer lightBuffer { float lightArray[]; };
layout (binding = 0) uniform sampler2D samp;
layout (binding = 1) uniform samplerCube t;
layout (binding = 2) uniform sampler2D height;
layout (binding = 3) uniform sampler2DShadow shadowTex;
layout (binding = 4) uniform sampler2D normalTex;
layout (binding = 5) uniform sampler2D detailTex;

vec3 lightDir, L, N, V, R, ambient, diffuse, specular, thisAmbient, thisDiffuse, thisSpecular;
float cosTheta, cosPhi, intensity, attenuationFactor, dist;
int i,f;
vec4 tcolor, objLightingColor;

vec2 heightUV()
{
	float tiles = max(float(tileCount), 1.0);
	return tc / tiles;
}

vec3 estimateNormal(float offset, float heightScale)
{
	vec2 uv = heightUV();
	float h1 = heightScale * texture(height, vec2(uv.s, uv.t + offset)).r;
	float h2 = heightScale * texture(height, vec2(uv.s - offset, uv.t - offset)).r;
	float h3 = heightScale * texture(height, vec2(uv.s + offset, uv.t - offset)).r;
	vec3 v1 = vec3(0, h1, -1);
	vec3 v2 = vec3(-1, h2, 1);
	vec3 v3 = vec3(1, h3, 1);
	vec3 v4 = v2 - v1;
	vec3 v5 = v3 - v1;
	return normalize(cross(v4, v5));
}

float hash12(vec2 p)
{
	vec3 p3 = fract(vec3(p.xyx) * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

float noise2(vec2 p)
{
	vec2 cell = floor(p);
	vec2 local = fract(p);
	float a = hash12(cell);
	float b = hash12(cell + vec2(1.0, 0.0));
	float c = hash12(cell + vec2(0.0, 1.0));
	float d = hash12(cell + vec2(1.0, 1.0));
	vec2 u = local * local * (3.0 - 2.0 * local);
	return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p)
{
	float sum = 0.0;
	float amp = 0.5;
	float freq = 1.0;
	for (int layer = 0; layer < 4; layer++)
	{
		sum += amp * noise2(p * freq);
		freq *= 2.02;
		amp *= 0.5;
	}
	return sum;
}

float stoneHeight(vec2 uv)
{
	float broad = fbm(uv * 3.5);
	float grain = fbm(uv * 13.0) * 0.45;
	float cracks = 1.0 - abs(fbm(uv * 8.5 + vec2(3.1, 7.2)) * 2.0 - 1.0);
	cracks = smoothstep(0.72, 0.93, cracks);
	return broad * 0.34 + grain * 0.28 - cracks * 0.38;
}

float grassHeight(vec2 uv)
{
	float broad = fbm(uv * 4.5);
	float fine = fbm(uv * 18.0) * 0.35;
	float blades = sin((uv.x * 37.0 + uv.y * 11.0) + broad * 2.2) * 0.04;
	return broad * 0.34 + fine + blades;
}

mat3 fallbackTBN(vec3 baseNormal)
{
	vec3 baseN = normalize(baseNormal);
	vec3 helper = (abs(baseN.y) < 0.999) ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
	vec3 T = normalize(cross(helper, baseN));
	vec3 B = normalize(cross(baseN, T));
	return mat3(T, B, baseN);
}

vec2 getMappedUV(vec3 baseNormal, float scale)
{
	float s = max(scale, 0.0001);
	if (mappingMode == 0)
		return tc * s;

	vec3 baseN = normalize(baseNormal);
	vec3 absN = abs(baseN);
	if (absN.y >= absN.x && absN.y >= absN.z)
		return ((baseN.y >= 0.0) ? vec2(varyingVertPos.x, -varyingVertPos.z) : vec2(varyingVertPos.x, varyingVertPos.z)) * s;
	if (absN.x >= absN.z)
		return ((baseN.x >= 0.0) ? vec2(-varyingVertPos.z, varyingVertPos.y) : vec2(varyingVertPos.z, varyingVertPos.y)) * s;
	return ((baseN.z >= 0.0) ? vec2(varyingVertPos.x, varyingVertPos.y) : vec2(-varyingVertPos.x, varyingVertPos.y)) * s;
}

mat3 getMappedTBN(vec3 baseNormal, float scale)
{
	if (mappingMode == 0)
	{
		vec3 dp1 = dFdx(varyingVertPos);
		vec3 dp2 = dFdy(varyingVertPos);
		vec2 uv = tc * max(scale, 0.0001);
		vec2 duv1 = dFdx(uv);
		vec2 duv2 = dFdy(uv);
		float det = duv1.x * duv2.y - duv1.y * duv2.x;
		if (abs(det) < 0.00001) return fallbackTBN(baseNormal);

		vec3 T = normalize((dp1 * duv2.y - dp2 * duv1.y) / det);
		vec3 B = normalize((-dp1 * duv2.x + dp2 * duv1.x) / det);
		vec3 baseN = normalize(baseNormal);
		return mat3(T, B, baseN);
	}

	vec3 baseN = normalize(baseNormal);
	vec3 absN = abs(baseN);
	vec3 T;
	vec3 B;
	vec3 axisNormal;

	if (absN.y >= absN.x && absN.y >= absN.z)
	{
		T = vec3(1.0, 0.0, 0.0);
		B = (baseN.y >= 0.0) ? vec3(0.0, 0.0, -1.0) : vec3(0.0, 0.0, 1.0);
		axisNormal = (baseN.y >= 0.0) ? vec3(0.0, 1.0, 0.0) : vec3(0.0, -1.0, 0.0);
	}
	else if (absN.x >= absN.z)
	{
		T = (baseN.x >= 0.0) ? vec3(0.0, 0.0, -1.0) : vec3(0.0, 0.0, 1.0);
		B = vec3(0.0, 1.0, 0.0);
		axisNormal = (baseN.x >= 0.0) ? vec3(1.0, 0.0, 0.0) : vec3(-1.0, 0.0, 0.0);
	}
	else
	{
		T = (baseN.z >= 0.0) ? vec3(1.0, 0.0, 0.0) : vec3(-1.0, 0.0, 0.0);
		B = vec3(0.0, 1.0, 0.0);
		axisNormal = (baseN.z >= 0.0) ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 0.0, -1.0);
	}

	return mat3(normalize(T), normalize(B), axisNormal);
}

vec3 applyBump(vec3 baseNormal)
{
	mat3 tbn = getMappedTBN(baseNormal, surfaceScale);
	vec3 T = normalize(tbn[0]);
	vec3 B = normalize(tbn[1]);
	vec3 baseN = normalize(tbn[2]);
	vec2 uv = getMappedUV(baseNormal, surfaceScale);

	float eps = 0.015;
	float h = (bumpStyle == 1) ? grassHeight(uv) : stoneHeight(uv);
	float hx = (bumpStyle == 1) ? grassHeight(uv + vec2(eps, 0.0)) : stoneHeight(uv + vec2(eps, 0.0));
	float hy = (bumpStyle == 1) ? grassHeight(uv + vec2(0.0, eps)) : stoneHeight(uv + vec2(0.0, eps));
	float bumpStrength = (bumpStyle == 1) ? 0.32 : 0.85;
	return normalize(baseN + bumpStrength * ((h - hx) * T + (h - hy) * B));
}

vec3 applyNormalMap(vec3 baseNormal)
{
	mat3 tbn = getMappedTBN(baseNormal, surfaceScale);
	vec2 uv = getMappedUV(baseNormal, surfaceScale);
	vec3 mapN = texture(normalTex, uv).xyz * 2.0 - 1.0;
	mapN.y = -mapN.y;
	mapN = normalize(vec3(mapN.xy * 1.15, mapN.z));
	return normalize(tbn * mapN);
}

vec3 applySurfaceNormal(vec3 baseNormal)
{
	vec3 n = normalize(baseNormal);
	if (useNormalMap == 1) return applyNormalMap(n);
	if (useBumpMap == 1) return applyBump(n);
	return n;
}

float computeShadow(vec3 normalForShadow, vec3 lightVector)
{
	if (hasShadowMap == 0 || receivesShadow == 0)
		return 0.0;

	vec3 projCoords = shadowCoord.xyz / max(shadowCoord.w, 0.00001);
	projCoords = projCoords * 0.5 + 0.5;
	if (shadowCoord.w <= 0.0 ||
			projCoords.z <= 0.0 || projCoords.z >= 1.0 ||
			projCoords.x <= 0.001 || projCoords.x >= 0.999 ||
			projCoords.y <= 0.001 || projCoords.y >= 0.999)
		return 0.0;

	float bias = max(0.0015 * (1.0 - max(dot(normalForShadow, lightVector), 0.0)), 0.00045);
	float edgeDistance = min(min(projCoords.x, 1.0 - projCoords.x), min(projCoords.y, 1.0 - projCoords.y));
	float edgeFade = smoothstep(0.015, 0.055, edgeDistance);
	vec2 texelSize = 1.0 / vec2(textureSize(shadowTex, 0));
	float lit = 0.0;

	for (int x = -1; x <= 1; x++)
	{
		for (int y = -1; y <= 1; y++)
		{
			vec2 offset = vec2(x, y) * texelSize * 1.35;
			lit += texture(shadowTex, vec3(projCoords.xy + offset, projCoords.z - bias));
		}
	}
	return (1.0 - (lit / 9.0)) * edgeFade;
}

void calcPositionalLight()
{
	thisDiffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta, 0.0);
	thisSpecular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi, 0.0), material.shininess);
}

void calcSpotLight()
{
	float cosAngle = clamp(dot(-L, normalize((light.direction).xyz)), 0.0, 1.0);
	float angleD = degrees(acos(cosAngle));

	if (angleD > light.cutoffAngle)
		intensity = 0.0;
	else
		intensity = pow(cosAngle, light.offAxisExponent);

	thisDiffuse = intensity * light.diffuse.xyz * material.diffuse.xyz * max(cosTheta, 0.0);
	thisSpecular = intensity * light.specular.xyz * material.specular.xyz * pow(max(cosPhi, 0.0), material.shininess);
}

vec4 baseObjectColor(vec3 normalForReflection, vec3 viewVector)
{
	if (solidColor == 1)
		return vec4(color, 1.0);
	if (envMapped == 1)
	{
		vec3 reflected = reflect(-viewVector, normalForReflection);
		return vec4(texture(t, reflected).xyz, 1.0);
	}
	if (has_texture == 0)
		return vec4(0.7, 0.7, 0.7, 1.0);
	vec4 nearTex = texture(samp, tc);
	if (useTextureDetailBlend == 1 && hasDetailTexture == 1)
	{
		float cameraDistance = length(cameraPos - varyingVertPos);
		float blendStart = min(textureDetailNear, textureDetailFar);
		float blendEnd = max(textureDetailNear + 0.01, textureDetailFar);
		float blendWidth = max(blendEnd - blendStart, 0.0001);
		float broadNoise = noise2(varyingVertPos.xz * 0.035);
		float fineNoise = noise2(varyingVertPos.xz * 0.11 + vec2(17.0, 9.0));
		float noisyDistance = cameraDistance + ((broadNoise * 0.7 + fineNoise * 0.3) - 0.5) * blendWidth * 0.28;
		float blend = smoothstep(blendStart, blendEnd, noisyDistance);
		float farScale = 1.0;
		if (heightMapped == 1)
			farScale = 0.22;
		else if (mappingMode == 1)
			farScale = 0.55;
		vec4 farTex = texture(detailTex, tc * farScale);
		if (heightMapped == 1)
		{
			float nearLum = dot(nearTex.rgb, vec3(0.299, 0.587, 0.114));
			float farLum = dot(farTex.rgb, vec3(0.299, 0.587, 0.114));
			float lumRatio = clamp(nearLum / max(farLum, 0.035), 0.72, 1.32);
			farTex.rgb = mix(farTex.rgb, farTex.rgb * lumRatio, 0.55);
		}
		return mix(nearTex, farTex, blend);
	}
	return nearTex;
}

float surfaceSpecularScale()
{
	if (envMapped == 1)
		return 0.65;
	if (useBumpMap == 1 || mappingMode == 1)
		return 0.10;
	if (useTextureDetailBlend == 1)
		return 0.12;
	if (has_texture == 1)
		return 0.25;
	return 0.35;
}

void main(void)
{
	vec3 baseNormal;
	if (heightMapped == 1)
		baseNormal = estimateNormal(.005, 5.0);
	else
		baseNormal = normalize(varyingNormal);

	N = applySurfaceNormal(baseNormal);
	V = normalize(cameraPos - varyingVertPos);

	f = fields_per_light;
	ambient = vec3(0, 0, 0);
	diffuse = vec3(0, 0, 0);
	specular = vec3(0, 0, 0);

	for (i = 0; i < num_lights; i++)
	{
		light.position = vec3(lightArray[i*f+0], lightArray[i*f+1], lightArray[i*f+2]);
		lightDir = light.position - varyingVertPos;
		L = normalize(lightDir);
		R = normalize(reflect(-L, N));
		cosTheta = dot(L, N);
		cosPhi = dot(V, R);

		light.ambient = vec4(lightArray[i*f+3], lightArray[i*f+4], lightArray[i*f+5], 1.0);
		light.diffuse = vec4(lightArray[i*f+6], lightArray[i*f+7], lightArray[i*f+8], 1.0);
		light.specular = vec4(lightArray[i*f+9], lightArray[i*f+10], lightArray[i*f+11], 1.0);
		light.constantAttenuation = lightArray[i*f+12];
		light.linearAttenuation = lightArray[i*f+13];
		light.quadraticAttenuation = lightArray[i*f+14];
		light.range = lightArray[i*f+15];
		light.direction = vec3(lightArray[i*f+16], lightArray[i*f+17], lightArray[i*f+18]);
		light.cutoffAngle = lightArray[i*f+19];
		light.offAxisExponent = lightArray[i*f+20];
		light.type = lightArray[i*f+21];
		light.enabled = lightArray[i*f+22];

		if (light.enabled == 1.0)
		{
			thisAmbient = (globalAmbient + (light.ambient * material.ambient)).xyz;
			ambient = max(ambient, thisAmbient);

			if (light.type == 0.0)
				calcPositionalLight();
			else
				calcSpotLight();

			dist = distance(varyingVertPos, light.position);
			attenuationFactor = 1.0 / (light.constantAttenuation + light.linearAttenuation * dist + light.quadraticAttenuation * dist * dist);

			float shadow = (i == shadowLightIndex) ? computeShadow(N, L) : 0.0;
			float shadowTerm = 1.0 - clamp(shadow * shadowStrength, 0.0, 1.0);
			diffuse = min(vec3(1, 1, 1), diffuse + shadowTerm * attenuationFactor * thisDiffuse);
			specular = min(vec3(1, 1, 1), specular + shadowTerm * attenuationFactor * thisSpecular);
		}
	}
	specular *= surfaceSpecularScale();

	if (hasLighting == 0)
	{
		objLightingColor = baseObjectColor(N, V);
	}
	else
	{
		tcolor = baseObjectColor(N, V);
		if (envMapped == 1)
		{
			vec3 reflectionBoost = tcolor.rgb * (0.08 + 0.12 * material.specular.rgb);
			objLightingColor = min(vec4(reflectionBoost + tcolor.rgb * (ambient + diffuse) + specular, tcolor.a), vec4(1, 1, 1, 1));
		}
		else if (has_texture == 0 && solidColor == 0)
		{
			objLightingColor = min(0.5 * vec4((ambient + diffuse + specular), 1.0), vec4(1, 1, 1, 1));
		}
		else
		{
			objLightingColor = min((tcolor * vec4((ambient + diffuse), 1.0) + vec4(specular, 0.0)), vec4(1, 1, 1, 1));
		}
	}

	float outAlpha = (isTransparent == 1) ? alpha * objLightingColor.a : objLightingColor.a;
	if (isTransparent == 1 && flipNormal < 0.0)
		outAlpha *= 0.8;

	vec3 finalColor = objLightingColor.rgb;
	if (useFog == 1)
	{
		float cameraDistance = length(cameraPos - varyingVertPos);
		float fogFactor = clamp((fogEnd - cameraDistance) / max(fogEnd - fogStart, 0.0001), 0.0, 1.0);
		finalColor = mix(fogColor.rgb, finalColor, fogFactor);
	}

	fragColor = vec4(finalColor, outAlpha);
}
