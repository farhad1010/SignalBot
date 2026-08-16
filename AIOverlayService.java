package com.farhad.quotexsignal;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AIOverlayService extends Service {

    public static final String ACTION_AI_RESULT = "com.farhad.quotexsignal.AI_RESULT";
    private WindowManager windowManager;
    private LinearLayout panel;
    private TextView signalText, confidenceText, scoreText, reasonText;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!ACTION_AI_RESULT.equals(intent.getAction())) return;
            updatePanel(
                    intent.getStringExtra("signal"),
                    intent.getIntExtra("confidence", 0),
                    intent.getIntExtra("score", 0),
                    intent.getStringExtra("reason")
            );
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createPanel();
        IntentFilter filter = new IntentFilter(ACTION_AI_RESULT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    private TextView text(String value, float size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private void createPanel() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(22, 18, 22, 18);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(238, 18, 20, 27));
        bg.setCornerRadius(28f);
        bg.setStroke(1, Color.argb(100, 255, 255, 255));
        panel.setBackground(bg);

        TextView title = text("AI SIGNAL", 14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        signalText = text("WAIT", 30);
        signalText.setTypeface(null, android.graphics.Typeface.BOLD);
        confidenceText = text("Confidence  —", 13);
        scoreText = text("Strength  —/5", 13);
        reasonText = text("Scanning chart…", 12);
        reasonText.setGravity(Gravity.CENTER);
        reasonText.setPadding(4, 12, 4, 0);

        panel.addView(title, new LinearLayout.LayoutParams(-1, 32));
        panel.addView(signalText, new LinearLayout.LayoutParams(-1, 55));
        panel.addView(confidenceText, new LinearLayout.LayoutParams(-1, 26));
        panel.addView(scoreText, new LinearLayout.LayoutParams(-1, 26));
        panel.addView(reasonText, new LinearLayout.LayoutParams(-1, -2));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                330, WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 12;
        params.y = 110;
        windowManager.addView(panel, params);
    }

    private void updatePanel(String signal, int confidence, int score, String reason) {
        if (signal == null) signal = "WAIT";
        if (reason == null || reason.trim().isEmpty()) reason = "No clear setup detected.";
        signal = signal.toUpperCase();
        signalText.setText(signal);
        confidenceText.setText("Confidence  " + confidence + "%");
        scoreText.setText("Strength  " + score + "/5");
        reasonText.setText(reason);
        if ("BUY".equals(signal)) signalText.setTextColor(Color.rgb(35, 220, 130));
        else if ("SELL".equals(signal)) signalText.setTextColor(Color.rgb(255, 82, 92));
        else signalText.setTextColor(Color.WHITE);
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        if (panel != null && windowManager != null) {
            try { windowManager.removeView(panel); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
