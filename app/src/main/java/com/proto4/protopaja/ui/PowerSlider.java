package com.proto4.protopaja.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by user on 27.07.17.
 */

public class PowerSlider extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = PowerSlider.class.getSimpleName();

    private SurfaceHolder holder;
    private Listener listener;
    private int width, height;

    private int backgroundColor;

    private float power;

    private float[] lastDown;

    private Slider slider;

    private float minPower, maxPower;

    private static final float MAX_POWER = 254;
    private static final float POWER_ARC_START = -180;


    public PowerSlider(Context context) {
        super(context);
        init(context);
    }
    public PowerSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    public PowerSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        width = height = 0;
        power = 0;
        minPower = 0;
        maxPower = 254;
        lastDown = new float[]{0,0};
        holder = getHolder();
        holder.addCallback(this);
        backgroundColor = 0xffa0a0b0;
        Log.d(TAG, "Surface initialized");
    }

    public void setPower(int _power) {
        power = _power;
    }

    public void setMinPower(int _minPower) {
        Log.d(TAG, "minPower set: " + _minPower);
        minPower = _minPower;
        if (power < minPower)
            power = minPower;
    }

    public void setMaxPower(int _maxPower) {
        Log.d(TAG, "maxPower set: " + _maxPower);
        maxPower = _maxPower;
        if (power > maxPower)
            power = maxPower;
    }

    public void setListener(Listener _listener) {
        Log.d(TAG, "setListener()");
        listener = _listener;
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
        } else if (e.getAction() == MotionEvent.ACTION_UP) {        // set power and notify listener
            if (lastDown[0] == x && lastDown[1] == y) { // click
                Log.d(TAG, "click: x=" + x + " y=" + y);
                float rad = (float)Math.atan2(y-slider.posy, x - slider.posx);
                if (rad > -Math.PI && rad < -Math.PI/3*2)           // power to min
                    power = minPower;
                else if (rad > -Math.PI/3*2 && rad < -Math.PI/3)    // power to half
                    power = (maxPower-minPower)/2 + minPower;
                else if (rad < 0)                                   // power to max
                    power = maxPower;
            }
            Log.d(TAG, "power to listener...");
            if (listener != null)
                listener.onPowerSet((int)power);
        } else {
            float rad = (float)Math.atan2(y-slider.posy, x - slider.posx);
            if (rad > -Math.PI && rad < 0)
                power = (float)(1+rad/Math.PI)*(maxPower-minPower) + minPower;
            else if (rad > Math.PI/2)
                power = minPower;
            else
                power = maxPower;
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
        Paint halfCirclePaint, powerArcPaint, percentagePaint;
        float posx, posy, radius;

        Slider(float x, float y, float r) {
            posx = x; posy = y; radius = r;
            rect = new RectF(x-r, y-r, x+r, y+r);
            rectArc = new RectF(x-r*0.9f, y-r*0.9f, x+r*0.9f, y+r*0.9f);
            powerArcPaint = new Paint();
            powerArcPaint.setStyle(Paint.Style.STROKE);
            powerArcPaint.setStrokeWidth(2*(r-r*0.9f));
            powerArcPaint.setColor(0xff0000ff);
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

            int br = (int)(255*(power-minPower)/(maxPower-minPower));
            int color = (0xff << 24) + (br << 16) + (br << 8) + br;
            halfCirclePaint.setColor(color);

            // draw half circle
            canvas.drawArc(rect, POWER_ARC_START, 180, false, halfCirclePaint);

            // draw power arc
            canvas.drawArc(rectArc, POWER_ARC_START, getArcEnd(), false, powerArcPaint);

            // draw power percentage
            canvas.drawText((int)((power-minPower)/(maxPower-minPower)*100) + "%", posx, posy, percentagePaint);
        }

        float getArcEnd() {
            return (int)(power != 0 ? (power-minPower)/(maxPower-minPower) * 180: 0);
        }
    }

    public interface Listener {
        void onPowerSet(int power);
    }
}
