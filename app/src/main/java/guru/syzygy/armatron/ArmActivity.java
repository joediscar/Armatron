package guru.syzygy.armatron;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import au.edu.federation.utils.Vec2f;
import guru.syzygy.servo.Servo;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import static guru.syzygy.armatron.ArmActivity.EFFECT.BASELEFT;
import static guru.syzygy.armatron.ArmActivity.EFFECT.BASERIGHT;
import static guru.syzygy.armatron.ArmActivity.EFFECT.GRIPPERCLOSE;
import static guru.syzygy.armatron.ArmActivity.EFFECT.GRIPPEROPEN;
import static guru.syzygy.armatron.ArmActivity.EFFECT.NOACTION;
import static guru.syzygy.armatron.ArmActivity.EFFECT.WRISTDOWN;
import static guru.syzygy.armatron.ArmActivity.EFFECT.WRISTUP;

/**
 *   The main Activity class for the Armatron project.
 */


@SuppressWarnings("All")
public class ArmActivity extends IOIOActivity {

    static final int MS_TO_MOVE = 500;  // Number of milliseconds to complete a move

    Looper looper;  // Variable to hold the looper object of the IOIO class

    Queue<String> queue = new LinkedBlockingQueue<String>();  // Queue up strings to send to the SSC-32 board

    ArrayList<String> record = new ArrayList<String>();  // Recording of all STEPS taken

    Servo[] servos; // An array to hold servo objects

    // These are the variables to use for the User Interface
    static ImageView imageView;  // The graphical representation of the arm
    SeekBar touchSeekBar;        // The touch bar for controlling the wrist rotation
    LinearLayout chain_panel;    // The area where we can display the angles of the arm
    TextView bar_angle;          // To show current settings of the touch seek bar

    int scrollServoIndex = 0;

    ImageButton button_add;
    ImageButton button_play;
    ImageButton button_delete;
    ImageButton button_wipe;
    ImageButton button_save;
    ImageButton button_load;

    ImageButton button_zero;
    TextView recorded_steps;

    // This hashtable holds the linear layouts for the records in the chain_panel
    Hashtable<String, LinearLayout> boneViews = new Hashtable<String, LinearLayout>();

    // The object that represents the arm.
    static ArmModel arm;

    // The geometry of the screen
    float screenWidth;
    float screenHeight;

    /**
     * Set the screen geometry
     */
    private void whatIsScreen() {
        WindowManager wm = getWindowManager();
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        screenHeight = size.y;
        screenWidth = size.y;
    }

    /**
     * Initialize the servoes to their starting values.
     */
    private void initializeServos() {
        servos = new Servo[6];

        // Name, channel on SSC-32 board, minimum pulsewidth value, maximum pulsewidth value, minimum angle range, maximum angle range, starting angle
        servos[0] = new Servo("base",    0, 553, 2520, 0, 180, 90d);  //  Rotating Base  constraints are normally -98 to 99

        servos[1] = new Servo("bottom",  1, 553, 2520, 0, 180, 100d);  //  Bottom Servo  HS805BB
        servos[2] = new Servo("middle",  2, 553, 2520, 360, 180, 360d);  //  Mid-Servo      HS755HB
        servos[3] = new Servo("wrist",   3, 553, 2520, 0, 180, 0d);  //  Top Servo    HS645MG

        servos[4] = new Servo("gripper", 4, 553, 2520, 0, 180, 0d);  //  Rotator
        servos[5] = new Servo("rotator", 5, 553, 2520, 0, 180, 0d);  //  Gripper


        // Make sure all the non-arm related servos are at the correct angles.
        servos[0].moveToAngle(90);
        servos[3].moveToAngle(90);
        servos[4].moveToAngle(90);
        servos[5].moveToAngle(90);
    }

    /**
     * Create a model of the arm.  We only control two arm bones the "bottom" shoulder and the "middle" elbow.
     */
    private void createArm() {
        arm  = new ArmModel();

        // Create the arm  parameters are name, length (in inches), starting angle, clockwise constraint and counterclockwise restraint.
        addBone(servos[1].getName(), 6.0, 100, 90, 90);    // Bottom Servo can go to 30 degrees
        addBone(servos[2].getName(), 8.0, 0,  135, 0);    // Middle

    }

    /**
     * Put the arm back into the starting position and clear the queue
     */
    private void zero() {
        initializeServos();
        createArm();
        queue.clear();
        if (scrollServoIndex >= 0) {
            Servo baseServo = servos[scrollServoIndex];
            System.out.println("##### Base Servo is "+baseServo.getName()+" at "+baseServo.getCurrentValue());
            updateProgressBar(baseServo);
        }
    }

    private void wipeRecord() {
        record.clear();
        addRecord();
    }

    private void addRecord() {
        record.add(getServoCommands());
        showSteps();
    }


    private void deleteRecord() {
        if (record.size() < 1) return;
        record.remove(record.size()-1);
        showSteps();
    }

    private void showSteps() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recorded_steps.setText(""+record.size()+" steps in queue");
            }
        });

    }

    private void playRecord() {
        for (String command: record) {
            enqueue(command);
            /*
            try {
                Thread.sleep(1500);
            } catch (Exception ex) {

            }
            */
        }
    }



    /**
     * The main Android onCreate method.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arm);
        Prefs.getInstance(this);

        whatIsScreen();
        //
        //
        //
        //
        //Mid-Servo -> O----------O   <--- Top-Servo
        //            /            \   Rotator
        //           /              o<   <--- Gripper
        //         O       <- Bottom Servo
        //    |--------|
        //    |________|   <- Rotating Base
        //
        //
        // The arm (as of May 2017) uses HS-645MG  servos.  Which results in
        //     Max Travel: 197 degrees
        //     PWM signal range 553-2520 usec
        //

        // Find the widgets we use for the UI
        touchSeekBar = (SeekBar) findViewById(R.id.servo_angle);
        chain_panel = (LinearLayout) findViewById(R.id.chain_panel);
        bar_angle = (TextView) findViewById(R.id.bar_angle);

        // Buttons
        button_add = (ImageButton) findViewById(R.id.button_add);
        button_delete = (ImageButton) findViewById(R.id.button_delete);
        button_play = (ImageButton) findViewById(R.id.button_play);
        button_wipe = (ImageButton) findViewById(R.id.button_wipe);
        button_zero = (ImageButton) findViewById(R.id.button_zero);
        button_save = (ImageButton) findViewById(R.id.button_save);
        button_load = (ImageButton) findViewById(R.id.button_load);


        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMacro();
            }
        });

        button_load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMacro();
            }
        });

        View.OnClickListener ocl = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view == button_add) addRecord();
                if (view == button_delete) deleteRecord();
                if (view == button_play) playRecord();
                if (view == button_wipe) wipeRecord();
            }
        };

        button_zero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zero();
                draw();
                enqueue(armToServos());
            }
        });

        button_add.setOnClickListener(ocl);
        button_delete.setOnClickListener(ocl);
        button_play.setOnClickListener(ocl);
        button_wipe.setOnClickListener(ocl);

        recorded_steps = (TextView) findViewById(R.id.recorded_steps);


        // If you touch the chainpanel, you get a toast that tells you what the values mean
        chain_panel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getBaseContext(), "Arm Angle / Servo angle / PWM", Toast.LENGTH_SHORT).show();
            }
        });


        zero();

        // What does the slider control?  Here, we tell it we control servo[5] ... the rotator
        setScrollServo(5);  // Scrollbar will control the rotator

        // Create the panel
        createChainPanel();

        wipeRecord();

    }

    /**
     * Create a bone in the arm based on the current servo settings.
     *
     * @param name
     * @param servo
     * @param length
     */
    LocalBone addBone(String name, double length, double requestedAngle, float constraintCW, float constraintCCW) {
        System.out.println(":: SETTING BONE "+name+" to "+requestedAngle);
        LocalBone b = arm.addBone(name, length, requestedAngle, constraintCW, constraintCCW);
        System.out.println(" SETTING asked for "+requestedAngle+" and got "+b.getAngleDeg());
        return b;
    }


    /**
     * Attach the seekbar to a particular servo
     *
     * @param servoNum
     */
    public void setScrollServo(int servoNum) {
        scrollServoIndex = servoNum;
        Servo baseServo = servos[servoNum];

        final DecimalFormat df = new DecimalFormat("#.0");

        touchSeekBar.setOnSeekBarChangeListener(null);

        touchSeekBar.setMax((int) baseServo.getServoMaxValue() - (int) baseServo.getServoMinValue());
        updateProgressBar(baseServo);


        touchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int oldProgress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // Progress Changed

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (oldProgress != seekBar.getProgress()) {
                    // There was a value change... we have to queue up the command

                    Servo baseServo = servos[scrollServoIndex];
                    int p = seekBar.getProgress() + (int) baseServo.getServoMinValue();
                    baseServo.setCurrentValue(p);
                    // bar_angle.setText(df.format(baseServo.getAngle())+" degrees / "+baseServo.getCurrentValue()+" ... "+baseServo.angleToPW(baseServo.getAngle())+" PWM ");
                    updateProgressBar(baseServo);

                    enqueue(baseServo.getSSCCommand()+" T500");
                    oldProgress = seekBar.getProgress();
                }
            }


        });


    }

    public void updateProgressBar(final Servo servo) {
        final DecimalFormat df = new DecimalFormat("#.0");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                touchSeekBar.setProgress(servo.getCurrentValue() - (int) servo.getServoMinValue());
                bar_angle.setText(servo.getName()+" "+df.format(servo.getAngle())+" degrees / "+servo.getCurrentValue()+" ... "+servo.angleToPW(servo.getAngle())+" PWM ");
            }
        });

    }

    /**
     * Convenience function to redraw the arm inside a UI thread
     */
    void draw() {
        runOnUiThread(new Runnable() {
            public void run() {
                //drawArm();
                drawArmViaVectors();
            }
        });

    }


    /**
     * Create the information panel that shows what the arm model and servos are set for
     */
    void createChainPanel() {
        LayoutInflater inflater = getLayoutInflater();
        for (LocalBone b: arm.getBones()) {
            final int servoIndex = findServoIndex(b.getName());
            LinearLayout aChainView = (LinearLayout) inflater.inflate(R.layout.bone_record, null);
            boneViews.put(b.getName(), aChainView);;
            chain_panel.addView(aChainView);
        }
    }

    /**
     * Update the chainpanel records with the various information on the bones and servos
     */
    void displayBones() {
        DecimalFormat df = new DecimalFormat("0.00");
        for (LocalBone b: arm.getBones()) {
            LinearLayout aChainView = boneViews.get(b.getName());
            Servo servo = findServo(b.getName());
            if (servo == null) continue;
            TextView textName = (TextView) aChainView.findViewById(R.id.boneName);
            TextView boneAngle = (TextView) aChainView.findViewById(R.id.bone_angle);
            TextView servoAngle = (TextView) aChainView.findViewById(R.id.servo_external_angle);
            TextView servoInternal = (TextView) aChainView.findViewById(R.id.servo_internal);

            textName.setText(b.getName());   // Name of the bone/servo
            boneAngle.setText(df.format(b.getAngleDeg())); // What angle the arm model says this bone is at
            servoAngle.setText(df.format(servo.getAngle())); // What angle the servo says this bone is at
            servoInternal.setText(df.format(servo.getCurrentValue())); // And what the PWM value of the servo is at

        }
    }




    /**
     * A method to create our IOIO thread.
     *
     * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
     */
    @Override
    protected IOIOLooper createIOIOLooper() {
        looper = new Looper();
        return looper;
    }

    /**
     * This comes from the IOIO demonstration to show what version the IOIO is at.
     *
     * @param ioio
     * @param title
     */
    private void showVersions(IOIO ioio, String title) {
        toast(String.format("%s\n" +
                        "IOIOLib: %s\n" +
                        "Application firmware: %s\n" +
                        "Bootloader firmware: %s\n" +
                        "Hardware: %s",
                title,
                ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
    }

    /**
     * Convenience function to display the toast.
     *
     * @param message
     */
    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

//

    /**
     * Method that gets called once all the UI geometries are finished.  This
     * is where we find the imageView, get its dimensions etc.  If we had tried
     * to do this in onCreate() there would be a possibility that the imageView
     * hadn't finished initializing.
     *
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        imageView = (ImageView) findViewById(R.id.imageView);

        // Associate a gesturelistener to the imageview.  The GestureListener is called
        // only after the activity has finished assessing a touch... and decides not to handle
        // it by returning "false."
        imageView.setOnTouchListener(new GestureListener(this));

        // Draw the arm
        draw();

        // Make sure the arm geometry is passed into the servos
        String command = armToServos();

        // Queue the command to the servos
        enqueue(command);



    }


    //  The following are variables used exclusively for gripper control.
    Vec2f lastV1 = null;        // The last position for touch #1
    Vec2f lastV2 = null;        // The last position for touch #2
    Vec2f startTouch = null;    // Where the action down touch started.
    float lastDistance = Float.MAX_VALUE;   // The last distance measured

    int sampleCount = 0;        // How many times the current touch was detected

    // The following are the last DX and DY (change in x and change in y) for the two-fingered touch
    int lsdx1 = 0;
    int lsdx2 = 0;
    int lsdy1 = 0;
    int lsdy2 = 0;

    // What effects certain gestures have
    enum EFFECT { NOACTION, WRISTUP, WRISTDOWN, GRIPPEROPEN, GRIPPERCLOSE, ROTATECW, ROTATECCW, BASELEFT, BASERIGHT };

    // The effect result of the last touch
    EFFECT lastEffect;

    /**
     * Handles the tocuh event.  If the event is a one fingered touch, we return false to allow
     * individual widget touch handlers to handle it.  Here, we're only interested in two-finger
     * touches
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        EFFECT effect = NOACTION;
        int numberOfTouches = ev.getPointerCount();

        int action = MotionEventCompat.getActionMasked(ev);

        long now = System.currentTimeMillis();

        // If there is only one finger, then this touch isn't for us
        if (numberOfTouches < 2) {
            return false;
        }

        // Get both points
        Vec2f v1 = new Vec2f( MotionEventCompat.getX(ev, 0), MotionEventCompat.getY(ev, 0));
        Vec2f v2 = new Vec2f (MotionEventCompat.getX(ev,1), MotionEventCompat.getY(ev,1));

        // First touch sets the pivot point
        if (action == MotionEvent.ACTION_DOWN) {
            sampleCount = 0;
            startTouch = v1;
        }

        // The touch on the left should always be the first point
        if (v2.x < v1.x) {
            Vec2f t = v1;
            v1 = v2;
            v2 = t;
        }

        // Calculate distances
        float distanceV1V2 = distance(v1,v2);           // The distance between the touch points
        float deltaDistanceV1 = distance(lastV1, v1);   // The distances between the last touch point and the current one
        float deltaDistanceV2 = distance(lastV2, v2);   // ... same as above for the second touch point

        // Calculate the change in X and Y between the last touches and this one
        float dx1 = Util.dx(lastV1, v1);
        float dx2 = Util.dx(lastV2, v2);
        float dy1 = Util.dy(lastV1, v1);
        float dy2 = Util.dy(lastV2, v2);

        // We use the "d" factor to lower the resolution of the touch.  A factor of 10, for example,
        // makes the touch points a 10x10 pixel area.  The "w" stands for "window"
        int d = 10;
        float wdx1 = ((int) dx1 / d)*d;
        float wdx2 = ((int) dx2 / d)*d;
        float wdy1 = ((int) dy1 / d)*d;
        float wdy2 = ((int) dy2 / d)*d;

        // Most of the time we're only interested in the direction and not the magnitude.
        // These s..n variables are just the signs of the changes.
        int sdx1 = Util.sign(wdx1);
        int sdx2 = Util.sign(wdx2);
        int sdy1 = Util.sign(wdy1);
        int sdy2 = Util.sign(wdy2);

        // If this touch movement is basically the same direction and parameters of the last gesture
        // we count this as a new sample.  the sampleCount is a mechanism we use to determine how
        // "sure" the gesture is
        if (sdx1 == lsdx1 && sdx2 == lsdx2 && lsdy1 == sdy1 && lsdy2 == sdy2) {
            sampleCount++;
        } else {
            sampleCount = 0;
        }
        lsdx1 = sdx1;
        lsdx2 = sdx2;
        lsdy1 = sdy1;
        lsdy2 = sdy2;

        // How many nonzeros?  Another mechanism to determine how "sure" the gesture is is to make
        // sure that there are a significant number of changes in the gesture movement
        int nz = 0;
        if (sdx1 != 0) nz++;
        if (sdx2 != 0) nz++;
        if (sdy1 != 0) nz++;
        if (sdy2 != 0) nz++;

        // Must have two changes in order to be valid, and must have at least 3 events doing the same thing
        if (nz < 2 || sampleCount < 2) {

        } else {
            // So now we know we're doing an event, we determine what the gesture means

            if (sdy1 == -1 && sdy2 == -1 && (sdx1 == 0 || sdx2 == 0)) { // Wrist Up
                // Two fingers sliding up moves the wrist up
                effect = WRISTUP;
            } else if (sdy1 == 1 && sdy2 == 1 && (sdx1 == 0 || sdx2 == 0 )) { // Wrist Down
                // Two fingers sliding down moves the wrist down
                effect = WRISTDOWN;
            } else if (sdx1 < 0 && sdx2 < 0 && (sdy1 == 0 || sdy2 == 0)) { // Base left
                // Two fingers going left moves the base to the left
                effect = BASELEFT;
            } else if (sdx1 > 0 && sdx2 > 0 && (sdy1 == 0 || sdy2 == 0)) { // Bas right
                // Two fingers moving right moves the base to the right
                effect = BASERIGHT;
            } else if (sdx1 >= 0 && sdx2 < 0) { // Gripper close
                // Pinching closes the gripper
                effect = GRIPPERCLOSE;
            } else if (sdx1 <= 0 && sdx2 > 0 ) { // Gripper Open
                // Spreading opens the gripper
                effect = GRIPPEROPEN;
            }

            System.out.format("==== %2d = %+2d %+2d %+2d %+2d  %20s  (%f, %f) and (%f, $f)", sampleCount, sdx1, sdy1, sdx2, sdy2, effect.toString(), dx1, dy1, dx2, dy2);

            handleEffect(effect, distanceV1V2);
            sampleCount = 0;

        }
        lastV1 = v1;
        lastV2 = v2;



        return true;
    }

    /**
     * This method actually performs the effect.  Note that this method also potentially
     * can rotate the gripper.  But when I was testing the gesture (making a two finger
     * rotating motion), it kept getting confused with gripper open/close.  So I removed
     * that gesture recognition from the onTouch evaluation.  The effect is still handled
     * here in case I make a new gesture (three fingers?) to detect the desire to rotate the
     * wrist.
     *
     *
     * @param effect
     * @param value
     */
    private void handleEffect(EFFECT effect, double value) {

        Servo gripper = findServo("gripper");
        Servo rotator = findServo("rotator");
        Servo wrist = findServo("wrist");
        Servo base = findServo("base");

        switch (effect) {
            case WRISTUP : {
                wrist.setCurrentValue(wrist.getCurrentValue() + 50);
            } break;

            case WRISTDOWN: {
                wrist.setCurrentValue(wrist.getCurrentValue() - 50);
            } break;

            case GRIPPERCLOSE: {
                double proportion = (value / .5d) / ((double) screenWidth);
                int lastm = gripper.getCurrentValue();
                int m = (int) ((double) gripper.getServoMaxValue() - (double) (gripper.getServoMaxValue() - gripper.getServoMinValue()) * proportion + (double) gripper.getServoMinValue());
                gripper.setCurrentValue(m);
            } break;

            case GRIPPEROPEN: {
                gripper.setCurrentValue(gripper.getCurrentValue() - 800);
            } break;

            case ROTATECW: {
                rotator.setCurrentValue(rotator.getCurrentValue() + 250);
            } break;

            case ROTATECCW: {
                rotator.setCurrentValue(rotator.getCurrentValue() - 250);
            } break;

            case BASELEFT: {
                base.setCurrentValue(base.getCurrentValue() - 50);
            } break;

            case BASERIGHT: {
                base.setCurrentValue(base.getCurrentValue() + 50);
            } break;
        }

        // Queue up the command for the gripper, rotator, wrist and base
        enqueue(gripper.getSSCCommand()+" "+rotator.getSSCCommand()+" "+wrist.getSSCCommand()+ " " + base.getSSCCommand());
    }

    /**
     * Conveniently calculates the distance between two points.   If either point is null, the
     * distance is zero.
     *
     * @param start
     * @param end
     * @return
     */
    private float distance(Vec2f start, Vec2f end) {
        if (start == null || end == null) return 0;
        return Vec2f.distanceBetween(start, end);
    }

    /**
     * Draw the arm on the canvas of the imageview.  THIS method (not currently used)
     * draws the arm using the bone's start/end location.
     */
    private void drawArm() {

        // If imageView isn't set yet, ignore the call
        if (imageView == null) return;


        Vec2f effector = arm.getChain().getEffectorLocation();

        // Width and height of the imageview
        int width = (int) imageView.getWidth();
        int height = (int) imageView.getHeight();


        // Create a bitmap-backed canvas that we'll loadinto the imageview
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        ArrayList<LocalBone> bones = arm.getBones();

        // The bones will be painted in Blue
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10f);

        double chainLength = arm.getTotalLength();

        Vec2f conversion = new Vec2f(width, height);

        for (LocalBone bone: bones) {
            Vec2f start = bone.getBone().getStartLocation();
            Vec2f end = bone.getBone().getEndLocation();

            Vec2f screenStart = convertArmToScreen(start);
            Vec2f screenEnd = convertArmToScreen(end);

            double screenAngle = Math.atan2((double) (screenEnd.y - screenStart.y), (screenEnd.x - screenStart.x));
            System.out.println(">>> Angle Compare Bone reports: "+bone.getAngleDeg()+" screen says "+screenAngle);

            // System.out.println("Draw from: ("+startX+","+startY+") to ("+endX+","+endY+")");

            canvas.drawLine(screenStart.x, screenStart.y, screenEnd.x, screenEnd.y, paint);
        }

        imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    /**
     * Draw the arm using the angles between the bones.  We're currently using this method of drawing
     * because it is easier to compare what is really happening in the servos by using vector draws.
     *
     */
    public void drawArmViaVectors() {
        if (imageView == null) return;


        int width = (int) imageView.getWidth();
        int height = (int) imageView.getHeight();



        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);




        // Draw the bones in RED (vs. Blue to indicate that vector draw is being used instead)
        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(5f);

        // First draw the semicircle
        float armLength = arm.getChain().getChainLength();
        canvas.drawArc(0f,0f,getSmallerDimension(),getSmallerDimension(),180f,180f,false,paint);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(30f);

        ArrayList<LocalBone> bones = arm.getBones();

        for (LocalBone bone: bones) {

            // The start location of the bone
            Vec2f start = bone.getBone().getStartLocation();

            // The angle the bone is at.
            double angle = bone.getAngleDeg(); // Angle

            // Calculate end vector
            Vec2f end = getEndPoint(start, bone.getBone().length(), angle);
            Vec2f boneend = bone.getBone().getEndLocation();

            // Where we should plot the calculated start/end on the screen
            Vec2f screenStart = convertArmToScreen(start);
            Vec2f screenEnd = convertArmToScreen(end);

            // Draw the line
            canvas.drawLine(screenStart.x, screenStart.y, screenEnd.x, screenEnd.y, paint);
        }

        // Set the bitmap into the image view
        imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    /**
     * Conveniently draws a circle on the canvas at the given arm coordinates.  Convenient if
     * you want to plot a circle representing the effector point.
     *
     * @param canvas
     * @param armPointCoords - the arm coordinates in arm measurements
     * @param radius
     * @param color
     */
    public void circlePlotArmCoords(Canvas canvas, Vec2f armPointCoords, float radius, int color) {
        Vec2f effector = convertArmToScreen(armPointCoords);

        float effectorULx = effector.x - radius;
        float effectorULy = effector.y + radius;
        float effectorLRx = effector.x + radius;
        float effectorLRy = effector.y - radius;

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(10f);
        canvas.drawArc(effectorULx, effectorULy, effectorLRx, effectorLRy, 0f, 360f, false, paint);
    }

    /**
     * Conveniently draws a circle on the canvas at the given screen coordinates
     *
     * @param canvas
     * @param screenPointCoords
     * @param radius
     * @param color
     */
    public void circlePlotScreenCoords(Canvas canvas, Vec2f screenPointCoords, float radius, int color) {

        float effectorULx = screenPointCoords.x - radius;
        float effectorULy = screenPointCoords.y + radius;
        float effectorLRx = screenPointCoords.x + radius;
        float effectorLRy = screenPointCoords.y - radius;

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(10f);
        canvas.drawArc(effectorULx, effectorULy, effectorLRx, effectorLRy, 0f, 360f, false, paint);
    }


    /**
     * Method to convert arm coordinates to screen coordinates.
     *
     * @param armCoord
     * @return
     */
    public  static  Vec2f convertArmToScreen(Vec2f armCoord) {
        float armX = armCoord.x;
        float armY = armCoord.y;

        if (imageView == null) return null;

        float chainLength = arm.getTotalLength();

        // Arm coords go from -chainLength to +chainLength.  Convert to zero - chainLength*2
        armX += chainLength;
        armY += chainLength;

        // Arm coords go from UL = 0,(chainLength*2) to LR (chainLength*2),0  Whereas the screen
        // goes from UL (0,0) to (screenWidth,screenHeight).  We have to convert the arm coords
        // to be similar to screen coords.
        armY = chainLength*2 - armY;

        float smallerDimension = getSmallerDimension();

        // Create the ratios we will use
        float ratioX = (smallerDimension) / (chainLength*2f);
        float ratioY = (smallerDimension) / (chainLength*2f);

        float screenX = ratioX * armX;
        float screenY = ratioY * armY;

        Vec2f retval = new Vec2f(screenX, screenY);
        return retval;
    }

    public static float getSmallerDimension() {
        int width = (int) imageView.getWidth();
        int height = (int) imageView.getHeight();

        float smallerDimension = width;
        if (height < smallerDimension)   smallerDimension = height;
        return smallerDimension;

    }

    protected void enqueue(String command) {
        System.out.println(":: QQQQQQQUEING: "+command);
        queue.add(command);
    }

    /**
     * Move servos into the position as represented in the arm object
     *
     */
    public String armToServos() {

        ArrayList<LocalBone> bones = arm.getBones();

        float lastAngle = 0f;

        for (LocalBone bone: bones) {

            float angle = bone.getAngleDeg(); // Angle
            Servo servo = findServo(bone.getName());
            if (servo == null) continue;
            double servoAngle = angle - lastAngle;

            // Normalize the angle
            servoAngle = Util.normalize(servoAngle);
            servo.moveToAngle(servoAngle);
            lastAngle = angle;
        }


        displayBones();

        return getServoCommands();
    }

    /**
     * Query all the servos and get their SSC-32 commands, concatenate them and add
     * the SSc-32 command to execute the move in one second.
     * @return
     */
    public String getServoCommands() {
        String command = "";
        for (Servo s: servos) {
            command = command + (command.equals("")?"":" ") + s.getSSCCommand();
        }
        command = command + " T"+MS_TO_MOVE;
        return command;
    }


    /**
     * Find the named servo in servo array
     *
     * @param name
     * @return
     */
    public Servo findServo(String name) {
        for (int i=0; i<servos.length; ++i) {
            if (servos[i].getName().equalsIgnoreCase(name)) return servos[i];
        }
        return null;
    }

    /**
     * Find the named servo in the servo array and get its index number
     *
     * @param name
     * @return
     */
    public int findServoIndex(String name) {
        for (int i=0; i<servos.length; ++i) {
            if (servos[i].getName().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }


    /**
     * Calculate the end point given a start point, length and angle.
     *
     * @param startPoint
     * @param length
     * @param angleDeg
     * @return
     */
    public Vec2f getEndPoint(Vec2f startPoint, double length, double angleDeg) {
        // if (angleDeg > 180f ) angleDeg = 90d - angleDeg;

        double radians = Util.degToR(angleDeg);
        double dx = Math.cos(radians)*length;
        double dy = Math.sin(radians)*length;

        double x = Util.round(startPoint.x + dx,1);
        double y = Util.round(startPoint.y + dy,1);


        return new Vec2f((float) x,(float) y);
    }


    /**
     * Save the current macro (record).  This method will
     * display the appropriate dialog and handle the inputs
     */
    private void saveMacro() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_save);
        dialog.setTitle("Save Current Script");

        // The widgets in the dialog
        final EditText dialog_save_file = (EditText) dialog.findViewById(R.id.dialog_save_file);
        final Button dialog_button_save = (Button) dialog.findViewById(R.id.dialog_button_save);
        final ImageButton dialog_button_cancel = (ImageButton) dialog.findViewById(R.id.dialog_button_cancel);

        // If cancel is hit, cancel.
        dialog_button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });


        // Handle save button.  Ignore the save if the filename wasn't input
        dialog_button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = dialog_save_file.getText().toString().trim();
                if ( filename.equals("")) return;

                // We save the macro to the app preferences
                Prefs.saveMacro(filename, record);

                // And close the dialog
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Display a dialog to the user with all the macros that have been saved
     * to AppPreferences.  Allow him to select one, and load it into the
     * records variable.
     *
     */
    private void loadMacro() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_load);

        // Buttons in our dialog
        final LinearLayout dialog_load_macros = (LinearLayout) dialog.findViewById(R.id.dialog_load_macros);
        final Button dialog_load_button_cancel = (Button) dialog.findViewById(R.id.dialog_load_button_cancel);

        // On hitting cancel, cancel the dialog
        dialog_load_button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        // Get macros from preferences
        ArrayList<String> macroNames = Prefs.getMacroNames();

        // Put a button for each macro
        for (final String macro: macroNames) {
            Button b = new Button(this);
            b.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            b.setText(macro);

            // If this button is clicked, set the record global
            // variable to the macro, and refresh the macro display,
            // and close the dialog.
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    record = Prefs.getMacro(macro);
                    showSteps();
                    dialog.dismiss();
                }
            });
            dialog_load_macros.addView(b);
        }

        dialog.show();

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    class Looper extends BaseIOIOLooper {

        private int ledPin = 0;
        private int rxPin = 1;
        private int txPin = 2;
        private int BAUDRATE = 9600;

        private Uart.Parity PARITY = Uart.Parity.NONE;
        private Uart.StopBits STOPBITS = Uart.StopBits.ONE;
        OutputStream out;
        PrintStream printout;

        InputStream in;

        private boolean ledState = false;

        Uart uart;
        DigitalOutput led_;

        Thread receiver;

        @Override
        protected void setup() throws ConnectionLostException {
            led_ = ioio_.openDigitalOutput(ledPin, ledState);
            uart = ioio_.openUart(rxPin, txPin, BAUDRATE, PARITY, STOPBITS);
            out = uart.getOutputStream();
            printout = new PrintStream(out);

            in = uart.getInputStream();

        }

        @Override
        public void disconnected() {
            uart.close();
        }

        synchronized int waitFor(int... ch) {
            int c = -1;
            try {
                while ((c = in.read()) != -1) {
                    if (ch == null) return c;
                    for (int check : ch) {
                        if (check == c) return c;
                    }
                }
            } catch (Exception ex) {

            }
            return c;
        }

        synchronized void waitForEmpty() {
            int ch = 0;
            do {
                sendCommand("Q\n");
                ch = waitFor('+', '.');
            } while (ch != '.' && ch != -1);
        }

        /**
         * loop() is called at regular intervals.   It is the main loop to handle things.  For Fluffy
         * the main loop will check for commands and send them to the SSC board.
         */
        @Override
        public void loop() {
            do {
                try {
                    String nextCommand = queue.remove();
                    if (nextCommand != null) {
                        // Send command to SSC-32
                        System.out.println("***** COMMAND: " + nextCommand);
                        sendCommand(nextCommand);
                        waitForEmpty();
                        ledState = !ledState;
                        led_.write(ledState);

                    } else break;
                } catch (Exception ex) {
                    // No more queue items
                    // ex.printStackTrace();
                    break;
                }
            } while (true);
        }

        private void sendCommand(String command) {
            // printout.println(command+(char) 13);
            // printout.flush();
            try {
                for (int i = 0; i < command.length(); ++i) {
                    int ch = command.charAt(i);
                    // System.out.print((char) ch);
                    out.write(ch);
                }
                out.write(13);
                // System.out.println("");
            } catch (Exception ex) {
                System.out.println("Exception writing to IOBoard");
                ex.printStackTrace();
            }
        }

    }

}
