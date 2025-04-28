package com.example.fooddispensercontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private boolean isTouched = false;
    private Device controlledDevice;
    public interface OnMoveListener {
        void onMove(float steering, boolean forward);
    }
    private OnMoveListener listener;

    public Joystick(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public Joystick(Context context, AttributeSet attributes) {
        super(context, attributes);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public Joystick(Context context, AttributeSet attributes, int style) {
        super(context, attributes, style);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        centerX = (float) getWidth() / 2;
        centerY = (float) getHeight() / 2;
        baseRadius = (float) Math.min(getWidth(), getHeight()) / 3;
        hatRadius = (float) Math.min(getWidth(), getHeight()) / 5;
        touchX = centerX;
        touchY = centerY;
        drawJoystick(centerX, centerY);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float distance = (float) Math.sqrt(Math.pow(event.getX() - centerX, 2) + Math.pow(event.getY() - centerY, 2));
                if (distance < baseRadius) {
                    touchX = event.getX();
                    touchY = event.getY();
                } else {
                    float ratio = baseRadius / distance;
                    touchX = centerX + (event.getX() - centerX) * ratio;
                    touchY = centerY + (event.getY() - centerY) * ratio;
                }
                isTouched = true;
                drawJoystick(touchX, touchY);
                break;

            case MotionEvent.ACTION_UP:
                touchX = centerX;
                touchY = centerY;
                isTouched = false;
                drawJoystick(touchX, touchY);
                break;
        }
        if (controlledDevice != null) {
            controlledDevice.setSteeringAngle(getSteeringAngle());
            controlledDevice.setMovingForward(isForward());
        }
        if (listener != null) {
            listener.onMove(getSteeringAngle(), isForward());
        }
        return true;
    }

    private void drawJoystick(float newX, float newY) {
        if (getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            Paint colors = new Paint();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            colors.setColor(Color.GRAY);
            colors.setAlpha(100);
            canvas.drawCircle(centerX, centerY, baseRadius, colors);

            colors.setColor(Color.WHITE);
            colors.setAlpha(255);
            canvas.drawCircle(newX, newY, hatRadius, colors);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public float getNormalizedX() {
        return (touchX - centerX) / baseRadius;
    }

    public float getNormalizedY() {
        return (touchY - centerY) / baseRadius;
    }

    public boolean isPressed() {
        return isTouched;
    }

    public int getSteeringAngle() {
        return (int)(getNormalizedX() * 90);
    }

    public boolean isForward() {
        return getNormalizedY() > 0;
    }

    public boolean isBackward() {
        return getNormalizedY() < 0;
    }

    public void setControlledDevice(Device device) {
        this.controlledDevice = device;
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.listener = listener;
    }

}
