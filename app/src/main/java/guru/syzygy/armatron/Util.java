package guru.syzygy.armatron;

import au.edu.federation.utils.Vec2f;

/**
 * Useful routines
 * Created by discar on 5/28/2017.
 */

public class Util {

    /**
     * Rounds a double value.
     *
     * @param value
     * @param places
     * @return
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * Normalizes an angle so that the degrees are always 0 <= x < 360
     * @param angle
     * @return
     */
    public static double normalize(double angle) {
        if (angle < 0) return angle + 360d;
        if (angle >= 360d) return angle - 360d;
        return angle;

    }

    /**
     * Calculate the slope of a line segment comprised of two points
     *
     * @param v1 - Start point
     * @param v2 - End point
     * @return
     */
    public static float slope(Vec2f v1, Vec2f v2) {

        float dx = v2.x - v1.x;
        float dy = v2.y - v2.y;
        return dy/dx;
    }

    /**
     * Just get the change in X for two points (v1 is start, v2 is end)
     *
     * @param v1   - Start point
     * @param v2    - End point
     * @return
     */
    public static float dx(Vec2f v1, Vec2f v2) {
        if (v1 == null || v2 == null) return 0;
        return v2.x - v1.x;
    }

    /**
     * Just get the change in Y for two points
     *
     * @param v1 - Start point
     * @param v2 - End point
     * @return
     */
    public static float dy(Vec2f v1, Vec2f v2) {
        if (v1 == null || v2 == null) return 0;
        return v2.y - v1.y;
    }

    /**
     * Returns an integer which represents the signum of the given float.
     * -1 is negative
     * 0 is zero
     * 1 is positive
     *
     * @param n
     * @return
     */
    public static int sign(float n) {
        return (int) Math.signum(n);


    }

    /**
     * Convenience function to calculate a unit vector from an angle
     * @param angle
     * @return
     */
    public static Vec2f calcUV(double angle) {
        return Vec2f.getDirectionUV( new Vec2f(0f, 0f), new Vec2f((float) Math.cos(degToR(angle)), (float) Math.sin(degToR(angle))));
        // Vec2f v = new Vec2f((float) Util.round(Math.cos(degToR(angle)),1), (float) Util.round(Math.sin(degToR(angle)),1));
        // return v;
    }

    /**
     * Convert degrees to radians
     *
     * @param angle
     * @return
     */
    public static double degToR(double angle) {
        return round1000(angle/180d*Math.PI);
    }

    /**
     * Convert radians to degrees
     *
     * @param radians
     * @return
     */
    public static double RToDeg(double radians) {
        return round1000(radians*180/Math.PI);
    }

    /**
     * Round a double to the nearest 1000.
     *
     * @param d
     * @return
     */
    public static double round1000(double d) {
        return Math.round(d*1000.0+0.5)/1000.0;
    }

    /**
     * Return the xy coordinate of the given vector
     *
     * @param v
     * @return
     */
    public static String xy(Vec2f v) {
        return v.toString();
    }


}
