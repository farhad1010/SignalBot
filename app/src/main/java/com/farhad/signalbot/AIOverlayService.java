package com.farhad.quotexsignal;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AIOverlayService extends Service {

    public static final String ACTION_AI_RESULT =
            "com.farhad.quotexsignal.AI_RESULT";

    private WindowManager windowManager;
    private LinearLayout panel;

    private TextView signalText;
    private TextView confidenceText;
    private TextView scoreText;
    private TextView reasonText;

    private final BroadcastReceiver receiver =
            new BroadcastReceiver() {

        @Override
        public void onReceive(
                Context context,
                Intent intent) {

            if (!ACTION_AI_RESULT.equals(intent.getAction())) {
                return;
            }

            String signal =
                    intent.getStringExtra("signal");

            int confidence =
                    intent.getIntExtra("confidence", 0);

            int score =
                    intent.getIntExtra("score", 0);

            String reason =
                    intent.getStringExtra("reason");

            updatePanel(
                    signal,
                    confidence,
                    score,
                    reason
            );
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        createPanel();

        IntentFilter filter =
                new IntentFilter(ACTION_AI_RESULT);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(receiver, filter);
        }
    }

    private void createPanel() {

        windowManager =
                (WindowManager)
                        getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);

        panel.setOrientation(
                LinearLayout.VERTICAL
        );

        panel.setPadding(
                28,
                24,
                28,
                24
        );

        panel.setBackgroundColor(
                Color.rgb(20, 20, 25)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "AI TRADING SIGNAL"
        );

        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setGravity(Gravity.CENTER);

        signalText =
                new TextView(this);

        signalText.setText("WAIT");
        signalText.setTextColor(Color.WHITE);
        signalText.setTextSize(32);
        signalText.setGravity(Gravity.CENTER);

        confidenceText =
                new TextView(this);

        confidenceText.setText(
                "Confidence: --%"
        );

        confidenceText.setTextColor(
                Color.LTGRAY
        );

        confidenceText.setTextSize(15);
        confidenceText.setGravity(Gravity.CENTER);

        scoreText =
                new TextView(this);

        scoreText.setText(
                "Score: --/5"
        );

        scoreText.setTextColor(
                Color.LTGRAY
        );

        scoreText.setTextSize(15);
        scoreText.setGravity(Gravity.CENTER);

        reasonText =
                new TextView(this);

        reasonText.setText(
                "Waiting for AI analysis..."
        );

        reasonText.setTextColor(
                Color.WHITE
        );

        reasonText.setTextSize(13);
        reasonText.setPadding(
                0,
                18,
                0,
                0
        );

        panel.addView(title);
        panel.addView(signalText);
        panel.addView(confidenceText);
        panel.addView(scoreText);
        panel.addView(reasonText);

        int type;

        if (Build.VERSION.SDK_INT >= 26) {
            type =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;
        } else {
            type =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        330,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.RIGHT |
                Gravity.CENTER_VERTICAL;

        params.x = 12;
        params.y = 0;

        windowManager.addView(
                panel,
                params
        );
    }

    private void updatePanel(
            String signal,
            int confidence,
            int score,
            String reason
    ) {

        if (signal == null) {
            signal = "WAIT";
        }

        if (reason == null) {
            reason = "No explanation";
        }

        signalText.setText(
                signal.toUpperCase()
        );

        confidenceText.setText(
                "Confidence: " +
                confidence +
                "%"
        );

        scoreText.setText(
                "Score: " +
                score +
                "/5"
        );

        reasonText.setText(reason);

        if ("BUY".equalsIgnoreCase(signal)) {

            signalText.setTextColor(
                    Color.rgb(0, 220, 120)
            );

        } else if ("SELL".equalsIgnoreCase(signal)) {

            signalText.setTextColor(
                    Color.rgb(255, 80, 80)
            );

        } else {

            signalText.setTextColor(
                    Color.WHITE
            );
        }
    }

    @Override
    public void onDestroy() {

        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }

        if (panel != null &&
                windowManager != null) {

            windowManager.removeView(panel);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
