package running_Dead;

/**
 * Shared gameplay tuning values for Running_Dead.
 * Keeping these constants in one file makes it clear which numbers are design choices
 * instead of hidden magic values scattered across movement, AI, building, and networking.
 * Connected to: Read by almost every gameplay system; keeps shared tuning out of individual systems.
 */
public final class GameConstants {
    private GameConstants() {}

    public static final float HOME_W = 8f;
    public static final float HOME_H = 4f;
    public static final float HOME_D = 12f;
    public static final float HOME_FLOOR_Y = 0.05f;
    public static final float HOME_SCALE = 0.65f;

    public static final float SMILING_MAN_SCALE = 0.58f;
    public static final float SMILING_MAN_STILL_SPAWN_SECONDS = 30.0f;
    public static final float SMILING_MAN_HALFTIME_SPAWN_SECONDS = 150.0f;
    public static final float SMILING_MAN_DETECTION_RADIUS = 95.0f;
    public static final float SMILING_MAN_TABLE_HIDDEN_DETECTION_RADIUS = 19.0f;
    public static final float SMILING_MAN_DETECTION_CLOSE_RADIUS = 4.0f;
    public static final float SMILING_MAN_DETECTION_DOT = 0.087f;
    public static final float SMILING_MAN_COLLISION_RADIUS = 1.35f;
    public static final int SMILING_MAN_DAMAGE = 50;
    public static final float SMILING_MAN_STARE_WARNING_SECONDS = 1.55f;
    public static final float SMILING_MAN_FAKE_IDLE_CHANCE = 0.28f;
    public static final float NOISE_SPRINT_RADIUS = 42.0f;
    public static final float NOISE_BUILD_RADIUS = 60.0f;
    public static final float NOISE_ROCK_RADIUS = 70.0f;
    public static final float HUMAN_LAST_CHANCE_ESCAPE_CHANCE = 0.10f;
    public static final int HUMAN_LAST_CHANCE_ESCAPE_HEALTH = 1;
    public static final float ZOMBIE_TRACKER_INTERVAL_SECONDS = 30.0f;
    public static final float WORLD_TERRAIN_SIZE = 380.0f;
    public static final float WORLD_EDGE_LIMIT = 184.0f;
    public static final float NAME_LABEL_MAX_DISTANCE = WORLD_TERRAIN_SIZE * 0.15f;
    public static final float FLASHLIGHT_BLIND_RANGE = 22.0f;
    public static final float FLASHLIGHT_BLIND_DOT = 0.90f;
    public static final float FLASHLIGHT_BLIND_SECONDS = 1.0f;
    public static final float FLASHLIGHT_BLIND_COOLDOWN_SECONDS = 15.0f;
    public static final float FLASHLIGHT_BATTERY_SECONDS = 20.0f;
    public static final float FLASHLIGHT_BLIND_BATTERY_COST = 2.0f;
    public static final int TAGE_MAX_SKIN_BONES = 256;

    public static final int INPUT_DEVICE_KEYBOARD_MOUSE = 0;
    public static final int INPUT_DEVICE_XBOX = 1;

    public static final int HELP_OFF = 0;
    public static final int HELP_KEYBOARD = 1;
    public static final int HELP_XBOX = 2;

    public static final float OVER_LEFT = 0.72f;
    public static final float OVER_BOTTOM = 0.72f;
    public static final float OVER_W = 0.26f;
    public static final float OVER_H = 0.26f;

    public static final int ITEM_NONE = 0;
    public static final int ITEM_FLASHLIGHT = 1;
    public static final int ITEM_POTION = 2;
    public static final int ITEM_ROCK = 3;
    public static final int ITEM_BABY_ZOMBIE = 4;

    public static final int PICKUP_DASH = 0;
    public static final int PICKUP_INVIS = 1;
    public static final int PICKUP_BUILD = 2;
    public static final int PICKUP_ROCK = 3;
    public static final int PICKUP_BABY_ZOMBIE = 4;
    public static final int PICKUP_FLASHLIGHT = 5;
    public static final int PICKUP_POTION = 6;
    public static final int PICKUP_BUILD_METAL = 7;
    public static final int PICKUP_BUILD_GLASS = 8;
    public static final int PICKUPS_PER_TYPE = 15;
    public static final int MAX_SMILING_MEN = 5;
    public static final float SMILING_MAN_PROXIMITY_RADIUS = 58.0f;

    public static final float MUSHROOM_MON_SCALE = 3.0f;
    public static final float MUSHROOM_MON_DETECTION_RADIUS = 8.0f;
    public static final float MUSHROOM_MON_EXPLOSION_RADIUS = 1.5f;
    public static final int TABLE_COVER_COUNT = 15;
    public static final int RANDOM_HOUSE_COUNT = 5;
    public static final int RANDOM_WALL_COVER_COUNT = 18;
    public static final int RANDOM_ROOF_COVER_COUNT = 16;

    public static final int BUILD_MATERIAL_WOOD = 0;
    public static final int BUILD_MATERIAL_METAL = 1;
    public static final int BUILD_MATERIAL_GLASS = 2;

    public static final int[] ZOMBIE_TAG_PICKUP_TYPES = buildPickupTypes();

    private static int[] buildPickupTypes() {
        int[] types = new int[PICKUPS_PER_TYPE * 9];
        int index = 0;
        int[] eachType = {
            PICKUP_DASH, PICKUP_INVIS, PICKUP_BUILD, PICKUP_ROCK,
            PICKUP_BABY_ZOMBIE, PICKUP_FLASHLIGHT, PICKUP_POTION,
            PICKUP_BUILD_METAL, PICKUP_BUILD_GLASS
        };
        for (int type : eachType) {
            for (int i = 0; i < PICKUPS_PER_TYPE; i++) {
                types[index++] = type;
            }
        }
        return types;
    }

    public static int buildMaterialHits(int materialType) {
        if (materialType == BUILD_MATERIAL_METAL) return 5;
        if (materialType == BUILD_MATERIAL_GLASS) return 1;
        return 2;
    }

    public static String buildMaterialName(int materialType) {
        if (materialType == BUILD_MATERIAL_METAL) return "METAL";
        if (materialType == BUILD_MATERIAL_GLASS) return "GLASS";
        return "WOOD";
    }
}
