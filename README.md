A broad refactor and cleanup to slim down MyGame, organize the project by responsibility, and fix a few leftover inconsistencies. 
Mostly structural, with a small number of real gameplay and networking fixes.

Changes I did:

- Slimmed down MyGame.java — turned it into the main facade/entry point instead of a giant all-in-one file
  
- Split game data into dedicated holders — moved mutable runtime state into MyGameState and scene/resources into MyGameAssets

- Extracted major systems — separated lifecycle and gameplay logic into focused classes such as
  MyGameLoaders, MyGameWorldBuilder, MyGameInitializer, MyGameUpdater, MyGameLighting, MyGameHUDSystem, MyGamePhotoSystem,
  MyGameBuildSystem, MyGameCameraSystem, MyGameMovementSystem, MyGameItemSystem, MyGameMouseLookSystem, MyGameNetworkingSystem, and MyGameVisualSystem
  
- Removed inlined world-building and input-binding code from MyGame.java — moved setup logic into helper/system classes to reduce clutter
  
- Reorganized action classes — moved actions out of the main game file and grouped them into a3/actions/build, camera, movement, equipables, and interaction
  
- Reorganized multiplayer/server code — moved ghost/client networking code into a3.networking and moved server code into a3.server
  
- Added a dedicated visual/environment system — moved skybox switching and axes toggling out of the item system into MyGameVisualSystem
  
- Standardized naming — cleaned up old naming leftovers so the player-controlled object is consistently treated as the avatar, including renaming dolphinForward() to avatarForward()
  
- Fixed avatar model checks — updated item carry offsets to use the actual avatar ids playerModel1 and playerModel2 instead of "player" checks
  
- Fixed flashlight spawn behavior — moved the flashlight so it no longer spawns on top of the player and gets picked up immediately
  
- Removed dead pitch code — deleted the unused no-op pitch path and the stale pitch action classes that were no longer part of input bindings
  
- Moved build preview updates to a better home — took build-preview responsibility out of the item system and put it in the main update flow
  
- Cleaned imports and static access — removed unused imports and fixed places that were calling static members through instances
  
- Hardened server packet handling — added validation so malformed UDP/TCP packets are ignored instead of crashing the server
  
- Updated build/cleanup scripts — changed compile.bat, run_server.bat, and clearGAMEclassFiles.bat so they match the new package/folder structure
