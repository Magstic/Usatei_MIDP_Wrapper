package com.nttdocomo.ui.mld.container;

import java.util.Vector;

public final class MldFile {
    public final int noteExtraBytes;
    public final int exstSize;
    public final Vector tracks;

    public MldFile(
            int noteExtraBytes,
            int exstSize,
            Vector tracks) {
        this.noteExtraBytes = noteExtraBytes;
        this.exstSize = exstSize;
        this.tracks = copyVector(tracks);
    }

    private static Vector copyVector(Vector source) {
        Vector copy = new Vector();
        int i;
        for (i = 0; i < source.size(); i++) {
            copy.addElement(source.elementAt(i));
        }
        return copy;
    }
}

