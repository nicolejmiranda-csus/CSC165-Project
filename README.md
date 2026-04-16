**Refactoring and Deletion**

I did a broad refactor and cleanup of the project so that `MyGame.java` would no longer be one very large file. The main goal was to make the program easier to read, organize, and maintain. Most of the work was structural, but I also fixed a small number of gameplay and networking issues that were left over.

`MyGame.java` was slimmed down so that it now works more like the main entry point and controller for the game instead of holding almost all of the logic by itself. I moved mutable runtime values into `MyGameState` and moved scene objects, models, textures, and related resources into `MyGameAssets`.

I also split major parts of the game into separate classes based on responsibility. These include `MyGameLoaders`, `MyGameWorldBuilder`, `MyGameInitializer`, `MyGameUpdater`, `MyGameLighting`, `MyGameHUDSystem`, `MyGamePhotoSystem`, `MyGameBuildSystem`, `MyGameCameraSystem`, `MyGameMovementSystem`, `MyGameItemSystem`, `MyGameMouseLookSystem`, `MyGameNetworkingSystem`, and `MyGameVisualSystem`. This was done so that each class would handle one main area of the game instead of mixing many unrelated features together.

The inlined world-building code and input-binding code were removed from `MyGame.java` and moved into helper or system classes. I also reorganized the action classes so they are no longer mixed into the main game package. They are now grouped into folders such as `a3/actions/build`, `camera`, `movement`, `equipables`, and `interaction`.

The multiplayer and server code was also reorganized. Ghost and client networking classes were moved into `a3.networking`, and the server classes were moved into `a3.server`. In addition, I added packet validation so that malformed UDP or TCP packets are ignored instead of crashing the server.

I added a dedicated visual and environment system called `MyGameVisualSystem`. This was used to move features such as skybox switching and axes toggling out of the item system, since those features are visual and not really part of item behavior.

I also did a consistency and cleanup pass to make naming clearer, remove leftover dolphin-era code, and make the split between scene-object terms and gameplay terms more consistent. As part of this, the old decorative dolphin world asset was removed from the project. This included deleting the decorative dolphin model, texture, related asset fields, and the world-builder code connected to it, so the project no longer carries unused dolphin-specific content.

Naming was also standardized in a few places. The internal state field `selectedAvatar` was renamed to `selectedAvatarType` so it matches the getter and setter naming and reads more clearly. Scene-object naming continues to use `avatar` for the player-controlled `GameObject` and for transform-related helpers such as `avatarForward()`, while gameplay and state terms such as `playerYaw` were left as player-based names so the code still distinguishes scene transforms from player state.

I also centralized the avatar model checks by adding a shared helper for checking whether the current avatar is `playerModel2`. This removed repeated raw string comparisons across multiple files. Builder, camera, item, and ghost-selection code were cleaned up to use the shared helper or other consistent comparisons instead of scattered ad hoc checks. I also improved a few local variable names, such as changing some transform-related locals to names like `avatarLoc`, so they match the rest of the refactor and read more naturally.

The final leftover dolphin wording was also removed from comments and text so the `a3` code no longer uses dolphin-era naming at all. After this rename and removal pass, I recompiled the project to verify that it still builds cleanly.

A few smaller fixes were also made. The item carry offset checks were corrected so they use the actual avatar ids `playerModel1` and `playerModel2` instead of checking for `"player"`. The flashlight spawn position was changed so it no longer appears directly on the player and gets picked up immediately. I also removed dead pitch code, including the unused no-op pitch path and old pitch action classes that were no longer being used by the input bindings.

The build preview update logic was moved to a better place in the main update flow instead of being kept inside the item system. I also cleaned up imports and corrected places where static members were being accessed through instances.

Finally, I updated the support scripts including `compile.bat`, `run_server.bat`, and `clearGAMEclassFiles.bat` so they match the new package and folder structure.
