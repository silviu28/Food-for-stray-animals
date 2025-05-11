package com.example.fooddispensercontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
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
    private boolean joystickTouched = false;

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_controller);
        configureFlags();

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
        ImageButton photoresistorBtn = this.findViewById(R.id.photoresistorButton);

        engineBtn.setColorFilter(Color.GREEN);
        photoresistorBtn.setColorFilter(Color.GREEN);

        headLightsBtn.setOnClickListener(v -> {
            connectedDevice.toggleHeadlights();

            runOnUiThread(() -> {
                if (connectedDevice.getHeadLightsState()) {
                    ((ImageButton) v).setColorFilter(Color.GREEN);

                    if (connectedDevice.getPhotoresistorState()) {
                        connectedDevice.togglePhotoresistor();
                        photoresistorBtn.setColorFilter(Color.RED);
                    }
                } else {
                    ((ImageButton) v).setColorFilter(Color.RED);
                }
            });
        });

        emergencyBtn.setOnClickListener(v -> {
            connectedDevice.toggleHazardLights();
            runOnUiThread(() -> {
                if (connectedDevice.getEmergencyLightsState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        brakeBtn.setOnClickListener(v -> {
            connectedDevice.toggleBrakes();
            runOnUiThread(() -> {
                if (connectedDevice.getBrakesState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        engineBtn.setOnClickListener(v -> {
            connectedDevice.toggleEngine();
            runOnUiThread(() -> {
                if (connectedDevice.getEngineState()) {
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                } else {
                    ((ImageButton) v).setColorFilter(Color.RED);
                    ((SeekBar) findViewById(R.id.speedSlider)).setProgress(125);
                }
            });
        });

        tipperBtn.setOnClickListener(v -> {
            connectedDevice.toggleTipper();
            runOnUiThread(() -> {
                if (connectedDevice.getTipperState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        rightSignalBtn.setOnClickListener(v -> {
            connectedDevice.toggleRightSignal();
            runOnUiThread(() -> {
                if (connectedDevice.getRightSignalState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        leftSignalBtn.setOnClickListener(v -> {
            connectedDevice.toggleLeftSignal();
            runOnUiThread(() -> {
                if (connectedDevice.getLeftSignalState())
                    ((ImageButton) v).setColorFilter(Color.GREEN);
                else
                    ((ImageButton) v).setColorFilter(Color.RED);
            });
        });

        photoresistorBtn.setOnClickListener(v -> {
            connectedDevice.togglePhotoresistor();

            runOnUiThread(() -> {
                if (connectedDevice.getPhotoresistorState()) {
                    ((ImageButton) v).setColorFilter(Color.GREEN);

                    if (connectedDevice.getHeadLightsState()) {
                        connectedDevice.toggleHeadlights();
                        headLightsBtn.setColorFilter(Color.RED);
                    }
                } else {
                    ((ImageButton) v).setColorFilter(Color.RED);
                }
            });
        });
    }

    private void configureJoystick() {
        Joystick joystick = this.findViewById(R.id.joystick);
        joystick.setControlledDevice(this.connectedDevice);

        joystick.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    joystickTouched = true;
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    joystickTouched = false;
                    connectedDevice.sendSteering(90);
                    connectedDevice.setSpeed(0);
                    break;
            }
            return false;
        });

        joystick.setOnSteeringChangedListener(angle -> {
            TextView steerText = findViewById(R.id.steeringText);
            steerText.setText("Steering: " + angle + "°");
        });


        final Handler handler = new Handler();
        final int[] latestSteering = {90};
        final Runnable[] steeringRunnable = {null};

        joystick.setOnSteeringChangedListener(steering -> {
            latestSteering[0] = steering;
            if (steeringRunnable[0] != null)
                handler.removeCallbacks(steeringRunnable[0]);

            steeringRunnable[0] = () -> {
                if (joystickTouched && connectedDevice.getEngineState()) {
                    connectedDevice.sendSteering(latestSteering[0]);
                }
            };
            handler.postDelayed(steeringRunnable[0], 100);
        });

        SeekBar speedbar = this.findViewById(R.id.speedSlider);
        speedbar.setMax(250);
        speedbar.setProgress(125);
        speedbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                joystick.onSpeedChanged(progress);

                runOnUiThread(() -> {
                    TextView engineText = findViewById(R.id.engineText);
                    TextView directionText = findViewById(R.id.directionText);

                    if (progress > 125) {
                        engineText.setText("Engine: " + (progress - 125) + " km/h");
                        directionText.setText("Direction: Forward");
                    } else if (progress < 125) {
                        engineText.setText("Engine: " + (125 - progress) + " km/h");
                        directionText.setText("Direction: Backward");
                    } else {
                        engineText.setText("Engine: 0 km/h");
                        directionText.setText("Direction: Forward");
                    }
                });
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
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

    private void configureFlags() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}
