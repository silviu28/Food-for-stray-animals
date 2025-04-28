package com.example.fooddispensercontroller;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class Device {
    private boolean engineOn = false;
    private boolean tipperReleased = false;
    private int speed = 0;
    private int steeringAngle = 0;
    private boolean emergencyLightsOn = false;
    private boolean brakesOn = false;
    private boolean headLightsOn = false;
    private String address;
    private boolean movingForward = true;
//    private boolean[] directions = new boolean[4];

    private final int HEADLIGHTS = 3001;
    private final int BRAKES = 3002;
    private final int EMERGENCY_LIGHTS = 3003;
    private final int LEFT_INDICATOR = 3004;
    private final int RIGHT_INDICATOR = 3005;
    private final int REVERSE = 3006;
    private final int TIPPER_SERVO = 3007;
    private final int MOTOR = 3008;
    private final int STEERING_RESET = 3009;
    private final int MOTOR_BASE = 1125;
    private final int STEERING_BASE = 2000;

    private BluetoothSocket socket;
    private OutputStream outputStream;

    public Device() { /* empty, for debug */ }
    public Device(BluetoothSocket socket) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.address = socket.getRemoteDevice().getAddress();
    }

    public void sendCommand(int command) {
        try {
            outputStream.write(
                    new byte[] {(byte)(command & 0xFF), (byte)((command >> 8) & 0xFF)}
            );
            outputStream.flush();
        } catch (IOException e) {
            Log.e("Device", "Error sending command", e);
        }
    }

    public boolean getEngineState() {
        return engineOn;
    }

    public boolean getTipperState() {
        return tipperReleased;
    }

    public double getSpeed() {
        return speed;
    }

    public float getSteeringAngle() {
        return steeringAngle;
    }

    public boolean getEmergencyLightsState() {
        return emergencyLightsOn;
    }

    public boolean getBrakesState() {
        return brakesOn;
    }

    public boolean getHeadLightsState() {
        return headLightsOn;
    }

    public String getAddress() {
        return address;
    }

//    public boolean getDirection(Direction direction) {
//        switch (direction) {
//            case UP:
//                return directions[0];
//            case DOWN:
//                return directions[1];
//            case LEFT:
//                return directions[2];
//            case RIGHT:
//                return directions[3];
//            default:
//                throw new IllegalArgumentException("Invalid direction");
//        }
//    }

    public boolean getMovingForward() {
        return movingForward;
    }

    public void setSpeed(int speed) {
        assert Math.abs(speed) <= 125;
        this.speed = speed;
        sendCommand(MOTOR_BASE + speed);
    }

    public void setSteeringAngle(int steeringAngle) {
        assert steeringAngle <= 180;
        this.steeringAngle = steeringAngle;
        sendCommand(STEERING_BASE + steeringAngle);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setMovingForward(boolean movingForward) {
        this.movingForward = movingForward;
    }

    public void toggleHeadlights() {
        headLightsOn = !headLightsOn;
        sendCommand(HEADLIGHTS);
    }

    public void toggleBrakes() {
        brakesOn = !brakesOn;
        sendCommand(BRAKES);
    }

    public void toggleEmergencyLights() {
        emergencyLightsOn = !emergencyLightsOn;
        sendCommand(EMERGENCY_LIGHTS);
    }

    public void toggleLeftIndicator() {
        sendCommand(LEFT_INDICATOR);
    }

    public void toggleRightIndicator() {
        sendCommand(RIGHT_INDICATOR);
    }

    public void toggleReverse() {
        sendCommand(REVERSE);
    }

    public void toggleTipper() {
        sendCommand(TIPPER_SERVO);
    }

    public void toggleMotor() {
        sendCommand(MOTOR); // motor should halt
    }

    public void resetSteering() {
        this.steeringAngle = 0;
        sendCommand(STEERING_RESET);
    }

}
