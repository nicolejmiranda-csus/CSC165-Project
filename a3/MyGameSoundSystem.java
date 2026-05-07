package a3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.joml.Vector3f;

import a3.networking.GhostAvatar;
import tage.Camera;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;

public class MyGameSoundSystem {
    private static final String SOUND_FLASHLIGHT = "flashlight";
    private static final String SOUND_EQUIP = "equip";
    private static final String SOUND_POTION = "potion";

    private final MyGame game;
    private final HashMap<UUID, MyGamePlayerSoundSet> remotePlayerSounds = new HashMap<>();
    private final HashMap<Integer, MyGameSmilingManSoundSet> smilingManSounds = new HashMap<>();
    private boolean loaded = false;
    private boolean countdownFivePlayed = false;

    private IAudioManager audioMgr;
    private AudioResource walkResource;
    private AudioResource runResource;
    private AudioResource zombieSoundsResource;
    private AudioResource pickupResource;
    private AudioResource countdownResource;
    private AudioResource flashlightResource;
    private AudioResource equipResource;
    private AudioResource ambienceResource;
    private AudioResource zombieBreathResource;
    private AudioResource scaryLaughResource;
    private AudioResource potionResource;
    private AudioResource girlBreathResource;
    private AudioResource guyBreathResource;
    private AudioResource woodResource;
    private AudioResource smilingManWalkResource;
    private AudioResource smilingManRunResource;
    private AudioResource smilingManBreathResource;
    private AudioResource smilingManHelloResource;
    private AudioResource mushroomMonWalkResource;
    private AudioResource mushroomMonChaseResource;
    private AudioResource mushroomMonSmokeHissResource;
    private AudioResource mushroomMonPoofResource;

    private MyGameMushroomMonSoundSet mushroomMonSounds;
    private MyGamePlayerSoundSet localPlayerSounds;
    private Sound pickupSound;
    private Sound countdownFiveSound;
    private Sound flashlightClickSound;
    private Sound itemEquipSound;
    private Sound scaryAmbienceSound;
    private Sound scaryLaughSound;
    private Sound potionHealSound;
    private Sound woodPlaceSound;

    public MyGameSoundSystem(MyGame game) {
        this.game = game;
    }

    public void loadSounds() {
        try {
            audioMgr = MyGame.getEngine().getAudioManager();
            walkResource = resource("mono_eaglaxle-dirt-footsteps-2-455146.wav");
            runResource = resource("mono_freesound_community-running-on-dirt-87203.wav");
            zombieSoundsResource = resource("freesound_community-zombie-sounds-95180.wav");
            pickupResource = resource("mono_freesound_community-energy-1-107099.wav");
            countdownResource = resource("mono_freesound_community-female-robotic-countdown-5-to-1-47653.wav");
            flashlightResource = resource("mono_freesound_community-flashlight-clicking-on-105809.wav");
            equipResource = resource("mono_freesound_community-item-equip-6904.wav");
            ambienceResource = resource("mono_freesound_community-scary-night-ambience-75274.wav");
            zombieBreathResource = resource("mono_freesound_community-zombie-breathing-70682.wav");
            scaryLaughResource = resource("mono_oliper18-scary-laugh-377526.wav");
            potionResource = resource("mono_ribhavagrawal-energy-drink-effect-230559.wav");
            girlBreathResource = resource("mono_hasin2004-breathing-fast-247449.wav");
            guyBreathResource = resource("mono_u_hn60smked5-sound-effect-breathing-hard-122341.wav");
            woodResource = resource("mono_u_scysdwddsp-wood-effect-254997.wav");
            smilingManWalkResource = resource("jokerzillagames-walking-366933.wav");
            smilingManRunResource = resource("freesound_community-running-sounds-6003.wav");
            smilingManBreathResource = resource("ribhavagrawal-heavy-breathing-sound-effect-type-03-294201.wav");
            smilingManHelloResource = resource("freesound_community-hello-81683.wav");
            mushroomMonWalkResource = resource("mono_mushroom-walk.wav");
            mushroomMonChaseResource = resource("mono_mushroom-chase.wav");
            mushroomMonSmokeHissResource = resource("mono_mushroom-smoke-hiss.wav");
            mushroomMonPoofResource = resource("mono_mushroom-poof.wav");

            localPlayerSounds = createPlayerSoundSet();
            pickupSound = oneShot(pickupResource, 75, 2f, 45f, 1.6f);
            countdownFiveSound = oneShot(countdownResource, 85, 1f, 80f, 0f);
            flashlightClickSound = oneShot(flashlightResource, 80, 1f, 35f, 1.4f);
            itemEquipSound = oneShot(equipResource, 78, 1f, 35f, 1.4f);
            scaryAmbienceSound = loop(ambienceResource, 32, 1f, 100f, 0f);
            scaryLaughSound = oneShot(scaryLaughResource, 88, 3f, 95f, 1.0f);
            potionHealSound = oneShot(potionResource, 78, 1f, 40f, 1.3f);
            woodPlaceSound = oneShot(woodResource, 82, 2f, 55f, 1.2f);
            loaded = true;
        } catch (RuntimeException e) {
            loaded = false;
            System.out.println("sound setup failed: " + e.getMessage());
        }
    }

    public void startAmbience() {
        if (!loaded || scaryAmbienceSound == null) return;
        playLoop(scaryAmbienceSound, localSoundLocation());
    }

    public void update(float dt) {
        if (!loaded) return;
        updateEarParameters();
        updateAmbienceLocation();
        updatePlayerLoops();
        updateCountdownCue();
    }

    public void stopAll() {
        if (localPlayerSounds != null) localPlayerSounds.stopAll();
        for (MyGamePlayerSoundSet sounds : remotePlayerSounds.values()) sounds.stopAll();
        remotePlayerSounds.clear();
        stopAllSmilingMen();
        stopMushroomMon();
        stop(scaryAmbienceSound);
    }

    public void playPickupCollect(Vector3f location) {
        playAt(pickupSound, location);
    }

    public void playPickupCollect() {
        playPickupCollect(localSoundLocation());
    }

    public void playCountdownFive() {
        playAt(countdownFiveSound, localSoundLocation());
    }

    public void playFlashlightClick() {
        Vector3f location = localSoundLocation();
        playAt(flashlightClickSound, location);
        broadcastSound(SOUND_FLASHLIGHT, location);
    }

    public void playItemEquip() {
        Vector3f location = localSoundLocation();
        playAt(itemEquipSound, location);
        broadcastSound(SOUND_EQUIP, location);
    }

    public void playRemoteSound(String type, Vector3f location) {
        if (SOUND_FLASHLIGHT.equals(type)) playAt(flashlightClickSound, location);
        else if (SOUND_EQUIP.equals(type)) playAt(itemEquipSound, location);
        else if (SOUND_POTION.equals(type)) playAt(potionHealSound, location);
    }

    public void playScaryLaughForPlayer(UUID playerId) {
        playAt(scaryLaughSound, playerLocation(playerId));
    }

    public void playPotionHeal() {
        Vector3f location = localSoundLocation();
        playAt(potionHealSound, location);
        broadcastSound(SOUND_POTION, location);
    }

    public void playBuildPlace(Vector3f location) {
        playAt(woodPlaceSound, location);
    }

    public void updateSmilingManLoop(int index, Vector3f location, boolean walking, boolean running) {
        if (!loaded) return;
        getSmilingManSounds(index).update(location, walking, running);
    }

    public void playSmilingManHello(int index, Vector3f location) {
        if (!loaded) return;
        getSmilingManSounds(index).playHello(location);
    }

    public void stopSmilingMan(int index) {
        MyGameSmilingManSoundSet sounds = smilingManSounds.remove(index);
        if (sounds != null) sounds.stopAll();
    }

    public void stopAllSmilingMen() {
        for (MyGameSmilingManSoundSet sounds : smilingManSounds.values()) sounds.stopAll();
        smilingManSounds.clear();
    }

    public void updateMushroomMonLoop(Vector3f location, boolean wandering, boolean chasing, boolean exploded) {
        if (!loaded) return;
        getMushroomMonSounds().update(location, wandering, chasing, exploded);
    }

    public void playMushroomMonPoof(Vector3f location) {
        if (!loaded) return;
        getMushroomMonSounds().playPoof(location);
    }

    public void stopMushroomMon() {
        if (mushroomMonSounds != null) mushroomMonSounds.stopAll();
    }

    private AudioResource resource(String fileName) {
        return audioMgr.createAudioResource(fileName, AudioResourceType.AUDIO_SAMPLE);
    }

    private MyGamePlayerSoundSet createPlayerSoundSet() {
        if (game.isPlayerModel2Selected()) {
            return new MyGamePlayerSoundSet(
                    loop(walkResource, 52, 1.2f, 28f, 1.7f),
                    loop(runResource, 55, 1.2f, 35f, 1.6f),
                    loop(zombieSoundsResource, 55, 1.2f, 35f, 1.6f),
                    loop(girlBreathResource, 58, 1.2f, 30f, 1.5f),
                    loop(guyBreathResource, 72, 1.2f, 34f, 1.4f));
        }
        return new MyGamePlayerSoundSet(
                loop(walkResource, 52, 1.2f, 28f, 1.7f),
                loop(runResource, 55, 1.2f, 35f, 1.6f),
                loop(zombieBreathResource, 48, 2f, 35f, 1.5f),
                loop(girlBreathResource, 58, 1.2f, 30f, 1.5f),
                loop(guyBreathResource, 72, 1.2f, 34f, 1.4f));
    }

    private MyGameSmilingManSoundSet getSmilingManSounds(int index) {
        return smilingManSounds.computeIfAbsent(index, key -> createSmilingManSoundSet());
    }

    private MyGameSmilingManSoundSet createSmilingManSoundSet() {
        return new MyGameSmilingManSoundSet(
                loop(smilingManWalkResource, 70, 3f, 70f, 1.0f),
                loop(smilingManRunResource, 86, 3f, 100f, 0.9f),
                loop(smilingManBreathResource, 88, 3f, 95f, 0.9f),
                oneShot(smilingManHelloResource, 92, 3f, 100f, 0.8f));
    }

    private MyGameMushroomMonSoundSet getMushroomMonSounds() {
        if (mushroomMonSounds == null) mushroomMonSounds = createMushroomMonSoundSet();
        return mushroomMonSounds;
    }

    private MyGameMushroomMonSoundSet createMushroomMonSoundSet() {
        return new MyGameMushroomMonSoundSet(
                loop(mushroomMonWalkResource, 68, 3f, 50f, 1.1f),
                loop(mushroomMonChaseResource, 78, 3f, 60f, 1.0f),
                loop(mushroomMonSmokeHissResource, 82, 3f, 55f, 1.0f),
                oneShot(mushroomMonPoofResource, 90, 3f, 80f, 0.9f));
    }

    private Sound loop(AudioResource resource, int volume, float minDistance, float maxDistance, float rolloff) {
        return sound(resource, volume, true, minDistance, maxDistance, rolloff);
    }

    private Sound oneShot(AudioResource resource, int volume, float minDistance, float maxDistance, float rolloff) {
        return sound(resource, volume, false, minDistance, maxDistance, rolloff);
    }

    private Sound sound(AudioResource resource, int volume, boolean looping, float minDistance, float maxDistance, float rolloff) {
        Sound sound = new Sound(resource, looping ? SoundType.SOUND_MUSIC : SoundType.SOUND_EFFECT, volume, looping);
        sound.initialize(audioMgr);
        sound.setMinDistance(minDistance);
        sound.setMaxDistance(maxDistance);
        sound.setRollOff(rolloff);
        sound.setLocation(localSoundLocation());
        return sound;
    }

    private void updateEarParameters() {
        if (audioMgr == null || audioMgr.getEar() == null || game.assets.avatar == null) return;
        Camera camera = MyGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();
        audioMgr.getEar().setLocation(game.assets.avatar.getWorldLocation());
        audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
    }

    private void updateAmbienceLocation() {
        if (scaryAmbienceSound == null) return;
        scaryAmbienceSound.setLocation(localSoundLocation());
        if (!scaryAmbienceSound.getIsPlaying()) scaryAmbienceSound.play();
    }

    private void updatePlayerLoops() {
        if (game.assets.avatar != null && localPlayerSounds != null) {
                localPlayerSounds.update(
                    game.assets.avatar.getWorldLocation(),
                    game.animationSystem.wasMovingThisFrame() && !game.animationSystem.wasRunningThisFrame() && game.state.onGround,
                    game.animationSystem.wasMovingThisFrame() && game.animationSystem.wasRunningThisFrame() && game.state.onGround,
                    game.state.localPlayerZombie,
                    game.isPlayerModel2Selected(),
                    localPanicIntensity());
        }

        HashSet<UUID> liveIds = new HashSet<>();
        if (game.state.gm != null) {
            for (GhostAvatar ghost : game.state.gm.getGhostAvatarsSnapshot()) {
                UUID id = ghost.getID();
                liveIds.add(id);
                MyGamePlayerSoundSet sounds = remotePlayerSounds.computeIfAbsent(id, key -> createPlayerSoundSet());
                String anim = game.getRemoteAnimationState(id);
                boolean running = "RUN".equals(anim) || "ZOMBIE_RUN".equals(anim);
                boolean walking = "WALK".equals(anim) || "ZOMBIE_WALK".equals(anim);
                sounds.update(
                        ghost.getWorldLocation(),
                        walking,
                        running,
                        game.state.remoteZombieStates.getOrDefault(id, false),
                        "playerModel2".equals(ghost.getAvatarType()),
                        0f);
            }
        }

        remotePlayerSounds.entrySet().removeIf(entry -> {
            if (liveIds.contains(entry.getKey())) return false;
            entry.getValue().stopAll();
            return true;
        });
    }

    private void updateCountdownCue() {
        boolean intermission = game.state.zombieIntermissionStarted
                && !game.state.zombieRoundActive
                && !game.isMatchOver();
        double remaining = game.state.zombieIntermissionRemaining;
        if (!intermission || remaining > 5.5) {
            countdownFivePlayed = false;
            return;
        }
        if (!countdownFivePlayed && remaining > 0.0 && remaining <= 5.0) {
            countdownFivePlayed = true;
            playCountdownFive();
        }
    }

    private float localPanicIntensity() {
        if (game.state.localPlayerZombie || game.state.health <= 0) return 0f;
        float healthPanic = 1.0f - Math.max(0f, Math.min(1f, game.state.health / (float) game.state.maxHealth));
        float distance = game.smilingManSystem.distanceToClosestSpawned(game.assets.avatar.getWorldLocation());
        float proximityPanic = distance == Float.MAX_VALUE ? 0f : 1.0f - Math.max(0f, Math.min(1f, distance / 45.0f));
        return Math.max(healthPanic, proximityPanic);
    }

    private void playAt(Sound sound, Vector3f location) {
        if (!loaded || sound == null || location == null) return;
        try {
            sound.setLocation(new Vector3f(location).add(0f, 1.0f, 0f));
            sound.play();
        } catch (RuntimeException ignored) {
        }
    }

    private void playLoop(Sound sound, Vector3f location) {
        if (sound == null || location == null) return;
        sound.setLocation(location);
        if (!sound.getIsPlaying()) sound.play();
    }

    private void stop(Sound sound) {
        if (sound == null) return;
        try {
            if (sound.getIsPlaying()) sound.stop();
        } catch (RuntimeException ignored) {
        }
    }

    private Vector3f localSoundLocation() {
        return game.assets.avatar == null ? new Vector3f() : new Vector3f(game.assets.avatar.getWorldLocation()).add(0f, 1.0f, 0f);
    }

    private Vector3f playerLocation(UUID id) {
        if (id == null || (game.state.protClient != null && id.equals(game.state.protClient.getID()))) return localSoundLocation();
        if (game.state.gm != null) {
            GhostAvatar ghost = game.state.gm.getGhostAvatar(id);
            if (ghost != null) return new Vector3f(ghost.getWorldLocation()).add(0f, 1.0f, 0f);
        }
        return localSoundLocation();
    }

    private void broadcastSound(String type, Vector3f location) {
        if (game.state.protClient != null && game.state.isClientConnected && location != null) {
            game.state.protClient.sendSoundMessage(type, location);
        }
    }
}
