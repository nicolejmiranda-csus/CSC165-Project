# New: What I Changed Recently

This update focuses on **Xbox One controller support** and making the **HUD show the correct controls** based on whether the player is using keyboard/mouse or controller.

---

## Xbox One Controller Support

Added Xbox One controller support for both **human** and **zombie** players.

### Controller Inputs

| Xbox Control | Action |
|---|---|
| Left joystick | Move |
| Right joystick | Rotate camera and tilt camera |
| D-pad | Uses the POV hat |
| A | Jump |
| B | Toggle build mode |
| X | Use flashlight for humans / invisibility for zombies |
| Y | Equip potion for humans / cycle build material while building |
| LB | Equip rock for humans / switch wall and roof while building |
| RB | Use dash for humans / equip baby zombie for zombies |
| LT | Uses the left trigger axis |
| RT | Uses the right trigger axis and works like left click |
| Select | Open the full map |
| Start | Open the instructions |
| Left joystick click | Run |
| Right joystick click | Switch POV/camera mode |
| Home button | Opens instructions if Windows sends that button to the game |

---

## Xbox Button Mapping Fix

Mapped Xbox buttons using **TAGE’s zero-based button numbering**.

| Xbox Button | TAGE Button / Axis |
|---|---|
| A | Button 0 |
| B | Button 1 |
| X | Button 2 |
| Y | Button 3 |
| LB | Button 4 |
| RB | Button 5 |
| Select | Button 6 |
| Start | Button 7 |
| Left stick click | Button 8 |
| Right stick click | Button 9 |
| Home | Button 10 |
| LT | `Axis.Z` |
| RT | `Axis.RZ` |

---

## Controller Gameplay Support

The controller now works with the important gameplay systems.

### Human Player Support

Humans can now use the controller to:

- Move
- Run
- Jump
- Use flashlight
- Use potion
- Equip rock
- Dash
- Build
- Place build pieces
- Use the map

### Zombie Player Support

Zombies can now use the controller to:

- Move
- Run
- Jump
- Use invisibility
- Equip baby zombie
- Throw baby zombie
- Attack
- Use the map

### Additional Gameplay Support

- Build mode works with controller.
- Full map mode works with controller.
- RT works like left click for:
  - Using items
  - Attacking
  - Throwing
  - Placing build pieces

---

## D-pad Support

Added useful D-pad behavior.

### Outside Full Map Mode

| D-pad Input | Action |
|---|---|
| Up | Use flashlight or invisibility |
| Left | Use rock or switch build piece |
| Right | Use dash, baby zombie, or rotate build piece |
| Down | Toggle build mode |

### Inside Full Map Mode

| Input | Action |
|---|---|
| D-pad | Pan the full map |
| LT | Zoom the full map |
| RT | Zoom the full map |

---

## Device-Aware HUD

The HUD now detects the player’s last input device.

- If the player presses a keyboard key or uses the mouse, the HUD changes to keyboard controls.
- If the player presses an Xbox button or moves the joystick, the HUD changes to Xbox controls.
- The normal gameplay helper text now matches the device the player is actually using.
- The instructions page now has separate keyboard and Xbox versions.

---

## Instruction Page Update

Updated instruction cycling behavior.

| Input | Result |
|---|---|
| Press `I` once | Show keyboard controls |
| Press `I` again | Show Xbox One controller controls |
| Press `I` again | Close instructions |
| Press Xbox Start | Open Xbox instructions first |

The instruction text also changes based on whether the player is a **human** or **zombie**.

---

## New / Updated Files

### Added

```text
running_Dead/MyGameInputDeviceAction.java
running_Dead/actions/interaction/XboxPovAction.java
running_Dead/actions/interaction/XboxTriggerAction.java
running_Dead/actions/interaction/XboxYAction.java
