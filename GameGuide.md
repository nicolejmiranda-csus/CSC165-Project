## 1. Game Information

| Field            | Value                       |
| ---------------- | --------------------------- |
| **Game Name**    | Running Dead                |
| **Team Members** | Givin Yang, Nicole Espinoza |
| **Section**      | CSC 165 — Section 02        |
| **Term**         | Spring 2026                 |

---

## 2. Screenshot

![[gem3.png|541]]
![[gem4.png|540]]

---

## 3. Compiling and Running the Game
### Step 1 — Compile

Run the provided batch script from the project root:

```
compile.bat
```

### Step 2 — Start the Network Server

Run the server first (required for multiplayer):

```
run_server.bat
```

The server listens on port **6010** using **UDP**. It also broadcasts on UDP port **5555** for LAN auto-discovery.

### Step 3 — Run a Client

```
run_client.bat
```

Or for single-player / host-only:

```
run_singleplayer.bat
```

---

## 4. How to Play

### Roles

Each round one player is assigned the **Zombie** role; all others are **Humans**.

| Role | Objective |
|---|---|
| **Human** | Survive for 300 seconds (5 minutes). Avoid being tagged by the zombie. |
| **Zombie** | Tag all humans before the timer runs out. |

### Round Structure

| Phase | Duration | Description |
|---|---|---|
| **Intermission** | 30 seconds | Humans prepare and build; pickups spawn across the map. |
| **Survival Round** | 300 seconds | Zombie hunts humans; humans evade and defend. |
| **Post-Round** | 5 seconds | Winner announced on HUD. |

**Humans Win** if at least one human survives the full 300 seconds.  
**Zombies Win** if all humans are tagged before time runs out.

### Health and Items

- Each player starts with **100 HP**.
- **Health Potions** restore **35 HP**.
- Humans can collect and equip: Flashlight, Rocks (max 3), Potion, Dash ability.
- Zombies can equip: Invisibility ability, Baby Zombie projectiles (max 2).

### Events

| Event                        | Trigger                                | Effect                                                             |
| ---------------------------- | -------------------------------------- | ------------------------------------------------------------------ |
| **Day/Night Cycle**          | Every 150 seconds                      | Lighting, fog, and skybox shift between day and night.             |
| **NIGHT FALLS**              | When night begins                      | HUD announcement; flashlights become critical for humans.          |
| **DAY BREAKS**               | When day begins                        | HUD announcement; improved visibility returns.                     |
| **Pickup Spawn**             | Intermission start                     | 135 pickups scattered across 9 types spawn across the map.         |
| **Pickup Respawn**           | 30 seconds after collected             | Each pickup reappears at its original location.                    |
| **Smiling Man**              | Spawns after 30 seconds or 150 seconds | AI threat that wanders and reacts to noise and eye contact.        |
| **Mushroom Mon**             | Always present                         | Explosive NPC that spawns at map edges and detonates near players. |
| **Round Start**              | After intermission                     | Zombie role assigned by server; survival timer begins.             |
| **HUMANS WIN / ZOMBIES WIN** | Round end                              | HUD broadcast with result.                                         |

### Scoring
There is no point-based scoring. The result is purely win/loss per round based on the objective above.

---

## 5. Player Controls

### Keyboard and Mouse

| Key | Action |
|---|---|
| **W** | Move forward |
| **S** | Move backward |
| **A** | Strafe left |
| **D** | Strafe right |
| **Left Arrow** | Rotate camera left |
| **Right Arrow** | Rotate camera right |
| **Left Shift** | Run |
| **Space** | Jump |
| **F** | Equip flashlight (Human) / Use invisibility ability (Zombie) |
| **Q** | Equip rock (Human) / Equip baby zombie (Zombie) |
| **R** | Use dash (Human) / Equip baby zombie (Zombie) / Rotate build piece |
| **C** | Equip health potion |
| **E** | Toggle build mode |
| **B** | Cycle build material (Wood → Metal → Glass) |
| **. (Period)** | Raise build piece height |
| **N** | Lower build piece height |
| **Mouse Left Click** | Primary action: use/throw equipped item, attack (Zombie), place build piece |
| **Mouse Movement** | Look / camera rotation (when mouse-look is active) |
| **Tab** | Toggle mouse-look mode |
| **G** | Toggle camera mode (third-person / first-person) |
| **Y** | Swap shoulder-camera side |
| **M** | Open full map |
| **I** | Cycle instructions (keyboard controls → Xbox controls → off) |
| **T** | Toggle axes visualization |
| **F1** | Show help |
| **F2** | Toggle physics debug mode |
| **F3** | Toggle zombie role (testing only) |
| **Up Arrow** | Elevate camera (full map mode) |
| **Down Arrow** | Lower camera (full map mode) |
| **Page Up** | Zoom camera out |
| **Page Down** | Zoom camera in |
| **K / J / L** | Pan full map up / left / right |
| **U / O** | Zoom full map out / in |

### Xbox One Controller

| Xbox Control | TAGE Mapping | Action |
|---|---|---|
| **Left Stick (Y-axis)** | Axis.Y | Move forward / backward |
| **Left Stick (X-axis)** | Axis.X | Strafe left / right |
| **Right Stick (RX-axis)** | Axis.RX | Rotate camera |
| **Right Stick (RY-axis)** | Axis.RY | Tilt camera up / down |
| **A** | Button 0 | Jump |
| **B** | Button 1 | Toggle build mode |
| **X** | Button 2 | Equip flashlight (Human) / Use invisibility (Zombie) |
| **Y** | Button 3 | Equip potion (Human) / Cycle build material (build mode) |
| **LB** | Button 4 | Equip rock (Human) / Switch wall or roof (build mode) |
| **RB** | Button 5 | Use dash (Human) / Equip baby zombie (Zombie) |
| **Select** | Button 6 | Open full map |
| **Start** | Button 7 | Open instructions |
| **Left Stick Click** | Button 8 | Run |
| **Right Stick Click** | Button 9 | Switch POV / camera mode |
| **Home** | Button 10 | Open instructions |
| **LT** | Axis.Z | Zoom full map (in map mode) |
| **RT** | Axis.RZ | Primary action (use item, attack, throw, place build piece) |

**D-pad — Outside Full Map Mode:**

| D-pad Direction | Action |
|---|---|
| Up | Equip flashlight (Human) / Use invisibility (Zombie) |
| Left | Equip rock (Human) / Switch build piece |
| Right | Use dash (Human) / Equip baby zombie (Zombie) / Rotate build piece |
| Down | Toggle build mode |

**D-pad — Inside Full Map Mode:**

| D-pad Direction | Action |
|---|---|
| Any direction | Pan the full map |
| LT / RT | Zoom the full map |

The HUD automatically switches between keyboard and Xbox control layouts based on the last detected input device.

---

## 6. Lighting

The game uses a dynamic lighting system that changes throughout the day/night cycle (150 seconds day, 150 seconds night — 300 seconds total).

### Lights

**Sun Light (Directional/Positional)**
The primary scene light moves in a circular arc across the sky. During the day its diffuse color is a warm white (1.00, 0.94, 0.82); as night falls it lerps to near-black (0.04, 0.05, 0.08). The light position shifts height and arc angle to simulate sunrise and sunset.

**Player Night Light (Point Light) — turns on at night**
Active only for human players and only during night. Positioned approximately 2.65 units above the player's head. Diffuse: (0.95, 0.84, 0.58). This light is intentionally local-only and not networked, providing a personal ambient glow so the human can see their immediate surroundings. It turns off automatically when day returns.

**Flashlight (Spotlight) — turns on/off on player input**
A spotlight with a 30-degree cutoff angle. Diffuse: (2.80, 2.55, 1.85); Specular: (2.20, 2.05, 1.65). Attenuation is tuned for realistic beam falloff. The flashlight state is networked, so all clients can see when another player has theirs active. Pressing **F** (keyboard) or **X** (controller) toggles it.

### Global Properties That Change with Time of Day

| Property | Day Value | Night Value |
|---|---|---|
| **Global Ambient** | (0.42, 0.42, 0.40) | (0.11, 0.12, 0.16) |
| **Fog Color** | (0.58–0.75, 0.66–0.84, 0.76–0.94) | (0.035, 0.052, 0.085) |
| **Fog Range** | 95–340 units | 42–185 units |
| **Skybox** | Sky 04 (day cubemap) | Sky 15 (night cubemap) |

**Zombie Vision:** The zombie player always receives a minimum of 70% ambient light so they retain reasonable visibility at night, representing their undead perception.

---

## 7. Network Protocol Changes

A NetworkDiscovery helper class was added to TAGE to support LAN auto-discovery. When a client is launched with the AUTO token instead of an explicit server address, it broadcasts a UDP discovery packet on port 6011. The server listens on that port through a discovery responder and replies with the game server’s port and protocol. The client then uses the IP address from the response packet to connect automatically, allowing multiplayer clients to join without any manual IP configuration. This makes it easier to run the game on lab or local network machines where the server IP may change. No changes were made to the existing message formats or packet types in the base CSC 165 network protocol; the discovery system only helps locate the server before the normal client-server connection begins.

---

## 8. Changes and Additions to TAGE

### New Files Added to TAGE

| File | Description |
|---|---|
| `CameraOrbit3D.java` | Third-person orbit camera with orbit, elevate, and zoom controls; clamped angles and avatar-relative positioning |
| `networking/NetworkDiscovery.java` | LAN server auto-discovery helper using UDP broadcasts and an `AUTO` token |
| `nodeControllers/BobbingController.java` | Sine-wave bobbing controller with per-object timing, phase offset, and `rebase()` |
| `shapes/ManualPyramid.java` | Manually built pyramid mesh with UVs, normals, and material setup |
| `shapes/ManualQuad.java` | Manually built quad mesh with UVs, normals, and material setup |

### Modified TAGE Files

| File | Changes |
|---|---|
| `Camera.java` | Added `yaw()` and `pitch()` camera rotation helpers |
| `GameObject.java` | Added local `yaw()`, `pitch()`, `globalYaw()`; added detail-texture and normal-map fields, getters, and setters |
| `HUDmanager.java` | Expanded from 2 HUD strings to 5; added depth-tested world-positioned HUD labels |
| `Viewport.java` | Added `setEnabled()` / `isEnabled()` enable/disable support |
| `RenderStates.java` | Added render flags for shadow, bump mapping, normal mapping, fog, texture detail blend, mapping mode, bump style, and surface scale |
| `RenderSystem.java` | Added shadow-map rendering, shadow shaders, screen flash overlay, cloud overlay, fog/detail controls, disabled viewport skipping, and world-HUD matrix support |
| `RenderQueue.java` | Now clears `transparentQueue` before rebuilding to prevent stale transparent objects from carrying over |
| `RenderObjectStandard.java` | Sends fog, camera position, shadow, normal-map, detail-texture, bump, and transparency uniforms/textures to shaders |
| `RenderObjectAnimation.java` | Same render feature support as standard objects, plus SSBO upload for skin matrices and transparency handling |
| `AnimatedShape.java` | Added `MAX_SKIN_BONES = 256`, `MATRIX4` animation support, inverse bind matrices, Rigify skeleton repair/stretch handling, and safer animation validation |
| `Animation.java` | Tracks `MATRIX4` frame format; can read a bone's full matrix from animation data |
| `ImportedModel.java` | OBJ importer now handles blank/comment lines, flexible whitespace, missing UVs/normals, generated UV fallback, and negative OBJ indices |
| `GameConnectionClient.java` | Replaced unsynchronized `ArrayList` packet queues with `ConcurrentLinkedQueue` |
| `TCPClientSocket.java` | Initializes and reuses object streams; adds send/receive locks; flushes and resets output stream |
| `VariableFrameRateGame.java` | Added overridable hooks for screen flash color, opacity, vignette, and cloud night factor |
| `Utils.java` | Image loading now throws a clearer error when `ImageIO.read()` returns null |
| `JOALAudioManager.java` | Fixed Javadoc link from `sage.audio` to `tage.audio` |

### Custom Shaders

| Shader | Purpose |
|---|---|
| `StandardVert.glsl` / `StandardFrag.glsl` | Main vertex/fragment shader with ADS lighting, fog, and shadow map sampling |
| `SkeletalVert.glsl` | Vertex shader with bone-weighted skeletal animation |
| `ShadowVert.glsl` / `ShadowFrag.glsl` | Shadow map generation pass |
| `SkyBoxVert.glsl` / `SkyBoxFrag.glsl` | Skybox cubemap rendering |
| `CloudFrag.glsl` | Procedural cloud/sky color effects |
| `ScreenFlashVert.glsl` / `ScreenFlashFrag.glsl` | Full-screen flash effect for damage feedback |
| `heightCompute.glsl` | GPU-side terrain height computation |

---

## 9. Game Statement

| Property | Description |
|---|---|
| **Genre** | Multiplayer asymmetric survival / tag |
| **Theme** | Zombie apocalypse — a group of human survivors must outlast an undead hunter in a decaying open world |
| **Dimensionality** | 3D — third-person perspective with an optional first-person POV and a full top-down map view |
| **Activities** | Moving through terrain; collecting and managing items (flashlights, potions, rocks, dash pickups); building protective structures (walls and roofs in wood, metal, or glass); evading or hunting other players; surviving AI threats (Smiling Man, Mushroom Mon); using abilities (flashlight blinding, invisibility, dash) |

---

## 10. Project Requirements — Visible in the Game

| Requirement                 | Where to See It in the Game                                                                                                                                                                                                                                                                                                                                             |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **External Models**         | Player characters, NPCs (Smiling Man, Mushroom Mon), trees, rocks, items (flashlight, health potion, dash pickup), and building pieces (walls, roofs) are all rendered from external `.obj` models visible throughout the game world.                                                                                                                                   |
| **Networked Multiplayer**   | A second player appears as a synchronized ghost avatar in the world. Their position, animation, equipped item, and flashlight state update in real time. The server assigns roles, manages the round timer, and broadcasts win/loss results to all clients.                                                                                                             |
| **Skybox & Terrain**        | A cubemap skybox surrounds the scene at all times. During the day the Sky 04 cubemap is active; at night it switches to the Sky 15 night sky. The terrain is a heightmap-based landscape that players walk across, with trees and rocks placed on its surface.                                                                                                          |
| **Lights**                  | Three light types are active in the scene. The sun light shifts color and position across the sky throughout the day/night cycle. At night, a point light appears above the human player's head to provide local visibility. The flashlight (toggled with F or X on controller) casts a visible spotlight cone that other players can also see.                         |
| **HUD**                     | The HUD displays the player's current health, equipped item, remaining round time, event announcements ("NIGHT FALLS", "DAY BREAKS", "HUMANS WIN"), floating name labels above other players, and a context-sensitive control reference that switches between keyboard and Xbox layouts based on the active input device. Pressing M opens a full top-down map overlay. |
| **Hierarchical Scenegraph** | Equipped items (flashlight, rock, potion) are attached as children of the player node and move with the character. The flashlight spotlight is a child of the flashlight model, so it tracks the player's hand position and direction automatically. Building pieces are placed as children of the world root and remain fixed in the scene.                            |
| **Animation**               | Player characters play idle, walk, and run animations that blend based on movement speed. The Smiling Man NPC cycles through idle, wave, walk, and run animations depending on its AI state. The Mushroom Mon plays a walk animation as it approaches players.                                                                                                          |
| **NPCs**                    | Two NPC types are active in the world at all times. Smiling Man wanders the terrain, reacts to nearby sound and movement, and stares at the player when in range — staring for too long triggers a threat response. Mushroom Mon spawns at the edges of the play area, walks toward players, and explodes when it gets close enough.                                    |
| **Physics**                 | Rocks and baby zombies thrown by players follow realistic projectile arcs and collide with terrain and structures. Player movement is grounded to the heightmap terrain. If a player clips into geometry, a stuck-recovery system nudges them free.                                                                                                                     |

---

## 11. Requirements Not Successfully Implemented

All project requirements were successfully implemented.

---

## 12. Techniques Beyond the Stated Requirements

- **Shadow mapping** — a shadow render pass generates a depth map that is sampled in `StandardFrag.glsl` to cast real-time shadows.
- **Dual-skybox day/night switching** — the cubemap texture swaps between a day sky and a night sky with synchronized ambient and fog transitions.
- **Full rigid body physics** — LibBulletJME provides rock and baby zombie projectile trajectories, terrain collision, and a stuck-recovery system that nudges players out of geometry.
- **Two distinct AI behavior trees** — Smiling Man uses vision-cone detection and stare mechanics; Mushroom Mon uses proximity-triggered explosion logic. Both are implemented with a reusable behavior tree framework added to TAGE.
- **LAN auto-discovery** — clients broadcast a UDP discovery packet and receive the server address automatically, requiring no manual IP configuration.
---

## 13. Team Member Contributions

##### Givin Yang:
 - Skybox
 - Lights
 - HUD
 - Animation, Sound, Physics, Networking for:
	 - Player Model 1, SmilingMan
- NPC:
	- SmilingMan
 - Models designed
	- Player Model 1
	- SmilingMan
	- Health potion
	- Trees
	- Rocks
	- Houses
- Shadows
- Item pickup
- Build function
- LAN auto-discovery

##### Nicole Espinoza:
 - Terrain/Heightmap
 - Animation, Sound, Physics, Networking for:
	 - Player Model 2, MushroomMon
- NPC:
	- MushroomMon
 - Models designed
	- Player Model 2
	- MushroomMon
	- Table
	- Flashlight

---

## 14. Assets

### Self-Created Assets

**Character and NPC Models**

| Asset                        | Description                                    |
| ---------------------------- | ---------------------------------------------- |
| `boy_character_textured.obj` | First human player character model and texture |
| `playerModel2.obj`           | Second human player character model            |
| `babyZombie.obj`             | Baby zombie projectile model                   |
| `smilingMan.obj`             | Smiling Man AI NPC                             |
| `mushMon_Model.obj`          | Mushroom Mon AI  NPC                           |

**Environmental Models**

| Asset                                                | Description               |
| ---------------------------------------------------- | ------------------------- |
| `tree1.obj`, `tree2.obj`                             | Additional tree models    |
| `RockPile1.obj`                                      | Rock pile prop            |
| `table.obj`                                          | Table prop (cover object) |

**Item and Building Models**

| Asset                    | Description          |
| ------------------------ | -------------------- |
| `flashlight.obj`         | Flashlight item      |
| `healthpotion1.obj`      | Health potion pickup |
| `lowpolyhood.obj / .mtl` | Building roof piece  |

**Textures**

| File                      | Used For                              |
| ------------------------- | ------------------------------------- |
| `playerModel1.jpg`        | Player model 1 skin                   |
| `ZombiePlayerModel1.jpg`  | Player model 1 zombie skin            |
| `SmilingMan.png`          | Smiling Man NPC texture               |
| `mushMon_Txt.png`         | Mushroom Mon NPC texture              |
| `playerModel2.png`        | Player model 2 skin                   |
| `ZombiePlayerModel2.png`  | Player model 2 zombie skin            |
| `babyZombie.png`          | Baby zombie texture                   |
| `health_potion.png`       | Health potion item texture            |
| `flashlightTx.png`        | Flashlight model texture              |
| `tableTx.png`             | Table prop texture                    |
| `hood_leather.png`        | Building roof (lowpolyhood) texture   |
| `DeadOakTreeTrunk.png`    | dead oak tree trunk                   |
| `DeadSpruceTreeTrunk.png` | dead spruce tree trunk                |
| `OakTreeLeaf.png`         | oak tree leaf                         |
| `OakTreeTrunk.png`        | oak leaf tree trunk                   |
| `SpruceTreeLeaf.png`      | spruce tree leaf                      |
| `SpruceTreeTrunk.png`     | spruce tree trunk                     |
| `hood_leather.png`        | invi item                             |


**Skeletal Animation Data**

- Player animations: idle, walk, run (human and zombie variants)
- Smiling Man animations: idle, wave, walk, run (with blend transitions and outro)
- Mushroom Mon animation: walk

**Heightmap**
- `heightmap.png`

**Shaders**

All `.glsl` files listed in Section 8 were written for this project.

---
### Assets from CSC 155 / CSC 165 Distributions

| File                     | Source                         |
| ------------------------ | ------------------------------ |
| `dolphinHighPoly.obj`    | CSC 155 / CSC 165 distribution |
| `Dolphin_HighPolyUV.jpg` | CSC 155 / CSC 165 distribution |
| `fluffyClouds.jpg`       | CSC 155 / CSC 165 distribution |

---

## 15. Third-Party Asset Sources and Permissions

### Models
| File                                                    | Source                                                                              | License / Permission       |
| ------------------------------------------------------- | ----------------------------------------------------------------------------------- | -------------------------- |
| `OakTree1.obj`, `OakTree2.obj`, `OakTree3.obj`          | https://jaks.itch.io/lowpolyforestpack                                              | Free to use stated on page |
| `SpruceTree1.obj`, `SpruceTree2.obj`, `SpruceTree3.obj` | https://jaks.itch.io/lowpolyforestpack                                              | Free to use stated on page |
| `DeadOak1.obj`, `DeadOak2.obj`, `DeadOak3.obj`          | https://jaks.itch.io/lowpolyforestpack                                              | Free to use stated on page |
| `DeadSpruce2.obj`                                       | https://jaks.itch.io/lowpolyforestpack                                              | Free to use stated on page |
| `Rock1.obj`, `Rock2.obj`, `Rock10.obj`, `Rock11.obj`    | https://jaks.itch.io/lowpolyforestpack                                              | Free to use stated on page |
| `BigRock1.obj`, `BigRock2.obj`                          | https://jaks.itch.io/lowpolyforestpack                                              | Free to use stated on page |
| `dashPickup.obj / .mtl`                                 | https://free3d.com/3d-model/child-running-with-backpack-v2--55569.html?dd_referrer= | Free to use stated on page |

### Textures

| File                                                             | Source                                                                                                                                                                                                          | License / Permission                                                   |
| ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `Wood Texture 1.png` / `Wood Texture 1_NRM.png`                  | https://owletrius.itch.io/wood-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `Wood Texture 2.png` / `Wood Texture 2_NRM.png`                  | https://owletrius.itch.io/wood-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `Wood Texture 3.png` / `Wood Texture 3_NRM.png`                  | https://owletrius.itch.io/wood-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `Wood Texture 4.png` / `Wood Texture 4_NRM.png`                  | https://owletrius.itch.io/wood-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `bigsand.png`                                                    | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `bricks.png`                                                     | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `bricks2.png`                                                    | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `drygrass.png`                                                   | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `grass2.png`                                                     | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `ground.png`                                                     | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `metal.png`                                                      | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `rockTexture1.png`                                               | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `rockwall.png`                                                   | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `rustymetal.png`                                                 | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `rustywall.png`                                                  | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `rustywall2.png`                                                 | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `rustywall3.png`                                                 | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `sand.png`                                                       | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `sky.png`                                                        | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `treewood.png`                                                   | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `wall.png`                                                       | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `wall2.png`                                                      | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `wall3.png`                                                      | https://zlab.itch.io/realistic-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `woodplanks.png`                                                 | https://owletrius.itch.io/wood-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `woodplanks2.png`                                                | https://owletrius.itch.io/wood-texture-pack                                                                                                                                                                     | Free to use stated on page                                             |
| `abstract-background-with-patterned-glass-texture 1024x1024.jpg` | [Link](https://www.magnific.com/free-photo/abstract-background-with-patterned-glass-texture_17207553.htm#fromView=keyword&page=1&position=4&uuid=986a5950-06c4-41f3-8a5b-7a357f10951a&query=Game+glass+texture) | [Free for commercial use](https://www.magnific.com/legal/terms-of-use) |

---
### Sounds

All sound files are mono `.wav` files used for in-game audio. The filenames below reflect the original (stereo) source files; mono-prefixed copies (`mono_*.wav`) are downmixed versions of the same sources.

Website source: https://pixabay.com/
License/Permission: https://pixabay.com/service/license-summary/

| File                                                            | Source                                                                      |
| --------------------------------------------------------------- | --------------------------------------------------------------------------- |
| `eaglaxle-dirt-footsteps-2-455146.wav`                          | https://pixabay.com/sound-effects/search/dirt/                              |
| `freesound_community-energy-1-107099.wav`                       | https://pixabay.com/sound-effects/search/sad%20pick%20up/?duration=0-30     |
| `freesound_community-female-robotic-countdown-5-to-1-47653.wav` | https://pixabay.com/sound-effects/search/robotic%20countdown/               |
| `freesound_community-flashlight-clicking-on-105809.wav`         | https://pixabay.com/sound-effects/search/flashlight/                        |
| `freesound_community-hello-81683.wav`                           | https://pixabay.com/sound-effects/people-hello-81683/                       |
| `freesound_community-item-equip-6904.wav`                       | https://pixabay.com/sound-effects/search/pick%20up/                         |
| `freesound_community-monster-breathing-67456.wav`               | https://pixabay.com/sound-effects/search/breathing/?pagi=2                  |
| `freesound_community-running-on-dirt-87203.wav`                 | https://pixabay.com/sound-effects/search/dirt/                              |
| `freesound_community-running-sounds-6003.wav`                   | https://pixabay.com/sound-effects/film-special-effects-running-sounds-6003/ |
| `freesound_community-scary-night-ambience-75274.wav`            | https://pixabay.com/sound-effects/search/scary/                             |
| `freesound_community-zombie-breathing-70682.wav`                | https://pixabay.com/sound-effects/search/zombie/                            |
| `freesound_community-zombie-sounds-95180.wav`                   | https://pixabay.com/sound-effects/search/zombie/                            |
| `hasin2004-breathing-fast-247449.wav`                           | https://pixabay.com/sound-effects/search/breathing/?pagi=2                  |
| `jokerzillagames-walking-366933.wav`                            | https://pixabay.com/sound-effects/film-special-effects-walking-366933/      |
| `oliper18-scary-laugh-377526.wav`                               | https://pixabay.com/sound-effects/search/scary/                             |
| `ribhavagrawal-energy-drink-effect-230559.wav`                  | https://pixabay.com/sound-effects/search/drink/                             |
| `ribhavagrawal-heavy-breathing-sound-effect-type-03-294201.wav` | https://pixabay.com/sound-effects/search/heavy%20breathing/                 |
| `sound_garage-cat-meow-8-fx-306184.wav`                         | https://pixabay.com/sound-effects/search/meow/                              |
| `u_hn60smked5-sound-effect-breathing-hard-122341.wav`           | https://pixabay.com/sound-effects/search/heavy%20breathing/                 |
| `u_scysdwddsp-wood-effect-254997.wav`                           | https://pixabay.com/sound-effects/search/wood%20block/?duration=0-30        |
| `mushroom-chase.wav`                                            | https://pixabay.com/sound-effects/search/cute/                              |
| `mushroom-poof.wav`                                             | https://pixabay.com/sound-effects/search/poof/                              |
| `mushroom-smoke-hiss.wav`                                       | https://pixabay.com/sound-effects/search/smoke/                             |
| `mushroom-walk.wav`                                             | https://pixabay.com/sound-effects/search/squeaky%20shoes/                   |

---
### Skyboxes

| File / Set      | Source                                            | License / Permission       |
| --------------- | ------------------------------------------------- | -------------------------- |
| `sky04_cubemap` | https://opengameart.org/content/cloudy-skyboxes-0 | Free to use stated on page |
| `sky15_night`   | https://opengameart.org/content/cloudy-skyboxes-0 | Free to use stated on page |

---
## 16. Lab Machines Tested On

 - **ECS-FALLOUT**
 - **ECS-CENTIPEDE**
