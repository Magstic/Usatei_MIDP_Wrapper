package com.nttdocomo.ui.graphics3d;

import com.nttdocomo.ui.util3d.Transform;

public interface Graphics3D {
    void setClipRectFor3D(int x, int y, int width, int height);
    void setParallelView(int width, int height);
    void setPerspectiveView(float zNear, float zFar, float angle);
    void setPerspectiveView(float zNear, float zFar, int width, int height);
    void setTransform(Transform t);
    void addLight(Light light, Transform transform);
    void resetLights();
    void renderObject3D(DrawableObject3D obj, Transform transform);
    void flushBuffer();
    void setClearColor(int argb);
    void setFog(Object fog);
}

