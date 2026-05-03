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

    void update(Vector3f location, boolean walking, boolean running, boolean zombie, boolean playerModel2) {
        Vector3f source = new Vector3f(location).add(0f, 1.0f, 0f);
        boolean humanRunning = running && !zombie;
        setLoop(walk, source, walking && !running);
        setLoop(run, source, running);
        setLoop(zombieBreath, source, running && zombie);
        setLoop(girlBreath, source, humanRunning && playerModel2);
        setLoop(guyBreath, source, humanRunning && !playerModel2);
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
}
