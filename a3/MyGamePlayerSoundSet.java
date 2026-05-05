package a3;

import org.joml.Vector3f;

import tage.audio.Sound;

class MyGamePlayerSoundSet {
    private final Sound walk;
    private final Sound run;
    private final Sound zombieBreath;
    private final Sound girlBreath;
    private final Sound guyBreath;

    MyGamePlayerSoundSet(Sound walk, Sound run, Sound zombieBreath, Sound girlBreath, Sound guyBreath) {
        this.walk = walk;
        this.run = run;
        this.zombieBreath = zombieBreath;
        this.girlBreath = girlBreath;
        this.guyBreath = guyBreath;
    }

    void update(Vector3f location, boolean walking, boolean running, boolean zombie, boolean playerModel2, float panicIntensity) {
        Vector3f source = new Vector3f(location).add(0f, 1.0f, 0f);
        boolean humanRunning = running && !zombie;
        boolean panicking = !zombie && panicIntensity > 0.05f;
        setLoop(walk, source, walking && !running);
        setLoop(run, source, running);
        setLoop(zombieBreath, source, running && zombie);
        int breathVolume = 42 + Math.round(46f * Math.max(0f, Math.min(1f, panicIntensity)));
        setVolume(girlBreath, breathVolume);
        setVolume(guyBreath, breathVolume);
        setLoop(girlBreath, source, (humanRunning || panicking) && playerModel2);
        setLoop(guyBreath, source, (humanRunning || panicking) && !playerModel2);
    }

    void stopAll() {
        stop(walk);
        stop(run);
        stop(zombieBreath);
        stop(girlBreath);
        stop(guyBreath);
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

    private void stop(Sound sound) {
        if (sound == null) return;
        try {
            if (sound.getIsPlaying()) sound.stop();
        } catch (RuntimeException ignored) {
        }
    }

    private void setVolume(Sound sound, int volume) {
        if (sound == null) return;
        try {
            sound.setVolume(volume);
        } catch (RuntimeException ignored) {
        }
    }
}
