package com.nttdocomo.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import com.nttdocomo.ui.mld.container.MldFile;
import com.nttdocomo.ui.mld.container.MldParser;
import com.nttdocomo.ui.mld.container.TrackChunk;
import com.nttdocomo.ui.mld.event.TrackDecodeResult;
import com.nttdocomo.ui.mld.event.TrackDecoder;
import com.nttdocomo.ui.mld.playback.MidiBytesBuilder;
import com.nttdocomo.ui.mld.timeline.PlaybackTimeline;
import com.nttdocomo.ui.mld.timeline.TimelineCompiler;

final class MldMediaSound implements MediaSound {
    private static final int BUFFER_SIZE = 1024;

    private final String resourcePath;
    private boolean loaded;
    private Throwable loadError;
    private byte[] initialMidi;
    private byte[] loopMidi;
    private boolean hasLoop;

    MldMediaSound(String path) {
        resourcePath = normalizePath(path);
    }

    public synchronized void use() {
        if (loaded || loadError != null) {
            return;
        }

        try {
            load();
            loaded = true;
        } catch (Throwable t) {
            loadError = t;
            System.out.println("[DoJa] MLD load failed: " + resourcePath + " (" + t + ")");
        }
    }

    synchronized byte[] getInitialMidi() {
        use();
        return initialMidi;
    }

    synchronized byte[] getLoopMidi() {
        use();
        return loopMidi;
    }

    synchronized boolean hasLoop() {
        use();
        return hasLoop && loopMidi != null;
    }

    synchronized Throwable getLoadError() {
        use();
        return loadError;
    }

    String getResourcePath() {
        return resourcePath;
    }

    private void load() throws IOException {
        byte[] bytes = loadResourceBytes(resourcePath);
        MldFile file = new MldParser().parse(bytes);
        Vector decodedTracks = decodeTracks(file);
        PlaybackTimeline timeline = new TimelineCompiler().compile(file, decodedTracks);
        MidiBytesBuilder builder = new MidiBytesBuilder();

        initialMidi = builder.build(timeline);
        hasLoop = timeline.loopInfo != null && timeline.loopInfo.hasLoop;
        if (hasLoop) {
            loopMidi = builder.buildLoopBody(timeline);
        }
    }

    private static Vector decodeTracks(MldFile file) throws IOException {
        Vector decoded = new Vector();
        TrackDecoder decoder = new TrackDecoder();
        int i;

        for (i = 0; i < file.tracks.size(); i++) {
            TrackChunk track = (TrackChunk) file.tracks.elementAt(i);
            TrackDecodeResult result = decoder.decode(file, track);
            decoded.addElement(result);
        }
        return decoded;
    }

    private static byte[] loadResourceBytes(String path) throws IOException {
        String name = path;
        String[] candidates;
        int i;

        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        candidates = new String[] { "/" + name, "/res/" + name };

        for (i = 0; i < candidates.length; i++) {
            InputStream input = null;
            try {
                input = MldMediaSound.class.getResourceAsStream(candidates[i]);
                if (input != null) {
                    return readAll(input);
                }
            } finally {
                closeQuietly(input);
            }
        }

        throw new IOException("Resource not found: " + path);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static String normalizePath(String uri) {
        String path = uri == null ? "" : uri;
        if (path.startsWith("resource:///")) {
            path = path.substring("resource:///".length());
        }
        return path;
    }

    private static void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }

}

