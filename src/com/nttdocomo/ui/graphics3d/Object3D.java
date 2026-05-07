package com.nttdocomo.ui.graphics3d;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.m3g.Image2D;

// Base class for all DoJa 3D objects (shim over M3G)
public class Object3D {

    private static final boolean D4D_DEBUG = false;

    // DoJa type constants (official SDK values from doja_classes.zip)
    public static final int TYPE_NONE         = 0;
    public static final int TYPE_ACTION_TABLE = 1;
    public static final int TYPE_FIGURE       = 2;
    public static final int TYPE_TEXTURE      = 3;
    public static final int TYPE_FOG          = 4;
    public static final int TYPE_LIGHT        = 5;
    public static final int TYPE_PRIMITIVE    = 6;
    public static final int TYPE_GROUP        = 7;
    public static final int TYPE_GROUP_MESH   = 8;

    protected int objectType;
    protected int currentTime;
    protected boolean disposed;

    private static void debugD4D(String message) {
        if (D4D_DEBUG) {
            System.out.println("[D4D] " + message);
        }
    }

    private static void debugD4D(String message, Throwable t) {
        if (D4D_DEBUG) {
            System.out.println("[D4D] " + message);
            t.printStackTrace();
        }
    }

    protected Object3D(int type) {
        this.objectType = type;
        this.currentTime = 0;
        this.disposed = false;
    }

    public int getType() {
        return objectType;
    }

    public void setTime(int time) {
        this.currentTime = time;
    }

    public int getTime() {
        return currentTime;
    }

    public void dispose() {
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    // Load from resource name (replaces DoJa Connector.openInputStream("resource:///..."))
    public static Object3D createInstance(String resourceName) throws IOException {
        String name = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        String[] paths = { "/" + name, "/res/" + name };
        InputStream is = null;
        int i;
        for (i = 0; i < paths.length; i++) {
            is = Object3D.class.getResourceAsStream(paths[i]);
            if (is != null) {
                break;
            }
        }
        if (is == null) {
            throw new IOException("Resource not found: " + name);
        }
        try {
            return createInstance(is);
        } finally {
            is.close();
        }
    }

    // Load DoJa 3D object from stream: auto-detects BMP texture vs D4D model
    public static Object3D createInstance(InputStream is) throws IOException {
        byte[] data = readFully(is);
        return createInstance(data);
    }

    // Load DoJa 3D object from byte array
    public static Object3D createInstance(byte[] data) {
        if (data == null || data.length < 2) {
            throw new RuntimeException("Invalid 3D object data");
        }
        if (data[0] == 0x42 && data[1] == 0x4D) {
            return loadBmpTexture(data);
        }
        return loadD4dModel(data);
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    // Parse BMP data and create Texture with M3G Image2D
    // Supports 8bpp (palette), 24bpp, 32bpp uncompressed BMP
    private static Texture loadBmpTexture(byte[] data) {
        int pixelOffset = readInt32LE(data, 10);
        int dibSize = readInt32LE(data, 14);
        int width = readInt32LE(data, 18);
        int height = readInt32LE(data, 22);
        int bpp = readInt16LE(data, 28);
        int compression = readInt32LE(data, 30);
        int numColors = readInt32LE(data, 46);

        if (compression != 0 && compression != 3) {
            throw new RuntimeException("Unsupported BMP compression: " + compression);
        }

        byte[] palR = null;
        byte[] palG = null;
        byte[] palB = null;
        int keyR = 0;
        int keyG = 0;
        int keyB = 0;
        int texW;
        int texH;
        byte[] pixels;
        int bytesPerPixel;
        int rowStride;
        boolean hasTransparentPixels = false;
        boolean hasTranslucentPixels = false;
        int y;

        if (bpp == 8) {
            int palCount = (numColors > 0) ? numColors : 256;
            int palOffset = 14 + dibSize;
            int i;
            palR = new byte[palCount];
            palG = new byte[palCount];
            palB = new byte[palCount];
            for (i = 0; i < palCount; i++) {
                int po = palOffset + i * 4;
                palB[i] = data[po];
                palG[i] = data[po + 1];
                palR[i] = data[po + 2];
            }
        } else if (pixelOffset + 2 < data.length) {
            keyR = data[pixelOffset + 2] & 0xFF;
            keyG = data[pixelOffset + 1] & 0xFF;
            keyB = data[pixelOffset] & 0xFF;
        }

        texW = nextPow2(width);
        texH = nextPow2(height);
        pixels = new byte[texW * texH * 4];
        bytesPerPixel = (bpp == 8) ? 1 : bpp / 8;
        rowStride = ((width * bytesPerPixel + 3) / 4) * 4;

        for (y = 0; y < height; y++) {
            int srcRow = pixelOffset + (height - 1 - y) * rowStride;
            int dstRow = y * texW * 4;
            int x;
            for (x = 0; x < width; x++) {
                int srcIdx = srcRow + x * bytesPerPixel;
                int dstIdx = dstRow + x * 4;
                boolean isKey = false;
                if (bpp == 8) {
                    int idx = data[srcIdx] & 0xFF;
                    isKey = (idx == 0);
                    if (idx < palR.length) {
                        pixels[dstIdx] = palR[idx];
                        pixels[dstIdx + 1] = palG[idx];
                        pixels[dstIdx + 2] = palB[idx];
                    }
                } else if (srcIdx + 2 < data.length) {
                    int pr = data[srcIdx + 2] & 0xFF;
                    int pg = data[srcIdx + 1] & 0xFF;
                    int pb = data[srcIdx] & 0xFF;
                    pixels[dstIdx] = (byte) pr;
                    pixels[dstIdx + 1] = (byte) pg;
                    pixels[dstIdx + 2] = (byte) pb;
                    isKey = (pr == keyR && pg == keyG && pb == keyB);
                }
                if (isKey) {
                    pixels[dstIdx + 3] = 0;
                    hasTransparentPixels = true;
                } else {
                    pixels[dstIdx + 3] = (byte) 0xFF;
                }
            }
        }

        Image2D image = new Image2D(Image2D.RGBA, texW, texH, pixels);
        Texture tex = new Texture();
        tex.setImage(image, hasTransparentPixels, hasTranslucentPixels);
        return tex;
    }

    private static int nextPow2(int n) {
        int v = 1;
        while (v < n) {
            v <<= 1;
        }
        return v;
    }

    private static Object3D loadD4dModel(byte[] d4d) {
        try {
            return D4DLoader.loadGroup(d4d);
        } catch (Exception e) {
            debugD4D("D4D Loader failed: " + e.getMessage(), e);
            return new Group();
        }
    }

    private static int readInt32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF)
             | ((data[offset + 1] & 0xFF) << 8)
             | ((data[offset + 2] & 0xFF) << 16)
             | ((data[offset + 3] & 0xFF) << 24);
    }

    private static int readInt16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}

