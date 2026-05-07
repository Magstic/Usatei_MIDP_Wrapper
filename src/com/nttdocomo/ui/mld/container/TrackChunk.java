package com.nttdocomo.ui.mld.container;

import com.nttdocomo.ui.mld.util.ByteArrayUtil;

public final class TrackChunk {
    public final int index;
    public final byte[] payload;

    public TrackChunk(int index, byte[] payload) {
        this.index = index;
        this.payload = ByteArrayUtil.copy(payload);
    }
}

