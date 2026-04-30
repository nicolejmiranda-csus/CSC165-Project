package a3;

public final class GameConstants {
    private GameConstants() {}

    public static final float HOME_W = 8f;
    public static final float HOME_H = 4f;
    public static final float HOME_D = 12f;
    public static final float HOME_FLOOR_Y = 0.05f;
    public static final float HOME_SCALE = 0.65f;

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

    public static final int[] ZOMBIE_TAG_PICKUP_TYPES = {
        PICKUP_DASH, PICKUP_DASH, PICKUP_DASH, PICKUP_DASH, PICKUP_DASH,
        PICKUP_DASH, PICKUP_DASH, PICKUP_DASH,
        PICKUP_INVIS, PICKUP_INVIS, PICKUP_INVIS, PICKUP_INVIS,
        PICKUP_INVIS, PICKUP_INVIS,
        PICKUP_BUILD, PICKUP_BUILD, PICKUP_BUILD, PICKUP_BUILD,
        PICKUP_BUILD, PICKUP_BUILD, PICKUP_BUILD, PICKUP_BUILD,
        PICKUP_BUILD, PICKUP_BUILD,
        PICKUP_ROCK, PICKUP_ROCK, PICKUP_ROCK, PICKUP_ROCK,
        PICKUP_ROCK, PICKUP_ROCK, PICKUP_ROCK, PICKUP_ROCK,
        PICKUP_BABY_ZOMBIE, PICKUP_BABY_ZOMBIE, PICKUP_BABY_ZOMBIE,
        PICKUP_BABY_ZOMBIE, PICKUP_BABY_ZOMBIE, PICKUP_BABY_ZOMBIE,
        PICKUP_FLASHLIGHT, PICKUP_FLASHLIGHT, PICKUP_FLASHLIGHT,
        PICKUP_FLASHLIGHT, PICKUP_FLASHLIGHT, PICKUP_FLASHLIGHT,
        PICKUP_POTION, PICKUP_POTION, PICKUP_POTION, PICKUP_POTION,
        PICKUP_POTION, PICKUP_POTION, PICKUP_POTION, PICKUP_POTION
    };
}
