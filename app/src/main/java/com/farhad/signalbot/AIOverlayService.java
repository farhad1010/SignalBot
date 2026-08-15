package com.farhad.quotexsignal;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AIOverlayService extends Service {

    private WindowManager windowManager;
    private LinearLayout panel;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(28, 24, 28, 24);
        panel.setBackgroundColor(Color.rgb(20, 20, 25));

        TextView title = new TextView(this);
        title.setText("AI TRADING SIGNAL");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setGravity(Gravity.CENTER);

        TextView signal = new TextView(this);
        signal.setText("WAIT");
        signal.setTextColor(Color.WHITE);
        signal.setTextSize(32);
        signal.setGravity(Gravity.CENTER);
        signal.setPadding(0, 20, 0, 10);

        TextView confidence = new TextView(this);
        confidence.setText("Confidence: --%");
        confidence.setTextColor(Color.LTGRAY);
        confidence.setTextSize(15);
        confidence.setGravity(Gravity.CENTER);

        TextView score = new TextView(this);
        score.setText("Score: --/5");
        score.setTextColor(Color.LTGRAY);
        score.setTextSize(15);
        score.setGravity(Gravity.CENTER);

        TextView reason = new TextView(this);
        reason.setText("Waiting for screen analysis...");
        reason.setTextColor(Color.WHITE);
        reason.setTextSize(13);
        reason.setPadding(0, 18, 0, 0);

        panel.addView(title);
        panel.addView(signal);
        panel.addView(confidence);
        panel.addView(score);
        panel.addView(reason);

        int type;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        330,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        params.x = 12;
        params.y = 0;

        windowManager.addView(panel, params);
    }

    @Override
    public void onDestroy() {
        if (panel != null && windowManager != null) {
            windowManager.removeView(panel);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
