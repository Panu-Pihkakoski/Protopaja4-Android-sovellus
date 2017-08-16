package com.proto4.protopaja.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by user on 4.08.17.
 */

public class RoundSlider extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = RoundSlider.class.getSimpleName();


    private SurfaceHolder holder;
    private Listener listener;
    private int width, height;

    private int backgroundColor;

    private float value, minValue, maxValue;

    private boolean showPercentage, showValue, showKelvins, flipped;

    private float[] lastDown;

    private Slider slider;

    private static final float VALUE_ARC_START = -180;


    public RoundSlider(Context context) {
        super(context);
        init(context);
    }
    public RoundSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    public RoundSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        width = height = 0;
        value = 0;
        minValue = 0;
        maxValue = 254;
        lastDown = new float[]{0,0};
        holder = getHolder();
        holder.addCallback(this);
        backgroundColor = 0xffffffff;
        showPercentage = showValue = showKelvins = flipped = false;
        Log.d(TAG, "Surface initialized");
    }

    public void setValue(int _value) {
        value = _value;
        if (value < minValue)
            value = minValue;
        else if (value > maxValue)
            value = maxValue;
    }

    public void setMinValue(int _minValue) {
        Log.d(TAG, "setting min value: " + _minValue);
        minValue = _minValue;
        if (value < minValue)
            value = minValue;
    }

    public void setMaxValue(int _maxValue) {
        Log.d(TAG, "setting max value: " + _maxValue);
        maxValue = _maxValue;
        if (value > maxValue)
            value = maxValue;
    }

    public void setShowPercentage(boolean show) {
        showPercentage = show;
        if (show) {
            showValue = false;
            showKelvins = false;
        }
    }

    public void setShowValue(boolean show) {
        showValue = show;
        if (show) {
            showPercentage = false;
            showKelvins = false;
        }
    }

    public void setShowKelvins(boolean show) {
        showKelvins = show;
        if (show) {
            showPercentage = false;
            showValue = false;
        }
    }

    public void setFlipped(boolean flip) {
        flipped = flip;
    }

    public void setListener(Listener _listener) {
        Log.d(TAG, "setListener()");
        listener = _listener;
    }

    public void setSliderBackground(int color) {
        backgroundColor = color;
    }

    public void update() {
        renderContents();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        renderContents();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout");
        width = r-l; height = b-t;
        float radius = Math.min(width/2, height);
        if (slider == null)
            slider = new Slider(width/2, radius, radius);
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onMeasure(int wms, int hms) {
        Log.d(TAG, "onMeasure(" + MeasureSpec.getSize(wms) + ", " + MeasureSpec.getSize(hms) +")");
        setMeasuredDimension(MeasureSpec.getSize(wms), MeasureSpec.getSize(wms)/2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            lastDown[0] = x; lastDown[1] = y;
        } else if (e.getAction() == MotionEvent.ACTION_UP) {        // set value and notify listener
            if (lastDown[0] == x && lastDown[1] == y) { // click
                Log.d(TAG, "click: x=" + x + " y=" + y);
                float rad = (float)Math.atan2(y-slider.posy, x - slider.posx);
                if (flipped) rad *= -1;
                Log.d(TAG, "rad=" + rad);
                if (rad > -Math.PI && rad < -Math.PI/3*2)           // value to min
                    value = minValue;
                else if (rad > -Math.PI/3*2 && rad < -Math.PI/3)    // value to half
                    value = (maxValue-minValue)/2 + minValue;
                else if (rad < 0)                                   // value to max
                    value = maxValue;
            }
            Log.d(TAG, "power to listener...");
            if (listener != null)
                listener.onValueSet((int)value);
        } else {
            float rad = (float)Math.atan2(y-slider.posy, x - slider.posx);
            if (flipped) rad *= -1;
            Log.d(TAG, "rad=" + rad);
            if (rad > -Math.PI && rad < 0)
                value = (float)(1+rad/Math.PI)*(maxValue-minValue) + minValue;
            else if (rad > Math.PI/2)
                value = minValue;
            else
                value = maxValue;
        }

        renderContents();
        return true;
    }

    private void renderContents() {
        Canvas canvas = getHolder().lockCanvas();
        canvas.drawColor(0xffffffff);
        if (slider != null)
            slider.draw(canvas);
        getHolder().unlockCanvasAndPost(canvas);
    }

    private class Slider {
        RectF rectArc, rect;
        Paint halfCirclePaint, valueArcPaint, percentagePaint;
        float posx, posy, radius;

        Slider(float x, float y, float r) {
            if (flipped) y -= r;

            posx = x; posy = y; radius = r;

            rect = new RectF(x-r, y-r, x+r, y+r);
            rectArc = new RectF(x-r*0.9f, y-r*0.9f, x+r*0.9f, y+r*0.9f);
            valueArcPaint = new Paint();
            valueArcPaint.setStyle(Paint.Style.STROKE);
            valueArcPaint.setStrokeWidth(2*(r-r*0.9f));
            valueArcPaint.setColor(0xff0000ff);
            halfCirclePaint = new Paint();
            halfCirclePaint.setStyle(Paint.Style.FILL);
            halfCirclePaint.setColor(0xff808080);
            percentagePaint = new Paint();
            percentagePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            percentagePaint.setColor(0xff0000ff);
            percentagePaint.setTextSize(r/2);
            percentagePaint.setTextAlign(Paint.Align.CENTER);
        }

        void draw(Canvas canvas) {

            canvas.drawColor(backgroundColor);

            int br = (int)(255*(value-minValue)/(maxValue-minValue));
            int color = (0xff << 24) + (br << 16) + (br << 8) + br;
            halfCirclePaint.setColor(color);

            // draw half circle
            canvas.drawArc(rect, VALUE_ARC_START, flipped ? 360 : 180, false, halfCirclePaint);

            if (showKelvins) {
                int c = 0xff000000;
                int b = (int)(255*(value-minValue)/(maxValue-minValue));
                int r = 255-b;
                c += (r << 16) + b;
                valueArcPaint.setColor(c);
            }

            // draw value arc
            canvas.drawArc(rectArc, VALUE_ARC_START, getArcEnd(), false, valueArcPaint);

            if (showPercentage)     // draw value percentage
                canvas.drawText((int)((value-minValue)/(maxValue-minValue)*100) + "%",
                        posx, posy + (flipped ? radius/2 : -radius/5), percentagePaint);
            else if (showValue)
                canvas.drawText(Integer.toString((int)value),
                        posx, posy + (flipped ? radius/2 : -radius/5), percentagePaint);
            else if (showKelvins)
                canvas.drawText(Integer.toString((int)value*100) + "K",
                        posx, posy + (flipped ? radius/2 : -radius/5), percentagePaint);
        }

        float getArcEnd() {
            return (int)(value != 0 ? (value-minValue)/(maxValue-minValue) * (flipped ? -180 : 180): 0);
        }
    }

    public interface Listener {
        void onValueSet(int value);
    }
}
