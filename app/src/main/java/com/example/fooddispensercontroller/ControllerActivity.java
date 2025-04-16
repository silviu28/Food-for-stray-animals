package com.example.fooddispensercontroller;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ControllerActivity extends AppCompatActivity {

    private Device connectedDevice = new Device(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_controller);

        Button upBtn = this.findViewById(R.id.buttonUp);
        Button dwnBtn = this.findViewById(R.id.buttonDown);
        Button lftBtn = this.findViewById(R.id.buttonLeft);
        Button rgtBtn = this.findViewById(R.id.buttonRight);
    }
}
