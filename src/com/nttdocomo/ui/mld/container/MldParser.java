package com.nttdocomo.ui.mld.container;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import com.nttdocomo.ui.mld.util.ByteArrayUtil;

public final class MldParser {
    public MldFile parse(byte[] data) throws IOException {
        int offset;
        int noteExtraBytes = 0;
        int exstSize = 0;
        boolean noteSeen = false;
        boolean exstSeen = false;
        Vector tracks = new Vector();

        if (data.length < 13) {
            throw new IOException("MLD file too small");
        }

        String magic = decodeAscii(data, 0, 4);
        if (!"melo".equals(magic)) {
            throw new IOException("Unsupported MLD magic: " + magic);
        }

        offset = 13;
        while (offset < data.length) {
            String chunkId;
            ChunkSpec spec;
            int payloadLength;
            int payloadStart;
            int payloadEnd;
            byte[] payload;

            if (offset + 4 > data.length) {
                throw new IOException("Truncated top-level chunk id at 0x" + Integer.toHexString(offset));
            }

            chunkId = decodeAscii(data, offset, 4);
            spec = specForTopLevelChunk(chunkId);
            if (spec == null) {
                throw new IOException("Unsupported top-level chunk: " + chunkId + " at 0x" + Integer.toHexString(offset));
            }

            if (spec.lengthFieldBytes == 2) {
                if (offset + 6 > data.length) {
                    throw new IOException("Truncated top-level chunk header at 0x" + Integer.toHexString(offset));
                }
                payloadLength = readBe16(data, offset + 4);
                payloadStart = offset + 6;
            } else {
                if (offset + 8 > data.length) {
                    throw new IOException("Truncated top-level chunk header at 0x" + Integer.toHexString(offset));
                }
                payloadLength = (int) readBe32(data, offset + 4);
                payloadStart = offset + 8;
            }

            payloadEnd = payloadStart + payloadLength;
            ensureRange(data.length, payloadStart, payloadEnd, "top-level chunk " + chunkId);

            payload = ByteArrayUtil.copyRange(data, payloadStart, payloadEnd);

            if ("note".equals(chunkId)) {
                if (!noteSeen) {
                    if (payloadLength != 2) {
                        throw new IOException("Top-level note chunk must use a 2-byte payload");
                    }
                    noteExtraBytes = readBe16(payload, 0);
                    noteSeen = true;
                }
            } else if ("exst".equals(chunkId)) {
                if (!exstSeen) {
                    if (payloadLength != 2) {
                        throw new IOException("Top-level exst chunk must use a 2-byte payload");
                    }
                    exstSize = readBe16(payload, 0);
                    exstSeen = true;
                }
            }

            if ("trac".equals(chunkId)) {
                tracks.addElement(new TrackChunk(tracks.size(), payload));
            }

            offset = payloadEnd;
        }

        return new MldFile(
                noteExtraBytes,
                exstSize,
                tracks);
    }

    private static ChunkSpec specForTopLevelChunk(String chunkId) {
        if (isInfoChunkId(chunkId)) {
            return new ChunkSpec(2);
        }
        if ("adat".equals(chunkId)) {
            return new ChunkSpec(4);
        }
        if ("trac".equals(chunkId)) {
            return new ChunkSpec(4);
        }
        if (isTopLevelBe16ResourceChunkId(chunkId)) {
            return new ChunkSpec(2);
        }
        return null;
    }

    private static boolean isInfoChunkId(String chunkId) {
        return "vers".equals(chunkId)
                || "sorc".equals(chunkId)
                || "prot".equals(chunkId)
                || "auth".equals(chunkId)
                || "titl".equals(chunkId)
                || "copy".equals(chunkId)
                || "date".equals(chunkId)
                || "note".equals(chunkId)
                || "exst".equals(chunkId)
                || "supt".equals(chunkId);
    }

    private static boolean isTopLevelBe16ResourceChunkId(String chunkId) {
        return "thrd".equals(chunkId) || "ainf".equals(chunkId);
    }

    private static void ensureRange(int length, int start, int end, String label) throws IOException {
        if (start < 0 || end < start || end > length) {
            throw new IOException("Invalid range for " + label);
        }
    }

    private static int readBe16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static long readBe32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | ((long) (data[offset + 3] & 0xFF));
    }

    private static String decodeAscii(byte[] data, int offset, int length) throws IOException {
        try {
            return new String(data, offset, length, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e.toString());
        }
    }

    private static final class ChunkSpec {
        final int lengthFieldBytes;

        ChunkSpec(int lengthFieldBytes) {
            this.lengthFieldBytes = lengthFieldBytes;
        }
    }
}

