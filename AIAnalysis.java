package com.farhad.quotexsignal;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAnalysis {

    public interface Callback {
        void onResult(String signal, int confidence, int score, String reason);
        void onError(String error);
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void analyze(String apiKey, Bitmap bitmap, Callback callback) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 68, stream);
                bitmap.recycle();
                String base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                JSONObject body = new JSONObject();
                body.put("model", "gpt-5.6");

                JSONArray input = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");

                JSONArray content = new JSONArray();
                JSONObject text = new JSONObject();
                text.put("type", "input_text");
                text.put("text", buildPrompt());
                content.put(text);

                JSONObject image = new JSONObject();
                image.put("type", "input_image");
                image.put("image_url", "data:image/jpeg;base64," + base64);
                content.put(image);

                message.put("content", content);
                input.put(message);
                body.put("input", input);

                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/responses")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String raw = response.body() == null ? "" : response.body().string();
                    if (!response.isSuccessful()) {
                        String detail = raw;
                        try {
                            JSONObject err = new JSONObject(raw);
                            detail = err.optJSONObject("error") != null
                                    ? err.getJSONObject("error").optString("message", raw)
                                    : raw;
                        } catch (Exception ignored) { }
                        postError(callback, "API " + response.code() + ": " + shorten(detail));
                        return;
                    }

                    JSONObject json = new JSONObject(raw);
                    String outputText = extractOutputText(json);
                    JSONObject ai = new JSONObject(cleanJson(outputText));

                    String signal = ai.optString("signal", "WAIT").toUpperCase(Locale.US);
                    if (!signal.equals("BUY") && !signal.equals("SELL") && !signal.equals("WAIT")) {
                        signal = "WAIT";
                    }

                    int confidence = clamp(ai.optInt("confidence", 0), 0, 100);
                    int score = clamp(ai.optInt("score", 0), 0, 5);
                    String reason = ai.optString("reason", "No clear setup detected.");

                    String finalSignal = signal;
                    mainHandler.post(() -> callback.onResult(finalSignal, confidence, score, reason));
                }
            } catch (Exception e) {
                postError(callback, e.getMessage() == null ? "Unknown error" : e.getMessage());
            }
        }).start();
    }

    private String buildPrompt() {
        return "You are a trading-chart vision assistant. Analyze ONLY what is visibly supported by the screenshot. " +
                "Identify the trading chart, candles, visible indicators, trend, momentum, support/resistance and price action. " +
                "Ignore app buttons, ads, account balances and unrelated UI. Do not invent hidden data. " +
                "Give a conservative short-term directional signal. If the chart is unclear, obstructed, too small, or evidence conflicts, return WAIT. " +
                "This is decision support, not a guarantee of profit. Return ONLY valid JSON, no markdown: " +
                "{\"signal\":\"BUY|SELL|WAIT\",\"confidence\":0,\"score\":0,\"reason\":\"short reason\"}. " +
                "confidence must be 0-100 and score 0-5. Never claim certainty or guaranteed profit.";
    }

    private String extractOutputText(JSONObject json) throws Exception {
        if (json.has("output_text")) {
            return json.optString("output_text", "");
        }

        JSONArray output = json.optJSONArray("output");
        if (output == null) throw new Exception("No AI output received");

        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item == null || !"message".equals(item.optString("type"))) continue;
            JSONArray content = item.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject part = content.optJSONObject(j);
                if (part != null && "output_text".equals(part.optString("type"))) {
                    return part.optString("text", "");
                }
            }
        }
        throw new Exception("No AI output received");
    }

    private String cleanJson(String text) throws Exception {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) throw new Exception("AI returned invalid JSON");
        return value.substring(start, end + 1);
    }

    private void postError(Callback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private String shorten(String text) {
        if (text == null) return "Unknown API error";
        text = text.replace('\n', ' ').trim();
        return text.length() > 180 ? text.substring(0, 180) : text;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
