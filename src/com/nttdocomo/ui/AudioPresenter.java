package com.nttdocomo.ui;

import com.nttdocomo.ui.mld.playback.J2meMidiPlayer;

public class AudioPresenter extends MediaPresenter implements J2meMidiPlayer.Listener {
    private static final int EVENT_FINISHED = 3;
    private static final int ATTR_KEY = 3;
    private static final int ATTR_VOLUME = 4;
    private static final int ATTR_TEMPO = 5;
    private static final int ATTR_LOOP_COUNT = 6;
    private static final int FALLBACK_FINISH_DELAY_MS = 300;

    private final J2meMidiPlayer midiPlayer = new J2meMidiPlayer();
    private MediaSound sound;
    private boolean playing;
    private boolean loopBodyPhaseActive;
    private int volume = 100;
    private int tempo = 100;
    private int key = 5;
    private int loopCount = 1;
    private int playGeneration;

    public static AudioPresenter getAudioPresenter() {
        return new AudioPresenter();
    }

    public void setSound(MediaSound sound) {
        stop();
        this.sound = sound;
    }

    public void setAttribute(int attr, int value) {
        super.setAttribute(attr, value);
        switch (attr) {
            case ATTR_KEY:
                key = value;
                midiPlayer.setPitchSemitones(value - 5);
                break;
            case ATTR_VOLUME:
                volume = value;
                midiPlayer.setVolume(value);
                break;
            case ATTR_TEMPO:
                tempo = value;
                midiPlayer.setRatePercent(value);
                break;
            case ATTR_LOOP_COUNT:
                loopCount = value;
                break;
            default:
                break;
        }
    }

    public void play() {
        MldMediaSound mldSound;
        byte[] midiBytes;
        final int generation;

        synchronized (this) {
            playing = true;
            loopBodyPhaseActive = false;
            generation = ++playGeneration;
        }

        if (!(sound instanceof MldMediaSound)) {
            scheduleFallbackFinish(generation);
            return;
        }

        mldSound = (MldMediaSound) sound;
        midiBytes = mldSound.getInitialMidi();
        if (midiBytes == null || mldSound.getLoadError() != null) {
            scheduleFallbackFinish(generation);
            return;
        }

        try {
            midiPlayer.play(midiBytes, shouldUseMldLoopBody(mldSound) ? 1 : normalizedLoopCount(), this);
            applyLiveAttributes();
        } catch (Throwable t) {
            System.out.println("[DoJa] MIDI playback failed: " + mldSound.getResourcePath() + " (" + t + ")");
            scheduleFallbackFinish(generation);
        }
    }

    public void stop() {
        synchronized (this) {
            playing = false;
            loopBodyPhaseActive = false;
            playGeneration++;
        }
        midiPlayer.close();
    }

    public void onPlaybackCompleted() {
        int generation;
        MldMediaSound mldSound;

        synchronized (this) {
            if (!playing) {
                return;
            }
            generation = ++playGeneration;
        }

        if (sound instanceof MldMediaSound) {
            mldSound = (MldMediaSound) sound;
            if (shouldUseMldLoopBody(mldSound) && !loopBodyPhaseActive) {
                startLoopBody(mldSound, generation);
                return;
            }
        }

        synchronized (this) {
            if (!playing || generation != playGeneration) {
                return;
            }
            playing = false;
        }
        fireMediaAction(EVENT_FINISHED, 0);
    }

    public void onPlaybackError(String message) {
        int generation;
        synchronized (this) {
            if (!playing) {
                return;
            }
            generation = ++playGeneration;
        }
        scheduleFallbackFinish(generation);
    }

    private void startLoopBody(MldMediaSound mldSound, int generation) {
        byte[] loopMidi = mldSound.getLoopMidi();

        if (loopMidi == null) {
            scheduleFallbackFinish(generation);
            return;
        }

        try {
            synchronized (this) {
                if (!playing || generation != playGeneration) {
                    return;
                }
                loopBodyPhaseActive = true;
            }
            midiPlayer.play(loopMidi, -1, 0L, this);
            applyLiveAttributes();
        } catch (Throwable t) {
            System.out.println("[DoJa] MIDI loop playback failed: " + mldSound.getResourcePath() + " (" + t + ")");
            scheduleFallbackFinish(generation);
        }
    }

    private void scheduleFallbackFinish(final int generation) {
        if (loopCount < 0 || listener == null) {
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(FALLBACK_FINISH_DELAY_MS);
                } catch (InterruptedException e) {
                }

                boolean notify;
                synchronized (AudioPresenter.this) {
                    notify = playing && generation == playGeneration;
                    if (notify) {
                        playing = false;
                    }
                }
                if (notify) {
                    fireMediaAction(EVENT_FINISHED, 0);
                }
            }
        }).start();
    }

    private boolean shouldUseMldLoopBody(MldMediaSound mldSound) {
        return loopCount < 0 && mldSound.hasLoop();
    }

    private int normalizedLoopCount() {
        return loopCount < 0 ? -1 : max(1, loopCount);
    }

    private void applyLiveAttributes() {
        midiPlayer.setVolume(volume);
        midiPlayer.setRatePercent(tempo);
        midiPlayer.setPitchSemitones(key - 5);
    }

    private static int max(int left, int right) {
        return left > right ? left : right;
    }
}

