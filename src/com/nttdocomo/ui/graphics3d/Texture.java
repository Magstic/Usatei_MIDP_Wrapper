package com.nttdocomo.ui.graphics3d;

import javax.microedition.m3g.Image2D;
import javax.microedition.m3g.Texture2D;
import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.CompositingMode;

// Texture resource for 3D rendering (DoJa API shim over M3G Texture2D)
public class Texture extends Object3D {

    private Texture2D m3gTexture;
    private Image2D m3gImage;
    private int imageWidth;
    private int imageHeight;
    private boolean hasTransparentPixels;
    private boolean hasTranslucentPixels;

    public Texture() {
        super(TYPE_TEXTURE);
    }

    // Set texture from M3G Image2D (used by resource loading bridge)
    public void setImage(Image2D image) {
        setImage(image, false, false);
    }

    void setImage(Image2D image, boolean hasTransparentPixels, boolean hasTranslucentPixels) {
        this.m3gImage = image;
        this.hasTransparentPixels = hasTransparentPixels;
        this.hasTranslucentPixels = hasTranslucentPixels;
        if (image != null) {
            this.imageWidth = image.getWidth();
            this.imageHeight = image.getHeight();
            this.m3gTexture = new Texture2D(image);
            this.m3gTexture.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
            this.m3gTexture.setWrapping(Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT);
        } else {
            this.imageWidth = 0;
            this.imageHeight = 0;
            this.m3gTexture = null;
        }
    }

    public int getImageWidth() { return imageWidth; }
    public int getImageHeight() { return imageHeight; }
    boolean hasTransparentPixels() { return hasTransparentPixels; }
    boolean hasTranslucentPixels() { return hasTranslucentPixels; }

    public Texture2D getM3GTexture() {
        return m3gTexture;
    }

    public Image2D getM3GImage() {
        return m3gImage;
    }
}

