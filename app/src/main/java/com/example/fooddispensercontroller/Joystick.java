package com.example.fooddispensercontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class Joystick extends SurfaceView implements SurfaceHolder.Callback {
    private float centerX, centerY;
    private float baseRadius, hatRadius;
    private float touchX, touchY;

    private Device controlledDevice;
    private long lastSteeringSent = 0;

    private long lastSpeedSent = 0;

    private static final int DEBOUNCE_MS = 100;

    public interface OnSteeringChangedListener {
        void onSteeringChanged(int angle);
    }

    private OnSteeringChangedListener onSteeringChangedListener;

    public Joystick(Context context) {
        super(context);
        init();
    }

    public Joystick(Context context, AttributeSet attributes) {
        super(context, attributes);
        init();
    }

    public Joystick(Context context, AttributeSet attributes, int style) {
        super(context, attributes, style);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        centerX = (float) getWidth() / 2;
        centerY = (float) getHeight() / 2;
        baseRadius = (float) Math.min(getWidth(), getHeight()) / 3;
        hatRadius = (float) Math.min(getWidth(), getHeight()) / 6;
        touchX = centerX;
        touchY = centerY;
        drawJoystick(centerX, centerY);
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                float dx = x - centerX;
                float dy = y - centerY;
                double angleRad = Math.atan2(-dy, dx);
                int angle = (int) Math.toDegrees(angleRad);
                angle = (angle + 360) % 360;
                if (angle > 180) angle = 360 - angle;

                if (System.currentTimeMillis() - lastSteeringSent > DEBOUNCE_MS) {
                    if (controlledDevice != null) {
                        controlledDevice.sendSteering(angle);
                        lastSteeringSent = System.currentTimeMillis();
                    }
                }

                touchX = x;
                touchY = y;
                drawJoystick(touchX, touchY);
                break;

            case MotionEvent.ACTION_UP:
                touchX = centerX;
                touchY = centerY;
                drawJoystick(centerX, centerY);

                if (controlledDevice != null) {
                    controlledDevice.sendSteering(90);
                }

                break;
        }

        return true;
    }

    private void drawJoystick(float newX, float newY) {
        if (getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            Paint paint = new Paint();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            paint.setColor(Color.GRAY);
            paint.setAlpha(100);
            canvas.drawCircle(centerX, centerY, baseRadius, paint);

            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
            canvas.drawCircle(newX, newY, hatRadius, paint);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public void setControlledDevice(Device device) {
        this.controlledDevice = device;
    }

    public void setOnSteeringChangedListener(OnSteeringChangedListener listener) {
        this.onSteeringChangedListener = listener;
    }

    public void onSpeedChanged(int sliderValue) {
        if (controlledDevice == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSpeedSent < 100) return;  // debounce: o comandă la 100ms

        lastSpeedSent = now;

        if (sliderValue == 125) {
            controlledDevice.setSpeed(0);
            return;
        }

        int speed = sliderValue < 125 ? 125 - sliderValue : sliderValue - 125;
        boolean forward = sliderValue >= 125;

        if (forward != controlledDevice.getForwardState()) {
            controlledDevice.toggleReverse();
        }
        controlledDevice.setSpeed(speed);
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
