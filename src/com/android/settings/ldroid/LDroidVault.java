package com.android.settings.ldroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class LDroidVault extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.ldroid_vault);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
