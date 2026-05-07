package com.nttdocomo.ui.util3d;

// 3D vector with x,y,z float components (DoJa API shim)
public class Vector3D {

    private float x, y, z;

    public Vector3D() {
        this.x = 0.0f;
        this.y = 0.0f;
        this.z = 0.0f;
    }

    public Vector3D(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector3D(Vector3D v) {
        set(v);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public void setX(float x) {
        requireFinite(x);
        this.x = x;
    }

    public void setY(float y) {
        requireFinite(y);
        this.y = y;
    }

    public void setZ(float z) {
        requireFinite(z);
        this.z = z;
    }

    public void set(float x, float y, float z) {
        requireFinite(x);
        requireFinite(y);
        requireFinite(z);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(Vector3D v) {
        if (v == null) {
            throw new NullPointerException();
        }
        set(v.x, v.y, v.z);
    }

    public void add(float x, float y, float z) {
        set(FastMath.add(this.x, x), FastMath.add(this.y, y), FastMath.add(this.z, z));
    }

    public void add(Vector3D v) {
        if (v == null) {
            throw new NullPointerException();
        }
        add(v.x, v.y, v.z);
    }

    // Normalize this vector to unit length
    public void normalize() {
        int ix = FastMath.floatToInnerInt(x);
        int iy = FastMath.floatToInnerInt(y);
        int iz = FastMath.floatToInnerInt(z);
        if (ix == 0 && iy == 0 && iz == 0) {
            throw new ArithmeticException();
        }

        long lengthSquared = (long) ix * (long) ix + (long) iy * (long) iy + (long) iz * (long) iz;
        int length = roundDouble(Math.sqrt(lengthSquared));
        if (length == 0) {
            throw new ArithmeticException();
        }

        set(
            FastMath.innerIntToFloat((int) ((((long) ix) << 12) / length)),
            FastMath.innerIntToFloat((int) ((((long) iy) << 12) / length)),
            FastMath.innerIntToFloat((int) ((((long) iz) << 12) / length)));
    }

    // Dot product: this . v
    public float dot(Vector3D v) {
        return dot(this, v);
    }

    // Static dot product
    public static float dot(Vector3D v1, Vector3D v2) {
        if (v1 == null || v2 == null) {
            throw new NullPointerException();
        }
        return FastMath.add(
            FastMath.add(FastMath.mul(v1.x, v2.x), FastMath.mul(v1.y, v2.y)),
            FastMath.mul(v1.z, v2.z));
    }

    // Cross product: this = this x v
    public void cross(Vector3D v) {
        cross(this, v);
    }

    // Cross product: this = u x v
    public void cross(Vector3D u, Vector3D v) {
        if (u == null || v == null) {
            throw new NullPointerException();
        }
        float ux = u.x;
        float uy = u.y;
        float uz = u.z;
        float vx = v.x;
        float vy = v.y;
        float vz = v.z;
        set(
            FastMath.sub(FastMath.mul(uy, vz), FastMath.mul(uz, vy)),
            FastMath.sub(FastMath.mul(uz, vx), FastMath.mul(ux, vz)),
            FastMath.sub(FastMath.mul(ux, vy), FastMath.mul(uy, vx)));
    }

    private static void requireFinite(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException();
        }
    }

    private static int roundDouble(double value) {
        return (value >= 0.0d) ? (int) (value + 0.5d) : (int) (value - 0.5d);
    }
}

