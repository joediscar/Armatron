package guru.syzygy.armatron;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;

import au.edu.federation.utils.Vec2f;

/**
 *
 * This class handles a single touch inside the Imageview that shows the arm depiction
 * The touch screen coordinates are where we want the arm effector to move to.
 *
 * Created by discar on 5/28/2017.
 */

public class GestureListener implements View.OnTouchListener {
    float lastTouchX;
    float lastTouchY;

    ArmActivity activity;

    public GestureListener(ArmActivity activity) {
        this.activity = activity;
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {

        // dimensions of the view
        float screenWidth = view.getMeasuredWidth();
        float screenHeight = view.getMeasuredHeight();

        // We choose the smaller dimension
        if (screenWidth < screenHeight) screenHeight = screenWidth;
        if (screenHeight < screenWidth) screenWidth = screenHeight;

        // Where the touch takes place
        final float touchX = event.getX();
        final float touchY = event.getY();

        //

        // If the touch is the same as last time, just return
        if (touchX == lastTouchX && touchY == lastTouchY) return false;   // This is the same one as last time
        lastTouchX = touchX;
        lastTouchY = touchY;

        // The length of the arm determines how "wide" our depiction of the arm will be
        float chainlength = activity.arm.getTotalLength();

        // Screen is UL (0,0) to LR(screenWidth, screenHeight) but arm is UL(-chainlength,chainlength) to LR(chainlength, -chainlength)
        // So convert
        float propoX = touchX/screenWidth;
        float propoY = touchY/screenHeight;

        // Calculate the screen coordinates into real-world measurements
        float armX = chainlength*2 * propoX - chainlength;
        float armY = chainlength - chainlength*2 * propoY;

        //
        System.out.println("-----> TOUCH ("+touchX+","+touchY+")  in screen "+screenWidth+"x"+screenHeight+" Move Arm to: ("+(int) armX+", "+(int) armY+")");

        // Ignore anything below zero
        if (armY < 0) return false;

        // Calculate where the arm needs to move
        activity.arm.solveForTarget((double) armX, (double) armY);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.drawArmViaVectors();
                Bitmap bitmap = ((BitmapDrawable) activity.imageView.getDrawable()).getBitmap();
                Canvas canvas = new Canvas(bitmap);

                // For debugging purposes, it is convenient to see where our touch really was
                Vec2f touchVec = new Vec2f(touchX, touchY);
                activity.circlePlotScreenCoords(canvas, touchVec, 10f, Color.GREEN); // Touch location

                // And to display where the effector actually ended up at
                Vec2f effector = activity.arm.getChain().getEffectorLocation();
                activity.circlePlotArmCoords(canvas, effector, 10f, Color.BLACK); // Effector location
            }
        });


        String command = activity.armToServos();
        activity.enqueue(command);

        return true;

    }


}
