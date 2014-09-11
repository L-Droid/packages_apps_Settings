/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
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

import android.app.admin.DevicePolicyManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.util.slim.DeviceUtils;

import com.android.settings.crdroid.SeekBarPreferenceCHOS;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockscreenInterface";

    private static final int DLG_ENABLE_EIGHT_TARGETS = 0;

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_LOCKSCREEN_ALL_WIDGETS = "lockscreen_all_widgets";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String KEY_ENABLE_MAXIMIZE_WIGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    private static final String PREF_LOCKSCREEN_EIGHT_TARGETS = "lockscreen_eight_targets";
    private static final String LOCK_BEFORE_UNLOCK = "lock_before_unlock";
    private static final String PREF_LOCKSCREEN_SHORTCUTS = "lockscreen_shortcuts";
    private static final String KEY_DISABLE_FRAME = "lockscreen_disable_frame";
    private static final String PREF_LOCKSCREEN_TORCH = "lockscreen_torch";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";
    private static final String KEY_SEE_THROUGH = "see_through";
    private static final String KEY_BLUR_RADIUS = "blur_radius";

    private static final int DLG_ALL_WIDGETS = 0;

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mAllWidgets;
    private CheckBoxPreference mEnableCameraWidget;
    private CheckBoxPreference mEnableModLock;
    private CheckBoxPreference mEnableMaximizeWidgets;
    private CheckBoxPreference mLockBeforeUnlock;
    private CheckBoxPreference mDisableFrame;
    private CheckBoxPreference mSeeThrough;
    private SeekBarPreferenceCHOS mBlurRadius;

    private ListPreference mBatteryStatus;
    private CheckBoxPreference mLockscreenEightTargets;
    private CheckBoxPreference mLockRingBattery;
    private CheckBoxPreference mGlowpadTorch;
    private Preference mShortcuts;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Find categories
        PreferenceCategory generalCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mAllWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ALL_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        mEnableMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_MAXIMIZE_WIGETS);

        mBatteryStatus = (ListPreference) prefs.findPreference(KEY_BATTERY_STATUS);
        int batteryStatus = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0);
        mBatteryStatus.setValue(String.valueOf(batteryStatus));
        mBatteryStatus.setSummary(mBatteryStatus.getEntry());
        mBatteryStatus.setOnPreferenceChangeListener(this);

        mLockscreenEightTargets = (CheckBoxPreference) findPreference(
                PREF_LOCKSCREEN_EIGHT_TARGETS);
        mLockscreenEightTargets.setChecked(Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_EIGHT_TARGETS, 0) == 1);
        mLockscreenEightTargets.setOnPreferenceChangeListener(this);

        mGlowpadTorch = (CheckBoxPreference) findPreference(
                PREF_LOCKSCREEN_TORCH);
        mGlowpadTorch.setChecked(Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_GLOWPAD_TORCH, 0) == 1);
        mGlowpadTorch.setOnPreferenceChangeListener(this);

        mEnableModLock = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MODLOCK_ENABLED);
        if (mEnableModLock != null) {
            mEnableModLock.setOnPreferenceChangeListener(this);
        }

        if (!DeviceUtils.deviceSupportsTorch(getActivity())) {
            prefs.removePreference(mGlowpadTorch);
        }

        mShortcuts = (Preference) findPreference(PREF_LOCKSCREEN_SHORTCUTS);
        mShortcuts.setEnabled(!mLockscreenEightTargets.isChecked());

        mLockRingBattery = (CheckBoxPreference) findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
        mLockRingBattery.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);

        // Keyguard widget frame
        mDisableFrame = (CheckBoxPreference) findPreference(KEY_DISABLE_FRAME);

        // Enable all widgets
        mAllWidgets.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0) == 1);
        mAllWidgets.setOnPreferenceChangeListener(this);

        // Lock before Unlock
        mLockBeforeUnlock = (CheckBoxPreference) findPreference(LOCK_BEFORE_UNLOCK);

        // lockscreen see through
        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_THROUGH);
        if (mSeeThrough != null) {
            mSeeThrough.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);
        }

        // Blur radius
        mBlurRadius = (SeekBarPreferenceCHOS) findPreference(KEY_BLUR_RADIUS);
        if (mBlurRadius != null) {
            mBlurRadius.setValue(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
            mBlurRadius.setOnPreferenceChangeListener(this);
        }
        
        // Enable or disable camera widget based on device and policy
        if (Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
            mLockUtils.setCameraEnabled(false);
        } else if (mLockUtils.isSecure()) {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }

        boolean canEnableModLockscreen = false;
        final String keyguardPackage = getActivity().getString(
                com.android.internal.R.string.config_keyguardPackage);
        final Bundle keyguard_metadata = Utils.getApplicationMetadata(
                getActivity(), keyguardPackage);
        if (keyguard_metadata != null) {
            canEnableModLockscreen = keyguard_metadata.getBoolean(
                    "com.cyanogenmod.keyguard", false);
        }

        if (mEnableModLock != null && !canEnableModLockscreen) {
            generalCategory.removePreference(mEnableModLock);
            mEnableModLock = null;
        }

        // Remove cLock settings item if not installed
        if (!Utils.isPackageInstalled(getActivity(), "com.cyanogenmod.lockclock")) {
            widgetsCategory.removePreference(findPreference(KEY_LOCK_CLOCK));
        }

        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    mEnableMaximizeWidgets);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update custom widgets and camera
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(mLockUtils.getWidgetsEnabled());
        }

        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());
        }

        // Update mod lockscreen status
        if (mEnableModLock != null) {
            ContentResolver cr = getActivity().getContentResolver();
            boolean checked = Settings.System.getInt(
                    cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED, 1) == 1;
            mEnableModLock.setChecked(checked);
        }

        // Lockscreen frame
        if (mDisableFrame != null) {
            mDisableFrame.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED, 0) == 1);
            mDisableFrame.setOnPreferenceChangeListener(this);
        }

        updateAvailableModLockPreferences();
    }

    private void updateAvailableModLockPreferences() {
        if (mEnableModLock == null) {
            return;
        }

        boolean enabled = !mEnableModLock.isChecked();
        if (mEnableKeyguardWidgets != null) {
            // Enable or disable lockscreen widgets based on policy
            if(!checkDisabledByPolicy(mEnableKeyguardWidgets,
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL)) {
                mEnableKeyguardWidgets.setEnabled(enabled);
            }
        }
        if (mEnableMaximizeWidgets != null) {
            mEnableMaximizeWidgets.setEnabled(enabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            mLockUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;
        } else if (KEY_ENABLE_CAMERA.equals(key)) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
            return true;
        } else if (preference == mSeeThrough) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH,
                    mSeeThrough.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mLockBeforeUnlock) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCK_BEFORE_UNLOCK,
                    mLockBeforeUnlock.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mLockRingBattery) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, mLockRingBattery.isChecked()
                    ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mLockscreenEightTargets) {
            showDialogInner(DLG_ENABLE_EIGHT_TARGETS, (Boolean) objValue);
            return true;
        } else if (preference == mBlurRadius) {
                    Settings.System.putInt(getContentResolver(),
            Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer) objValue);
            return true;
        } else if (preference == mGlowpadTorch) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_GLOWPAD_TORCH,
                    (Boolean) objValue ? 1 : 0);
            return true;
        } else if (preference == mEnableModLock) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED,
                    value ? 1 : 0);
            // force it so update picks up correct values
            ((CheckBoxPreference) preference).setChecked(value);
            updateAvailableModLockPreferences();
            return true;
        } else if (preference == mDisableFrame) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED,
                    (Boolean) objValue ? 1 : 0);
            return true;
        } else if (preference == mAllWidgets) {
            final boolean checked = (Boolean) objValue;
            if (checked) {
                showDialogInner(DLG_ALL_WIDGETS);
            } else {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0);
            }
            return true;
        }

        return false;
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return (getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) > 0);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     * @return True if disabled.
     */
    private boolean checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
        return disabled;
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockscreenInterface getOwner() {
            return (LockscreenInterface) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final boolean state = getArguments().getBoolean("state");
            switch (id) {
                case DLG_ALL_WIDGETS:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lockscreen_allow_all_title)
                    .setMessage(R.string.lockscreen_allow_all_warning)
                    .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            disableSetting();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 1);
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_ALL_WIDGETS:
                    disableSetting();
                    break;
                default:
                    // None for now
            }
        }

        private void disableSetting() {
            if (getOwner().mAllWidgets != null) {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0);
                getOwner().mAllWidgets.setChecked(false);
            }
        }
    }
}
