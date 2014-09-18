/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings.ldroid;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class MoreDeviceSettings extends SettingsPreferenceFragment {
    private static final String TAG = "MoreDeviceSettings";

    private static final String KEY_SENSORS_MOTORS_CATEGORY = "sensors_motors_category";
    private static final String KEY_DISPLAY_CALIBRATION_CATEGORY = "display_calibration_category";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.more_device_settings);
        ContentResolver resolver = getContentResolver();
    }

}
