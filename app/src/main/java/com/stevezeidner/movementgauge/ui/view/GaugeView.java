package com.stevezeidner.movementgauge.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.stevezeidner.movementgauge.R;

/**
 * View that draws a gauge and controls the needle
 */
public class GaugeView extends View {

    private static final String TAG = GaugeView.class.getSimpleName();

    private RectF faceRect;

    private Paint scaleMinorTickPaint;
    private Paint scaleMajorTickPaint;
    private Paint scaleValuePaint;
    private RectF scaleRect;

    private Paint handPaint;
    private Path handPath;
    private Paint handScrewPaint;
    private Paint backgroundPaint;

    private Bitmap background;

    private Handler handler;

    // scale configuration
// scale configuration
    private static final int totalNicks = 70;
    private static final float degreesPerNick = 360.0f / totalNicks;
    private static final int centerDegree = 60; // the one in the top center (12 o'clock)
    private static final int minDegrees = 0;
    private static final int maxDegrees = 100;

    // hand dynamics -- all are angular expressed in F degrees
    private boolean handInitialized = false;
    private float handPosition = centerDegree;
    private float handTarget = centerDegree;
    private float handVelocity = 0.0f;
    private float handAcceleration = 0.0f;
    private long lastHandMoveTime = -1L;

    public GaugeView(Context context) {
        super(context);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        handler = new Handler();
        initDrawingTools();
    }

    private void initDrawingTools() {
        faceRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

        scaleMinorTickPaint = new Paint();
        scaleMinorTickPaint.setStyle(Paint.Style.STROKE);
        scaleMinorTickPaint.setColor(getResources().getColor(R.color.scale_tick_minor));
        scaleMinorTickPaint.setStrokeWidth(0.005f);
        scaleMinorTickPaint.setAntiAlias(true);

        scaleMajorTickPaint = new Paint();
        scaleMajorTickPaint.setStyle(Paint.Style.STROKE);
        scaleMajorTickPaint.setColor(getResources().getColor(R.color.scale_tick_major));
        scaleMajorTickPaint.setStrokeWidth(0.007f);
        scaleMajorTickPaint.setAntiAlias(true);

        scaleValuePaint = new Paint();
        scaleValuePaint.setColor(getResources().getColor(R.color.scale_tick_major));
        scaleValuePaint.setTextSize(0.05f);
        scaleValuePaint.setTypeface(Typeface.SANS_SERIF);
        scaleValuePaint.setTextAlign(Paint.Align.CENTER);
        scaleValuePaint.setAntiAlias(true);

        float scalePosition = 0.00f;
        scaleRect = new RectF();
        scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
                faceRect.right - scalePosition, faceRect.bottom - scalePosition);

        handPaint = new Paint();
        handPaint.setAntiAlias(true);
        handPaint.setColor(0xff392f2c);
        handPaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
        handPaint.setStyle(Paint.Style.FILL);

        handPath = new Path();
        handPath.moveTo(0.5f, 0.5f + 0.2f);
        handPath.lineTo(0.5f - 0.010f, 0.5f + 0.2f - 0.007f);
        handPath.lineTo(0.5f - 0.002f, 0.5f - 0.32f);
        handPath.lineTo(0.5f + 0.002f, 0.5f - 0.32f);
        handPath.lineTo(0.5f + 0.010f, 0.5f + 0.2f - 0.007f);
        handPath.lineTo(0.5f, 0.5f + 0.2f);
        handPath.addCircle(0.5f, 0.5f, 0.025f, Path.Direction.CW);

        handScrewPaint = new Paint();
        handScrewPaint.setAntiAlias(true);
        handScrewPaint.setColor(0xff493f3c);
        handScrewPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setFilterBitmap(true);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
        Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        int chosenDimension = Math.min(chosenWidth, chosenHeight);

        setMeasuredDimension(chosenDimension, chosenDimension);
    }


    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredSize();
        }
    }

    private void drawScale(Canvas canvas) {
        //canvas.drawOval(scaleRect, scalePaint);

        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        for (int i = 0; i < totalNicks; ++i) {
            float y1Major = scaleRect.top;
            float y1Minor = scaleRect.top - 0.015f;
            float y2Major = y1Major - 0.050f;
            float y2Minor = y2Major + 0.015f;

            int value = nickToDegree(i);

            if (i % 5 == 0) {
                if (value >= minDegrees && value <= maxDegrees) {
                    canvas.drawLine(0.5f, y1Major, 0.5f, y2Major, scaleMajorTickPaint);
                    String valueString = Integer.toString(value);
                    drawTextOnCanvasWithMagnifier(canvas, valueString, 0.5f, y1Major + 0.06f, scaleValuePaint);
                }
            } else {
                if (value >= minDegrees && value <= maxDegrees) {
                    canvas.drawLine(0.5f, y1Minor, 0.5f, y2Minor, scaleMinorTickPaint);
                }
            }

            canvas.rotate(degreesPerNick, 0.5f, 0.5f);
        }
        canvas.restore();
    }

    public static void drawTextOnCanvasWithMagnifier(Canvas canvas, String text, float x, float y, Paint paint) {
        if (android.os.Build.VERSION.SDK_INT <= 15) {
            //draw normally
            canvas.drawText(text, x, y, paint);
        }
        else {
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

    private int nickToDegree(int nick) {
        int rawDegree = ((nick < totalNicks / 2) ? nick : (nick - totalNicks)) * 2;
        int shiftedDegree = rawDegree + centerDegree;
        return shiftedDegree;
    }

    private float degreeToAngle(float degree) {
        return (degree - centerDegree) / 2.0f * degreesPerNick;
    }

    // in case there is no size specified
    private int getPreferredSize() {
        return 300;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);

        float scale = (float) getWidth();
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.scale(scale, scale);

//        drawHand(canvas);

        canvas.restore();

//        if (handNeedsToMove()) {
//            moveHand();
//        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "Size changed to " + w + "x" + h);
        regenerateBackground();
    }

    private void drawBackground(Canvas canvas) {
        if (background == null) {
            Log.w(TAG, "Background not created");
        } else {
            canvas.drawBitmap(background, 0, 0, backgroundPaint);
        }
    }

    private void regenerateBackground() {
        // free the old bitmap
        if (background != null) {
            background.recycle();
        }

        background = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas backgroundCanvas = new Canvas(background);
        float scale = (float) getWidth();
        backgroundCanvas.scale(scale, scale);

        drawScale(backgroundCanvas);
    }

}
