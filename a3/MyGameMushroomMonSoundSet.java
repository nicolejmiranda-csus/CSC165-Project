package a3;

import org.joml.Vector3f;

import tage.audio.Sound;

class MyGameMushroomMonSoundSet {
    private final Sound walk;
    private final Sound chase;
    private final Sound smokeHiss;
    private final Sound poof;

    MyGameMushroomMonSoundSet(Sound walk, Sound chase, Sound smokeHiss, Sound poof) {
        this.walk = walk;
        this.chase = chase;
        this.smokeHiss = smokeHiss;
        this.poof = poof;
    }

    void update(Vector3f location, boolean wandering, boolean chasing, boolean exploded) {
        Vector3f source = soundLocation(location);
        setLoop(walk, source, wandering);
        setLoop(chase, source, chasing);
        setLoop(smokeHiss, source, chasing && !exploded);
    }

    void playPoof(Vector3f location) {
        playOneShot(poof, soundLocation(location));
    }

    void stopAll() {
        stop(walk);
        stop(chase);
        stop(smokeHiss);
        stop(poof);
    }

    private Vector3f soundLocation(Vector3f location) {
        return location == null ? new Vector3f() : new Vector3f(location).add(0f, 1.0f, 0f);
    }

    private void setLoop(Sound sound, Vector3f location, boolean shouldPlay) {
        if (sound == null) return;
        try {
            sound.setLocation(location);
            if (shouldPlay) {
                if (!sound.getIsPlaying()) sound.play();
            } else if (sound.getIsPlaying()) {
                sound.stop();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void playOneShot(Sound sound, Vector3f location) {
        if (sound == null) return;
        try {
            sound.setLocation(location);
            sound.play();
        } catch (RuntimeException ignored) {
        }
    }

    private void stop(Sound sound) {
        if (sound == null) return;
        try {
            if (sound.getIsPlaying()) sound.stop();
        } catch (RuntimeException ignored) {
        }
    }
}
