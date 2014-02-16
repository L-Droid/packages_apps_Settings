/*
 * Copyright (C) 2014 The Dirty Unicorns Project
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

import android.app.ActivityManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class Download extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    Preference mPAGapps;
    Preference mBanksGapps;
    Preference mXposed;
    Preference mXposedMod;
    Preference mGoogleCamera;
    Preference mPhilzTouch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.download);

        final ContentResolver resolver = getActivity().getContentResolver();

        mPAGapps = findPreference("pa_gapps");
        mBanksGapps = findPreference("banks_gapps");
        mXposed = findPreference("xposed");
        mXposedMod = findPreference("xposed_mod");
        mGoogleCamera = findPreference("google_camera");
	mPhilzTouch = findPreference ("philz_touch");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPAGapps) {
            Uri uri = Uri.parse("http://goo.gl/3TR6AN");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        } else if (preference == mBanksGapps) {
            Uri uri = Uri.parse("http://goo.gl/Pt6DoB");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        } else if (preference == mXposed) {
            Uri uri = Uri.parse("http://goo.gl/8VaxVQ");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        } else if (preference == mXposedMod) {
            Uri uri = Uri.parse("http://goo.gl/5J860t");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        } else if (preference == mGoogleCamera) {
            Uri uri = Uri.parse("http://goo.gl/9ADfbH");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        } else if (preference == mPhilzTouch) {
            Uri uri = Uri.parse("http://goo.gl/hZAzTm");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
         return true;
    }

    public static class DeviceAdminLockscreenReceiver extends DeviceAdminReceiver {}

}
