What I changed

I removed the old photo-taking system because it no longer matched the direction of the project. This included deleting the photo system, the old photo actions, and the old win condition where players had to take pictures and place them. The game is now focused on surviving, tagging, collecting items, building defenses, and using role-based abilities.

The main flow of the game is now based on an intermission, a timed round, and player roles. When enough players are connected, the game waits through an intermission, then randomly selects one player to become the starting zombie, and then begins a 300-second survival round. Humans win if at least one human survives until the timer reaches zero. Zombies win if all humans are tagged or converted before time runs out. After a round ends, the game resets into another intermission instead of stopping completely.

I added a full TAGE physics system through MyGamePhysicsSystem. This system creates physics objects for the local player, remote players, pickups, projectiles, build pieces, terrain, trees, rocks, and scenery blockers. Because of this, the game can detect when players touch pickups, when zombies collide with humans, when projectiles hit players or scenery, and when players run into build pieces or world objects. I also added colliders for the player and remote ghost players so zombie tagging can happen through collision and attack logic.

I added a role system for humans and zombies. If a zombie tags a human, that human becomes a zombie. If a human’s health reaches zero, that human also becomes a zombie. Zombies do not use health, and I did this so future NPC zombies can focus only on living human players. I also changed the HUD so it only shows information that matters to the player’s current role. Humans do not see zombie-only resources, and zombies do not see human-only resources such as build materials.

For humans, I added dash, rocks, building materials, flashlights, and health potions. For zombies, I added invisibility and baby zombie projectiles. This gives both roles their own gameplay style. I also cleaned up the HUD so it is easier to understand. It now shows the current role, round state, timer, health, ability readiness, and only the resources that matter to that player.

I added random spawn logic so players do not always start in the same place. The spawn system tries to avoid blocked areas such as scenery, trees, rocks, and the house. This helps make rounds feel less predictable and avoids unfair starts.

I also added synced pickups across multiplayer. Pickups now have server-tracked locations, active states, respawn timers, and pickup rewards. When one player collects a pickup, the server hides it for everyone and later respawns it somewhere else. This prevents different clients from seeing different pickup locations or availability. I also increased the number of pickups around the map so the game can support a larger classroom demo with more players.

I changed the pickup models so they better match their purpose. Building materials use a cube with a wood texture. Rocks use the rock model and rock texture. Dash uses the dash pickup model. Invisibility uses the hood model. Baby zombie pickups use the baby zombie model and texture. I also changed the hood pickup to use a leather-style texture so it no longer looks like a rock.

I added health potions and flashlights as collectible items. Health potions can be equipped and used to restore health. Flashlights can be equipped and toggled on and off. I also made the potion look more like clear glass, and I made the collectible potions and flashlights bob up and down like the other pickups so they are easier to notice.

I added held item visuals for the player. When a human equips a flashlight, potion, or rock, the item appears near the player’s hand. When a zombie equips a baby zombie, the baby zombie appears near the zombie’s hand before being thrown. I also made equipped items exclusive, so equipping one item replaces the current one instead of letting several items conflict visually or mechanically.

I changed the controls to make the game easier to play while using the mouse to look around. Humans use F for flashlight equip or unequip, C for potion equip or unequip, Q for rock equip or unequip, R for dash, and E for build mode. While in build mode, Q switches the build piece, R rotates the build piece, and left mouse click places it. Left mouse click also uses the currently equipped item, such as toggling the flashlight, drinking the potion, or throwing a rock.

I also updated zombie controls. Zombies use F for invisibility, R to equip or unequip the baby zombie projectile, and left mouse click to throw the baby zombie if it is equipped. If the zombie does not have a baby zombie equipped, then left mouse click becomes the zombie attack or tag action and can also damage build pieces.

I added building gameplay for humans. Humans can collect build materials and place walls, floors, roofs, and related build pieces. These build pieces are synced in multiplayer so all players see the same structures. I also added physics colliders to them so players cannot walk through them. Zombies can attack build pieces, and after enough hits, the build piece is destroyed and removed for everyone.

I adjusted roof and floor behavior so players can stand on build pieces instead of sliding off right away. The movement and physics systems now check for walkable build surfaces and terrain height so the player can move across terrain, rocks, and build surfaces more naturally.

I added projectile gameplay for both sides. Humans can throw rocks to slow zombies, and zombies can throw baby zombies to slow humans. The projectiles use physics bodies, move through the world, collide with players or scenery, and are synced across multiplayer. This gives both roles a ranged option instead of making the game depend only on direct contact.

I added zombie invisibility and synced it across multiplayer. When a zombie uses invisibility, the zombie becomes hidden for other players and the ability state is sent through the network. When the timer ends, the zombie becomes visible again. This makes invisibility more useful for sneaking up on humans.

I also worked on the terrain and scenery. The terrain now uses the grass texture and height map, and I adjusted the texture tiling so the ground does not look stretched. I randomly placed trees, dead trees, rocks, and rock piles around the map using the available OBJ files and textures. I also adjusted object scaling so trees are larger, rocks are smaller, and the larger rocks and rock piles fit the world better.

I worked on snapping scenery to the terrain as well. Trees and rocks are placed using terrain height and model bounds so they sit closer to the ground instead of floating or sinking too much. I also added scenery colliders for trees and rocks so players cannot walk through them. At the same time, I kept the collider sizes smaller so they feel closer to the visible model instead of acting like oversized invisible walls.

I added multiplayer tracking for important gameplay state. The server now tracks round state, intermission, active pickups, pickup respawns, roles, health, invisibility, builds, projectiles, animations, sounds, and win conditions. This makes the game more consistent across all clients.

I updated the networking code in ProtocolClient, GameServerUDP, and GameServerTCP so clients can send and receive messages for roles, tags, health, abilities, projectiles, slows, pickups, builds, build removal, animation states, round states, and sound effects. I also added server-side classes such as ServerGameState, PickupState, and ServerMessenger to organize the authoritative multiplayer state.

I added player animation syncing. The local player tracks whether they are idle, walking, or running, and sends that animation state to the server. Remote players then play the matching animation so other players can see whether someone is idle, walking, or running.

I also added zombie visual changes. When playerModel1 becomes a zombie, the texture changes to the zombie player texture. Other player models use a zombie-colored visual state. When a player becomes human again during reset or intermission, their normal appearance is restored.

I added a WAV-only sound system using TAGE audio. I removed MP3 support and deleted the MP3 sound files because I am now using WAV versions instead. I also created mono WAV versions because OpenAL spatial audio works better with mono sound sources. The game now uses TAGE and OpenAL 3D sound for footsteps, running, pickup collection, countdown, flashlight click, equip sounds, ambience, zombie breathing, scary laugh, potion healing, human breathing, and building sounds.

I also synced important sound effects in multiplayer. Sounds such as flashlight clicks, item equip, potion use, pickup collection, build placement, zombie selection laugh, countdown, and movement loops are either synced directly through sound packets or based on synced gameplay state. Positional sounds use world locations, so players can hear sounds from the left, right, closer, or farther away depending on where the sound source is in the game world.

I changed the skybox behavior too. Instead of using number keys to manually switch skyboxes, the game now randomly chooses between skybox 1, skybox 2, or no skybox when a new round starts. The full map is still available with M, but I removed the small corner viewport because it was visually distracting.

I also made some engine-level fixes. I fixed the transparent render queue so transparent objects do not keep duplicating. I improved the OBJ importer so it handles the new model files better. I also adjusted animation support so the animated player model can correctly load and play its idle, walk, and run animations.
