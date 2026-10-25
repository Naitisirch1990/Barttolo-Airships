package net.barttolo.airships;

/** Conservative voxel-model bounds, with a 3-block buffer. No Forge dependency. */
public final class AirshipSafetyMath {
    private AirshipSafetyMath() {}
    public static final int HALF_LENGTH=27;
    public static final int HALF_WIDTH=17;
    public static final int BUFFER=3;
    public static int extentX(double yaw) {
        double r=Math.toRadians(yaw);
        return (int)Math.ceil(Math.abs(Math.cos(r))*HALF_LENGTH+Math.abs(Math.sin(r))*HALF_WIDTH-1.0e-9)+BUFFER;
    }
    public static int extentZ(double yaw) {
        double r=Math.toRadians(yaw);
        return (int)Math.ceil(Math.abs(Math.sin(r))*HALF_LENGTH+Math.abs(Math.cos(r))*HALF_WIDTH-1.0e-9)+BUFFER;
    }
    public static int[] samples(int min,int max) {
        if (min>max) throw new IllegalArgumentException("min > max");
        return new int[]{min,min+(max-min)/4,min+(max-min)/2,min+3*(max-min)/4,max};
    }
    public static boolean isValidAltitude(double shipY,int minBuildHeight,int maxBuildHeight) {
        return shipY-10>=minBuildHeight && shipY+24<maxBuildHeight;
    }
    public static boolean hasClearance(int height,double shipY) {
        return shipY-10>height+2; // keel stays at least three blocks above sampled terrain
    }
}
