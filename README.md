## New: What I Changed

### SmilingMan NPC System

I added a full SmilingMan NPC system instead of having SmilingMan just stand in the world as a model.

- SmilingMan has his own AI controller.
- Each SmilingMan has its own per-NPC state.
- SmilingMan behavior is split into clear phases.
- Target data is stored separately so the chase logic is easier to manage.
- Sound handling is separated from the main AI logic.

I split the SmilingMan code into separate files so the structure is easier to copy for future NPCs.

- `MyGameSmilingManSystem`
- `MyGameSmilingManAgent`
- `MyGameSmilingManPhase`
- `MyGameSmilingManHumanTarget`
- `MyGameSmilingManSoundSet`

### SmilingMan Animation

I added skeletal animation support for SmilingMan because he had too many bones.

- SmilingMan uses a custom animated model created in Blender.
- I exported the mesh, skeleton, texture, and animation clips into TAGE files.
- The AI system plays the correct animation based on what SmilingMan is doing.

SmilingMan now has these animation clips:

- Idle
- Walk forward
- Idle wave
- Run transition
- Run initial
- Run loop
- Run outro
- Run-to-stand

### TAGE Animation Updates

I updated the TAGE animation system so SmilingMan's rig can work correctly.

- TAGE now supports larger skeletal rigs.
- The animated renderer uploads skeletal matrices more safely.
- The skeletal shader path supports more complex bone data.
- These changes were needed because SmilingMan's rig is more complex than the original player model rig.

I also added transparency support for animated models.

- Animated characters can now use opacity like normal static objects.
- This lets `playerModel1` become transparent when needed.
- This is used when a human hides inside a table.

### SmilingMan Spawning

I added SmilingMan spawning rules.

- SmilingMan can spawn when humans stand still for too long.
- The stillness timer is shared between players.
- For example, several players standing still briefly can add up toward the spawn timer.
- SmilingMen can also spawn around half-time in the round.
- There can now be up to five SmilingMen in one round.

### SmilingMan Wandering And Searching

I added SmilingMan wandering behavior.

- When he is not chasing anyone, he walks around the map.
- He pauses sometimes.
- He turns slowly.
- He behaves more naturally instead of moving randomly every frame.

I also added two kinds of detection.

- A long forward vision range with a wide viewing angle.
- A closer proximity range that helps him move toward nearby humans even if he does not directly see them yet.

This makes SmilingMan more dangerous without making him instantly target every player across the whole map.

### SmilingMan Targeting

I added target collection behavior.

- When SmilingMan first sees humans, he does not immediately pick one.
- He keeps walking briefly while collecting humans that enter his detection range.
- After the collection timer ends, he randomly chooses one valid human from that stored list.
- Once he chooses a target, he focuses on that human.

I also made SmilingMen avoid chasing the same human when possible.

- If one SmilingMan is already chasing a player, another SmilingMan tries not to claim that same target.
- This spreads pressure across the map instead of stacking every SmilingMan onto one player.

### SmilingMan Chase And Attacks

I added SmilingMan chase behavior.

- He faces the chosen target.
- He waves before chasing.
- He transitions into running.
- He loops his running animation while chasing.
- His run speed is 1.25 times the human run speed.

I added SmilingMan attack behavior.

- When SmilingMan collides with a human, he deals damage.
- He stops after the hit.
- He plays his run outro and stand transition.
- He waves again.
- Then he returns to chasing if the target is still human.
- If the target becomes a zombie, SmilingMan forgets that target and returns to wandering.

I added stuck logic.

- If SmilingMan gets stuck pacing in the same area for several seconds, he can abandon the current target.
- He can choose another remembered human from the list he collected earlier.
- This prevents him from getting trapped forever by unreachable players.

### SmilingMan And Builds

I added SmilingMan build-breaking behavior.

- SmilingMan can damage human-built structures.
- He can break through builds instead of getting stuck forever.
- Stronger materials take more hits to break.
- Building can buy humans time, but it is not a permanent defense.

I also added build and cover collision support for NPCs.

- SmilingMan can detect when world objects or build pieces are blocking him.
- He can route around scenery or break builds when needed.
- SmilingMan line of sight checks cover and build pieces.

### Noise And Fake Idle

I added a noise system for SmilingMan.

- Sprinting creates noise.
- Building creates noise.
- Throwing rocks creates noise.
- Noise can attract SmilingMen toward the source.

I also added fake idle behavior.

- Sometimes SmilingMan acts like he has not noticed a player.
- Then he can suddenly turn or react.
- This makes his behavior feel less predictable.

### SmilingMan Sound

I added SmilingMan sound effects.

- Walking sounds play while he is walking.
- Running sounds play while he is chasing.
- Heavy breathing plays for chase tension.
- A hello sound plays during his wave animation.

These sounds are connected to his behavior state, so walking, running, chasing, and waving all have different audio feedback.

### Human Fear Feedback

I added human panic breathing.

- Breathing becomes more intense when the human has low health.
- Breathing also becomes more intense when SmilingMan is close.
- This gives the player danger feedback without relying only on the HUD.

I also added SmilingMan screen warnings.

- The screen can darken around the edges when SmilingMan sees or chases the local player.
- This makes danger feel more atmospheric.
- It gives the player a warning before or during a chase.

### Flashlight Battery And Blinding

I added flashlight battery gameplay.

- Flashlights no longer work forever.
- The battery lasts around 20 seconds.
- Turning the flashlight off lets it recharge over time.
- Blinding a zombie or SmilingMan costs extra battery.
- This makes the flashlight powerful but prevents endless spam.

I added flashlight blinding.

- Humans can blind zombie players with the flashlight.
- Zombie players get a white screen effect for a short time.
- Humans can also blind SmilingMan.
- SmilingMan is stunned briefly before continuing.
- Zombies and SmilingMan both have blind cooldowns so they cannot be blinded repeatedly with no counterplay.
- SmilingMan blind state is synced so the stun is consistent in multiplayer.

### Screen Overlay Rendering

I added a screen overlay system to TAGE.

- The engine can draw a full-screen white flash for flashlight blinds.
- The engine can draw a dark vignette for SmilingMan warnings.
- This supports flashlight blind effects, chase warnings, glass builds, and table hiding feedback.

### Table Hiding

I added table hiding gameplay.

- Tables are scattered around the map.
- Humans can walk through tables.
- When a human is inside a table, that human becomes transparent.
- This makes the human harder for zombie players to see.
- SmilingMan has a shorter detection range against humans hiding inside tables.

I removed the manually placed center table.

- At least 15 tables are now placed around the map.
- Tables are spread out through the world scatter system.
- This gives multiple players hiding options.

### Random World Cover

I removed the manually placed center house and replaced it with randomized cover.

- The game now generates multiple house-like structures around the map.
- These use the same kind of wall and roof pieces as the player build system.
- The map feels less empty.
- Humans have more natural cover.

I added randomized wall cover.

- Single walls can spawn.
- Connected wall groups can spawn.
- 2x2-style wall cover can spawn.
- Tall stacked wall cover can spawn.
- These pieces use real collision.

I added randomized roof cover.

- Single roof ramps can spawn.
- Two roofs can spawn next to each other.
- Two-roof stair and ramp patterns can spawn.
- I fixed roof stair placement so the upper roof snaps above and forward from the lower roof.

I also changed connected cover materials so they match.

- Houses use the same material across connected pieces.
- Connected wall groups use one material.
- Connected roof groups use one material.
- This prevents generated structures from looking mismatched.

### Build Materials

I added build material types.

- Wood takes 2 hits.
- Metal takes 5 hits.
- Glass takes 1 hit.
- Each material now has a different gameplay purpose.

I updated build material visuals.

- Wood uses the wood planks texture.
- Metal uses the rusty metal texture.
- Glass uses the patterned glass texture with transparency.
- Health potions use a red liquid look instead of blue.

I added glass hiding behavior.

- Humans can see through glass.
- Zombie players can visually see through glass.
- SmilingMan cannot see through glass build pieces.
- This makes glass useful for hiding from SmilingMan, but not a perfect defense against real players.

### Build Support And Collapse

I added support-collapse behavior for builds.

- Humans can build upward.
- Upper pieces need support below them.
- If the bottom support is destroyed, unsupported pieces do not disappear all at once.
- The structure collapses from bottom to top over time.
- This gives humans a chance to jump away or react.

I updated build networking.

- Build messages now include material type.
- Remove-build messages also include the needed material data.
- All clients see the same material, texture, and durability.

### Last-Chance Escape

I added a last-chance escape mechanic for humans.

- When a human is about to be converted at 0 health, there is a small chance they survive.
- A lucky human respawns somewhere else with 1 health.
- The starting zombie does not get this chance.
- This prevents the round from breaking by having no zombie.

I also updated SmilingMan target handling for this mechanic.

- If SmilingMan's target escapes through this chance, SmilingMan does not stay locked into a broken chase.
- He can treat it like a failed target and choose another remembered human.

### Random Heightmaps And Terrain

I added multiple randomized heightmaps.

- The game now loads heightmap 1, 2, 3, and 4.
- A new round randomly chooses one of them.
- This makes each round feel less repetitive.

I fixed the new heightmap image formats.

- Some new heightmaps were 16-bit grayscale PNGs.
- TAGE and JOGL could not load that format.
- I converted them into TAGE-safe 8-bit PNG formats.

I added terrain physics rebuilding.

- When the heightmap changes, the terrain physics mesh is rebuilt.
- The physical terrain now matches the visual terrain.
- Players, pickups, scenery, and cover are placed based on the new terrain height.

### World Placement And Boundaries

I updated scenery and cover planting.

- Random trees, rocks, tables, houses, walls, and roofs are placed without unsafe OpenGL height sampling during startup.
- After OpenGL and physics are ready, the objects are snapped safely to the terrain.
- This fixed the crash caused by sampling terrain height too early.

I added world-edge boundaries.

- Players are clamped inside the playable terrain area.
- Players cannot run off the map.
- Players cannot fall past the terrain edge.
- This makes the world safer for larger multiplayer rounds.

### Movement And Camera

I normalized diagonal movement.

- Pressing forward and sideways at the same time no longer gives extra speed.
- Humans and zombies move at the intended speed in every direction.

I changed camera controls.

- Arrow keys rotate the camera around the player.
- Arrow keys no longer move the player left and right.
- Page Up zooms in.
- Page Down zooms out.
- The HUD tells players that `G` switches point of view.

### HUD Updates

I added more HUD information for gameplay clarity.

- Role
- Round state
- Health
- Build material counts
- Flashlight battery
- Coordinates labeled with X, Y, and Z
- Zombie tracking information
- Instructions for human and zombie controls

I also added zombie tracking HUD behavior.

- Zombies periodically receive the closest human's position.
- This helps zombie players find humans during large rounds.

### Items And Scenery Counts

I increased world item counts for larger rounds.

- More flashlights
- More building materials
- More dash abilities
- More invisibility abilities
- More rocks
- More health potions
- More baby zombie pickups

I updated item and scenery visuals.

- Flashlights are scaled larger so they are easier to see.
- Collectible items are scattered across the whole map.
- There are more trees and rocks around the map.
- Trees are scaled larger so the world feels more like a forest.

### Multiplayer Sync

I added SmilingMan multiplayer sync.

- SmilingMan spawned status is synced.
- SmilingMan position is synced.
- SmilingMan yaw is synced.
- SmilingMan animation state is synced.
- This helps all players see the same SmilingMan behavior.

I updated multiplayer messages for the new gameplay systems.

- SmilingMan messages
- Build material messages
- Health changes
- Ability states
- Blind effects
- Round state
- Pickup state

I updated server round snapshots.

- A joining or reconnecting player can receive current round information.
- The snapshot includes roles, health, abilities, pickups, builds, and round status.
- This helps late-joining clients catch up.

### Update Loop And Constants

I updated the game loop through `MyGameUpdater`.

- The SmilingMan system updates each frame.
- The build system updates each frame.
- The visual hiding system updates each frame.
- The sound system updates each frame.
- The item system updates each frame.
- Networking pieces update in the correct order.

I added new constants for the expanded systems.

- SmilingMan speed and detection values
- Maximum SmilingMan count
- World size
- Flashlight battery values
- Blind cooldowns
- Pickup counts
- Build material types
- World cover counts
- Randomized map settings

### Engine Rendering Fixes

I made more engine-level rendering fixes.

- I added screen overlay hooks to `VariableFrameRateGame`.
- I updated `RenderSystem` to draw screen overlays.
- I improved transparency handling.
- I updated animated rendering so animated characters can use opacity.
- These changes support flashlight blinds, SmilingMan warnings, glass builds, and table hiding.

## What I Changed BEFORE

### Project Direction

I removed the old photo-taking system because it no longer matched the direction of the project.

- Deleted the old photo system.
- Removed the old photo actions.
- Removed the win condition where players had to take and place pictures.
- Changed the game focus to surviving, tagging, collecting items, building defenses, and using role-based abilities.

### Round Flow

I changed the game to use a full round system.

- The game starts with an intermission.
- When enough players are connected, the round begins.
- One player is randomly chosen as the starting zombie.
- Each round lasts 300 seconds.
- Humans win if at least one human survives until the timer reaches zero.
- Zombies win if all humans are tagged or converted before time runs out.
- After a round ends, the game resets back into intermission instead of stopping completely.

### Humans And Zombies

I added separate roles for humans and zombies.

- Zombies can tag humans and turn them into zombies.
- Humans also become zombies if their health reaches zero.
- Zombies do not use health, so the game can focus zombie and NPC logic on living human players.
- The HUD changes based on the player's role.
- Humans do not see zombie-only resources.
- Zombies do not see human-only resources such as building materials.

### Player Items And Abilities

I gave each role its own tools.

Humans can use:

- Dash
- Rocks
- Building materials
- Flashlights
- Health potions

Zombies can use:

- Invisibility
- Baby zombie projectiles
- Zombie attack or tag actions

I also cleaned up the HUD so it shows the current role, round state, timer, health, ability readiness, and only the resources that matter to that player.

### Pickups

I added synced pickups across multiplayer.

- Pickups have server-tracked locations.
- Pickups have active and inactive states.
- Pickups have respawn timers.
- Pickup rewards are handled by the server.
- When one player collects a pickup, it disappears for everyone.
- The pickup later respawns somewhere else.
- I increased the number of pickups so the game can support a larger classroom demo.

I also changed the pickup models so they are easier to understand.

- Building materials use a cube with a wood texture.
- Rocks use the rock model and rock texture.
- Dash uses the dash pickup model.
- Invisibility uses the hood model.
- Baby zombie pickups use the baby zombie model and texture.
- The hood pickup now uses a leather-style texture so it no longer looks like a rock.
- Potions and flashlights bob up and down so they are easier to notice.

### Held Items

I added visible held items for players.

- Humans show the flashlight, potion, or rock near their hand when equipped.
- Zombies show the baby zombie near their hand before throwing it.
- Equipped items are exclusive, so equipping one item replaces the current one.
- This prevents several held items from conflicting visually or mechanically.

### Controls

I changed the controls so the game is easier to play while using the mouse to look around.

Human controls:

- `F` equips or unequips the flashlight.
- `C` equips or unequips the potion.
- `Q` equips or unequips the rock.
- `R` uses dash.
- `E` toggles build mode.
- In build mode, `Q` switches the build piece.
- In build mode, `R` rotates the build piece.
- Left mouse click places a build piece or uses the currently equipped item.

Zombie controls:

- `F` uses invisibility.
- `R` equips or unequips the baby zombie projectile.
- Left mouse click throws the baby zombie if it is equipped.
- If no baby zombie is equipped, left mouse click becomes the zombie attack or tag action.
- Zombies can also damage build pieces.

### Building

I added building gameplay for humans.

- Humans can collect build materials.
- Humans can place walls, floors, roofs, and related build pieces.
- Build pieces are synced in multiplayer.
- Build pieces have physics colliders so players cannot walk through them.
- Zombies can attack build pieces.
- Damaged build pieces are destroyed and removed for everyone after enough hits.

I also improved build movement.

- Players can stand on floors and roofs.
- The movement system checks walkable build surfaces.
- The physics system checks terrain and build heights.
- Players can move across terrain, rocks, and build pieces more naturally.

### Projectiles

I added projectile gameplay for both sides.

- Humans can throw rocks to slow zombies.
- Zombies can throw baby zombies to slow humans.
- Projectiles use physics bodies.
- Projectiles move through the world and collide with players or scenery.
- Projectiles are synced across multiplayer.
- This gives both roles a ranged option instead of relying only on direct contact.

### Zombie Invisibility

I added zombie invisibility and synced it across multiplayer.

- When a zombie uses invisibility, other players see that zombie become hidden.
- The invisibility state is sent through the network.
- When the timer ends, the zombie becomes visible again.
- This makes invisibility useful for sneaking up on humans.

### Spawning

I added random spawn logic.

- Players no longer always start in the same place.
- The spawn system tries to avoid blocked areas.
- It avoids scenery, trees, rocks, and the house.
- This makes rounds less predictable and helps avoid unfair starts.

### Terrain And Scenery

I improved the terrain and world objects.

- The terrain now uses the grass texture and height map.
- I adjusted texture tiling so the ground does not look stretched.
- I randomly placed trees, dead trees, rocks, and rock piles around the map.
- I adjusted object scaling so trees are larger, rocks are smaller, and rock piles fit the world better.

I also made scenery sit better on the terrain.

- Trees and rocks use terrain height and model bounds when placed.
- This keeps them from floating or sinking too much.
- Trees and rocks have colliders so players cannot walk through them.
- Collider sizes are smaller so they feel closer to the visible models instead of acting like oversized invisible walls.

### Networking

I added multiplayer tracking for important gameplay state.

The server now tracks:

- Round state
- Intermission
- Active pickups
- Pickup respawns
- Roles
- Health
- Invisibility
- Builds
- Projectiles
- Animations
- Sounds
- Win conditions

I updated `ProtocolClient`, `GameServerUDP`, and `GameServerTCP` so clients can send and receive messages for:

- Roles
- Tags
- Health
- Abilities
- Projectiles
- Slows
- Pickups
- Builds
- Build removal
- Animation states
- Round states
- Sound effects

I also added server-side classes such as `ServerGameState`, `PickupState`, and `ServerMessenger` to organize the authoritative multiplayer state.

### Animations And Visuals

I added player animation syncing.

- The local player tracks whether they are idle, walking, or running.
- That animation state is sent to the server.
- Remote players play the matching animation.
- Other players can see whether someone is idle, walking, or running.

I also added zombie visual changes.

- When `playerModel1` becomes a zombie, its texture changes to the zombie player texture.
- Other player models use a zombie-colored visual state.
- When a player becomes human again during reset or intermission, their normal appearance is restored.

### Sound

I added a WAV-only sound system using TAGE audio.

- I removed MP3 support.
- I deleted the old MP3 sound files.
- I created mono WAV versions because OpenAL spatial audio works better with mono sound sources.

The game now uses TAGE and OpenAL 3D sound for:

- Footsteps
- Running
- Pickup collection
- Countdown
- Flashlight click
- Equip sounds
- Ambience
- Zombie breathing
- Scary laugh
- Potion healing
- Human breathing
- Building sounds

I also synced important sound effects in multiplayer.

- Flashlight clicks, item equip, potion use, pickup collection, build placement, zombie selection laugh, countdown, and movement loops are synced directly or based on synced gameplay state.
- Positional sounds use world locations.
- Players can hear sounds from the left, right, closer, or farther away depending on where the sound source is in the world.

### Skybox And Map View

I changed the skybox behavior.

- Number keys no longer manually switch skyboxes.
- A new round randomly chooses skybox 1, skybox 2, or no skybox.
- The full map is still available with `M`.
- I removed the small corner viewport because it was visually distracting.

### Engine Fixes

I also made engine-level fixes.

- I fixed the transparent render queue so transparent objects do not keep duplicating.
- I improved the OBJ importer so it handles the new model files better.
- I adjusted animation support so the animated player model can correctly load and play idle, walk, and run animations.
