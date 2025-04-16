package com.example.fooddispensercontroller;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button skipBtn = findViewById(R.id.button3);
        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                var intent = new Intent(MainActivity.this, ControllerActivity.class);
                startActivity(intent);
            }
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
}