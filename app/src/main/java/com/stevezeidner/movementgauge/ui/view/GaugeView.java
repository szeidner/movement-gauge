package com.stevezeidner.movementgauge.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.stevezeidner.movementgauge.R;

/**
 * View that draws a gauge and controls the needle
 */
public class GaugeView extends View {

    private static final String TAG = GaugeView.class.getSimpleName();

    private Paint scaleMinorTickPaint;
    private Paint scaleMajorTickPaint;
    private Paint scaleValuePaint;
    private Paint facePaint;
    private Paint dotPaint;
    private Paint dotShadowPaint;
    private RectF scaleRect;
    private RectF faceRect;
    private RectF dotRect;
    private RectF dotShadowRect;

    private Paint needlePaint;
    private Path needlePath;
    private Paint needleShadowPaint;
    private Path needleShadowPath;
    private Paint backgroundPaint;

    private Bitmap background;

    // scale config
    private static final int totalNicks = 80;
    private static final float degreesPerNick = 360.0f / totalNicks;
    private static final int centerDegree = 50; // the one in the top center (12 o'clock)
    private static final int minDegrees = 0;
    private static final int maxDegrees = 100;

    // needle dynamics -- all are angular expressed in F degrees
    private float needlePosition = 0.0f;
    private float needleTarget = centerDegree;
    private float needleVelocity = 0.0f;
    private float needleAcceleration = 1.0f;
    private long lastNeedleMoveTime = -1L;

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
        initDrawingTools();
    }

    private void initDrawingTools() {

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
        scaleValuePaint.setColor(getResources().getColor(R.color.scale_tick_value));
        scaleValuePaint.setTextSize(0.05f);
        scaleValuePaint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        scaleValuePaint.setTextAlign(Paint.Align.CENTER);
        scaleValuePaint.setAntiAlias(true);

        scaleRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

        facePaint = new Paint();
        facePaint.setAntiAlias(true);
        facePaint.setColor(getResources().getColor(R.color.face_instantaneous));
        facePaint.setStyle(Paint.Style.FILL);

        float scaleSize = 0.15f;
        faceRect = new RectF();
        faceRect.set(scaleRect.left + scaleSize, scaleRect.top + scaleSize,
                scaleRect.right - scaleSize, scaleRect.bottom - scaleSize);

        dotShadowPaint = new Paint();
        dotShadowPaint.setAntiAlias(true);
        dotShadowPaint.setColor(getResources().getColor(R.color.dot_shadow));
        dotShadowPaint.setStyle(Paint.Style.FILL);

        float facePadding = 0.16f;
        dotShadowRect = new RectF();
        dotShadowRect.set(faceRect.left + facePadding, faceRect.top + facePadding,
                faceRect.right - facePadding, faceRect.bottom - facePadding);

        dotPaint = new Paint();
        dotPaint.setAntiAlias(true);
        dotPaint.setColor(getResources().getColor(R.color.dot));
        dotPaint.setStyle(Paint.Style.FILL);

        float dotShadowPadding = 0.05f;
        dotRect = new RectF();
        dotRect.set(dotShadowRect.left + dotShadowPadding, dotShadowRect.top + dotShadowPadding,
                dotShadowRect.right - dotShadowPadding, dotShadowRect.bottom - dotShadowPadding);

        needlePaint = new Paint();
        needlePaint.setAntiAlias(true);
        needlePaint.setColor(getResources().getColor(R.color.needle));
        needlePaint.setStyle(Paint.Style.FILL);

        needlePath = new Path();
        needlePath.moveTo(0.5f + 0.010f, 0.47f);
        needlePath.lineTo(0.5f - 0.010f, 0.47f);
        needlePath.lineTo(0.5f - 0.002f, 0.47f - 0.35f);
        needlePath.lineTo(0.5f + 0.002f, 0.47f - 0.35f);

        needleShadowPaint = new Paint();
        needleShadowPaint.setAntiAlias(true);
        needleShadowPaint.setColor(getResources().getColor(R.color.needle_shadow));
        needleShadowPaint.setStyle(Paint.Style.FILL);

        float shadowXoffset = 0.007f;
        float shadowYoffset = 0.008f;
        needleShadowPath = new Path();
        needleShadowPath.moveTo(0.5f - shadowXoffset + 0.010f, 0.465f);
        needleShadowPath.lineTo(0.5f - shadowXoffset - 0.010f, 0.465f);
        needleShadowPath.lineTo(0.5f - shadowXoffset - 0.002f, 0.465f - 0.35f - shadowYoffset);
        needleShadowPath.lineTo(0.5f - shadowXoffset + 0.002f, 0.465f - 0.35f - shadowYoffset);

        backgroundPaint = new Paint();
        backgroundPaint.setFilterBitmap(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, needlePaint);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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

    private void drawFace(Canvas canvas) {
        canvas.drawOval(faceRect, facePaint);
        canvas.drawOval(dotShadowRect, dotShadowPaint);
        canvas.drawOval(dotRect, dotPaint);
    }

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

        drawNeedle(canvas);

        canvas.restore();

        if (needleNeedsToMove()) {
            moveNeedle();
        }
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
        drawFace(backgroundCanvas);
    }

    private void drawNeedle(Canvas canvas) {
        float needleAngle = degreeToAngle(needlePosition);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(needleAngle, 0.5f, 0.5f);
        canvas.drawPath(needleShadowPath, needleShadowPaint);
        canvas.drawPath(needlePath, needlePaint);
        canvas.restore();
    }

    private boolean needleNeedsToMove() {
        return Math.abs(needlePosition - needleTarget) > 0.01f;
    }

    private void moveNeedle() {
        if (!needleNeedsToMove()) {
            return;
        }

        if (lastNeedleMoveTime != -1L) {
            long currentTime = System.currentTimeMillis();
            float delta = (currentTime - lastNeedleMoveTime) / 1000.0f;

            float direction = Math.signum(needleVelocity);
            if (Math.abs(needleVelocity) < 90.0f) {
                needleAcceleration = 5.0f * (needleTarget - needlePosition);
            } else {
                needleAcceleration = 0.0f;
            }
            needlePosition += needleVelocity * delta;
            needleVelocity += needleAcceleration * delta;
            if ((needleTarget - needlePosition) * direction < 0.01f * direction) {
                needlePosition = needleTarget;
                needleVelocity = 0.0f;
                needleAcceleration = 0.0f;
                lastNeedleMoveTime = -1L;
            } else {
                lastNeedleMoveTime = System.currentTimeMillis();
            }
            invalidate();
        } else {
            lastNeedleMoveTime = System.currentTimeMillis();
            moveNeedle();
        }
    }
    
    public void setValue(float value) {
        if (value < minDegrees) {
            value = minDegrees;
        } else if (value > maxDegrees) {
            value = maxDegrees;
        }
        needleTarget = value;
        invalidate();
    }

    public void setFaceColor(int color) {
        facePaint.setColor(color);
        regenerateBackground();
        invalidate();
    }

}
