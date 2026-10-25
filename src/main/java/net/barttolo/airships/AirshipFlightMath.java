package net.barttolo.airships;

/** No Forge dependencies: tested with standard JDK 17 for safe flight arithmetic. */
public final class AirshipFlightMath {
    private AirshipFlightMath() {}
    public static double heading(double yaw, int turn) {
        if (turn < -1 || turn > 1) throw new IllegalArgumentException("turn must be -1..1");
        double angle = yaw + turn * 1.7;
        angle = ((angle + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return angle;
    }
    public static double dx(double yaw, int forward, double speed) {
        return -Math.sin(Math.toRadians(yaw)) * clamp(forward) * speed;
    }
    public static double dz(double yaw, int forward, double speed) {
        return Math.cos(Math.toRadians(yaw)) * clamp(forward) * speed;
    }
    public static double dy(int vertical) {return clamp(vertical) * 0.18;}
    private static int clamp(int x) {return Math.max(-1,Math.min(1,x));}
}
