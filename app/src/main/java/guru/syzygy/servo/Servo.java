package guru.syzygy.servo;

/**
 * Created by discar on 5/15/2017.
 *
 * Represents an individual servo.   The Servo is assumed to be attached to an SSC-32 board
 *
 */

@SuppressWarnings("All")
public class Servo {
    String name = "";
    int ssc32_port = 0;             // SSC-32 channel number
    double angle_min = -45.0;       // Smallest angle servo can handle
    int servo_min_value = 500;      // Equivalent smallest pulse width servo can handle
    double angle_max = 45.0;        // Largest angle that servo can handle
    int servo_max_value = 2500;     // Largest pulse width
    int currentValue = 1536;

    /**
     * Constructor
     *
     * @param name - name of the servo
     * @param ssc32_port - Which port on the SSC-32 it is plugged into
     * @param servo_min_value - Minimum PW of the servo
     * @param servo_max_value - Maximum PW of the servo
     * @param angle_min - What angle the servo is at if the PW is at its minimum
     * @param angle_max - What angle the servo is at if the PW is at its maximum
     * @param rotation  - The initial rotation of the servo (e.g. what angle the servo is at)
     */
    public Servo(String name, int ssc32_port, int servo_min_value, int servo_max_value, double angle_min, double angle_max, double rotation) {
        this.name = name;
        this.ssc32_port = ssc32_port;
        this.angle_min = angle_min;
        this.angle_max = angle_max;
        this.servo_min_value = servo_min_value;
        this.servo_max_value = servo_max_value;
        this.currentValue = angleToPW(rotation);
    }

    /**
     *  Move the servo to the specified angle.  We know what our rotation is, so we
     *  adjust for it.
     *
     * @param realworldAngle - Angle relative to the real world
     * @return
     */
    public String moveToAngle(double realworldAngle) {

        System.out.println(name+": moveToAngle("+realworldAngle+") ");
        if (!between(realworldAngle) && between(realworldAngle + 360d)) realworldAngle += 360d;
        if (!between(realworldAngle) && between(realworldAngle - 360d)) realworldAngle = realworldAngle - 360d;

        // realworldAngle += rotation;

        if (realworldAngle < 0) realworldAngle = realworldAngle + 360d;
        if (realworldAngle > 360d) realworldAngle = realworldAngle - 360d;

        double internalAngle = realworldAngle;
        // if (internalAngle > getServoMaxAngle()) internalAngle = getServoMaxAngle();

        int proposedValue = angleToPW(internalAngle);

        // if (proposedValue < servo_min_value || proposedValue > servo_max_value) throw new RuntimeException ("Move is out of bounds (real world angle "+realworldAngle+" & internal Angle "+internalAngle+") results in PW = "+proposedValue);


        System.out.println("--> Angle for "+name+" to be set to "+realworldAngle+" = "+servo_min_value+" < "+proposedValue+" < "+servo_max_value);
        if (proposedValue < servo_min_value ) proposedValue = servo_min_value;
        if (proposedValue > servo_max_value) proposedValue = servo_max_value;

        System.out.println("--> Adjusted "+name+" Angle: "+realworldAngle+" = "+servo_min_value+" < "+proposedValue+" < "+servo_max_value);

        currentValue = proposedValue;
        return getSSCCommand();
    }

    /**
     * Check if someAngle is between the servo min and max angles.  Since the servo could be inverted
     * (e.g. the min_angle is larger than the max_angle), we have to check this delicately.
     *
     *
     * @param someAngle
     * @return
     */
    boolean between(double someAngle) {
        double ll = getServoMinAngle();
        double ul = getServoMaxAngle();
        if (ll > ul) {
            ll = getServoMaxAngle();
            ul = getServoMinAngle();
        }
        if (ll <= someAngle && someAngle >= ul) return true;
        return false;
    }

    /**
     * Convert the given angle to the related PW value.
     *
     * @param newAngle
     * @return
     */
    public int angleToPW(double internalAngle) {

        double proposedAngle = internalAngle;

        double proportion = (proposedAngle - angle_min) / (angle_max - angle_min);

        int proposedValue = (int) ((double) (proportion * (double) (servo_max_value - servo_min_value)) + (double) servo_min_value);

        System.out.println(name+"::  angleToPW("+internalAngle+") anglemin = "+angle_min+" anglemax = "+angle_max+" proportion = "+proportion + " * "+(servo_max_value - servo_min_value) + " + "+servo_min_value + "= "+proposedValue);

        return proposedValue;
    }

    /**
     *  Get the current angle.  This angle  is the angle of the servo based purely on its PWM setting.  If the servo has been
     *  rotated by other servos, the caller should adjust the returned angle for it.
     *
     * @return
     */
    public double getAngle() {
        double proportion = ((double) currentValue - (double) servo_min_value) / ((double) servo_max_value - (double) servo_min_value);
        double internalAngle = (angle_max - angle_min) * proportion + angle_min;
        return internalAngle;
    }

    /**
     * Set the current PWM value of this servo... make sure that the value is within
     * the servo's constraints.
     *
     * @param currentValue
     */
    public void setCurrentValue(int currentValue) {
        if (currentValue < getServoMinValue()) currentValue = (int) getServoMinValue();
        if (currentValue > getServoMaxValue()) currentValue = (int) getServoMaxValue();
        this.currentValue = currentValue;
    }

    /**
     * Get the current PWM value
     *
     * @return
     */
    public int getCurrentValue() {
        return currentValue;
    }

    /**
     * @return
     */
    public double getServoMinValue() {
        return servo_min_value;
    }

    /**
     * Name of the servo
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Maximum PWM of this servo
     * @return
     */
    public double getServoMaxValue() {
        return servo_max_value;
    }

    /**
     * Leftmost angle of the servo
     * @return
     */
    public double getServoMinAngle() {
        return angle_min;
    }

    /**
     * Rightmost angle of the servo
     * @return
     */
    public double getServoMaxAngle() {
        return angle_max;
    }

    /**
     * Get the SSC command for this servo.
     *
     * @return
     */
    public String getSSCCommand() {

        String command = "#"+ssc32_port+" P"+(int) currentValue;
        // System.out.println("-> "+command);
        return command;
    }


    /**
     * Returns the angle of the servo when it is exactly between PWM min and PWM ax.
     * @return
     */
    public double servoZero() {
        return (angle_min + angle_max)/2.0d;
    }


}
