New: What I Changed On givin-glsl-shaders
Package Rename

I renamed the game package from a3 to running_Dead because that is the current name of the game.

Moved the main game files into running_Dead.
Moved the actions, networking, and server files into running_Dead.
Updated the build and run scripts so they use the new package.
Removed the old a3 files.
Shader And Graphics Upgrade

I added the CSC155 shader work into the CSC165 game.

Added shadow mapping.
Added normal mapping support.
Added procedural bump/detail effects.
Added fog.
Added reflection/environment mapping.
Added new shader files for shadows and clouds.

New shader files:

ShadowVert.glsl
ShadowSkeletalVert.glsl
ShadowFrag.glsl
CloudFrag.glsl
Shadow Fixes

I fixed the broken shadows that looked like giant black squares around the player.

Changed the shadow map to use proper shadow comparison.
Added PCF-style soft shadow sampling.
Added shadow edge fading so the shadow map square does not show.
Reduced shadow strength.
Increased bias to reduce shadow acne.
Stopped the terrain from casting shadows onto itself.
Stopped transparent objects from casting solid black shadows.
Stopped player avatars and tree foliage from creating huge fake shadows.
Object Shadows

I made important world objects work with shadows.

Rocks can cast shadows.
Pickups can cast shadows.
Tables can cast shadows.
Placed walls and roofs can cast shadows.
Build preview pieces do not cast shadows while placing.
Day And Night Cycle

I added a day/night system.

Day uses sky04_cubemap.
Night uses sky15_night.
Day lasts 150 seconds.
Night lasts 150 seconds.
Each round starts at a random point in the cycle.
The sun moves during the day, so shadows move too.
Night Gameplay

I changed visibility at night.

Humans get a small local-only glow so they are not completely blind.
Other players do not see that human glow.
Zombies get night vision so they can see better in the dark.
Flashlights are still the main visible light source for finding players at night.
Flashlight Improvements

I improved the flashlight.

Made the spotlight brighter.
Made it reach farther.
Made it aim with the camera direction.
Fixed the flashlight material so it looks dark and matte instead of shiny metal.
Texture Improvements

I fixed the issue where some objects looked like they were using the wrong grid-like texture.

Grass uses grass-style textures.
Rocks use rock-style textures.
Wood uses wood-style textures.
Metal uses metal-style textures.
Glass uses glass-style textures.
Potions and flashlights use their own matching textures.

I also added distance texture blending so objects can use better far/detail textures.

Generated Textures

I added generated texture assets under:

assets/textures/generated/

These include far/detail textures and normal maps for things like:

grass
rock
wood
metal
glass
brick
potion
flashlight
table
leaves
players
zombies
SmilingMan
MushroomMon
Engine Rendering Updates

I updated the TAGE rendering system so the new graphics features can work.

Changed files include:

tage/GameObject.java
tage/RenderStates.java
tage/RenderSystem.java
RenderObjectStandard.java
RenderObjectAnimation.java

These changes let objects control things like shadows, normal maps, reflections, fog, transparency, and texture blending.

Game System Updates

I updated the game systems that create or manage objects.

World objects now get the correct shadow and texture settings.
Build pieces now use material-specific visuals.
Pickups now use better render settings.
Transparent objects avoid bad shadow behavior.
The flashlight and potion visuals were cleaned up.
Build Scripts

I updated the project scripts for the new package name.

compile.bat
run_client.bat
run_server.bat
run_singleplayer.bat
clearGAMEclassFiles.bat
