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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter btAdapter;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private static final String TAG = "BluetoothActivity";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            var action = intent.getAction();
            if (Objects.equals(action, BluetoothDevice.ACTION_FOUND)) {
                var device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Toast.makeText(context, device.getName(), Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initDeviceSearch();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button skipBtn = findViewById(R.id.button3);
        skipBtn.setOnClickListener(v -> {
            var intent = new Intent(MainActivity.this, ControllerActivity.class);
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

            }
        );

        Button checkBtn = findViewById(R.id.validateCodeButton);
        checkBtn.setOnClickListener(v -> {
                @NotNull TextView codeField = findViewById(R.id.codeText);
                this.validateCode(codeField.getText().toString());
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                @NotNull TextView codeField = findViewById(R.id.codeText);
                codeField.setText(result.getContents());
                this.validateCode(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            Toast.makeText(this, "Scan failed", Toast.LENGTH_LONG).show();
        }
    }

    private void validateCode(String code) {
        // aici trebuie validat codul dat de placa
        if (code.length() != 6 || !code.matches("[0-9]+")) {
            var dialog = new AlertDialog.Builder(this);
            dialog.setMessage("Invalid code. Try again");
            dialog.setPositiveButton("Ok", null);
            dialog.setCancelable(true);
            dialog.show();
            return;
        }
        Toast.makeText(this, "Validating...", Toast.LENGTH_LONG).show();
    }

    private void initDeviceSearch() {
        final BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        this.btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionResult);

        List<String> reqPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            reqPermissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
            reqPermissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            reqPermissions.add(android.Manifest.permission.BLUETOOTH);
            reqPermissions.add(android.Manifest.permission.BLUETOOTH_ADMIN);
        }

        List<String> permissions = new ArrayList<>();
        for (var permission : reqPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission);
            }
        }

        if (!permissions.isEmpty())
            permissionLauncher.launch(permissions.toArray(new String[0]));
        else {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
            else {
                var bondedDevices = btAdapter.getBondedDevices();
                if (!bondedDevices.isEmpty()) {
                    for (BluetoothDevice device : bondedDevices) {
                        Toast.makeText(this, device.getName(), Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this, "No devices found", Toast.LENGTH_LONG).show();
                }
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }
                var filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(receiver, filter);
                btAdapter.startDiscovery();
            }
        }

    }
    private void handlePermissionResult(Map<String, Boolean> permissions) {
        boolean allGranted = true;
        for (var permission : permissions.entrySet()) {
            if (!permission.getValue()) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(enableBtIntent);
                var bondedDevices = btAdapter.getBondedDevices();
                if (!bondedDevices.isEmpty()) {
                    for (BluetoothDevice device : bondedDevices) {
                        Toast.makeText(this, device.getName(), Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this, "No devices found", Toast.LENGTH_LONG).show();
                }
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }
            }
            var filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);
            btAdapter.startDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}