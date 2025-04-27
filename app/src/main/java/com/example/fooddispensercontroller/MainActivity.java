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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.zxing.integration.android.IntentIntegrator;
//import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothActivity";
    private BluetoothAdapter btAdapter;
    private LinearLayout devicesContainer;
    private final List<String> foundDevices = new ArrayList<>();
    private final Handler scanHandler = new Handler();
    private boolean isScanning = false;

    // Bluetooth enable request launcher
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
                    String deviceInfo = device.getName() + "\n" + device.getAddress();
                    if (!foundDevices.contains(deviceInfo)) {
                        foundDevices.add(deviceInfo);
                        addDeviceToView(deviceInfo);
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

        devicesContainer = findViewById(R.id.devicesContainer);
        initBluetooth();

        Button skipBtn = findViewById(R.id.button3);
        skipBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControllerActivity.class);
            startActivity(intent);
        });

//        Button scanBtn = findViewById(R.id.scanCodeButton);
//        scanBtn.setOnClickListener(v -> startQrCodeScanner());
//
//        Button checkBtn = findViewById(R.id.validateCodeButton);
//        checkBtn.setOnClickListener(v -> {
//            TextView codeField = findViewById(R.id.codeText);
//            validateCode(codeField.getText().toString());
//        });
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
                    String deviceInfo = device.getName() + "\n" + device.getAddress();
                    if (!foundDevices.contains(deviceInfo)) {
                        foundDevices.add(deviceInfo);
                        addDeviceToView(deviceInfo);
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

    private void addDeviceToView(String deviceInfo) {
        runOnUiThread(() -> {
            TextView deviceView = new TextView(MainActivity.this);
            deviceView.setText(deviceInfo);
            deviceView.setPadding(32, 16, 32, 16);
            deviceView.setOnClickListener(v -> Toast.makeText(MainActivity.this, "Connecting to " + deviceInfo + "...", Toast.LENGTH_SHORT).show());
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

//    private void startQrCodeScanner() {
//        var integrator = new IntentIntegrator(this);
//        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
//        integrator.setPrompt("Scan the QR code shown on the device");
//        integrator.setCameraId(0);
//        integrator.setOrientationLocked(false);
//        integrator.setBeepEnabled(false);
//        integrator.setBarcodeImageEnabled(false);
//        integrator.setCaptureActivity(CaptureActivityAllOrientations.class);
//        integrator.initiateScan();
//    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (result != null) {
//            if (result.getContents() == null) {
//                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
//            } else {
//                TextView codeField = findViewById(R.id.codeText);
//                codeField.setText(result.getContents());
//                validateCode(result.getContents());
//            }
//        }
//    }

//    private void validateCode(String code) {
//        if (code.length() != 6 || !code.matches("[0-9]+")) {
//            new AlertDialog.Builder(this)
//                    .setMessage("Invalid code. It should be 6 digits.")
//                    .setPositiveButton("OK", null)
//                    .show();
//            return;
//        }
//        Toast.makeText(this, "Validating code...", Toast.LENGTH_SHORT).show();
//    }

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
}