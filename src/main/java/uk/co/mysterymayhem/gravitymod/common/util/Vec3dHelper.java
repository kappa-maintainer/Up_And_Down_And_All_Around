package uk.co.mysterymayhem.gravitymod.common.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Created by Mysteryem on 2016-10-01.
 */
public class Vec3dHelper {
    private static final double PI_OVER_180 = Math.PI / 180d;
    private static final double ONE_HUNDRED_EIGHTY_OVER_PI = 180d / Math.PI;
    private static final float ONE_HUNDRED_EIGHTY_OVER_PI_FLOAT = (float)(180d / Math.PI);
    public static final int PITCH = 0;
    public static final int YAW = 1;
    private static final double FRAC_BIAS = MathHelper.FRAC_BIAS;
    private static final double[] ASINE_TAB = MathHelper.ASINE_TAB;

    public static double getAbsolutePitchPrecise(Vec3d lookVec) {
        return -(Math.asin(lookVec.y) * (180D / Math.PI));
    }

    public static double getAbsoluteYawPrecise(Vec3d lookVec) {
        return (Math.atan2(-lookVec.x, lookVec.z) * (180D / Math.PI));
    }

    public static double[] getFastPitchAndYawFromVector(Vec3d xyz) {
        double pitch = -fastASin(xyz.y) * ONE_HUNDRED_EIGHTY_OVER_PI;
        double yaw = MathHelper.atan2(-xyz.x, xyz.z) * ONE_HUNDRED_EIGHTY_OVER_PI;
        return new double[]{pitch, yaw};
    }

    private static double fastASin(double input) {
        boolean negateResult = false;
        if (input < 0.0d) {
            input = -input;
            negateResult = true;
        }
        if (Double.isNaN(input) || input > 1d) {
            return Double.NaN;
        }

        double d2 = FRAC_BIAS + input;
        int i = (int)Double.doubleToRawLongBits(d2);
        return negateResult ? -ASINE_TAB[i] : ASINE_TAB[i];
    }

    public static Vec3d getFastUpwardsVector(float pitch, float yaw) {
        return getFastVectorForRotation(pitch + 90f, yaw);
    }

    public static Vec3d getFastVectorForRotation(float pitch, float yaw) {
        return Vec3d.fromPitchYaw(pitch, yaw);
    }

    public static float getPitch(Vec3d lookVec) {
        return (float)-(Math.asin(lookVec.y) * (180D / Math.PI));
    }

    public static double[] getPrecisePitchAndYawFromVector(Vec3d xyz) {
        double pitch = -(Math.asin(xyz.y) * ONE_HUNDRED_EIGHTY_OVER_PI);
        double yaw = Math.atan2(-xyz.x, xyz.z) * ONE_HUNDRED_EIGHTY_OVER_PI;
        return new double[]{pitch, yaw};
    }

    public static Vec3d getPreciseVectorForRotation(double pitch, double yaw) {
        double f = Math.cos(-yaw * PI_OVER_180 - Math.PI);
        double f1 = Math.sin(-yaw * PI_OVER_180 - Math.PI);
        double f2 = -Math.cos(-pitch * PI_OVER_180);
        double f3 = Math.sin(-pitch * PI_OVER_180);
        return new Vec3d((f1 * f2), f3, (f * f2));
    }

    public static float getYaw(Vec3d lookVec) {
        return (float)(Math.atan2(-lookVec.x, lookVec.z) * (180D / Math.PI));
    }
}
