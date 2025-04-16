package com.example.fooddispensercontroller;
public class Device {
    private boolean engineOn = false;
    private boolean tipperReleased = false;
    private double speed = 0.0;
    private float steeringAngle = 0.0f;
    private boolean emergencyLightsOn = false;
    private boolean brakesOn = false;
    private boolean headLightsOn = false;
    private String address;
    private boolean[] directions = new boolean[4];

    public Device(String address)
    {
        this.address = address;
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

    public boolean getDirection(Direction direction) {
        switch (direction) {
            case UP:
                return directions[0];
            case DOWN:
                return directions[1];
            case LEFT:
                return directions[2];
            case RIGHT:
                return directions[3];
            default:
                throw new IllegalArgumentException("Invalid direction");
        }
    }

    public void setEngineState(boolean engineOn) {
        this.engineOn = engineOn;
    }

    public void setTipperState(boolean tipperReleased) {
        this.tipperReleased = tipperReleased;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setSteeringAngle(float steeringAngle) {
        this.steeringAngle = steeringAngle;
    }

    public void setEmergencyLightsState(boolean emergencyLightsOn) {
        this.emergencyLightsOn = emergencyLightsOn;
    }

    public void setBrakesState(boolean brakesOn) {
        this.brakesOn = brakesOn;
    }

    public void setHeadLightsState(boolean headLightsOn) {
        this.headLightsOn = headLightsOn;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDirection(Direction direction, boolean value) {
        switch (direction) {
            case UP:
                directions[0] = value;
                break;
            case DOWN:
                directions[1] = value;
                break;
            case LEFT:
                directions[2] = value;
                break;
            case RIGHT:
                directions[3] = value;
                break;
            default:
                throw new IllegalArgumentException("Invalid direction");
        }
    }

}
