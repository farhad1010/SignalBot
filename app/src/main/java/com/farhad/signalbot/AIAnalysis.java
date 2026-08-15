package com.farhad.quotexsignal;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    private final OkHttpClient client = new OkHttpClient();

    public void analyze(
            String apiKey,
            Bitmap bitmap,
            Callback callback
    ) {

        new Thread(() -> {

            try {

                ByteArrayOutputStream stream =
                        new ByteArrayOutputStream();

                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        70,
                        stream
                );

                String base64 =
                        Base64.encodeToString(
                                stream.toByteArray(),
                                Base64.NO_WRAP
                        );

                JSONObject body = new JSONObject();

                body.put("model", "gpt-5.6");

                JSONArray input = new JSONArray();

                JSONObject message = new JSONObject();

                message.put("role", "user");

                JSONArray content = new JSONArray();

                JSONObject text = new JSONObject();

                text.put("type", "input_text");

                text.put(
                        "text",
                        "Analyze this trading screen. " +
                        "Return ONLY JSON with these fields: " +
                        "signal (BUY, SELL or WAIT), " +
                        "confidence (0-100), " +
                        "score (0-5), " +
                        "reason (short explanation). " +
                        "Do not claim certainty or guaranteed profit."
                );

                content.put(text);

                JSONObject image = new JSONObject();

                image.put(
                        "type",
                        "input_image"
                );

                image.put(
                        "image_url",
                        "data:image/jpeg;base64," + base64
                );

                content.put(image);

                message.put("content", content);

                input.put(message);

                body.put("input", input);

                Request request =
                        new Request.Builder()
                                .url(
                                        "https://api.openai.com/v1/responses"
                                )
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + apiKey
                                )
                                .addHeader(
                                        "Content-Type",
                                        "application/json"
                                )
                                .post(
                                        RequestBody.create(
                                                body.toString(),
                                                MediaType.parse(
                                                        "application/json"
                                                )
                                        )
                                )
                                .build();

                Response response =
                        client.newCall(request).execute();

                if (!response.isSuccessful()) {

                    callback.onError(
                            "AI API error: " +
                            response.code()
                    );

                    return;
                }

                String result =
                        response.body().string();

                JSONObject json =
                        new JSONObject(result);

                String outputText =
                        extractOutputText(json);

                JSONObject ai =
                        new JSONObject(outputText);

                String signal =
                        ai.optString(
                                "signal",
                                "WAIT"
                        );

                int confidence =
                        ai.optInt(
                                "confidence",
                                0
                        );

                int score =
                        ai.optInt(
                                "score",
                                0
                        );

                String reason =
                        ai.optString(
                                "reason",
                                "No explanation"
                        );

                new Handler(
                        Looper.getMainLooper()
                ).post(() ->
                        callback.onResult(
                                signal,
                                confidence,
                                score,
                                reason
                        )
                );

            } catch (Exception e) {

                new Handler(
                        Looper.getMainLooper()
                ).post(() ->
                        callback.onError(
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    private String extractOutputText(
            JSONObject json
    ) throws Exception {

        JSONArray output =
                json.getJSONArray("output");

        for (int i = 0; i < output.length(); i++) {

            JSONObject item =
                    output.getJSONObject(i);

            if (!"message".equals(
                    item.optString("type")
            )) {
                continue;
            }

            JSONArray content =
                    item.getJSONArray("content");

            for (int j = 0; j < content.length(); j++) {

                JSONObject part =
                        content.getJSONObject(j);

                if ("output_text".equals(
                        part.optString("type")
                )) {

                    return part.getString("text");
                }
            }
        }

        throw new IOException(
                "No AI output received"
        );
    }
}
