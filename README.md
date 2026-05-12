## New: What I Changed On `givin-javadocs-and-fixes`

This branch builds on the shader/graphics work from `givin-glsl-shaders` and focuses more on multiplayer, player names, terrain fixes, build fixes, physics, documentation, and run script cleanup.

### Floating Player Names

I added floating names above players.

* Humans only see human player names.
* Zombies only see zombie player names.
* Names are distance-limited to about 15% of the map.
* Names no longer show through trees, rocks, walls, or objects.
* Names use depth-buffer checks in TAGE.
* Human names disappear when hiding under tables.

### Multiplayer And Lobby Updates

I improved multiplayer startup and syncing.

* Added player name input before joining.
* Player names cannot be empty.
* Join messages show names like `Bob has joined the lobby`.
* Join sound plays when a client joins.
* Synced held items between clients.
* Synced zombie baby/flashlight item visibility.
* Improved server state snapshots for late-joining clients.
* Flashlight state is sent through movement packets so other clients can see it correctly.

### World And Terrain Fixes

I fixed random object placement and terrain snapping.

* Trees and rocks now use terrain height.
* Objects use model base offsets so they do not float or sink as much.
* Random scenery avoids overlapping other scenery.
* Added more trees and rocks.
* Made trees taller.
* Added CPU terrain-height sampling so placement does not require an OpenGL context.

### Build System Fixes

I fixed build mode placement and visuals.

* Build preview follows the player’s current terrain height.
* Build placement avoids OpenGL height errors on mouse click.
* Build pieces use wood, metal, and glass materials.
* Build pieces sync across clients.
* Build pieces can be damaged and removed.
* Support checks stop floating unsupported structures.

### Physics And Stuck Recovery

I improved collider behavior.

* Added scenery, build, and pickup physics registration.
* Added recovery when the player gets stuck in a collider.
* The player gets nudged out instead of staying trapped.
* Improved tree and scenery collider handling.

### TAGE Engine Updates

I updated TAGE to support the new gameplay and UI features.

* Added world-positioned HUD labels.
* Added depth-tested name label rendering.
* Updated HUD rendering so normal HUD stays visible while world labels respect depth.
* Added camera orbit, yaw, and pitch support.
* Added network discovery helper docs.
* Updated manual shapes and bobbing controller author/comment information.

### Comments And Documentation

I added grader-facing comments throughout the project.

* Every Java file in `running_Dead` has a purpose comment.
* Every file also has a `Connected to:` comment explaining where it is used.
* Important non-obvious systems have extra comments explaining why the code exists.
* Regenerated the TAGE Javadoc folder.
* Added TAGE Javadocs for our TAGE changes.

### Run Script And Native Path Fixes

I cleaned up the batch files.

* Removed the temporary `PATH` workaround from `.bat` files.
* Added `MyGameNativePathSanitizer` instead.
* The sanitizer fixes the broken Muse Hub / VS Code native path issue inside Java startup.
* Updated `run_client.bat`.
* Updated `run_server.bat`.
* Updated `run_singleplayer.bat`.


## New: What I Changed On `givin-glsl-shaders`

### Package Rename

I renamed the game package from `a3` to `running_Dead` because that is the current name of the game.

* Moved the main game files into `running_Dead`.
* Moved the actions, networking, and server files into `running_Dead`.
* Updated the build and run scripts so they use the new package.
* Removed the old `a3` files.

### Shader And Graphics Upgrade

I added the CSC155 shader work into the CSC165 game.

* Added shadow mapping.
* Added normal mapping support.
* Added procedural bump/detail effects.
* Added fog.
* Added reflection/environment mapping.
* Added new shader files for shadows and clouds.

New shader files:

* `ShadowVert.glsl`
* `ShadowSkeletalVert.glsl`
* `ShadowFrag.glsl`
* `CloudFrag.glsl`

### Shadow Fixes

I fixed the broken shadows that looked like giant black squares around the player.

* Changed the shadow map to use proper shadow comparison.
* Added PCF-style soft shadow sampling.
* Added shadow edge fading so the shadow map square does not show.
* Reduced shadow strength.
* Increased bias to reduce shadow acne.
* Stopped the terrain from casting shadows onto itself.
* Stopped transparent objects from casting solid black shadows.
* Stopped player avatars and tree foliage from creating huge fake shadows.

### Object Shadows

I made important world objects work with shadows.

* Rocks can cast shadows.
* Pickups can cast shadows.
* Tables can cast shadows.
* Placed walls and roofs can cast shadows.
* Build preview pieces do not cast shadows while placing.

### Day And Night Cycle

I added a day/night system.

* Day uses `sky04_cubemap`.
* Night uses `sky15_night`.
* Day lasts 150 seconds.
* Night lasts 150 seconds.
* Each round starts at a random point in the cycle.
* The sun moves during the day, so shadows move too.

### Night Gameplay

I changed visibility at night.

* Humans get a small local-only glow so they are not completely blind.
* Other players do not see that human glow.
* Zombies get night vision so they can see better in the dark.
* Flashlights are still the main visible light source for finding players at night.

### Flashlight Improvements

I improved the flashlight.

* Made the spotlight brighter.
* Made it reach farther.
* Made it aim with the camera direction.
* Fixed the flashlight material so it looks dark and matte instead of shiny metal.

### Texture Improvements

I fixed the issue where some objects looked like they were using the wrong grid-like texture.

* Grass uses grass-style textures.
* Rocks use rock-style textures.
* Wood uses wood-style textures.
* Metal uses metal-style textures.
* Glass uses glass-style textures.
* Potions and flashlights use their own matching textures.

I also added distance texture blending so objects can use better far/detail textures.

### Generated Textures

I added generated texture assets under:

* `assets/textures/generated/`

These include far/detail textures and normal maps for things like:

* grass
* rock
* wood
* metal
* glass
* brick
* potion
* flashlight
* table
* leaves
* players
* zombies
* SmilingMan
* MushroomMon

### Engine Rendering Updates

I updated the TAGE rendering system so the new graphics features can work.

Changed files include:

* `tage/GameObject.java`
* `tage/RenderStates.java`
* `tage/RenderSystem.java`
* `RenderObjectStandard.java`
* `RenderObjectAnimation.java`

These changes let objects control things like shadows, normal maps, reflections, fog, transparency, and texture blending.

### Game System Updates

I updated the game systems that create or manage objects.

* World objects now get the correct shadow and texture settings.
* Build pieces now use material-specific visuals.
* Pickups now use better render settings.
* Transparent objects avoid bad shadow behavior.
* The flashlight and potion visuals were cleaned up.

### Build Scripts

I updated the project scripts for the new package name.

* `compile.bat`
* `run_client.bat`
* `run_server.bat`
* `run_singleplayer.bat`
* `clearGAMEclassFiles.bat`
```
