/*
 * Copyright (C) 2021 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.robertogl.settingsextra;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.IBinder;
import android.os.UEventObserver;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyHandler extends Service {
    private AudioManager audioManager;
    private NotificationManager notificationManager;
    private Vibrator vibrator;
    private SharedPreferences sharedPreferences;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private boolean wasMuted = false;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int stream = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
            boolean state = intent.getBooleanExtra(AudioManager.EXTRA_STREAM_VOLUME_MUTED, false);
            if (stream == AudioSystem.STREAM_MUSIC && !state) {
                wasMuted = false;
            }
        }
    };

    private final UEventObserver alertSliderEventObserver = new UEventObserver() {
        private final Object lock = new Object();

        @Override
        public void onUEvent(UEvent event) {
            synchronized (lock) {
                String switchState = event.get("SWITCH_STATE");
                if (switchState != null) {
                    handleMode(Integer.parseInt(switchState));
                    return;
                }
                String state = event.get("STATE");
                if (state != null) {
                    boolean none = state.contains("USB=0");
                    boolean vibration = state.contains("HOST=0");
                    boolean silent = state.contains("null)=0");

                    if (none && !vibration && !silent) {
                        handleMode(POSITION_BOTTOM);
                    } else if (!none && vibration && !silent) {
                        Log.d(TAG, "middle");
                        handleMode(POSITION_MIDDLE);
                    } else if (!none && !vibration && silent) {
                        handleMode(POSITION_TOP);
                    }

                    return;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Context deviceProtectedContext = createDeviceProtectedStorageContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(deviceProtectedContext);

        registerReceiver(
                broadcastReceiver,
                new IntentFilter(AudioManager.STREAM_MUTE_CHANGED_ACTION)
        );
        alertSliderEventObserver.startObserving("tri-state-key");
        alertSliderEventObserver.startObserving("tri_state_key");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void vibrateIfNeeded(int mode) {
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                vibrator.vibrate(
                        MODE_SILENT_EFFECT,
                        HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES
                );
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                vibrator.vibrate(
                        MODE_VIBRATION_EFFECT,
                        HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES
            );
                break;
            /*case AudioManager.RINGER_MODE_NORMAL:
                vibrator.vibrate(
                        MODE_NORMAL_EFFECT,
                        HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES
            );
            break;*/
        }
    }

    private void handleMode(int position) {
        boolean muteMedia = sharedPreferences.getBoolean(MUTE_MEDIA_WITH_SILENT, true);

        int mode;
        switch (position) {
            case POSITION_TOP:
                mode = Integer.parseInt(sharedPreferences.getString(ALERT_SLIDER_TOP_KEY, "0"));
                break;
            case POSITION_MIDDLE:
                mode = Integer.parseInt(sharedPreferences.getString(ALERT_SLIDER_MIDDLE_KEY, "1"));
                break;
            case POSITION_BOTTOM:
                mode = Integer.parseInt(sharedPreferences.getString(ALERT_SLIDER_BOTTOM_KEY, "2"));
                break;
            default:
                return;
        }

        executorService.submit(() -> {
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                setZenMode(Settings.Global.ZEN_MODE_OFF);
                audioManager.setRingerModeInternal(mode);
                if (muteMedia) {
                    audioManager.adjustVolume(AudioManager.ADJUST_MUTE, 0);
                    wasMuted = true;
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
            case AudioManager.RINGER_MODE_NORMAL:
                setZenMode(Settings.Global.ZEN_MODE_OFF);
                audioManager.setRingerModeInternal(mode);
                if (muteMedia && wasMuted) {
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0);
                }
                break;
            case ZEN_PRIORITY_ONLY:
            case ZEN_TOTAL_SILENCE:
            case ZEN_ALARMS_ONLY:
                audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                setZenMode(mode - ZEN_OFFSET);
                if (muteMedia && wasMuted) {
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0);
                }
                break;
        }
        vibrateIfNeeded(mode);
    });
    }

    private void setZenMode(int zenMode) {
        // Set zen mode
        notificationManager.setZenMode(zenMode, null, TAG);

        // Wait until zen mode change is committed
        while (notificationManager.getZenMode() != zenMode) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static final String TAG = "KeyHandler";

    // Slider key positions
    private static final int POSITION_TOP = 1;
    private static final int POSITION_MIDDLE = 2;
    private static final int POSITION_BOTTOM = 3;

    // Preference keys
    private static final String ALERT_SLIDER_TOP_KEY = "config_top_position";
    private static final String ALERT_SLIDER_MIDDLE_KEY = "config_middle_position";
    private static final String ALERT_SLIDER_BOTTOM_KEY = "config_bottom_position";
    private static final String MUTE_MEDIA_WITH_SILENT = "config_mute_media";

    // ZEN constants
    private static final int ZEN_OFFSET = 2;
    private static final int ZEN_PRIORITY_ONLY = 3;
    private static final int ZEN_TOTAL_SILENCE = 4;
    private static final int ZEN_ALARMS_ONLY = 5;

    // Vibration attributes
    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK);

    // Vibration effects
    private static final VibrationEffect MODE_SILENT_EFFECT = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE);
    private static final VibrationEffect MODE_VIBRATION_EFFECT = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE);
}
