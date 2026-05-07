package com.nttdocomo.ui.graphics3d;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.CompositingMode;
import javax.microedition.m3g.PolygonMode;
import javax.microedition.m3g.TriangleStripArray;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;

// Geometric primitive with int[] arrays and param bitmask (DoJa API shim)
public class Primitive extends DrawableObject3D {

    // DoJa primitive type constants
    public static final int PRIMITIVE_POINTS        = 1;
    public static final int PRIMITIVE_LINES         = 2;
    public static final int PRIMITIVE_TRIANGLES     = 3;
    public static final int PRIMITIVE_QUADS         = 4;
    public static final int PRIMITIVE_POINT_SPRITES = 5;

    // DoJa param bitmask constants for attribute storage
    public static final int NORMAL_PER_FACE            = 0x0200;
    public static final int NORMAL_PER_VERTEX          = 0x0300;
    public static final int COLOR_NONE                 = 0x0400;
    public static final int COLOR_PER_FACE             = 0x0800;
    public static final int POINT_SPRITE_NONE          = 0x1000;
    public static final int POINT_SPRITE_PER_VERTEX    = 0x3000;
    public static final int TEXTURE_COORD_PER_VERTEX   = 0x3000;
    public static final int TEXTURE_COLORKEY           = 0x0010;

    private static final int NORMAL_MASK = 0x0300;
    private static final int TEXCOORD_MASK = 0x3000;
    private static final int[] QUAD_TRIANGLE_VERTICES = new int[] {
        0, 1, 3,
        1, 2, 3
    };

    private int primitiveType;
    private int param;
    private int numPrimitives;
    private int vertexCount;
    private int faceCount;

    private int[] vertexArray;
    private int[] normalArray;
    private int[] texCoordArray;
    private int[] colorArray;
    private Texture texture;

    private VertexBuffer m3gVertexBuffer;
    private TriangleStripArray m3gIndexBuffer;
    private Appearance m3gAppearance;
    private boolean dirty;
    private int lastBlendMode = -1;
    private float lastTransparency = -1.0f;
    private boolean lastTransparencySet = false;
    private boolean lastPerspectiveCorrection = false;
    private Texture lastTexture = null;

    public Primitive(int type, int param, int n) {
        super(TYPE_PRIMITIVE);
        this.primitiveType = type;
        this.param = param;
        this.numPrimitives = n;
        this.dirty = true;

        this.vertexCount = calcVertexCount(type, n);
        this.faceCount = n;

        this.vertexArray = new int[vertexCount * 3];

        int normalBits = param & NORMAL_MASK;
        if (normalBits == (NORMAL_PER_VERTEX & NORMAL_MASK)) {
            this.normalArray = new int[vertexCount * 3];
        } else if (normalBits == (NORMAL_PER_FACE & NORMAL_MASK)) {
            this.normalArray = new int[faceCount * 3];
        }

        if ((param & TEXCOORD_MASK) == TEXCOORD_MASK) {
            this.texCoordArray = new int[vertexCount * 2];
        }

        int colorBits = param & 0x0C00;
        if (colorBits == COLOR_PER_FACE) {
            this.colorArray = new int[faceCount];
        } else if (colorBits == COLOR_NONE) {
            this.colorArray = new int[1];
        }
    }

    private static int calcVertexCount(int type, int n) {
        switch (type) {
            case PRIMITIVE_QUADS:         return n * 4;
            case PRIMITIVE_TRIANGLES:     return n * 3;
            case PRIMITIVE_LINES:         return n * 2;
            case PRIMITIVE_POINTS:        return n;
            case PRIMITIVE_POINT_SPRITES: return n;
            default:                      return n * 4;
        }
    }

    public int[] getVertexArray() {
        dirty = true;
        return vertexArray;
    }

    public int[] getNormalArray() {
        dirty = true;
        return normalArray;
    }

    public int[] getTextureCoordArray() {
        dirty = true;
        return texCoordArray;
    }

    public int[] getColorArray() {
        dirty = true;
        return colorArray;
    }

    public void setTexture(Texture tex) {
        this.texture = tex;
        this.dirty = true;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setBlendMode(int mode) {
        this.blendMode = mode;
        this.blendModeSet = true;
        this.dirty = true;
    }

    public void setTransparency(float v) {
        this.transparency = v;
        this.transparencySet = true;
        this.dirty = true;
    }

    public void setPerspectiveCorrectionEnabled(boolean enabled) {
        this.perspectiveCorrectionEnabled = enabled;
        this.perspectiveCorrectionSet = true;
        this.dirty = true;
    }

    public static int degreeToAngle(float degree) {
        if (Float.isNaN(degree) || Float.isInfinite(degree)) return 0;
        int a = (int)(degree * 4096.0f / 360.0f) % 4096;
        if (a < 0) a += 4096;
        return a;
    }

    public static float angleToDegree(int angle) {
        int a = angle % 4096;
        if (a < 0) a += 4096;
        return a * 360.0f / 4096.0f;
    }

    private void buildM3GObjects() {
        if (!dirty && m3gVertexBuffer != null) return;
        if (vertexCount < 3) return;

        int renderVertexCount = getRenderVertexCount();

        short[] sv = new short[renderVertexCount * 3];
        for (int i = 0; i < renderVertexCount; i++) {
            int src = getSourceVertexIndex(i);
            int si = src * 3;
            int di = i * 3;
            if (si + 2 < vertexArray.length) {
                sv[di] = (short) vertexArray[si];
                sv[di + 1] = (short) vertexArray[si + 1];
                sv[di + 2] = (short) vertexArray[si + 2];
            }
        }
        VertexArray positions = new VertexArray(renderVertexCount, 3, 2);
        positions.set(0, renderVertexCount, sv);

        m3gVertexBuffer = new VertexBuffer();
        m3gVertexBuffer.setPositions(positions, 1.0f, null);

        if (normalArray != null) {
            int normalMode = param & NORMAL_MASK;
            if (normalMode == (NORMAL_PER_VERTEX & NORMAL_MASK)) {
                short[] sn = new short[renderVertexCount * 3];
                for (int i = 0; i < renderVertexCount; i++) {
                    int src = getSourceVertexIndex(i);
                    int si = src * 3;
                    int di = i * 3;
                    if (si + 2 < normalArray.length) {
                        sn[di] = (short) normalArray[si];
                        sn[di + 1] = (short) normalArray[si + 1];
                        sn[di + 2] = (short) normalArray[si + 2];
                    }
                }
                VertexArray normals = new VertexArray(renderVertexCount, 3, 2);
                normals.set(0, renderVertexCount, sn);
                m3gVertexBuffer.setNormals(normals);
            } else if (normalMode == (NORMAL_PER_FACE & NORMAL_MASK)) {
                short[] sn = new short[renderVertexCount * 3];
                int vpf = renderVertexCount / faceCount;
                for (int f = 0; f < faceCount; f++) {
                    short nx = (f * 3 < normalArray.length) ? (short) normalArray[f * 3] : 0;
                    short ny = (f * 3 + 1 < normalArray.length) ? (short) normalArray[f * 3 + 1] : 0;
                    short nz = (f * 3 + 2 < normalArray.length) ? (short) normalArray[f * 3 + 2] : 0;
                    for (int v = 0; v < vpf; v++) {
                        int idx = (f * vpf + v) * 3;
                        if (idx + 2 < sn.length) {
                            sn[idx] = nx;
                            sn[idx + 1] = ny;
                            sn[idx + 2] = nz;
                        }
                    }
                }
                VertexArray normals = new VertexArray(renderVertexCount, 3, 2);
                normals.set(0, renderVertexCount, sn);
                m3gVertexBuffer.setNormals(normals);
            }
        }

        if (texCoordArray != null && texture != null) {
            int texW = texture.getImageWidth();
            int texH = texture.getImageHeight();
            if (texW <= 0) texW = 128;
            if (texH <= 0) texH = 128;
            int maxDim = (texW > texH) ? texW : texH;
            float texScale = 1.0f / (float) maxDim;
            short[] st = new short[renderVertexCount * 2];
            for (int i = 0; i < renderVertexCount; i++) {
                int src = getSourceVertexIndex(i);
                int si = src * 2;
                int di = i * 2;
                if (si + 1 < texCoordArray.length) {
                    st[di] = (short)(texCoordArray[si] * maxDim / texW);
                    st[di + 1] = (short)(texCoordArray[si + 1] * maxDim / texH);
                }
            }
            VertexArray texCoords = new VertexArray(renderVertexCount, 2, 2);
            texCoords.set(0, renderVertexCount, st);
            m3gVertexBuffer.setTexCoords(0, texCoords, texScale, null);
        }

        int[] stripLengths;
        if (primitiveType == PRIMITIVE_QUADS) {
            stripLengths = new int[numPrimitives * 2];
            for (int i = 0; i < stripLengths.length; i++) {
                stripLengths[i] = 3;
            }
        } else if (primitiveType == PRIMITIVE_TRIANGLES) {
            stripLengths = new int[numPrimitives];
            for (int i = 0; i < numPrimitives; i++) {
                stripLengths[i] = 3;
            }
        } else {
            stripLengths = new int[] { renderVertexCount };
        }
        m3gIndexBuffer = new TriangleStripArray(0, stripLengths);

        dirty = false;
        lastBlendMode = -1;
    }

    private int getRenderVertexCount() {
        if (primitiveType == PRIMITIVE_QUADS) {
            return numPrimitives * 6;
        }
        return vertexCount;
    }

    private int getSourceVertexIndex(int renderVertexIndex) {
        if (primitiveType == PRIMITIVE_QUADS) {
            int quad = renderVertexIndex / 6;
            int corner = QUAD_TRIANGLE_VERTICES[renderVertexIndex % 6];
            return quad * 4 + corner;
        }
        return renderVertexIndex;
    }

    private void updateAppearance() {
        if (blendMode == lastBlendMode && transparency == lastTransparency
                && transparencySet == lastTransparencySet
                && perspectiveCorrectionEnabled == lastPerspectiveCorrection
                && texture == lastTexture && m3gAppearance != null) {
            return;
        }

        m3gAppearance = new Appearance();

        CompositingMode cm = new CompositingMode();
        int runtimeBlend = getRuntimeBlendMode();
        boolean cutoutTexture = texture != null
                && texture.hasTransparentPixels()
                && !texture.hasTranslucentPixels();
        boolean fullyOpaqueObject = !transparencySet || getTransparencyAlphaByte() == 255;
        setRuntimeBlendValue(cm, runtimeBlend);
        if (cutoutTexture) {
            cm.setAlphaThreshold(0.5f);
        }
        if (runtimeBlend == CompositingMode.REPLACE || (cutoutTexture && fullyOpaqueObject)) {
            cm.setDepthTestEnable(true);
            cm.setDepthWriteEnable(true);
            cm.setColorWriteEnable(true);
            cm.setAlphaWriteEnable(true);
        } else {
            cm.setDepthTestEnable(true);
            cm.setDepthWriteEnable(false);
            cm.setColorWriteEnable(true);
            cm.setAlphaWriteEnable(false);
        }
        m3gAppearance.setCompositingMode(cm);

        PolygonMode pm = new PolygonMode();
        pm.setCulling(PolygonMode.CULL_NONE);
        pm.setPerspectiveCorrectionEnable(perspectiveCorrectionEnabled);
        m3gAppearance.setPolygonMode(pm);

        if (texture != null && texture.getM3GTexture() != null) {
            m3gAppearance.setTexture(0, texture.getM3GTexture());
        }

        lastBlendMode = blendMode;
        lastTransparency = transparency;
        lastTransparencySet = transparencySet;
        lastPerspectiveCorrection = perspectiveCorrectionEnabled;
        lastTexture = texture;
    }

    public void render(javax.microedition.m3g.Graphics3D g3d,
                       javax.microedition.m3g.Transform worldTransform) {
        buildM3GObjects();
        updateAppearance();
        if (m3gVertexBuffer != null && m3gIndexBuffer != null && m3gAppearance != null) {
            int alpha;
            if (!transparencySet) {
                alpha = 255;
            } else {
                alpha = getTransparencyAlphaByte();
            }
            m3gVertexBuffer.setDefaultColor((alpha << 24) | 0x00FFFFFF);
            g3d.render(m3gVertexBuffer, m3gIndexBuffer, m3gAppearance, worldTransform);
        }
    }
}

