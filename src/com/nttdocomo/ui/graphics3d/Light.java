package com.nttdocomo.ui.graphics3d;

import com.nttdocomo.ui.util3d.Vector3D;

// Light source for 3D scene (DoJa API shim over M3G Light)
public class Light extends Object3D {

    // DoJa light mode constants
    public static final int AMBIENT     = 128;
    public static final int DIRECTIONAL = 129;
    public static final int OMNI        = 130;
    public static final int SPOT        = 131;

    private int mode;
    private float intensity;
    private int color;
    private Vector3D position;
    private Vector3D direction;
    private float spotAngle;
    private float spotExponent;
    private float attenuationConstant;
    private float attenuationLinear;
    private float attenuationQuadratic;

    // Backing M3G Light node
    private javax.microedition.m3g.Light m3gLight;

    public Light() {
        super(TYPE_LIGHT);
        this.mode = AMBIENT;
        this.intensity = 1.0f;
        this.color = 0x00FFFFFF;
        this.position = new Vector3D();
        this.direction = new Vector3D(0, 0, -1);
        this.spotAngle = 45.0f;
        this.spotExponent = 0.0f;
        this.attenuationConstant = 1.0f;
        this.attenuationLinear = 0.0f;
        this.attenuationQuadratic = 0.0f;
        this.m3gLight = new javax.microedition.m3g.Light();
        syncToM3G();
    }

    public void setMode(int mode) {
        this.mode = mode;
        syncToM3G();
    }

    public int getMode() {
        return mode;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
        syncToM3G();
    }

    public float getIntensity() {
        return intensity;
    }

    public void setColor(int rgb) {
        this.color = rgb;
        syncToM3G();
    }

    public int getColor() {
        return color;
    }

    public void setPosition(Vector3D v) {
        this.position.set(v);
    }

    public Vector3D getPosition() {
        return position;
    }

    public void setVector(Vector3D v) {
        this.direction.set(v);
    }

    public Vector3D getVector() {
        return direction;
    }

    public void setSpotAngle(float angle) {
        this.spotAngle = angle;
        syncToM3G();
    }

    public void setSpotExponent(float exponent) {
        this.spotExponent = exponent;
        syncToM3G();
    }

    public void setAttenuation(float constant, float linear, float quadratic) {
        this.attenuationConstant = constant;
        this.attenuationLinear = linear;
        this.attenuationQuadratic = quadratic;
        syncToM3G();
    }

    public static int getMaxLights() {
        return 8;
    }

    // Sync DoJa light state to M3G Light node
    private void syncToM3G() {
        int m3gMode;
        switch (mode) {
            case DIRECTIONAL: m3gMode = javax.microedition.m3g.Light.DIRECTIONAL; break;
            case OMNI:        m3gMode = javax.microedition.m3g.Light.OMNI; break;
            case SPOT:        m3gMode = javax.microedition.m3g.Light.SPOT; break;
            default:          m3gMode = javax.microedition.m3g.Light.AMBIENT; break;
        }
        m3gLight.setMode(m3gMode);
        m3gLight.setIntensity(intensity);
        m3gLight.setColor(color);

        if (m3gMode == javax.microedition.m3g.Light.SPOT) {
            m3gLight.setSpotAngle(spotAngle);
            m3gLight.setSpotExponent(spotExponent);
        }
        if (m3gMode == javax.microedition.m3g.Light.OMNI ||
            m3gMode == javax.microedition.m3g.Light.SPOT) {
            m3gLight.setAttenuation(attenuationConstant, attenuationLinear, attenuationQuadratic);
        }
    }

    public javax.microedition.m3g.Light getM3GLight() {
        syncToM3G();
        return m3gLight;
    }
}

