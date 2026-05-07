package com.nttdocomo.ui.graphics3d;

import com.nttdocomo.ui.util3d.Transform;

// Abstract base for all renderable 3D objects (DoJa API shim)
public abstract class DrawableObject3D extends Object3D {

    // Blend mode constants
    public static final int BLEND_NORMAL = 0;
    public static final int BLEND_ALPHA  = 32;
    public static final int BLEND_ADD    = 64;

    protected int blendMode;
    protected float transparency;
    protected boolean perspectiveCorrectionEnabled;
    protected boolean blendModeSet;
    protected boolean transparencySet;
    protected boolean perspectiveCorrectionSet;

    protected DrawableObject3D(int type) {
        super(type);
        this.blendMode = BLEND_NORMAL;
        this.transparency = 0.0f;
        this.perspectiveCorrectionEnabled = false;
        this.blendModeSet = false;
        this.transparencySet = false;
        this.perspectiveCorrectionSet = false;
    }

    public abstract void setBlendMode(int mode);
    public abstract void setTransparency(float v);
    public abstract void setPerspectiveCorrectionEnabled(boolean enabled);

    public int getBlendMode() {
        return blendMode;
    }

    public float getTransparency() {
        return transparency;
    }

    public boolean isPerspectiveCorrectionEnabled() {
        return perspectiveCorrectionEnabled;
    }

    protected final float getTransparencyFactor() {
        float alpha = transparency / 100.0f;
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;
        return alpha;
    }

    protected final int getTransparencyAlphaByte() {
        return (int) (getTransparencyFactor() * 255.0f + 0.5f);
    }

    protected final int getRuntimeBlendMode() {
        return toRuntimeBlendMode(blendMode);
    }

    protected static int toRuntimeBlendMode(int mode) {
        switch (mode) {
            case BLEND_ALPHA:
                return javax.microedition.m3g.CompositingMode.ALPHA;
            case BLEND_ADD:
                return javax.microedition.m3g.CompositingMode.ALPHA_ADD;
            default:
                return javax.microedition.m3g.CompositingMode.REPLACE;
        }
    }

    protected static boolean isTransparentRuntimeBlend(int runtimeBlend) {
        return runtimeBlend == javax.microedition.m3g.CompositingMode.ALPHA
            || runtimeBlend == javax.microedition.m3g.CompositingMode.ALPHA_ADD
            || runtimeBlend == 70;
    }

    protected static void setRuntimeBlendValue(javax.microedition.m3g.CompositingMode cm, int runtimeBlend) {
        if (runtimeBlend >= javax.microedition.m3g.CompositingMode.ALPHA
                && runtimeBlend <= javax.microedition.m3g.CompositingMode.REPLACE) {
            cm.setBlending(runtimeBlend);
        } else {
            cm.setBlending(javax.microedition.m3g.CompositingMode.REPLACE);
        }
    }

    // Collision detection between two drawable objects
    public static boolean isCross(DrawableObject3D a, Transform ta,
                                  DrawableObject3D b, Transform tb) {
        // TODO: implement bounding-box collision detection
        return false;
    }

    // Render this object via M3G - subclasses must implement
    public abstract void render(javax.microedition.m3g.Graphics3D g3d,
                                javax.microedition.m3g.Transform worldTransform);
}

