package com.stevezeidner.movementgauge.core;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.FloatMath;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Utility methods not necessarily tied to one activity or another
 */
public class Utility {
    /**
     * Fix bug with drawing canvas text in Android 4.0.3+
     *
     * @param canvas
     * @param text
     * @param x
     * @param y
     * @param paint
     */
    public static void drawTextOnCanvasWithMagnifier(Canvas canvas, String text, float x, float y, Paint paint) {
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            //draw normally
            canvas.drawText(text, x, y, paint);
        } else {
            //workaround
            float originalTextSize = paint.getTextSize();
            final float magnifier = 1000f;
            canvas.save();
            canvas.scale(1f / magnifier, 1f / magnifier);
            paint.setTextSize(originalTextSize * magnifier);
            canvas.drawText(text, x * magnifier, y * magnifier, paint);
            canvas.restore();
            paint.setTextSize(originalTextSize);
        }
    }

    /**
     * Round a float to a max number of decimals
     *
     * @param d
     * @param decimalPlace
     * @return
     */
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    /**
     * Generate a random number between two values
     *
     * @param low
     * @param high
     * @return
     */
    public static int randomBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    /**
     * Get total acceleration from 3-axis of acceleration
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static float totalAcceleration(float x, float y, float z) {
        return FloatMath.sqrt(x * x + y * y + z * z);
    }
}
