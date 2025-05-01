package com.example.fooddispensercontroller;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.zxing.integration.android.IntentIntegrator;
//import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothActivity";
    private BluetoothAdapter btAdapter;
    private LinearLayout devicesContainer;
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();
    private final Handler scanHandler = new Handler();
    private boolean isScanning = false;

    private final ActivityResultLauncher<Intent> enableBtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startBluetoothDiscovery();
                } else {
                    Toast.makeText(this, "Bluetooth is required", Toast.LENGTH_SHORT).show();
                }
            });

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    showRejectMessage();

                if (device != null && device.getName() != null) {
                    if (!foundDevices.contains(device)) {
                        foundDevices.add(device);
                        addDeviceToView(device);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                scanHandler.postDelayed(() -> {
                    if (isScanning && btAdapter != null) {
                        btAdapter.startDiscovery();
                    }
                }, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureFlags();

        devicesContainer = findViewById(R.id.devicesContainer);
        initBluetooth();

        Button skipBtn = findViewById(R.id.button3);
        skipBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControllerActivity.class);
            startActivity(intent);
        });
    }

    private void initBluetooth() {
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (btManager == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }

        btAdapter = btManager.getAdapter();
        devicesContainer.removeAllViews();

        ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionResult);

        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissions.isEmpty()) {
            permissionLauncher.launch(permissions.toArray(new String[0]));
        } else {
            startBluetoothDiscovery();
        }
    }

    private void startBluetoothDiscovery() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
            return;
        }

        devicesContainer.removeAllViews();
        foundDevices.clear();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            for (BluetoothDevice device : btAdapter.getBondedDevices()) {
                if (device.getName() != null) {
                    if (!foundDevices.contains(device)) {
                        foundDevices.add(device);
                        addDeviceToView(device);
                    }
                }
            }
        }

        isScanning = true;
        btAdapter.startDiscovery();
    }

    private void stopBluetoothDiscovery() {
        isScanning = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            showRejectMessage();

        if (btAdapter != null && btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void addDeviceToView(BluetoothDevice device) {
        runOnUiThread(() -> {
            TextView deviceView = new TextView(MainActivity.this);
            deviceView.setText(device.getName() + " " + device.getAddress());
            deviceView.setPadding(32, 16, 32, 16);
            deviceView.setOnClickListener(v -> connect(device.getAddress()));
            devicesContainer.addView(deviceView);

        });
    }

    private void handlePermissionResult(Map<String, Boolean> permissions) {
        boolean allGranted = true;
        for (Boolean granted : permissions.values()) {
            if (!granted) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startBluetoothDiscovery();
        } else {
            showRejectMessage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            showRejectMessage();

        if (isScanning && btAdapter != null && !btAdapter.isDiscovering()) {
            btAdapter.startDiscovery();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBluetoothDiscovery();
        scanHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBluetoothDiscovery();
        scanHandler.removeCallbacksAndMessages(null);
    }

    private void showRejectMessage() {
        Toast.makeText(this, "Nearby devices permission denied." +
                " Enable when prompted", Toast.LENGTH_LONG).show();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connect(String deviceAddress) {
        var device = btAdapter.getRemoteDevice(deviceAddress);
        runOnUiThread(() -> {
            Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, ControllerActivity.class);
            intent.putExtra("deviceAddress", deviceAddress);
            startActivity(intent);
            finish();
        });
    }

    private void configureFlags() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }
}