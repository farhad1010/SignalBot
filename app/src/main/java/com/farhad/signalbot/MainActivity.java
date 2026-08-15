package com.farhad.quotexsignal;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int SCREEN_CAPTURE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        EditText apiKey = findViewById(R.id.apiKey);
        Button startButton = findViewById(R.id.startButton);
        TextView status = findViewById(R.id.statusText);

        startButton.setOnClickListener(v -> {

            String key = apiKey.getText().toString().trim();

            if (key.isEmpty()) {
                status.setText("Enter OpenAI API key first");
                return;
            }

            getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putString("api_key", key)
                    .apply();

            if (!Settings.canDrawOverlays(this)) {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

                status.setText("Allow Display over other apps");
                return;
            }

            MediaProjectionManager manager =
                    (MediaProjectionManager)
                            getSystemService(MEDIA_PROJECTION_SERVICE);

            startActivityForResult(
                    manager.createScreenCaptureIntent(),
                    SCREEN_CAPTURE_REQUEST
            );
        });
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCREEN_CAPTURE_REQUEST
                && resultCode == Activity.RESULT_OK
                && data != null) {

            Intent serviceIntent =
                    new Intent(this, ScreenCaptureService.class);

            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("data", data);

            startForegroundService(serviceIntent);

            TextView status = findViewById(R.id.statusText);
            status.setText("AI screen analysis started");
        }
    }
}
