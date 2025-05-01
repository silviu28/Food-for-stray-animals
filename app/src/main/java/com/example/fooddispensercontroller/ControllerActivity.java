package com.example.fooddispensercontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
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

        try {
        var deviceAddress = getIntent().getStringExtra("deviceAddress");
        if (deviceAddress != null) {
                var device = adapter.getRemoteDevice(deviceAddress);
                var socket = device.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                );
                adapter.cancelDiscovery();
                socket.connect();
                this.connectedDevice = new Device(socket);
                Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            }
            } catch (IOException | NullPointerException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error connecting to device", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ControllerActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
                return;
            }


        Button reselectBtn = this.findViewById(R.id.selectDeviceButton);
        reselectBtn.setOnClickListener(v -> runOnUiThread(() -> {
            Intent intent = new Intent(ControllerActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }));

        this.addToggleListeners();
        this.configureJoystick();
        this.pollDevice();
    }

    private void addToggleListeners() {
        ImageButton headLightsBtn = this.findViewById(R.id.headlightsButton);
        ImageButton brakeBtn = this.findViewById(R.id.brakeButton);
        ImageButton emergencyBtn = this.findViewById(R.id.emergencyButton);
        ImageButton engineBtn = this.findViewById(R.id.engineButton);
        ImageButton tipperBtn = this.findViewById(R.id.tipperButton);
        ImageButton rightSignalBtn = this.findViewById(R.id.rightSignalButton);
        ImageButton leftSignalBtn = this.findViewById(R.id.leftSignalButton);

        headLightsBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleHeadlights();
            runOnUiThread(() -> {
                if (connectedDevice.getHeadLightsState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        emergencyBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleEmergencyLights();
            runOnUiThread(() -> {
                if (connectedDevice.getEmergencyLightsState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        brakeBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleBrakes();
            runOnUiThread(() -> {
                if (connectedDevice.getBrakesState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        engineBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleEngine();
            runOnUiThread(() -> {
                if (connectedDevice.getEngineState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else {
                    ((ImageButton) v).setColorFilter(Color.RED);
                    ((SeekBar) findViewById(R.id.speedSlider)).setProgress(0);
                }
            });
        });

        tipperBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleTipper();
            runOnUiThread(() -> {
                if (connectedDevice.getTipperState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        rightSignalBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleRightSignal();
            runOnUiThread(() -> {
                if (connectedDevice.getRightSignalState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        leftSignalBtn.setOnClickListener(v -> {
            this.connectedDevice.toggleLeftSignal();
            runOnUiThread(() -> {
                if (connectedDevice.getLeftSignalState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });
    }

    private void configureJoystick() {
        Joystick joystick = this.findViewById(R.id.joystick);
        joystick.setControlledDevice(this.connectedDevice);
        joystick.setOnMoveListener((steering, backward) -> {
            TextView steerText = findViewById(R.id.steeringText);
            TextView directionText = findViewById(R.id.directionText);

            runOnUiThread(() -> {
                steerText.setText("Steering: " + steering + "°");
                directionText.setText("Direction: " + (backward ? "Backward" : "Forward"));
            });
        });
        SeekBar speedbar = this.findViewById(R.id.speedSlider);
        speedbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                runOnUiThread(() -> {
                    TextView engineText = findViewById(R.id.engineText);
                    engineText.setText("Engine: " + progress + " km/h");
                });
                if (connectedDevice != null) {
                    connectedDevice.setSpeed(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
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
                    } else {
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
