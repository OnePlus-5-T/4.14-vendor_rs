package org.robertogl.settingsextra;

import androidx.preference.PreferenceFragmentCompat;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceFragmentCompat;

import android.util.Log;

public class SettingsExtraFragment extends PreferenceFragmentCompat {

    private static String TAG = "SettingsExtraFragment";

    private static boolean DEBUG = MainService.DEBUG;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager currentManager = this.getPreferenceManager();
        currentManager.setStorageDeviceProtected();
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Preference buttonSwap = getPreferenceScreen().findPreference("buttonSwap");
        buttonSwap.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String key = preference.getKey();
                if (DEBUG) Log.d(TAG, key);
                if ((boolean) newValue) Utils.writeToFile(Utils.keySwapNode, "1", getActivity());
                else Utils.writeToFile(Utils.keySwapNode, "0", getActivity());
                return true;
            }
        });

        final Preference displayMode = getPreferenceScreen().findPreference("display_mode");
        displayMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String key = preference.getKey();
                String value = (String) newValue;
                if (DEBUG) Log.d(TAG, (String) newValue);
                if (value.equals("srgb"))
                    Utils.writeToFile(Utils.displayModeSRGBNode, "1", getActivity());
                else if (value.equals("dcip3"))
                    Utils.writeToFile(Utils.displayModeDCIP3Node, "1", getActivity());
                else {
                    Utils.writeToFile(Utils.displayModeSRGBNode, "0", getActivity());
                    Utils.writeToFile(Utils.displayModeDCIP3Node, "0", getActivity());
                }
                return true;
            }
        });

        final Preference advancedReboot = getPreferenceScreen().findPreference("advanced_reboot");
        advancedReboot.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference){
                if (DEBUG) Log.d(TAG, "Selected Advanced Reboot");
                AdvancedRebootExtra mAdvancedReboot = new AdvancedRebootExtra();
                mAdvancedReboot.showAdvancedRebootOptions(getActivity());
                return true;
            }
        });

        final Preference OTAUpdate = getPreferenceScreen().findPreference("ota_update");
        OTAUpdate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference){
                if (DEBUG) Log.d(TAG, "Selected Advanced Reboot");
                SettingsExtraOTAUpdate mOTAUpdate = new SettingsExtraOTAUpdate();
                mOTAUpdate.startUpdate(getActivity());
                return true;
            }
        });

        final Preference vibrationIntensity = getPreferenceScreen().findPreference("vibration_settings");
        vibrationIntensity.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference){
                if (DEBUG) Log.d(TAG, "Selected Vibration Settings");
                VibrationExtra mVibrationExtra = new VibrationExtra();
                mVibrationExtra.showVibrationOptions(getActivity());
                return true;
            }
        });
    }


}
