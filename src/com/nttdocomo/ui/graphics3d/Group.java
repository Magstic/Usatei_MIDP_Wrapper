package com.nttdocomo.ui.graphics3d;

import com.nttdocomo.ui.util3d.Transform;
import java.util.Vector;

// Container for multiple 3D objects with shared transform (DoJa API shim)
public class Group extends DrawableObject3D {

    private Vector elements;
    private Transform groupTransform;
    // M3G scene graph root loaded from D4D
    private javax.microedition.m3g.Node m3gRoot;
    private Vector m3gAppearanceStates;
    private float m3gRootBaseAlphaFactor;

    public Group() {
        super(TYPE_GROUP);
        this.elements = new Vector();
        this.groupTransform = new Transform();
        this.m3gRoot = null;
        this.m3gAppearanceStates = new Vector();
        this.m3gRootBaseAlphaFactor = 1.0f;
    }

    // Set M3G scene graph root (from D4D loader)
    public void setM3GRoot(javax.microedition.m3g.Node node) {
        this.m3gRoot = node;
        captureM3GState();
    }

    // Drive M3G animation when setTime is called on D4D model
    public void setTime(int time) {
        super.setTime(time);
        if (m3gRoot != null) {
            m3gRoot.animate(time);
        }
    }

    public void addElement(Object3D obj) {
        elements.addElement(obj);
    }

    public void removeElement(int index) {
        elements.removeElementAt(index);
    }

    public Object3D getElement(int index) {
        return (Object3D) elements.elementAt(index);
    }

    public int getNumElements() {
        return elements.size();
    }

    public void setTransform(Transform t) {
        this.groupTransform.set(t);
    }

    public void getTransform(Transform t) {
        t.set(this.groupTransform);
    }

    public void setBlendMode(int mode) {
        this.blendMode = mode;
        this.blendModeSet = true;
        applyM3GState();
    }

    public void setTransparency(float v) {
        this.transparency = v;
        this.transparencySet = true;
        applyM3GState();
    }

    public void setPerspectiveCorrectionEnabled(boolean enabled) {
        this.perspectiveCorrectionEnabled = enabled;
        this.perspectiveCorrectionSet = true;
        applyM3GState();
    }

    // Render all child drawable objects via M3G
    public void render(javax.microedition.m3g.Graphics3D g3d,
                       javax.microedition.m3g.Transform worldTransform) {
        // Combine world transform with group transform: world * group
        javax.microedition.m3g.Transform combined = new javax.microedition.m3g.Transform();
        if (worldTransform != null) {
            combined.set(worldTransform);
        }
        combined.postMultiply(groupTransform.getM3GTransform());

        // D4D model: render M3G scene graph directly
        if (m3gRoot != null) {
            applyM3GState();
            g3d.render(m3gRoot, combined);
            return;
        }

        // Propagate group blendMode/transparency to drawable children
        boolean hasOverride = blendModeSet || transparencySet || perspectiveCorrectionSet;

        for (int i = 0; i < elements.size(); i++) {
            Object3D elem = (Object3D) elements.elementAt(i);
            if (elem instanceof DrawableObject3D) {
                DrawableObject3D child = (DrawableObject3D) elem;
                if (hasOverride) {
                    int savedBlend = child.blendMode;
                    float savedTransp = child.transparency;
                    boolean savedPersp = child.perspectiveCorrectionEnabled;
                    boolean savedBlendSet = child.blendModeSet;
                    boolean savedTranspSet = child.transparencySet;
                    boolean savedPerspSet = child.perspectiveCorrectionSet;
                    if (blendModeSet) {
                        child.blendMode = blendMode;
                        child.blendModeSet = true;
                    }
                    if (transparencySet) {
                        child.transparency = transparency;
                        child.transparencySet = true;
                    }
                    if (perspectiveCorrectionSet) {
                        child.perspectiveCorrectionEnabled = perspectiveCorrectionEnabled;
                        child.perspectiveCorrectionSet = true;
                    }
                    child.render(g3d, combined);
                    child.blendMode = savedBlend;
                    child.transparency = savedTransp;
                    child.perspectiveCorrectionEnabled = savedPersp;
                    child.blendModeSet = savedBlendSet;
                    child.transparencySet = savedTranspSet;
                    child.perspectiveCorrectionSet = savedPerspSet;
                } else {
                    child.render(g3d, combined);
                }
            }
        }
    }

    private void applyM3GState() {
        if (m3gRoot == null) return;
        if (m3gAppearanceStates.size() == 0) {
            captureM3GState();
        }
        if (transparencySet) {
            float alpha = m3gRootBaseAlphaFactor * getTransparencyFactor();
            if (alpha > 1.0f) alpha = 1.0f;
            m3gRoot.setAlphaFactor(alpha);
        } else {
            m3gRoot.setAlphaFactor(m3gRootBaseAlphaFactor);
        }

        for (int i = 0; i < m3gAppearanceStates.size(); i++) {
            applyAppearanceState((AppearanceState) m3gAppearanceStates.elementAt(i));
        }
    }

    private void captureM3GState() {
        m3gAppearanceStates.removeAllElements();
        if (m3gRoot == null) {
            m3gRootBaseAlphaFactor = 1.0f;
            return;
        }
        m3gRootBaseAlphaFactor = m3gRoot.getAlphaFactor();
        captureM3GState(m3gRoot);
    }

    private void captureM3GState(javax.microedition.m3g.Node node) {
        if (node instanceof javax.microedition.m3g.Mesh) {
            javax.microedition.m3g.Mesh mesh = (javax.microedition.m3g.Mesh) node;
            for (int i = 0; i < mesh.getSubmeshCount(); i++) {
                javax.microedition.m3g.Appearance appearance = mesh.getAppearance(i);
                if (appearance == null) {
                    appearance = new javax.microedition.m3g.Appearance();
                    mesh.setAppearance(i, appearance);
                }
                captureAppearance(appearance);
            }
        } else if (node instanceof javax.microedition.m3g.Sprite3D) {
            javax.microedition.m3g.Sprite3D sprite = (javax.microedition.m3g.Sprite3D) node;
            javax.microedition.m3g.Appearance appearance = sprite.getAppearance();
            if (appearance == null) {
                appearance = new javax.microedition.m3g.Appearance();
                sprite.setAppearance(appearance);
            }
            captureAppearance(appearance);
        }

        if (node instanceof javax.microedition.m3g.Group) {
            javax.microedition.m3g.Group group = (javax.microedition.m3g.Group) node;
            for (int i = 0; i < group.getChildCount(); i++) {
                captureM3GState(group.getChild(i));
            }
        }
    }

    private void captureAppearance(javax.microedition.m3g.Appearance appearance) {
        if (findAppearanceState(appearance) != null) return;
        m3gAppearanceStates.addElement(new AppearanceState(appearance));
    }

    private AppearanceState findAppearanceState(javax.microedition.m3g.Appearance appearance) {
        for (int i = 0; i < m3gAppearanceStates.size(); i++) {
            AppearanceState state = (AppearanceState) m3gAppearanceStates.elementAt(i);
            if (state.appearance == appearance) {
                return state;
            }
        }
        return null;
    }

    private void applyAppearanceState(AppearanceState state) {
        javax.microedition.m3g.Appearance appearance = state.appearance;

        javax.microedition.m3g.CompositingMode cm = restoreCompositingMode(state);
        if (blendModeSet) {
            int drawBlend = getRuntimeBlendMode();
            int baseBlend = state.getOriginalBlendMode();
            if (drawBlend != javax.microedition.m3g.CompositingMode.REPLACE || !isTransparentRuntimeBlend(baseBlend)) {
                if (cm == null) {
                    cm = new javax.microedition.m3g.CompositingMode();
                }
                setRuntimeBlendValue(cm, drawBlend);
                cm.setDepthTestEnable(true);
                cm.setColorWriteEnable(true);
                if (drawBlend == javax.microedition.m3g.CompositingMode.REPLACE) {
                    cm.setDepthWriteEnable(true);
                    cm.setAlphaWriteEnable(true);
                } else {
                    cm.setDepthWriteEnable(false);
                    cm.setAlphaWriteEnable(false);
                }
                appearance.setCompositingMode(cm);
            }
        }

        javax.microedition.m3g.PolygonMode pm = restorePolygonMode(state);
        if (perspectiveCorrectionSet) {
            if (pm == null) {
                pm = new javax.microedition.m3g.PolygonMode();
            }
            pm.setPerspectiveCorrectionEnable(perspectiveCorrectionEnabled);
            appearance.setPolygonMode(pm);
        }
    }

    private javax.microedition.m3g.CompositingMode restoreCompositingMode(AppearanceState state) {
        if (!state.hasCompositingMode) {
            state.appearance.setCompositingMode(null);
            return null;
        }
        javax.microedition.m3g.CompositingMode cm = state.appearance.getCompositingMode();
        if (cm == null) {
            cm = new javax.microedition.m3g.CompositingMode();
        }
        setRuntimeBlendValue(cm, state.blending);
        cm.setAlphaThreshold(state.alphaThreshold);
        cm.setDepthOffset(state.depthOffsetFactor, state.depthOffsetUnits);
        cm.setDepthTestEnable(state.depthTestEnable);
        cm.setDepthWriteEnable(state.depthWriteEnable);
        cm.setColorWriteEnable(state.colorWriteEnable);
        cm.setAlphaWriteEnable(state.alphaWriteEnable);
        state.appearance.setCompositingMode(cm);
        return cm;
    }

    private javax.microedition.m3g.PolygonMode restorePolygonMode(AppearanceState state) {
        if (!state.hasPolygonMode) {
            state.appearance.setPolygonMode(null);
            return null;
        }
        javax.microedition.m3g.PolygonMode pm = state.appearance.getPolygonMode();
        if (pm == null) {
            pm = new javax.microedition.m3g.PolygonMode();
        }
        pm.setCulling(state.culling);
        pm.setShading(state.shading);
        pm.setWinding(state.winding);
        pm.setTwoSidedLightingEnable(state.twoSidedLighting);
        pm.setLocalCameraLightingEnable(state.localCameraLighting);
        pm.setPerspectiveCorrectionEnable(state.perspectiveCorrection);
        state.appearance.setPolygonMode(pm);
        return pm;
    }

    private static final class AppearanceState {
        private final javax.microedition.m3g.Appearance appearance;
        private final boolean hasCompositingMode;
        private final int blending;
        private final float alphaThreshold;
        private final float depthOffsetFactor;
        private final float depthOffsetUnits;
        private final boolean depthTestEnable;
        private final boolean depthWriteEnable;
        private final boolean colorWriteEnable;
        private final boolean alphaWriteEnable;
        private final boolean hasPolygonMode;
        private final int culling;
        private final int shading;
        private final int winding;
        private final boolean twoSidedLighting;
        private final boolean localCameraLighting;
        private final boolean perspectiveCorrection;

        private AppearanceState(javax.microedition.m3g.Appearance appearance) {
            this.appearance = appearance;

            javax.microedition.m3g.CompositingMode cm = appearance.getCompositingMode();
            if (cm != null) {
                this.hasCompositingMode = true;
                this.blending = cm.getBlending();
                this.alphaThreshold = cm.getAlphaThreshold();
                this.depthOffsetFactor = cm.getDepthOffsetFactor();
                this.depthOffsetUnits = cm.getDepthOffsetUnits();
                this.depthTestEnable = cm.isDepthTestEnabled();
                this.depthWriteEnable = cm.isDepthWriteEnabled();
                this.colorWriteEnable = cm.isColorWriteEnabled();
                this.alphaWriteEnable = cm.isAlphaWriteEnabled();
            } else {
                this.hasCompositingMode = false;
                this.blending = javax.microedition.m3g.CompositingMode.REPLACE;
                this.alphaThreshold = 0.0f;
                this.depthOffsetFactor = 0.0f;
                this.depthOffsetUnits = 0.0f;
                this.depthTestEnable = true;
                this.depthWriteEnable = true;
                this.colorWriteEnable = true;
                this.alphaWriteEnable = true;
            }

            javax.microedition.m3g.PolygonMode pm = appearance.getPolygonMode();
            if (pm != null) {
                this.hasPolygonMode = true;
                this.culling = pm.getCulling();
                this.shading = pm.getShading();
                this.winding = pm.getWinding();
                this.twoSidedLighting = pm.isTwoSidedLightingEnabled();
                this.localCameraLighting = pm.isLocalCameraLightingEnabled();
                this.perspectiveCorrection = pm.isPerspectiveCorrectionEnabled();
            } else {
                this.hasPolygonMode = false;
                this.culling = javax.microedition.m3g.PolygonMode.CULL_BACK;
                this.shading = javax.microedition.m3g.PolygonMode.SHADE_SMOOTH;
                this.winding = javax.microedition.m3g.PolygonMode.WINDING_CCW;
                this.twoSidedLighting = false;
                this.localCameraLighting = false;
                this.perspectiveCorrection = false;
            }
        }

        private int getOriginalBlendMode() {
            return blending;
        }
    }
}

