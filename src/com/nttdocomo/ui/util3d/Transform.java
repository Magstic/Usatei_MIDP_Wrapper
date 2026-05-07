package com.nttdocomo.ui.util3d;

// 4x4 transformation matrix (DoJa API shim, delegates to M3G Transform)
public class Transform {

    // Internal M3G transform
    private javax.microedition.m3g.Transform m3gTransform;

    public Transform() {
        m3gTransform = new javax.microedition.m3g.Transform();
    }

    public Transform(Transform transform) {
        m3gTransform = new javax.microedition.m3g.Transform(transform.m3gTransform);
    }

    // Access to underlying M3G Transform for rendering bridge
    public javax.microedition.m3g.Transform getM3GTransform() {
        return m3gTransform;
    }

    public void setIdentity() {
        m3gTransform.setIdentity();
    }

    public void set(Transform transform) {
        m3gTransform.set(transform.m3gTransform);
    }

    // Row-major 16-element float array
    public void set(float[] matrix) {
        m3gTransform.set(matrix);
    }

    public void get(float[] matrix) {
        m3gTransform.get(matrix);
    }

    // DoJa-specific: set/get single element by index
    public void set(int index, float value) {
        float[] m = new float[16];
        m3gTransform.get(m);
        m[index] = value;
        m3gTransform.set(m);
    }

    public float get(int index) {
        float[] m = new float[16];
        m3gTransform.get(m);
        return m[index];
    }

    public void invert() {
        m3gTransform.invert();
    }

    public void transpose() {
        m3gTransform.transpose();
    }

    // DoJa: this = this * transform (same semantics as M3G postMultiply)
    public void multiply(Transform transform) {
        m3gTransform.postMultiply(transform.m3gTransform);
    }

    // DoJa: rotate(x,y,z,angle) where angle is in degrees
    // M3G: postRotate(angle,ax,ay,az) - note parameter order difference
    public void rotate(float x, float y, float z, float angle) {
        m3gTransform.postRotate(angle, x, y, z);
    }

    public void rotate(Vector3D v, float angle) {
        m3gTransform.postRotate(angle, v.getX(), v.getY(), v.getZ());
    }

    // DoJa: rotateQuat(x,y,z,w) - quaternion rotation
    // M3G: postRotateQuat(qx,qy,qz,qw)
    public void rotateQuat(float x, float y, float z, float w) {
        m3gTransform.postRotateQuat(x, y, z, w);
    }

    public void rotateQuat(Vector3D v, float w) {
        m3gTransform.postRotateQuat(v.getX(), v.getY(), v.getZ(), w);
    }

    // DoJa: scale(x,y,z) -> M3G: postScale(x,y,z)
    public void scale(float x, float y, float z) {
        m3gTransform.postScale(x, y, z);
    }

    public void scale(Vector3D v) {
        m3gTransform.postScale(v.getX(), v.getY(), v.getZ());
    }

    // DoJa: translate(x,y,z) -> M3G: postTranslate(x,y,z)
    public void translate(float x, float y, float z) {
        m3gTransform.postTranslate(x, y, z);
    }

    public void translate(Vector3D v) {
        m3gTransform.postTranslate(v.getX(), v.getY(), v.getZ());
    }

    // DoJa-specific: build view matrix from eye position, look-at target, up vector
    public void lookAt(Vector3D position, Vector3D look, Vector3D up) {
        float px = position.getX(), py = position.getY(), pz = position.getZ();
        float lx = look.getX(), ly = look.getY(), lz = look.getZ();
        float ux = up.getX(), uy = up.getY(), uz = up.getZ();

        // Forward: normalize(look - position)
        float fx = lx - px, fy = ly - py, fz = lz - pz;
        float fLen = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fLen != 0.0f) { fx /= fLen; fy /= fLen; fz /= fLen; }

        // Right-handed DoJa eye space: right x up == forward.
        float rx = uy * fz - uz * fy;
        float ry = uz * fx - ux * fz;
        float rz = ux * fy - uy * fx;
        float rLen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen != 0.0f) { rx /= rLen; ry /= rLen; rz /= rLen; }

        // Recalculate up: forward x right.
        float nux = fy * rz - fz * ry;
        float nuy = fz * rx - fx * rz;
        float nuz = fx * ry - fy * rx;

        // Build DoJa eye-coordinate matrix (row-major, +Z is forward)
        float[] m = new float[] {
             rx,  ry,  rz, -(rx * px + ry * py + rz * pz),
            nux, nuy, nuz, -(nux * px + nuy * py + nuz * pz),
             fx,  fy,  fz, -(fx * px + fy * py + fz * pz),
            0.0f, 0.0f, 0.0f, 1.0f
        };
        m3gTransform.set(m);
    }

    // DoJa-specific: transform a point by this matrix, result = M * v
    public void transVector(Vector3D v, Vector3D result) {
        float[] m = new float[16];
        m3gTransform.get(m);
        float vx = v.getX(), vy = v.getY(), vz = v.getZ();
        float rx = m[0] * vx + m[1] * vy + m[2] * vz + m[3];
        float ry = m[4] * vx + m[5] * vy + m[6] * vz + m[7];
        float rz = m[8] * vx + m[9] * vy + m[10] * vz + m[11];
        result.set(rx, ry, rz);
    }
}

