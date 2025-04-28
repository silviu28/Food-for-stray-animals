package com.example.fooddispensercontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;

public class ControllerActivity extends AppCompatActivity {

    private Device connectedDevice;

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_controller);

        var adapter = BluetoothAdapter.getDefaultAdapter();

        var deviceAddress = getIntent().getStringExtra("deviceAddress");
        if (deviceAddress != null) {
            var device = adapter.getRemoteDevice(deviceAddress);
            try {
                var socket = device.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                );
                adapter.cancelDiscovery();
                socket.connect();
                this.connectedDevice = new Device(socket);
            } catch (IOException e) {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
            }
        }

        this.addLightListeners();
        this.addSliderListeners();
        this.pollDevice();

        Joystick joystick = this.findViewById(R.id.joystick);
        joystick.setControlledDevice(this.connectedDevice);
//        joystick.setOnMoveListener((steering, forward) -> {
//            TextView steerText = findViewById(R.id.steeringText);
//            TextView directionText = findViewById(R.id.directionText);
//
//            runOnUiThread(() -> {
//                steerText.setText(String.format("Steering %d", steering));
//                directionText.setText(String.format("Direction %s", forward ? "Forward" : "Backward"));
//            });
//        });
    }

    private void addLightListeners() {
        Button headLightsBtn = this.findViewById(R.id.headlightsButton);
        Button engineBtn = this.findViewById(R.id.ioEngineButton);
        Button brakeBtn = this.findViewById(R.id.brakeButton);
        Button emergencyBtn = this.findViewById(R.id.emergencyButton);
        Button tipperBtn = this.findViewById(R.id.tipperButton);

        headLightsBtn.setOnClickListener(v -> this.connectedDevice.toggleHeadlights());
        emergencyBtn.setOnClickListener(v -> this.connectedDevice.toggleEmergencyLights());
        engineBtn.setOnClickListener(v -> this.connectedDevice.toggleMotor());
        brakeBtn.setOnClickListener(v -> this.connectedDevice.toggleBrakes());
        tipperBtn.setOnClickListener(v -> this.connectedDevice.toggleTipper());
    }

    private void addSliderListeners() {
        SeekBar steerBar = this.findViewById(R.id.steeringAngleBar);
        steerBar.setMin(-180);
        steerBar.setMax(180);
        steerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                connectedDevice.setSteeringAngle(steerBar.getProgress());
                TextView steerText = findViewById(R.id.steeringText);
                steerText.setText("Steering: " + steerBar.getProgress() + "°");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        SeekBar speedBar = this.findViewById(R.id.engineSpeedBar);
        speedBar.setMin(0);
        speedBar.setMax(10);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                connectedDevice.setSpeed(speedBar.getProgress());
                TextView speedText = findViewById(R.id.engineText);
                speedText.setText("Engine: " + speedBar.getProgress() + " km/h");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void pollDevice() {
        new Thread(() -> {
            try {
                while (true) {
                    if (this.connectedDevice.getAddress() != null) {
                        Thread.sleep(1000);
                        runOnUiThread(() -> {
                            TextView deviceLabel = findViewById(R.id.deviceLabel);
                            deviceLabel.setText(this.connectedDevice.getAddress());
                        });
                    }
                    else {
                        runOnUiThread(() -> {
                            TextView deviceLabel = findViewById(R.id.deviceLabel);
                            deviceLabel.setText("Not Connected");
                            deviceLabel.setTextColor(Color.RED);
                        Toast.makeText(this, "Connection has been lost.", Toast.LENGTH_SHORT).show();
                        });

                        break;
                    }
                }
            } catch (InterruptedException e) {
                Log.e("ControllerActivity", "Device polling error", e);
            }
        }).start();
    }
}
