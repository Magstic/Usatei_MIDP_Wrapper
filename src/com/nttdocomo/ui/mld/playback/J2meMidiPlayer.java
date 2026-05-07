package com.nttdocomo.ui.mld.playback;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.PitchControl;
import javax.microedition.media.control.RateControl;
import javax.microedition.media.control.TempoControl;
import javax.microedition.media.control.VolumeControl;

public final class J2meMidiPlayer implements PlayerListener {
    private static final String MIDI_CONTENT_TYPE = "audio/midi";

    public static interface Listener {
        void onPlaybackCompleted();

        void onPlaybackError(String message);
    }

    private Player player;
    private InputStream playerStream;
    private Listener listener;
    private boolean started;

    public synchronized void play(byte[] midiBytes, int loopCount, Listener callback)
            throws IOException, MediaException {
        play(midiBytes, loopCount, 0L, callback);
    }

    public synchronized void play(byte[] midiBytes, int loopCount, long startMillis, Listener callback)
            throws IOException, MediaException {
        CreatedPlayer created;
        Player createdPlayer;

        close();
        if (midiBytes == null || midiBytes.length == 0) {
            throw new IOException("Empty MIDI data");
        }

        created = createMidiPlayer(midiBytes);
        createdPlayer = created.player;

        try {
            createdPlayer.addPlayerListener(this);
        } catch (Throwable ignored) {
        }

        try {
            createdPlayer.realize();
        } catch (MediaException e) {
            closeCreatedPlayer(created);
            throw e;
        } catch (Throwable ignored) {
        }

        try {
            createdPlayer.prefetch();
        } catch (MediaException e) {
            closeCreatedPlayer(created);
            throw e;
        } catch (Throwable ignored) {
        }

        if (startMillis > 0L) {
            try {
                createdPlayer.setMediaTime(startMillis * 1000L);
            } catch (Throwable ignored) {
            }
        }

        try {
            createdPlayer.setLoopCount(loopCount < 0 ? -1 : max(1, loopCount));
        } catch (Throwable ignored) {
        }

        player = createdPlayer;
        playerStream = created.stream;
        listener = callback;

        try {
            createdPlayer.start();
            started = true;
        } catch (MediaException e) {
            close();
            throw e;
        }
    }

    public synchronized void close() {
        Player currentPlayer = player;
        InputStream currentStream = playerStream;

        player = null;
        playerStream = null;
        listener = null;
        started = false;

        if (currentPlayer != null) {
            try {
                currentPlayer.stop();
            } catch (Throwable ignored) {
            }
            try {
                currentPlayer.close();
            } catch (Throwable ignored) {
            }
        }
        closeQuietly(currentStream);
    }

    public synchronized void setVolume(int level) {
        VolumeControl control = volumeControl();
        if (control != null) {
            try {
                control.setLevel(clamp(0, 100, level));
            } catch (Throwable ignored) {
            }
        }
    }

    public synchronized void setRatePercent(int percent) {
        RateControl control = rateControl();
        int requested = max(1, percent) * 10000;

        if (control != null) {
            try {
                control.setRate(clamp(control.getMinRate(), control.getMaxRate(), requested));
            } catch (Throwable ignored) {
            }
        }
    }

    public synchronized void setPitchSemitones(int semitones) {
        PitchControl control = pitchControl();
        int requested = semitones * 1000;

        if (control != null) {
            try {
                control.setPitch(clamp(control.getMinPitch(), control.getMaxPitch(), requested));
            } catch (Throwable ignored) {
            }
        }
    }

    public void playerUpdate(Player source, String event, Object eventData) {
        if (event == null) {
            return;
        }
        if (PlayerListener.END_OF_MEDIA.equals(event)) {
            handleEndOfMedia(source);
        } else if (PlayerListener.ERROR.equals(event)) {
            handleError(source, eventData);
        }
    }

    private void handleEndOfMedia(Player source) {
        Listener callback;
        synchronized (this) {
            if (source == null || source != player) {
                return;
            }
            callback = listener;
            close();
        }
        if (callback != null) {
            callback.onPlaybackCompleted();
        }
    }

    private void handleError(Player source, Object eventData) {
        Listener callback;
        String message;
        synchronized (this) {
            if (source == null || source != player) {
                return;
            }
            callback = listener;
            message = eventData == null ? "MIDI unsupported" : eventData.toString();
            close();
        }
        if (callback != null) {
            callback.onPlaybackError(message);
        }
    }

    private static CreatedPlayer createMidiPlayer(byte[] midiBytes) throws IOException, MediaException {
        ByteArrayInputStream stream = new ByteArrayInputStream(midiBytes);
        try {
            Player createdPlayer = Manager.createPlayer(stream, MIDI_CONTENT_TYPE);
            return new CreatedPlayer(createdPlayer, stream);
        } catch (IOException e) {
            closeQuietly(stream);
            throw e;
        } catch (MediaException e) {
            closeQuietly(stream);
            throw e;
        }
    }

    private static int max(int left, int right) {
        return left > right ? left : right;
    }

    private static int clamp(int min, int max, int value) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeCreatedPlayer(CreatedPlayer created) {
        if (created == null) {
            return;
        }
        if (created.player != null) {
            try {
                created.player.close();
            } catch (Throwable ignored) {
            }
        }
        closeQuietly(created.stream);
    }

    private VolumeControl volumeControl() {
        Player currentPlayer = player;
        if (currentPlayer == null) {
            return null;
        }
        try {
            return (VolumeControl) currentPlayer.getControl("VolumeControl");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private RateControl rateControl() {
        Player currentPlayer = player;
        Object control;

        if (currentPlayer == null) {
            return null;
        }
        try {
            control = currentPlayer.getControl("TempoControl");
            if (control instanceof TempoControl) {
                return (TempoControl) control;
            }
        } catch (Throwable ignored) {
        }
        try {
            control = currentPlayer.getControl("RateControl");
            if (control instanceof RateControl) {
                return (RateControl) control;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private PitchControl pitchControl() {
        Player currentPlayer = player;
        if (currentPlayer == null) {
            return null;
        }
        try {
            return (PitchControl) currentPlayer.getControl("PitchControl");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class CreatedPlayer {
        final Player player;
        final InputStream stream;

        CreatedPlayer(Player player, InputStream stream) {
            this.player = player;
            this.stream = stream;
        }
    }
}

