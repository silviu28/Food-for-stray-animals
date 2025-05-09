package com.example.fooddispensercontroller;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

public class Device {
    private boolean photoresistorOn = true;
    private boolean engineOn = false;
    private boolean tipperReleased = false;
    private int speed = 0;
    private int steeringAngle = 90;
    private boolean hazardLightsOn = false;
    private boolean brakesOn = false;
    private boolean headLightsOn = false;
    private String address;
    private boolean movingForward = true;
    private boolean rightSignalOn = false;
    private boolean leftSignalOn = false;

    private final int HEADLIGHTS = 3001;
    private final int BRAKES = 3002;
    private final int HAZARD_LIGHTS = 3003;
    private final int LEFT_INDICATOR = 3004;
    private final int RIGHT_INDICATOR = 3005;
    private final int REVERSE = 3006;
    private final int TIPPER_SERVO = 3007;
    private final int MOTOR = 3008;
    private final int STEERING_RESET = 3009;
    private final int MOTOR_BASE = 1000;
    private final int STEERING_BASE = 2000;
    private final int PHOTORESISTOR = 3000;

    private BluetoothSocket socket;
    private OutputStream outputStream;

    public Device(BluetoothSocket socket) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.address = socket.getRemoteDevice().getAddress();
    }

    public void sendCommand(int command) {
        try {
            outputStream.write(
                    new byte[]{(byte) (command & 0xFF), (byte) ((command >> 8) & 0xFF)}
            );
            Log.d("Device", "Sent command: " + command +
                    "States" + " Engine: " + engineOn +  " Speed: " + speed + " Steering: " + steeringAngle + "Moving forward:" + movingForward);
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

    public int getSpeed() {
        return speed;
    }

    public int getSteeringAngle() {
        return steeringAngle;
    }

    public boolean getEmergencyLightsState() {
        return hazardLightsOn;
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

    public void setSpeed(int speed) {
        assert speed >= 0 && speed <= 250;
        this.speed = speed;

        int command;
        if (movingForward) {
            command = 1000 + 125 + speed;
        } else {
            command = 1125 - speed;
        }

        sendCommand(command);
    }

    public void setSteeringAngle(int steeringAngle) {
        assert steeringAngle <= 180;
        this.steeringAngle = steeringAngle;
        sendCommand(STEERING_BASE + steeringAngle);
    }

    public void toggleHeadlights() {
        headLightsOn = !headLightsOn;
        sendCommand(HEADLIGHTS);
    }

    public void toggleBrakes() {
        brakesOn = !brakesOn;
        sendCommand(BRAKES);
    }

    public void toggleHazardLights() {
        hazardLightsOn = !hazardLightsOn;
        sendCommand(HAZARD_LIGHTS);
    }

    public void toggleReverse() {
        movingForward = !movingForward;
        sendCommand(REVERSE);
    }

    public void toggleTipper() {
        tipperReleased = !tipperReleased;
        sendCommand(TIPPER_SERVO);
    }

    public void toggleEngine() {
        engineOn = !engineOn;
        if (!engineOn) speed = 0;
        sendCommand(MOTOR);
    }

    public void resetSteering() {
        this.steeringAngle = 90;
        sendCommand(STEERING_RESET);
    }

    public boolean getForwardState() {
        return movingForward;
    }

    public void toggleRightSignal() {
        rightSignalOn = !rightSignalOn;
        sendCommand(RIGHT_INDICATOR);
    }

    public void toggleLeftSignal() {
        leftSignalOn = !leftSignalOn;
        sendCommand(LEFT_INDICATOR);
    }

    public boolean getRightSignalState() {
        return rightSignalOn;
    }

    public boolean getLeftSignalState() {
        return leftSignalOn;
    }

    public boolean getPhotoresistorState() {
        return photoresistorOn;
    }

    public void togglePhotoresistor() {
        photoresistorOn = !photoresistorOn;
        sendCommand(PHOTORESISTOR);
    }

}
