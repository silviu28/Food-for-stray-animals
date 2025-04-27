package com.example.fooddispensercontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
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

        this.addDirectionListeners();
        this.addLightListeners();
        this.addSliderListeners();
        this.pollDevice();
    }

    private void addDirectionListeners() {
        Button upBtn = this.findViewById(R.id.buttonUp);
        Button dwnBtn = this.findViewById(R.id.buttonDown);
        Button lftBtn = this.findViewById(R.id.buttonLeft);
        Button rgtBtn = this.findViewById(R.id.buttonRight);
        Button cntBtn = this.findViewById(R.id.buttonCenter);

        upBtn.setOnClickListener(v -> this.connectedDevice.setDirection(Direction.UP, true));
        dwnBtn.setOnClickListener(v -> this.connectedDevice.setDirection(Direction.DOWN, true));
        lftBtn.setOnClickListener(v -> this.connectedDevice.setDirection(Direction.LEFT, true));
        rgtBtn.setOnClickListener(v -> this.connectedDevice.setDirection(Direction.RIGHT, true));
        cntBtn.setOnClickListener(v -> { /* ??? */});
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
                connectedDevice.setSteeringAngle((float) steerBar.getProgress());
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
                    }
                    else {
                        runOnUiThread(() ->
                        Toast.makeText(this, "Connection has been lost.", Toast.LENGTH_SHORT).show());
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
