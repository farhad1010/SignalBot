package com.farhad.signalbot;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;

public class MainActivity extends Activity {

    private static final int SCREEN_CAPTURE_REQUEST = 1001;
    private LinearLayout root;
    private TextView signal;
    private TextView confidence;
    private TextView duration;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLogin();
    }

    private void showLogin() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("SignalBot AI");
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);

        EditText password = new EditText(this);
        password.setHint("Enter Password");
        password.setInputType(0x00000081);

        Button login = new Button(this);
        login.setText("ENTER");

        login.setOnClickListener(v -> {
            if (password.getText().toString().equals("123456")) {
                showDashboard();
            } else {
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
            }
        });

        root.addView(title);
        root.addView(password,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(login);

        setContentView(root);
    }

    private void showDashboard() {

        root.removeAllViews();

        TextView title = new TextView(this);
        title.setText("SignalBot AI");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("Status: Waiting for chart");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);

        signal = new TextView(this);
        signal.setText("NO SIGNAL");
        signal.setTextSize(30);
        signal.setGravity(Gravity.CENTER);
        signal.setPadding(0, 50, 0, 30);

        confidence = new TextView(this);
        confidence.setText("Confidence: --%");
        confidence.setTextSize(20);
        confidence.setGravity(Gravity.CENTER);

        duration = new TextView(this);
        duration.setText("Suggested duration: --");
        duration.setTextSize(20);
        duration.setGravity(Gravity.CENTER);

        Button capture = new Button(this);
        capture.setText("START LIVE CHART ANALYSIS");

        capture.setOnClickListener(v -> requestScreenCapture());

        root.addView(title);
        root.addView(status);
        root.addView(signal);
        root.addView(confidence);
        root.addView(duration);
        root.addView(capture);

        setContentView(root);
    }

    private void requestScreenCapture() {

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);

        Intent intent = manager.createScreenCaptureIntent();

        startActivityForResult(
                intent,
                SCREEN_CAPTURE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCREEN_CAPTURE_REQUEST) {

            if (resultCode == RESULT_OK && data != null) {

                status.setText("Status: Live chart capture enabled");
                signal.setText("ANALYZING...");
                confidence.setText("Confidence: Calculating...");
                duration.setText("Suggested duration: Calculating...");

            } else {

                status.setText("Screen capture permission denied");
            }
        }
    }
}
