package com.farhad.signalbot;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView signalText;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("SignalBot");
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);

        statusText = new TextView(this);
        statusText.setText("Ready");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);

        signalText = new TextView(this);
        signalText.setText("WAITING FOR SIGNAL");
        signalText.setTextSize(26);
        signalText.setGravity(Gravity.CENTER);
        signalText.setPadding(0, 50, 0, 50);

        Button upButton = new Button(this);
        upButton.setText("UP");

        Button downButton = new Button(this);
        downButton.setText("DOWN");

        Button waitButton = new Button(this);
        waitButton.setText("WAIT");

        upButton.setOnClickListener(v -> {
            signalText.setText("UP SIGNAL");
            statusText.setText("Signal detected");
        });

        downButton.setOnClickListener(v -> {
            signalText.setText("DOWN SIGNAL");
            statusText.setText("Signal detected");
        });

        waitButton.setOnClickListener(v -> {
            signalText.setText("WAITING FOR SIGNAL");
            statusText.setText("Ready");
        });

        layout.addView(title);
        layout.addView(statusText);
        layout.addView(signalText);
        layout.addView(upButton);
        layout.addView(downButton);
        layout.addView(waitButton);

        setContentView(layout);
    }
}
