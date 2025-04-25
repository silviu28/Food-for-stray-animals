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
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothActivity";
    private BluetoothAdapter btAdapter;

    private LinearLayout devicesContainer;
    private final List<String> foundDevices = new ArrayList<>();

    private final Handler scanHandler = new Handler();
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (btAdapter != null && btAdapter.isEnabled()) {
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }
                btAdapter.startDiscovery();
            }
            scanHandler.postDelayed(this, 5000);
        }
    };

    private boolean isReceiverRegistered = false;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                if (device != null && device.getName() != null) {
                    String deviceInfo = device.getName() + "\n" + device.getAddress();
                    if (foundDevices.contains(deviceInfo)) return;
                    foundDevices.add(deviceInfo);

                    runOnUiThread(() -> {
                        Button deviceView = new Button(MainActivity.this);
                        deviceView.setText(deviceInfo);
                        deviceView.setPadding(32, 16, 32, 16);
                        devicesContainer.addView(deviceView);
                        deviceView.setOnClickListener(v -> {
                            Toast.makeText(MainActivity.this, "Connecting to " + deviceInfo, Toast.LENGTH_LONG).show();
                        });
                    });
                }
            }
        }
    };

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    startBluetoothDiscovery(); // Resume discovery if Bluetooth just turned on
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicesContainer = findViewById(R.id.devicesContainer);
        initDeviceSearch();

        Button skipBtn = findViewById(R.id.button3);
        skipBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControllerActivity.class);
            startActivity(intent);
        });

        Button scanBtn = findViewById(R.id.scanCodeButton);
        scanBtn.setOnClickListener(v -> {
            var integrator = new IntentIntegrator(MainActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan the QR code shown on the device");
            integrator.setCameraId(0);
            integrator.setOrientationLocked(false);
            integrator.setBeepEnabled(false);
            integrator.setBarcodeImageEnabled(false);
            integrator.setCaptureActivity(CaptureActivityAllOrientations.class);
            integrator.initiateScan();
        });

        Button checkBtn = findViewById(R.id.validateCodeButton);
        checkBtn.setOnClickListener(v -> {
            TextView codeField = findViewById(R.id.codeText);
            validateCode(codeField.getText().toString());
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                TextView codeField = findViewById(R.id.codeText);
                codeField.setText(result.getContents());
                validateCode(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            Toast.makeText(this, "Scan failed", Toast.LENGTH_LONG).show();
        }
    }

    private void validateCode(String code) {
        if (code.length() != 6 || !code.matches("[0-9]+")) {
            new AlertDialog.Builder(this)
                    .setMessage("Invalid code. Try again")
                    .setPositiveButton("Ok", null)
                    .show();
            return;
        }
        Toast.makeText(this, "Validating...", Toast.LENGTH_LONG).show();
    }

    private void initDeviceSearch() {
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

        List<String> reqPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            reqPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            reqPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            reqPermissions.add(Manifest.permission.BLUETOOTH);
            reqPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        List<String> permissions = new ArrayList<>();
        for (String permission : reqPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission);
            }
        }

        if (!permissions.isEmpty()) {
            permissionLauncher.launch(permissions.toArray(new String[0]));
        } else {
            startBluetoothDiscovery();
        }
    }

    private void startBluetoothDiscovery() {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            return;
        }

        devicesContainer.removeAllViews();
        foundDevices.clear();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        for (BluetoothDevice device : btAdapter.getBondedDevices()) {
            if (device.getName() != null) {
                String deviceInfo = device.getName() + "\n" + device.getAddress();
                if (!foundDevices.contains(deviceInfo)) {
                    foundDevices.add(deviceInfo);
                    TextView deviceView = new TextView(MainActivity.this);
                    deviceView.setText(deviceInfo);
                    deviceView.setPadding(32, 16, 32, 16);
                    devicesContainer.addView(deviceView);
                }
            }
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        isReceiverRegistered = true;

        btAdapter.startDiscovery();

        scanHandler.removeCallbacks(scanRunnable);
        scanHandler.post(scanRunnable);
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
            Toast.makeText(this, "Permissions denied, cannot use Bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (btAdapter != null && btAdapter.isDiscovering()) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
            isReceiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanHandler.removeCallbacks(scanRunnable);

        try {
            unregisterReceiver(btStateReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering btStateReceiver", e);
        }

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(receiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanHandler.removeCallbacks(scanRunnable);

        try {
            unregisterReceiver(btStateReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering btStateReceiver", e);
        }

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(receiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
    }
}
