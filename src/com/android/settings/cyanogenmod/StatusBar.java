/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.MSimTelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

import com.android.internal.util.slim.DeviceUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBar";

    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String STATUS_BAR_CUSTOM_HEADER = "custom_status_bar_header";
    private static final String STATUS_BAR_BRIGHTNESS = "statusbar_brightness_slider";
    private static final String PREF_CUSTOM_STATUS_BAR_COLOR = "custom_status_bar_color";
    private static final String PREF_STATUS_BAR_OPAQUE_COLOR = "status_bar_opaque_color";
    private static final String PREF_CUSTOM_SYSTEM_ICON_COLOR = "custom_system_icon_color";
    private static final String PREF_SYSTEM_ICON_COLOR = "system_icon_color";
    private static final String SMS_BREATH = "sms_breath";
    private static final String MISSED_CALL_BREATH = "missed_call_breath";
    private static final String VOICEMAIL_BREATH = "voicemail_breath";
    private static final String STATUS_BAR_NETWORK_STATS_TEXT_COLOR = "status_bar_network_stats_text_color";
    private static final String SHOW_LTE_OR_FOURGEE = "show_lte_or_fourgee";

    private static final String STATUS_BAR_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_STYLE_TEXT = "6";

    private CheckBoxPreference mCustomBarColor;
    private CheckBoxPreference mShowLteOrFourgee;
    private CheckBoxPreference mStatusbarSliderPreference;
    private ColorPickerPreference mBarOpaqueColor;
    private CheckBoxPreference mCustomIconColor;
    private ColorPickerPreference mIconColor;
    private ColorPickerPreference mStatusBarNetworkStatsTextColor;
    private ListPreference mSignalStyle;
    private CheckBoxPreference mStatusBarCustomHeader;
    private CheckBoxPreference mSMSBreath;
    private CheckBoxPreference mMissedCallBreath;
    private CheckBoxPreference mVoicemailBreath;

    private ContentObserver mSettingsObserver;

   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.status_bar);

        prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        mCustomBarColor = (CheckBoxPreference) prefSet.findPreference(PREF_CUSTOM_STATUS_BAR_COLOR);
        mCustomBarColor.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CUSTOM_STATUS_BAR_COLOR, 0) == 1);

        mStatusbarSliderPreference = (CheckBoxPreference) findPreference(STATUS_BAR_BRIGHTNESS);
        mStatusbarSliderPreference.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUSBAR_BRIGHTNESS_SLIDER, 0) == 1));

        mCustomIconColor = (CheckBoxPreference) prefSet.findPreference(PREF_CUSTOM_SYSTEM_ICON_COLOR);
        mCustomIconColor.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CUSTOM_SYSTEM_ICON_COLOR, 0) == 1);

        mBarOpaqueColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_OPAQUE_COLOR);
        mBarOpaqueColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_OPAQUE_COLOR, 0xff000000);
        mBarOpaqueColor.setSummary(getResources().getString(R.string.default_string));
        if (intColor == 0xff000000) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/system_bar_background_opaque", null, null));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mBarOpaqueColor.setSummary(hexColor);
        }
        mBarOpaqueColor.setNewPreviewColor(intColor);

        mIconColor = (ColorPickerPreference) findPreference(PREF_SYSTEM_ICON_COLOR);
        mIconColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_ICON_COLOR, -1);
        mIconColor.setSummary(getResources().getString(R.string.default_string));
        if (intColor == 0xffffffff) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/status_bar_clock_color", null, null));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mIconColor.setSummary(hexColor);
        }
        mIconColor.setNewPreviewColor(intColor);

        mSignalStyle = (ListPreference) prefSet.findPreference(STATUS_BAR_SIGNAL);
        int signalStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mSignalStyle.setValue(String.valueOf(signalStyle));
        mSignalStyle.setSummary(mSignalStyle.getEntry());
        mSignalStyle.setOnPreferenceChangeListener(this);

        if (Utils.isWifiOnly(getActivity())) {
            prefSet.removePreference(mSignalStyle);
        }

        mShowLteOrFourgee = (CheckBoxPreference) prefSet.findPreference(SHOW_LTE_OR_FOURGEE);
        if (!DeviceUtils.deviceSupportsLte(getActivity().getApplicationContext())) {
            prefSet.removePreference(mShowLteOrFourgee);
        }

        mStatusBarNetworkStatsTextColor = (ColorPickerPreference) findPreference(STATUS_BAR_NETWORK_STATS_TEXT_COLOR);
        mStatusBarNetworkStatsTextColor.setOnPreferenceChangeListener(this);
        int intNetworkColor = Settings.System.getInt(getActivity().getContentResolver(),
                 Settings.System.STATUS_BAR_NETWORK_STATS_TEXT_COLOR, -2);
        if (intNetworkColor == -2) {
              intNetworkColor = getResources().getColor(
                    com.android.internal.R.color.holo_blue_light);
                    mStatusBarNetworkStatsTextColor.setSummary(getResources().getString(R.string.color_default));
        } else {
              hexColor = String.format("#%08x", (0xffffffff & intColor));
              mStatusBarNetworkStatsTextColor.setSummary(hexColor);
        }
        mStatusBarNetworkStatsTextColor.setNewPreviewColor(intNetworkColor);

        mStatusBarCustomHeader = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_CUSTOM_HEADER);
        mStatusBarCustomHeader.setChecked(Settings.System.getInt(resolver,
            Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1);
        mStatusBarCustomHeader.setOnPreferenceChangeListener(this);

        mSMSBreath = (CheckBoxPreference) findPreference(SMS_BREATH);
        mSMSBreath.setChecked(Settings.System.getInt(resolver,
                Settings.System.KEY_SMS_BREATH, 0) == 1);
        mSMSBreath.setOnPreferenceChangeListener(this);

        mMissedCallBreath = (CheckBoxPreference) findPreference(MISSED_CALL_BREATH);
        mMissedCallBreath.setChecked(Settings.System.getInt(resolver,
                Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1);
        mMissedCallBreath.setOnPreferenceChangeListener(this);

        mVoicemailBreath = (CheckBoxPreference) findPreference(VOICEMAIL_BREATH);
        mVoicemailBreath.setChecked(Settings.System.getInt(resolver,
                Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1);
        mVoicemailBreath.setOnPreferenceChangeListener(this);

        if (Utils.isWifiOnly(getActivity())
                || (MSimTelephonyManager.getDefault().isMultiSimEnabled())) {
            prefSet.removePreference(mStatusBarCmSignal);
        }

        mCheckPreferences = true;
        return prefSet;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                true, mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarCustomHeader) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
           Helpers.restartSystemUI();
            return true;
        } else if (preference == mSMSBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mMissedCallBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mVoicemailBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mBarOpaqueColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_OPAQUE_COLOR, intHex);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mIconColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_ICON_COLOR, intHex);
            return true;
        } else if (preference == mStatusBarNetworkStatsTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_TEXT_COLOR, intHex);
            return true;
        } else if (preference == mSignalStyle) {
            int signalStyle = Integer.valueOf((String) newValue);
            int index = mSignalStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mSignalStyle.setSummary(mSignalStyle.getEntries()[index]);
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
	if (preference == mSMSBreath) {
            value = mSMSBreath.isChecked();
            Settings.System.putInt(getContentResolver(), 
                    Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mMissedCallBreath) {
            value = mMissedCallBreath.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mVoicemailBreath) {
            value = mVoicemailBreath.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
            return true;
	} else if (preference == mCustomBarColor) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_STATUS_BAR_COLOR,
            mCustomBarColor.isChecked() ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mStatusbarSliderPreference) {
            value = mStatusbarSliderPreference.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUSBAR_BRIGHTNESS_SLIDER, value ? 1 : 0);
            return true;
        } else if (preference == mCustomIconColor) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_SYSTEM_ICON_COLOR,
            mCustomIconColor.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowLteOrFourgee) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_LTE_OR_FOURGEE,
            mShowLteOrFourgee.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
