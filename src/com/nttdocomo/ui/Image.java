package com.nttdocomo.ui;

// DoJa Image bridge wrapping MIDP Image
public class Image {

    private javax.microedition.lcdui.Image midpImage;
    private int alpha = 255;

    public Image(javax.microedition.lcdui.Image img) {
        this.midpImage = img;
    }

    public javax.microedition.lcdui.Image getMIDPImage() {
        return midpImage;
    }

    // DoJa-specific: set global alpha for this image
    public void setAlpha(int alpha) {
        this.alpha = (alpha < 0) ? 0 : (alpha > 255) ? 255 : alpha;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getWidth() {
        return (midpImage != null) ? midpImage.getWidth() : 0;
    }

    public int getHeight() {
        return (midpImage != null) ? midpImage.getHeight() : 0;
    }
}

