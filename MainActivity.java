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

    private EditText apiKey;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        apiKey = findViewById(R.id.apiKey);
        Button startButton =
                findViewById(R.id.startButton);

        status =
                findViewById(R.id.statusText);

        apiKey.setText(
                getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                ).getString(
                        "api_key",
                        ""
                )
        );

        startButton.setOnClickListener(v ->
                startAI()
        );
    }

    private void startAI() {

        String key =
                apiKey.getText()
                        .toString()
                        .trim();

        if (key.isEmpty()) {

            status.setText(
                    "Please enter OpenAI API key"
            );

            return;
        }

        getSharedPreferences(
                "settings",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "api_key",
                        key
                )
                .apply();

        if (!Settings.canDrawOverlays(this)) {

            status.setText(
                    "Allow Display over other apps"
            );

            Intent intent =
                    new Intent(
                            Settings
                                    .ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse(
                                    "package:" +
                                    getPackageName()
                            )
                    );

            startActivity(intent);

            return;
        }

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        status.setText(
                "Requesting screen capture..."
        );

        startActivityForResult(
                manager.createScreenCaptureIntent(),
                SCREEN_CAPTURE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode !=
                SCREEN_CAPTURE_REQUEST) {

            return;
        }

        if (resultCode != Activity.RESULT_OK ||
                data == null) {

            status.setText(
                    "Screen capture permission denied"
            );

            return;
        }

        Intent serviceIntent =
                new Intent(
                        this,
                        ScreenCaptureService.class
                );

        serviceIntent.putExtra(
                "resultCode",
                resultCode
        );

        serviceIntent.putExtra(
                "data",
                data
        );

        if (android.os.Build.VERSION.SDK_INT >= 26) {

            startForegroundService(
                    serviceIntent
            );

        } else {

            startService(
                    serviceIntent
            );
        }

        status.setText(
                "AI analysis is running..."
        );
    }
}
