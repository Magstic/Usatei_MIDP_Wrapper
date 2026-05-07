package com.nttdocomo.ui;

// DoJa Graphics bridge wrapping MIDP Graphics for 2D rendering
public class Graphics {

    protected javax.microedition.lcdui.Graphics midpGraphics;
    protected javax.microedition.lcdui.Image backBuffer;
    private javax.microedition.lcdui.Image frontBuffer;
    private javax.microedition.lcdui.Graphics frontGraphics;
    protected javax.microedition.lcdui.Canvas parentCanvas;
    protected int screenWidth;
    protected int screenHeight;
    private int currentARGB = 0xFF000000;
    private long lastSynchronousFlush;
    private long lastFlush;
    private static final long MIN_FLUSH_INTERVAL_MS = 6L;
    private static final long SYNCHRONOUS_FLUSH_INTERVAL_MS = 50L;

    public Graphics() {}

    // Initialize with MIDP Canvas dimensions for double buffering
    public void init(int width, int height) {
        if (width <= 0) width = 240;
        if (height <= 0) height = 240;
        this.screenWidth = width;
        this.screenHeight = height;
        this.backBuffer = javax.microedition.lcdui.Image.createImage(width, height);
        this.midpGraphics = backBuffer.getGraphics();
        this.frontBuffer = javax.microedition.lcdui.Image.createImage(width, height);
        this.frontGraphics = frontBuffer.getGraphics();
        this.lastFlush = 0L;
        this.lastSynchronousFlush = 0L;
    }

    private void ensureSurface() {
        int width = screenWidth;
        int height = screenHeight;
        if (parentCanvas != null) {
            int canvasWidth = parentCanvas.getWidth();
            int canvasHeight = parentCanvas.getHeight();
            if (canvasWidth > 0) width = canvasWidth;
            if (canvasHeight > 0) height = canvasHeight;
        }
        if (width <= 0) width = 240;
        if (height <= 0) height = 240;
        if (backBuffer == null || midpGraphics == null
                || width != screenWidth || height != screenHeight) {
            init(width, height);
        }
    }

    // Access underlying MIDP Graphics for direct use
    public javax.microedition.lcdui.Graphics getMIDPGraphics() {
        ensureSurface();
        return midpGraphics;
    }

    // Access back buffer for flipping
    public javax.microedition.lcdui.Image getBackBuffer() {
        ensureSurface();
        return backBuffer;
    }

    public javax.microedition.lcdui.Image getDisplayBuffer() {
        ensureSurface();
        return frontBuffer;
    }

    // DoJa double buffering: lock starts drawing
    public void lock() {
        ensureSurface();
    }

    // DoJa double buffering: unlock flushes to screen
    public void unlock(boolean flush) {
        ensureSurface();
        if (flush && parentCanvas != null) {
            if (frontGraphics != null && backBuffer != null) {
                frontGraphics.drawImage(backBuffer, 0, 0,
                    javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT);
            }
            parentCanvas.repaint();
            long now = System.currentTimeMillis();
            if (now - lastSynchronousFlush >= SYNCHRONOUS_FLUSH_INTERVAL_MS) {
                parentCanvas.serviceRepaints();
                lastSynchronousFlush = System.currentTimeMillis();
            } else {
                Thread.yield();
            }
            paceFlush();
        }
    }

    private void paceFlush() {
        long now = System.currentTimeMillis();
        if (lastFlush != 0L) {
            long wait = MIN_FLUSH_INTERVAL_MS - (now - lastFlush);
            if (wait > 0L) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                }
                now = System.currentTimeMillis();
            }
        }
        lastFlush = now;
    }

    // Pack RGB into 0xFFRRGGBB
    public static int getColorOfRGB(int r, int g, int b) {
        checkColorComponent(r);
        checkColorComponent(g);
        checkColorComponent(b);
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    // Pack ARGB into 0xAARRGGBB
    public static int getColorOfRGB(int r, int g, int b, int a) {
        checkColorComponent(r);
        checkColorComponent(g);
        checkColorComponent(b);
        checkColorComponent(a);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static void checkColorComponent(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException();
        }
    }

    // Set drawing color from ARGB int
    public void setColor(int argb) {
        this.currentARGB = argb;
        midpGraphics.setColor(argb & 0x00FFFFFF);
    }

    // Draw DoJa Image at position
    public void drawImage(Image img, int x, int y) {
        if (img == null || img.getMIDPImage() == null) return;
        int imageAlpha = img.getAlpha();
        if (imageAlpha <= 0) return;
        javax.microedition.lcdui.Image src = img.getMIDPImage();
        if (imageAlpha >= 255) {
            midpGraphics.drawImage(src, x, y,
                javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT);
            return;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        int[] pixels = new int[width * height];
        src.getRGB(pixels, 0, width, 0, 0, width, height);
        applyImageAlpha(pixels, imageAlpha);
        midpGraphics.drawRGB(pixels, 0, width, x, y, width, height, true);
    }

    // Draw scaled sub-region of DoJa Image (nearest-neighbor scaling via RGB)
    public void drawScaledImage(Image img, int dx, int dy, int dw, int dh,
                                int sx, int sy, int sw, int sh) {
        if (img == null || img.getMIDPImage() == null) return;
        if (dw <= 0 || dh <= 0 || sw <= 0 || sh <= 0) return;
        int imageAlpha = img.getAlpha();
        if (imageAlpha <= 0) return;
        javax.microedition.lcdui.Image src = img.getMIDPImage();
        int imgW = src.getWidth();
        int imgH = src.getHeight();
        // Clamp source rect to image bounds
        if (sx < 0) { sw += sx; dx -= sx * dw / sw; sx = 0; }
        if (sy < 0) { sh += sy; dy -= sy * dh / sh; sy = 0; }
        if (sx + sw > imgW) sw = imgW - sx;
        if (sy + sh > imgH) sh = imgH - sy;
        if (sw <= 0 || sh <= 0) return;
        // No scaling needed: direct blit
        if (dw == sw && dh == sh && imageAlpha >= 255) {
            midpGraphics.setClip(dx, dy, dw, dh);
            midpGraphics.drawImage(src, dx - sx, dy - sy,
                javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT);
            midpGraphics.setClip(0, 0, screenWidth, screenHeight);
            return;
        }
        // Nearest-neighbor scale via getRGB
        int[] srcPixels = new int[sw * sh];
        src.getRGB(srcPixels, 0, sw, sx, sy, sw, sh);
        if (imageAlpha < 255) {
            applyImageAlpha(srcPixels, imageAlpha);
        }
        if (dw == sw && dh == sh) {
            midpGraphics.drawRGB(srcPixels, 0, sw, dx, dy, dw, dh, true);
            return;
        }
        int[] dstPixels = new int[dw * dh];
        for (int y = 0; y < dh; y++) {
            int srcY = y * sh / dh;
            int srcRowOff = srcY * sw;
            int dstRowOff = y * dw;
            for (int x = 0; x < dw; x++) {
                dstPixels[dstRowOff + x] = srcPixels[srcRowOff + x * sw / dw];
            }
        }
        midpGraphics.drawRGB(dstPixels, 0, dw, dx, dy, dw, dh, true);
    }

    private static void applyImageAlpha(int[] pixels, int alpha) {
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int srcAlpha = (pixel >>> 24) & 0xFF;
            if (srcAlpha == 0) {
                pixels[i] = 0;
                continue;
            }
            int outAlpha = (srcAlpha * alpha + 127) / 255;
            pixels[i] = (pixel & 0x00FFFFFF) | (outAlpha << 24);
        }
    }

    // Draw text string
    public void drawString(String str, int x, int y) {
        midpGraphics.drawString(str, x, y,
            javax.microedition.lcdui.Graphics.BASELINE | javax.microedition.lcdui.Graphics.LEFT);
    }

    // Fill rectangle (alpha-aware via drawRGB when needed)
    public void fillRect(int x, int y, int w, int h) {
        int alpha = (currentARGB >>> 24) & 0xFF;
        if (alpha == 0) return;
        if (alpha < 255 && w > 0 && h > 0) {
            int[] rgb = new int[w * h];
            for (int i = 0; i < rgb.length; i++) rgb[i] = currentARGB;
            midpGraphics.drawRGB(rgb, 0, w, x, y, w, h, true);
            return;
        }
        midpGraphics.fillRect(x, y, w, h);
    }

    // Fill polygon (DoJa-specific, alpha-aware via offscreen compositing)
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints < 3) return;
        int alpha = (currentARGB >>> 24) & 0xFF;
        if (alpha == 0) return;
        if (alpha < 255) {
            // Find bounding box
            int minX = xPoints[0], maxX = xPoints[0], minY = yPoints[0], maxY = yPoints[0];
            for (int i = 1; i < nPoints; i++) {
                if (xPoints[i] < minX) minX = xPoints[i];
                if (xPoints[i] > maxX) maxX = xPoints[i];
                if (yPoints[i] < minY) minY = yPoints[i];
                if (yPoints[i] > maxY) maxY = yPoints[i];
            }
            int bw = maxX - minX + 1, bh = maxY - minY + 1;
            if (bw <= 0 || bh <= 0) return;
            javax.microedition.lcdui.Image tmp = javax.microedition.lcdui.Image.createImage(bw, bh);
            javax.microedition.lcdui.Graphics tg = tmp.getGraphics();
            tg.setColor(0);
            tg.fillRect(0, 0, bw, bh);
            tg.setColor(0x00FFFFFF);
            int[] tx = new int[nPoints], ty = new int[nPoints];
            for (int i = 0; i < nPoints; i++) { tx[i] = xPoints[i] - minX; ty[i] = yPoints[i] - minY; }
            for (int i = 1; i < nPoints - 1; i++) {
                tg.fillTriangle(tx[0], ty[0], tx[i], ty[i], tx[i + 1], ty[i + 1]);
            }
            int[] pix = new int[bw * bh];
            tmp.getRGB(pix, 0, bw, 0, 0, bw, bh);
            for (int i = 0; i < pix.length; i++) {
                if ((pix[i] & 0x00FFFFFF) != 0) pix[i] = currentARGB;
                else pix[i] = 0;
            }
            midpGraphics.drawRGB(pix, 0, bw, minX, minY, bw, bh, true);
            return;
        }
        for (int i = 1; i < nPoints - 1; i++) {
            midpGraphics.fillTriangle(
                xPoints[0], yPoints[0],
                xPoints[i], yPoints[i],
                xPoints[i + 1], yPoints[i + 1]);
        }
    }

    // Fill arc (alpha-aware)
    public void fillArc(int x, int y, int w, int h, int startAngle, int arcAngle) {
        int alpha = (currentARGB >>> 24) & 0xFF;
        if (alpha == 0) return;
        if (alpha < 255 && w > 0 && h > 0) {
            javax.microedition.lcdui.Image tmp = javax.microedition.lcdui.Image.createImage(w, h);
            javax.microedition.lcdui.Graphics tg = tmp.getGraphics();
            tg.setColor(0);
            tg.fillRect(0, 0, w, h);
            tg.setColor(0x00FFFFFF);
            tg.fillArc(0, 0, w, h, startAngle, arcAngle);
            int[] pix = new int[w * h];
            tmp.getRGB(pix, 0, w, 0, 0, w, h);
            for (int i = 0; i < pix.length; i++) {
                if ((pix[i] & 0x00FFFFFF) != 0) pix[i] = currentARGB;
                else pix[i] = 0;
            }
            midpGraphics.drawRGB(pix, 0, w, x, y, w, h, true);
            return;
        }
        midpGraphics.fillArc(x, y, w, h, startAngle, arcAngle);
    }

    // Draw arc outline (alpha-aware)
    public void drawArc(int x, int y, int w, int h, int startAngle, int arcAngle) {
        int alpha = (currentARGB >>> 24) & 0xFF;
        if (alpha == 0) return;
        if (alpha < 255 && w > 0 && h > 0) {
            int pw = w + 2, ph = h + 2;
            javax.microedition.lcdui.Image tmp = javax.microedition.lcdui.Image.createImage(pw, ph);
            javax.microedition.lcdui.Graphics tg = tmp.getGraphics();
            tg.setColor(0);
            tg.fillRect(0, 0, pw, ph);
            tg.setColor(0x00FFFFFF);
            tg.drawArc(1, 1, w, h, startAngle, arcAngle);
            int[] pix = new int[pw * ph];
            tmp.getRGB(pix, 0, pw, 0, 0, pw, ph);
            for (int i = 0; i < pix.length; i++) {
                if ((pix[i] & 0x00FFFFFF) != 0) pix[i] = currentARGB;
                else pix[i] = 0;
            }
            midpGraphics.drawRGB(pix, 0, pw, x - 1, y - 1, pw, ph, true);
            return;
        }
        midpGraphics.drawArc(x, y, w, h, startAngle, arcAngle);
    }
}

