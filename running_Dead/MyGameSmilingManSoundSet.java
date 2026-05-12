package running_Dead;

import org.joml.Vector3f;

import tage.audio.Sound;

/**
 * Positional audio bundle for one Smiling Man.
 * Each agent owns its sounds so chase/stare audio follows the active monster.
 * Connected to: Created by MyGameSoundSystem and attached to Smiling Man agents.
 */
class MyGameSmilingManSoundSet {
    private final Sound walk;
    private final Sound run;
    private final Sound breath;
    private final Sound hello;

    MyGameSmilingManSoundSet(Sound walk, Sound run, Sound breath, Sound hello) {
        this.walk = walk;
        this.run = run;
        this.breath = breath;
        this.hello = hello;
    }

    void update(Vector3f location, boolean walking, boolean running) {
        Vector3f source = soundLocation(location);
        setLoop(walk, source, walking && !running);
        setLoop(run, source, running);
        setLoop(breath, source, running);
    }

    void playHello(Vector3f location) {
        playOneShot(hello, soundLocation(location));
    }

    void stopAll() {
        stop(walk);
        stop(run);
        stop(breath);
        stop(hello);
    }

    private Vector3f soundLocation(Vector3f location) {
        return location == null ? new Vector3f() : new Vector3f(location).add(0f, 1.6f, 0f);
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
