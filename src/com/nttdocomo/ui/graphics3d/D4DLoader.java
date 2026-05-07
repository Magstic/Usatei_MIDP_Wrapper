package com.nttdocomo.ui.graphics3d;

import java.io.ByteArrayOutputStream;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Loader;
import javax.microedition.m3g.Node;
import javax.microedition.m3g.World;

final class D4DLoader {

    private static final byte[] M3G_IDENTIFIER = {
        (byte) 0xAB, 0x4A, 0x53, 0x52, 0x31, 0x38, 0x34,
        (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final int D4D_VERSION_1 = 0x0100;
    private static final int D4D_MODE_SHARED = 3;
    private static final int D4D_WORLD_TYPE = 22;
    private static final int D4D_TYPE_127 = 127;

    private static final byte[][] SELECTOR_PREFIXES = {
        new byte[] { 0x48, 0x49 },
        new byte[] { (byte) 0x81, (byte) 0x80, (byte) 0x80, 0x3B },
        new byte[] { (byte) 0x83, (byte) 0xF9, (byte) 0xA2, 0x3E },
        new byte[] { 0x47, 0x03, (byte) 0x80, 0x3F, 0x17, (byte) 0xB7, (byte) 0xD1, (byte) 0xB8 },
        new byte[] { 0x77, (byte) 0xCC, 0x2B, 0x32, 0x56, 0x0E, (byte) 0xC9, (byte) 0xBF },
        new byte[] { 0x56, 0x0E, (byte) 0xC9, 0x3F },
        new byte[] { (byte) 0xE1, 0x2E, 0x65, 0x42 }
    };

    private static final int[] SELECTOR_ROWS = { 0, 2, 3, 4, 5, 6, 7 };

    private static final byte[][] TEMPLATE_ROWS = {
        new byte[] {
            0x48, 0x49, 0x5F, 0x4D, 0x41, 0x53, 0x43, 0x4F, 0x54, 0x43,
            0x41, 0x50, 0x53, 0x55, 0x4C, 0x45, 0x5F, 0x56, 0x34, (byte) 0xFF
        },
        new byte[] {
            0x48, 0x49, 0x5F, 0x4D, 0x41, 0x53, 0x43, 0x4F, 0x54, 0x43,
            0x41, 0x50, 0x53, 0x55, 0x4C, 0x45, 0x5F, 0x56, 0x34, (byte) 0xFF
        },
        new byte[] {
            0x48, 0x49,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF
        },
        new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        },
        new byte[] {
            0x77, (byte) 0xCC, 0x2B, 0x32, 0x56, 0x0E, (byte) 0xC9, (byte) 0xBF, 0x56, 0x0E,
            (byte) 0xC9, 0x3F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        },
        new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        },
        new byte[] {
            0x02, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        },
        new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        }
    };

    private D4DLoader() {
    }

    static Group loadGroup(byte[] d4d) throws Exception {
        byte[] m3g = convertToM3G(d4d);
        javax.microedition.m3g.Object3D[] loaded = Loader.load(m3g, 0);
        Node root = chooseRootNode(loaded);
        Group group = new Group();
        group.setM3GRoot(root);
        return group;
    }

    private static byte[] convertToM3G(byte[] d4d) {
        validateHeader(d4d);

        int dataSize = readInt32LE(d4d, 4);
        int end = 10 + dataSize;
        if (end > d4d.length) {
            throw new RuntimeException("D4D data size exceeds input length");
        }

        int lastNonEmptySection = findLastNonEmptySection(d4d, end);
        ParseContext context = new ParseContext();
        ByteArrayOutputStream sections = new ByteArrayOutputStream();

        int pos = 10;
        int sectionIndex = 0;
        while (pos + 9 <= end) {
            int compression = d4d[pos] & 0xFF;
            int totalLen = readInt32LE(d4d, pos + 1);
            int payloadLen = readInt32LE(d4d, pos + 5);
            int bodyLen = totalLen - 13;
            int tailPos = pos + totalLen - 4;
            long tailWord = readUInt32LE(d4d, tailPos);

            if (totalLen <= 0 || pos + totalLen > end) {
                throw new RuntimeException("Invalid D4D section length");
            }
            if (payloadLen < 0 || payloadLen > bodyLen) {
                throw new RuntimeException("Invalid D4D section payload length");
            }
            if (compression != 0) {
                throw new RuntimeException("Compressed raw D4D sections are not supported");
            }

            if (bodyLen > 0) {
                byte[] body = new byte[bodyLen];
                System.arraycopy(d4d, pos + 9, body, 0, bodyLen);

                if (context.hasKey && context.mode == D4D_MODE_SHARED) {
                    applyType127Patch(body, context);
                }

                byte[] m3gPayload = convertSectionBody(body, sectionIndex == lastNonEmptySection, context);
                if (m3gPayload.length > 0) {
                    byte[] m3gSection = buildDataSection(m3gPayload);
                    sections.write(m3gSection, 0, m3gSection.length);
                }
            }

            context.state = (context.state + tailWord) & 0xFFFFFFFFL;
            pos += totalLen;
            sectionIndex++;
        }

        int contentSize = sections.size();
        int totalFileSize = M3G_IDENTIFIER.length + 30 + contentSize;
        byte[] headerSection = buildHeaderSection(totalFileSize, contentSize);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(M3G_IDENTIFIER, 0, M3G_IDENTIFIER.length);
        out.write(headerSection, 0, headerSection.length);
        out.write(sections.toByteArray(), 0, contentSize);
        return out.toByteArray();
    }

    private static void validateHeader(byte[] d4d) {
        if (d4d == null || d4d.length < 10) {
            throw new RuntimeException("Invalid D4D object data");
        }
        if (d4d[0] != 'D' || d4d[1] != '4') {
            throw new RuntimeException("Unsupported D4D magic");
        }
        if (readInt16LE(d4d, 2) != D4D_VERSION_1) {
            throw new RuntimeException("Only raw D4D version 1 is supported");
        }
    }

    private static int findLastNonEmptySection(byte[] d4d, int end) {
        int pos = 10;
        int sectionIndex = 0;
        int lastNonEmpty = -1;
        while (pos + 9 <= end) {
            int totalLen = readInt32LE(d4d, pos + 1);
            int bodyLen = totalLen - 13;
            if (totalLen <= 0 || pos + totalLen > end) {
                throw new RuntimeException("Invalid D4D section length");
            }
            if (bodyLen > 0) {
                lastNonEmpty = sectionIndex;
            }
            pos += totalLen;
            sectionIndex++;
        }
        return lastNonEmpty;
    }

    private static byte[] convertSectionBody(byte[] body, boolean allowFinalWorldTrim, ParseContext context) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;
        while (pos + 5 <= body.length) {
            int type = body[pos] & 0xFF;
            int payloadLen = readInt32LE(body, pos + 1);
            int payloadStart = pos + 5;
            int payloadEnd = payloadStart + payloadLen;
            if (payloadLen < 0 || payloadEnd > body.length) {
                throw new RuntimeException("Invalid D4D object length");
            }

            byte[] payload = new byte[payloadLen];
            System.arraycopy(body, payloadStart, payload, 0, payloadLen);

            if (type == D4D_TYPE_127) {
                installType127(payload, context);
            } else {
                int emitLen = payloadLen;
                if (allowFinalWorldTrim && type == D4D_WORLD_TYPE && payloadEnd == body.length) {
                    int consumed = measureWorldConsumedLength(payload);
                    if (consumed >= 0 && consumed < emitLen) {
                        emitLen = consumed;
                    }
                }

                out.write(type);
                writeInt32LE(out, emitLen);
                out.write(payload, 0, emitLen);
            }

            pos = payloadEnd;
        }
        if (pos != body.length) {
            throw new RuntimeException("Trailing bytes remain inside D4D section body");
        }
        return out.toByteArray();
    }

    private static void installType127(byte[] payload, ParseContext context) {
        if (context.mode != D4D_MODE_SHARED) {
            throw new RuntimeException("Unexpected type 127 outside shared mode");
        }
        if (payload.length != 20) {
            throw new RuntimeException("Invalid type 127 payload size");
        }
        int row = resolveSelectorRow(payload);
        if (row < 0) {
            throw new RuntimeException("Unsupported type 127 selector");
        }
        context.hasKey = true;
        context.selectorRow = row;
    }

    private static int resolveSelectorRow(byte[] payload) {
        byte[] selector = new byte[8];
        int i;
        for (i = 0; i < 8; i++) {
            selector[i] = (byte) ((payload[2 + i] & 0xFF) ^ (payload[i & 1] & 0xFF));
        }
        for (i = 0; i < 8; i++) {
            int other = (payload[12 + i] & 0xFF) ^ (payload[10 + (i & 1)] & 0xFF);
            if ((selector[i] & 0xFF) != other) {
                return -1;
            }
        }

        for (i = 0; i < SELECTOR_PREFIXES.length; i++) {
            if (hasPrefix(selector, SELECTOR_PREFIXES[i])) {
                return SELECTOR_ROWS[i];
            }
        }
        return -1;
    }

    private static boolean hasPrefix(byte[] selector, byte[] prefix) {
        if (prefix.length > selector.length) {
            return false;
        }
        int i;
        for (i = 0; i < prefix.length; i++) {
            if (selector[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static void applyType127Patch(byte[] payload, ParseContext context) {
        if (!context.hasKey || payload.length <= 10) {
            return;
        }

        byte[] temp = new byte[20];
        System.arraycopy(TEMPLATE_ROWS[context.selectorRow], 0, temp, 0, 20);

        int k0 = (int) (context.state % 0xFFL);
        int k1 = (int) (context.state & 0x7FL);
        int k2 = (int) (context.state & 0x3FL);
        int k3 = (int) (context.state & 0x1FL);
        int[] key = { k0, k1, k2, k3 };

        int i;
        for (i = 0; i < temp.length; i++) {
            temp[i] = (byte) ((temp[i] & 0xFF) ^ key[i & 3]);
        }

        int windowStart = ((((temp[0] & 0xFF) ^ (temp[18] & 0xFF)) & 1) != 0) ? 2 : 10;
        long word0 = readUInt32LE(temp, windowStart);
        long word1 = readUInt32LE(temp, windowStart + 4);
        int offset = (int) ((word0 + word1) % (payload.length - 10));

        for (i = 0; i < 10; i++) {
            int patch = temp[windowStart + (i & 7)] & 0xFF;
            payload[offset + i] = (byte) ((payload[offset + i] & 0xFF) ^ patch);
        }
    }

    private static int measureWorldConsumedLength(byte[] payload) {
        try {
            int pos = 0;
            pos = skipObject3D(payload, pos, payload.length);
            pos = skipTransformable(payload, pos, payload.length);
            pos = skipNode(payload, pos, payload.length);
            pos = skipGroup(payload, pos, payload.length);
            pos = skipBytes(pos, 4, payload.length);
            pos = skipBytes(pos, 4, payload.length);
            return pos;
        } catch (RuntimeException ex) {
            return payload.length;
        }
    }

    private static int skipObject3D(byte[] data, int pos, int end) {
        int trackCount;
        int paramCount;
        int i;

        pos = skipBytes(pos, 4, end);
        trackCount = readCount(data, pos, end);
        pos += 4;
        pos = skipBytes(pos, trackCount * 4, end);

        paramCount = readCount(data, pos, end);
        pos += 4;
        for (i = 0; i < paramCount; i++) {
            int len;
            pos = skipBytes(pos, 4, end);
            len = readCount(data, pos, end);
            pos += 4;
            pos = skipBytes(pos, len, end);
        }
        return pos;
    }

    private static int skipTransformable(byte[] data, int pos, int end) {
        pos = skipBytes(pos, 1, end);
        if ((data[pos - 1] & 0xFF) != 0) {
            pos = skipBytes(pos, 40, end);
        }
        pos = skipBytes(pos, 1, end);
        if ((data[pos - 1] & 0xFF) != 0) {
            pos = skipBytes(pos, 64, end);
        }
        return pos;
    }

    private static int skipNode(byte[] data, int pos, int end) {
        pos = skipBytes(pos, 1, end);
        pos = skipBytes(pos, 1, end);
        pos = skipBytes(pos, 1, end);
        pos = skipBytes(pos, 4, end);
        pos = skipBytes(pos, 1, end);
        if ((data[pos - 1] & 0xFF) != 0) {
            pos = skipBytes(pos, 10, end);
        }
        return pos;
    }

    private static int skipGroup(byte[] data, int pos, int end) {
        int childCount = readCount(data, pos, end);
        pos += 4;
        pos = skipBytes(pos, childCount * 4, end);
        return pos;
    }

    private static int readCount(byte[] data, int pos, int end) {
        int value;
        skipBytes(pos, 4, end);
        value = readInt32LE(data, pos);
        if (value < 0) {
            throw new RuntimeException("Negative count in D4D payload");
        }
        return value;
    }

    private static int skipBytes(int pos, int len, int end) {
        if (len < 0 || pos < 0 || pos > end || end - pos < len) {
            throw new RuntimeException("Truncated D4D payload");
        }
        return pos + len;
    }

    private static byte[] buildHeaderSection(int totalFileSize, int contentSize) {
        byte[] section = new byte[30];
        section[0] = 0;
        writeInt32LE(section, 1, 30);
        writeInt32LE(section, 5, 17);
        section[9] = 0;
        writeInt32LE(section, 10, 12);
        section[14] = 1;
        section[15] = 0;
        section[16] = 0;
        writeInt32LE(section, 17, totalFileSize);
        writeInt32LE(section, 21, contentSize);
        section[25] = 0;
        writeInt32LE(section, 26, computeAdler32(section, 0, 26));
        return section;
    }

    private static byte[] buildDataSection(byte[] payload) {
        int totalLen = 13 + payload.length;
        byte[] section = new byte[totalLen];
        section[0] = 0;
        writeInt32LE(section, 1, totalLen);
        writeInt32LE(section, 5, payload.length);
        System.arraycopy(payload, 0, section, 9, payload.length);
        writeInt32LE(section, totalLen - 4, computeAdler32(section, 0, totalLen - 4));
        return section;
    }

    private static Node chooseRootNode(javax.microedition.m3g.Object3D[] loaded) {
        int i;
        if (loaded != null) {
            for (i = 0; i < loaded.length; i++) {
                if (loaded[i] instanceof World) {
                    return unwrapWorldNode((World) loaded[i]);
                }
            }
            for (i = 0; i < loaded.length; i++) {
                if (loaded[i] instanceof Node) {
                    return (Node) loaded[i];
                }
            }
        }
        return new javax.microedition.m3g.Group();
    }

    private static Node unwrapWorldNode(World world) {
        javax.microedition.m3g.Group plainGroup = new javax.microedition.m3g.Group();
        while (world.getChildCount() > 0) {
            Node child = world.getChild(0);
            world.removeChild(child);
            if (!(child instanceof Camera)) {
                plainGroup.addChild(child);
            }
        }
        return plainGroup;
    }

    private static int computeAdler32(byte[] data, int off, int len) {
        int a = 1;
        int b = 0;
        int i;
        for (i = 0; i < len; i++) {
            a = (a + (data[off + i] & 0xFF)) % 65521;
            b = (b + a) % 65521;
        }
        return (b << 16) | a;
    }

    private static void writeInt32LE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    private static void writeInt32LE(byte[] out, int off, int value) {
        out[off] = (byte) (value & 0xFF);
        out[off + 1] = (byte) ((value >>> 8) & 0xFF);
        out[off + 2] = (byte) ((value >>> 16) & 0xFF);
        out[off + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private static int readInt16LE(byte[] data, int off) {
        return (data[off] & 0xFF) | ((data[off + 1] & 0xFF) << 8);
    }

    private static int readInt32LE(byte[] data, int off) {
        return (data[off] & 0xFF)
            | ((data[off + 1] & 0xFF) << 8)
            | ((data[off + 2] & 0xFF) << 16)
            | ((data[off + 3] & 0xFF) << 24);
    }

    private static long readUInt32LE(byte[] data, int off) {
        return ((long) readInt32LE(data, off)) & 0xFFFFFFFFL;
    }

    private static final class ParseContext {
        long state;
        boolean hasKey;
        int selectorRow;
        int mode;

        ParseContext() {
            this.state = 0L;
            this.hasKey = false;
            this.selectorRow = 0;
            this.mode = D4D_MODE_SHARED;
        }
    }
}

