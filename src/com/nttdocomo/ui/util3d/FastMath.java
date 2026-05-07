package com.nttdocomo.ui.util3d;

// Fast approximate math operations in degrees (DoJa API shim)
public class FastMath {

    private static final int INNER_ONE = 4096;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);
    private static final float DEG_TO_INTERNAL_ANGLE = 2048.0f / 180.0f;

    public static float abs(float x) {
        requireFinite(x);
        return (x < 0) ? -x : x;
    }

    // Trigonometric functions - angle in degrees
    public static float sin(float a) {
        requireFinite(a);
        return innerIntToFloat(sinInner(toInternalAngle(a)));
    }

    public static float cos(float a) {
        requireFinite(a);
        return innerIntToFloat(cosInner(toInternalAngle(a)));
    }

    public static float tan(float a) {
        requireFinite(a);
        if (isTangentSingularity(a)) {
            return Math.sin(a * DEG_TO_RAD) >= 0.0d
                    ? Float.POSITIVE_INFINITY
                    : Float.NEGATIVE_INFINITY;
        }
        int sine = sinInner(toInternalAngle(a));
        int cosine = cosInner(toInternalAngle(a));
        if (cosine == 0) {
            return (float) Math.tan(a * DEG_TO_RAD);
        }
        return innerIntToFloat((int) ((((long) sine) << 12) / cosine));
    }

    // Inverse trig - returns degrees (CLDC has no Math.atan/asin/acos)
    public static float asin(float x) {
        requireFinite(x);
        if (x < -1.0f || x > 1.0f) {
            throw new ArithmeticException();
        }
        if (x == 1.0f) {
            return 90.0f;
        }
        if (x == -1.0f) {
            return -90.0f;
        }
        return atanImpl(x / (float) Math.sqrt(1.0 - x * x)) * RAD_TO_DEG;
    }

    public static float acos(float x) {
        requireFinite(x);
        if (x < -1.0f || x > 1.0f) {
            throw new ArithmeticException();
        }
        return 90.0f - asin(x);
    }

    public static float atan(float x) {
        requireFinite(x);
        return atanImpl(x) * RAD_TO_DEG;
    }

    public static float atan2(float x, float y) {
        requireFinite(x, y);
        if (x == 0.0f && y == 0.0f) {
            return Float.NaN;
        }
        if (x == 0.0f) {
            return 90.0f;
        }
        if (y == 0.0f) {
            return 0.0f;
        }

        float angle = atanImpl(y / x) * RAD_TO_DEG;
        return (angle < 0.0f) ? angle + 180.0f : angle;
    }

    private static final float PI_F = (float) Math.PI;

    // atan(x) in radians - Corrected Pade approximant for |x|<=1, identity for |x|>1
    private static float atanImpl(float x) {
        if (x < -1.0f || x > 1.0f) {
            float r = PI_F / 2.0f - atanImpl(1.0f / ((x < 0) ? -x : x));
            return (x < 0) ? -r : r;
        }
        float x2 = x * x;
        return x * (1.0f + 0.28125f * x2) / (1.0f + 0.5625f * x2);
    }

    // Basic arithmetic (DoJa provides these for float consistency)
    public static float add(float a, float b) {
        requireFinite(a, b);
        return innerIntToFloat(floatToInnerInt(a) + floatToInnerInt(b));
    }

    public static float sub(float a, float b) {
        return add(a, -b);
    }

    public static float mul(float a, float b) {
        requireFinite(a, b);
        return innerIntToFloat(mulInner(floatToInnerInt(a), floatToInnerInt(b)));
    }

    public static float div(float a, float b) {
        requireFinite(a, b);
        int divisor = floatToInnerInt(b);
        if (divisor == 0) {
            throw new ArithmeticException();
        }
        return innerIntToFloat((int) ((((long) floatToInnerInt(a)) << 12) / divisor));
    }

    public static float sqrt(float x) {
        requireFinite(x);
        if (x < 0.0f) {
            throw new ArithmeticException();
        }
        int inner = floatToInnerInt(x);
        if (inner < 0) {
            throw new ArithmeticException();
        }
        return innerIntToFloat(roundDouble(Math.sqrt((long) inner * (long) INNER_ONE)));
    }

    // Fixed-point conversion (DoJa internal representation)
    public static int floatToInnerInt(float f) {
        requireFinite(f);
        return roundFloat(f * INNER_ONE);
    }

    public static float innerIntToFloat(int i) {
        return i / (float) INNER_ONE;
    }

    private static void requireFinite(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException();
        }
    }

    private static void requireFinite(float a, float b) {
        requireFinite(a);
        requireFinite(b);
    }

    private static int mulInner(int a, int b) {
        return (int) ((((long) a) * ((long) b) + 2048L) >> 12);
    }

    private static int toInternalAngle(float degrees) {
        return roundFloat(degrees * DEG_TO_INTERNAL_ANGLE);
    }

    private static int sinInner(int angle) {
        return roundDouble(Math.sin(angle * Math.PI / 2048.0) * INNER_ONE);
    }

    private static int cosInner(int angle) {
        return sinInner(angle + 1024);
    }

    private static boolean isTangentSingularity(float a) {
        float r = a % 180.0f;
        return r == 90.0f || r == -90.0f;
    }

    private static int roundFloat(float value) {
        return (value >= 0.0f) ? (int) (value + 0.5f) : (int) (value - 0.5f);
    }

    private static int roundDouble(double value) {
        return (value >= 0.0d) ? (int) (value + 0.5d) : (int) (value - 0.5d);
    }
}

