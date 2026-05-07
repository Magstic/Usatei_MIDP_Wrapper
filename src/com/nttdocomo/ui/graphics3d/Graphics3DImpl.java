package com.nttdocomo.ui.graphics3d;

import com.nttdocomo.ui.util3d.Transform;
import java.util.Vector;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Camera;

// DoJa Graphics3D extends Graphics2 鈥?supports cast from Canvas.getGraphics()
// Lazy-bind lifecycle: renderObject3D auto-binds M3G, flushBuffer releases.
public class Graphics3DImpl extends com.nttdocomo.opt.ui.Graphics2 implements Graphics3D {

    // M3G singleton
    private javax.microedition.m3g.Graphics3D m3g;
    private boolean bound;

    // DoJa state: viewport
    private int clipX, clipY, clipW, clipH;
    private boolean clipSet;

    // DoJa state: projection
    private boolean perspectiveMode;
    private float zNear, zFar, fovAngle;
    private int projWidth, projHeight;
    private boolean projDirty;

    // DoJa state: view transform (eye coordinates)
    private Transform viewTransform;
    private boolean viewDirty;

    // DoJa state: lights
    private Vector lights;
    private Vector lightTransforms;
    private boolean lightsDirty;

    // M3G camera + background clear
    private Camera camera;
    private int clearColor;
    private boolean clearColorSet;

    public Graphics3DImpl() {
        super();
        this.m3g = javax.microedition.m3g.Graphics3D.getInstance();
        this.viewTransform = new Transform();
        this.lights = new Vector();
        this.lightTransforms = new Vector();
        this.camera = new Camera();
        this.perspectiveMode = true;
        this.zNear = 1.0f;
        this.zFar = 1000.0f;
        this.fovAngle = 60.0f;
        this.bound = false;
        this.clipSet = false;
        this.projDirty = true;
        this.viewDirty = true;
        this.lightsDirty = true;
        this.clearColor = 0xFF000000;
        this.clearColorSet = false;
    }

    // DoJa: set 3D clipping region within canvas
    public void setClipRectFor3D(int x, int y, int width, int height) {
        this.clipX = x;
        this.clipY = y;
        this.clipW = width;
        this.clipH = height;
        this.clipSet = true;
    }

    // DoJa: set parallel projection
    public void setParallelView(int width, int height) {
        this.perspectiveMode = false;
        this.projWidth = width;
        this.projHeight = height;
        this.zNear = 0.0f;
        this.zFar = 32767.0f;
        this.projDirty = true;
    }

    // DoJa: set perspective projection with fov angle
    public void setPerspectiveView(float zNear, float zFar, float angle) {
        this.perspectiveMode = true;
        this.zNear = zNear;
        this.zFar = zFar;
        this.fovAngle = angle;
        this.projDirty = true;
    }

    // DoJa: set perspective projection with near-plane dimensions
    public void setPerspectiveView(float zNear, float zFar, int width, int height) {
        this.perspectiveMode = true;
        this.zNear = zNear;
        this.zFar = zFar;
        float halfH = height / 2.0f;
        this.fovAngle = 2.0f * com.nttdocomo.ui.util3d.FastMath.atan(halfH / zNear);
        this.projWidth = width;
        this.projHeight = height;
        this.projDirty = true;
    }

    // DoJa: set the eye coordinate transform (view matrix)
    // Store reference 鈥?original DoJa JNI bridge reads Transform lazily at render time,
    // so game code that calls setTransform() then lookAt() on the same object works correctly.
    public void setTransform(Transform t) {
        this.viewTransform = (t != null) ? t : new Transform();
        this.viewDirty = true;
        this.lightsDirty = true;
    }

    // DoJa: add a light source with optional transform
    public void addLight(Light light, Transform transform) {
        lights.addElement(light);
        lightTransforms.addElement(transform);
        lightsDirty = true;
    }

    // DoJa: remove all lights
    public void resetLights() {
        lights.removeAllElements();
        lightTransforms.removeAllElements();
        lightsDirty = true;
    }

    // DoJa: render a drawable 3D object with world transform (auto-binds)
    // DoJa formula: A 脳 T where A=view matrix, T=world transform
    // We bake A脳T into the M3G object transform to bypass camera inversion issues
    public void renderObject3D(DrawableObject3D obj, Transform transform) {
        ensureBound();
        if (lightsDirty) {
            applyLights();
            lightsDirty = false;
        }
        // Compose view 脳 world: A 脳 T
        javax.microedition.m3g.Transform modelView = createDoJaToM3GEyeTransform();
        modelView.postMultiply(viewTransform.getM3GTransform());
        if (transform != null) {
            modelView.postMultiply(transform.getM3GTransform());
        }
        obj.render(m3g, modelView);
    }

    // DoJa: flush rendering buffer to screen, release M3G target
    public void flushBuffer() {
        if (bound) {
            m3g.releaseTarget();
            bound = false;
        }
    }

    // DoJa: set 3D background/clear color (ARGB)
    public void setClearColor(int argb) {
        this.clearColor = argb;
        this.clearColorSet = true;
    }

    // DoJa: set fog (stub - game doesn't use)
    public void setFog(Object fog) {
    }

    // Lazy bind: binds M3G target and applies all stored state
    private void ensureBound() {
        boolean justBound = false;
        if (!bound) {
            m3g.bindTarget(midpGraphics, true, 0);
            bound = true;
            justBound = true;
            projDirty = true;
            viewDirty = true;
            lightsDirty = true;
        }
        if (projDirty || viewDirty) {
            applyCamera();
            projDirty = false;
            viewDirty = false;
        }
        if (justBound) {
            // Only clear depth buffer; preserve 2D content (sky) already drawn
            Background bg = new Background();
            bg.setColorClearEnable(false);
            m3g.clear(bg);
        }
    }

    // Configure M3G camera from DoJa projection + view settings
    // DoJa native D4D uses a symmetric frustum (no aspect correction in projection).
    // Screen anisotropy is handled by viewport mapping, not the projection matrix.
    // Using aspect=1.0 to match the original behaviour.
    private void applyCamera() {
        if (perspectiveMode) {
            camera.setPerspective(fovAngle, 1.0f, zNear, zFar);
        } else {
            float h = (projHeight > 0) ? projHeight : 100.0f;
            camera.setParallel(h, 1.0f, zNear, zFar);
        }

        // Camera at identity 鈥?view transform is baked into each object in renderObject3D
        m3g.setCamera(camera, new javax.microedition.m3g.Transform());

        if (clipSet) {
            m3g.setViewport(clipX, clipY, clipW, clipH);
        } else {
            m3g.setViewport(0, 0, screenWidth, screenHeight);
        }
    }

    // Push all registered lights to M3G
    private void applyLights() {
        m3g.resetLights();
        for (int i = 0; i < lights.size(); i++) {
            Light light = (Light) lights.elementAt(i);
            Transform lt = (Transform) lightTransforms.elementAt(i);
            javax.microedition.m3g.Transform m3gLt = createDoJaToM3GEyeTransform();
            m3gLt.postMultiply(viewTransform.getM3GTransform());
            if (lt != null) {
                m3gLt.postMultiply(lt.getM3GTransform());
            }
            m3g.addLight(light.getM3GLight(), m3gLt);
        }
    }

    private static javax.microedition.m3g.Transform createDoJaToM3GEyeTransform() {
        javax.microedition.m3g.Transform transform = new javax.microedition.m3g.Transform();
        transform.set(new float[] {
            -1.0f, 0.0f, 0.0f, 0.0f,
             0.0f, 1.0f, 0.0f, 0.0f,
             0.0f, 0.0f, -1.0f, 0.0f,
             0.0f, 0.0f, 0.0f, 1.0f
        });
        return transform;
    }
}

