/*
 * Copyright (C) 2021 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.robertogl.settingsextra;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.IBinder;
import android.os.UEventObserver;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyHandler extends Service {
    private AudioManager audioManager;
    private Vibrator vibrator;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String TriStatePath = "/sys/devices/virtual/switch/tri-state-key/state";

    private final UEventObserver alertSliderEventObserver = new UEventObserver() {
        private final Object lock = new Object();

        @Override
        public void onUEvent(UEvent event) {
            synchronized (lock) {
                String switchState = event.get("SWITCH_STATE");
                if (switchState != null) {
                    vibrateIfNeeded(Integer.parseInt(switchState) - 1);
                    handleMode(Integer.parseInt(switchState));
                }
            }
        }
    };

    @Override
    public void onCreate() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Set the status at boot following the slider position
        // Do this in case the user changes the slider position while the phone is off, for example
        // Also, we solve an issue regarding the media volume that was never mute at boot
        handleMode(Integer.parseInt(Utils.readFromFile(TriStatePath)));

        alertSliderEventObserver.startObserving("tri-state-key");
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
        }
    }

    private void handleMode(int position) {
        int mode = position - 1;

        executorService.submit(() -> {
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                audioManager.setRingerModeInternal(mode);
                audioManager.adjustVolume(AudioManager.ADJUST_MUTE, 0);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
            case AudioManager.RINGER_MODE_NORMAL:
                audioManager.setRingerModeInternal(mode);
                audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0);
                break;
        }
    });
    }
    private static final String TAG = "KeyHandler";

    // Vibration attributes
    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK);

    // Vibration effects
    private static final VibrationEffect MODE_SILENT_EFFECT = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE);
    private static final VibrationEffect MODE_VIBRATION_EFFECT = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE);
}
