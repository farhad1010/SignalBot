package com.farhad.quotexsignal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    private static final String CHANNEL_ID =
            "AI_SCREEN_CAPTURE";

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private Handler handler;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private boolean processingFrame = false;

    private long lastAnalysisTime = 0;

    private static final long ANALYSIS_INTERVAL =
            5000;

    @Override
    public void onCreate() {
        super.onCreate();

        handler =
                new Handler(
                        Looper.getMainLooper()
                );

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "AI Trading Signal"
                        )
                        .setContentText(
                                "Live AI analysis is running"
                        )
                        .setSmallIcon(
                                android.R.drawable.ic_menu_view
                        )
                        .build();

        if (Build.VERSION.SDK_INT >= 29) {

            startForeground(
                    1,
                    notification,
                    ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );

        } else {

            startForeground(
                    1,
                    notification
            );
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        int resultCode =
                intent.getIntExtra(
                        "resultCode",
                        -1
                );

        Intent data =
                intent.getParcelableExtra(
                        "data"
                );

        if (resultCode == -1 ||
                data == null) {

            stopSelf();

            return START_NOT_STICKY;
        }

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        mediaProjection =
                manager.getMediaProjection(
                        resultCode,
                        data
                );

        startScreenCapture();

        startOverlay();

        return START_STICKY;
    }

    private void startScreenCapture() {

        android.util.DisplayMetrics metrics =
                getResources()
                        .getDisplayMetrics();

        screenWidth =
                metrics.widthPixels;

        screenHeight =
                metrics.heightPixels;

        screenDensity =
                metrics.densityDpi;

        imageReader =
                ImageReader.newInstance(
                        screenWidth,
                        screenHeight,
                        android.graphics.PixelFormat.RGBA_8888,
                        2
                );

        imageReader
                .setOnImageAvailableListener(
                        reader -> {

                            long now =
                                    System.currentTimeMillis();

                            if (processingFrame) {
                                return;
                            }

                            if (now -
                                    lastAnalysisTime <
                                    ANALYSIS_INTERVAL) {

                                return;
                            }

                            Image image =
                                    reader
                                            .acquireLatestImage();

                            if (image == null) {
                                return;
                            }

                            processingFrame =
                                    true;

                            lastAnalysisTime =
                                    now;

                            Bitmap bitmap =
                                    imageToBitmap(
                                            image
                                    );

                            image.close();

                            if (bitmap != null) {

                                analyzeFrame(
                                        bitmap
                                );

                            } else {

                                processingFrame =
                                        false;
                            }

                        },
                        handler
                );

        virtualDisplay =
                mediaProjection
                        .createVirtualDisplay(
                                "AITradingScreen",
                                screenWidth,
                                screenHeight,
                                screenDensity,
                                DisplayManager
                                        .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                imageReader
                                        .getSurface(),
                                null,
                                handler
                        );
    }

    private Bitmap imageToBitmap(
            Image image
    ) {

        Image.Plane[] planes =
                image.getPlanes();

        if (planes.length == 0) {
            return null;
        }

        ByteBuffer buffer =
                planes[0].getBuffer();

        int pixelStride =
                planes[0].getPixelStride();

        int rowStride =
                planes[0].getRowStride();

        int rowPadding =
                rowStride -
                        pixelStride *
                                screenWidth;

        int bitmapWidth =
                screenWidth +
                        rowPadding /
                                pixelStride;

        Bitmap bitmap =
                Bitmap.createBitmap(
                        bitmapWidth,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                );

        buffer.rewind();

        bitmap.copyPixelsFromBuffer(
                buffer
        );

        Bitmap cropped =
                Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        screenWidth,
                        screenHeight
                );

        bitmap.recycle();

        return cropped;
    }

    private void analyzeFrame(
            Bitmap bitmap
    ) {

        String apiKey =
                getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                )
                        .getString(
                                "api_key",
                                ""
                        );

        if (apiKey.isEmpty()) {

            bitmap.recycle();

            sendResult(
                    "WAIT",
                    0,
                    0,
                    "OpenAI API key is missing."
            );

            processingFrame = false;

            return;
        }

        AIAnalysis ai =
                new AIAnalysis();

        ai.analyze(
                apiKey,
                bitmap,
                new AIAnalysis.Callback() {

                    @Override
                    public void onResult(
                            String signal,
                            int confidence,
                            int score,
                            String reason
                    ) {

                        sendResult(
                                signal,
                                confidence,
                                score,
                                reason
                        );

                        processingFrame =
                                false;
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        sendResult(
                                "WAIT",
                                0,
                                0,
                                error
                        );

                        processingFrame =
                                false;
                    }
                }
        );
    }

    private void sendResult(
            String signal,
            int confidence,
            int score,
            String reason
    ) {

        Intent intent =
                new Intent(
                        AIOverlayService
                                .ACTION_AI_RESULT
                );

        intent.setPackage(
                getPackageName()
        );

        intent.putExtra(
                "signal",
                signal
        );

        intent.putExtra(
                "confidence",
                confidence
        );

        intent.putExtra(
                "score",
                score
        );

        intent.putExtra(
                "reason",
                reason
        );

        sendBroadcast(intent);
    }

    private void startOverlay() {

        if (Build.VERSION.SDK_INT >= 26) {

            startForegroundService(
                    new Intent(
                            this,
                            AIOverlayService.class
                    )
            );

        } else {

            startService(
                    new Intent(
                            this,
                            AIOverlayService.class
                    )
            );
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "AI Screen Capture",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    @Override
    public void onDestroy() {

        if (virtualDisplay != null) {

            virtualDisplay.release();

            virtualDisplay = null;
        }

        if (imageReader != null) {

            imageReader.close();

            imageReader = null;
        }

        if (mediaProjection != null) {

            mediaProjection.stop();

            mediaProjection = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
