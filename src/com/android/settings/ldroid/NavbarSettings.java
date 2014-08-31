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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import org.cyanogenmod.hardware.KeyDisabler;
import com.android.internal.util.slim.SlimActions;

public class NavbarSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";
    //private static final String DISABLE_NAV_KEYS = "disable_nav_keys";
    // Enable/disable nav bar	
    private static final String ENABLE_NAVIGATION_BAR = "enable_nav_bar";
    // Dialog for user protection on PIE and Navbar
    private static final int DLG_NAVIGATION_WARNING = 0;
    // Enable/disable hardware keys
    private static final String KEY_HW_KEYS_ON_NAVBAR = "hw_keys_on_navbar";
    private static final String KEY_MENU_ENABLED = "key_menu_enabled";
    private static final String KEY_BACK_ENABLED = "key_back_enabled";
    private static final String KEY_HOME_ENABLED = "key_home_enabled";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String CATEGORY_NAVBAR = "navigation_bar";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;
    private static final int ACTION_LAUNCH_CAMERA = 6;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;

    private CheckBoxPreference mNavigationBarLeftPref;
    // Enable/disable hardware keys
    private CheckBoxPreference mMenuKeyEnabled;
    private CheckBoxPreference mBackKeyEnabled;
    private CheckBoxPreference mHomeKeyEnabled;
    // Enable/disable nav bar
    private CheckBoxPreference mEnableNavigationBar;

    private PreferenceCategory mNavigationPreferencesCat;

    private Handler mHandler;

    // Used in user protection for PIE and navbar
    private SettingsObserver mSettingsObserver = new SettingsObserver(new Handler());
    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.NAVIGATION_BAR_SHOW), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateSettings();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.navbar_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;

        boolean hasAnyBindableKey = false;
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        mHandler = new Handler();

        // Force Navigation bar related options
        //mDisableNavigationKeys = (CheckBoxPreference) findPreference(DISABLE_NAV_KEYS);

        mNavigationPreferencesCat = (PreferenceCategory) findPreference(CATEGORY_NAVBAR);

        // Navigation bar left
        mNavigationBarLeftPref = (CheckBoxPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);

        // Only visible on devices that does not have a navigation bar already,
        // and don't even try unless the existing keys can be disabled
        /*boolean needsNavigationBar = false;
        if (KeyDisabler.isSupported()) {
            try {
                IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                needsNavigationBar = wm.needsNavigationBar();
            } catch (RemoteException e) {
            }

            if (needsNavigationBar) {
                prefScreen.removePreference(mDisableNavigationKeys);
            } else {
                // Remove keys that can be provided by the navbar
                updateDisableNavkeysOption();
                mNavigationPreferencesCat.setEnabled(mDisableNavigationKeys.isChecked());
            }
        } else {
            prefScreen.removePreference(mDisableNavigationKeys);
        }*/

        /*try {
            // Only show the navigation bar category on devices that has a navigation bar
            // unless we are forcing it via development settings
            boolean forceNavbar = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar()
                    || forceNavbar;

            if (hasNavBar) {
                if (!Utils.isPhone(getActivity())) {
                    mNavigationPreferencesCat.removePreference(mNavigationBarLeftPref);
                }
            } else if (needsNavigationBar || !isKeyDisablerSupported()) {
                // Hide navigation bar category
                prefScreen.removePreference(mNavigationPreferencesCat);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }*/

        // Used for enable/disable navbar
        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

            if (hasNavBar) {
                if (!Utils.isPhone(getActivity())) {
                    PreferenceCategory navCategory =
                            (PreferenceCategory) findPreference(CATEGORY_NAVBAR);
                    navCategory.removePreference(mNavigationBarLeftPref);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        boolean hasNavBarByDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        boolean enableNavigationBar = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_SHOW, hasNavBarByDefault ? 1 : 0) == 1;
        mEnableNavigationBar = (CheckBoxPreference) findPreference(ENABLE_NAVIGATION_BAR);
        mEnableNavigationBar.setChecked(enableNavigationBar);
        mEnableNavigationBar.setOnPreferenceChangeListener(this);
   
        // Enable/disable hardware keys
        mMenuKeyEnabled = (CheckBoxPreference) findPreference(KEY_MENU_ENABLED);
        mBackKeyEnabled = (CheckBoxPreference) findPreference(KEY_BACK_ENABLED);
        mHomeKeyEnabled = (CheckBoxPreference) findPreference(KEY_HOME_ENABLED);       
        if(!getResources().getBoolean(com.android.internal.R.bool.config_hwKeysOnNavBar)) {
            PreferenceCategory hwKeysOnNavBarOptions = (PreferenceCategory)
                    getPreferenceScreen().findPreference(KEY_HW_KEYS_ON_NAVBAR);
            getPreferenceScreen().removePreference(hwKeysOnNavBarOptions);
        }

	updateSettings();

        if (mEnableNavigationBar.isChecked()) {
            enableKeysPrefs();
        } else {
            resetKeys();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSettings();
        mSettingsObserver.observe();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    private void updateSettings() {
        boolean enableNavigationBar = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_SHOW,
                SlimActions.isNavBarDefault(getActivity()) ? 1 : 0) == 1;
        mEnableNavigationBar.setChecked(enableNavigationBar);

        updateNavbarPreferences(enableNavigationBar);
    }

    // Enable/disbale nav bar
    private void updateNavbarPreferences(boolean show) {}

    public void enableKeysPrefs() {
        mMenuKeyEnabled.setEnabled(true);
        mBackKeyEnabled.setEnabled(true);
        mHomeKeyEnabled.setEnabled(true);
        mMenuKeyEnabled.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.KEY_MENU_ENABLED, 1) == 1));
        mBackKeyEnabled.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.KEY_BACK_ENABLED, 1) == 1));
        mHomeKeyEnabled.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.KEY_HOME_ENABLED, 1) == 1));
    }

    public void resetKeys() {
        mMenuKeyEnabled.setEnabled(false);
        mBackKeyEnabled.setEnabled(false);
        mHomeKeyEnabled.setEnabled(false);
        mMenuKeyEnabled.setChecked(true);
        mBackKeyEnabled.setChecked(true);
        mHomeKeyEnabled.setChecked(true);
        Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.KEY_MENU_ENABLED, 1);
        Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.KEY_BACK_ENABLED, 1);
        Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.KEY_HOME_ENABLED, 1);
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;
        // Enable/disbale nav bar (used in custom nav bar dimensions)
	if (preference == mEnableNavigationBar) {
            if (!((Boolean) newValue) && !SlimActions.isPieEnabled(getActivity())
                    && SlimActions.isNavBarDefault(getActivity())) {
                showDialogInner(DLG_NAVIGATION_WARNING);
                return true;
            }
            value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_SHOW,
                    ((Boolean) newValue) ? 1 : 0);
            updateNavbarPreferences((Boolean) value);
            if (value) {
                enableKeysPrefs();
            } else {
                resetKeys();
            }
            return true;
        }
        return false;
    }

    /*private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int defaultBrightness = context.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

        Settings.System.putInt(context.getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
        KeyDisabler.setActive(enabled);

        // Save/restore button timeouts to disable them in softkey mode
        Editor editor = prefs.edit();

        if (enabled) {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, defaultBrightness);
            if (!prefs.contains("pre_navbar_button_backlight")) {
                editor.putInt("pre_navbar_button_backlight", currentBrightness);
            }
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, 0);
        } else {
            int oldBright = prefs.getInt("pre_navbar_button_backlight", -1);
            if (oldBright != -1) {
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.BUTTON_BRIGHTNESS, oldBright);
                editor.remove("pre_navbar_button_backlight");
            }
        }
        editor.commit();
    }*/

    /*private void updateDisableNavkeysOption() {
        boolean enabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) != 0;

        mDisableNavigationKeys.setChecked(enabled);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        // Disable hw-key options if they're disabled
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        // Toggle backlight control depending on navbar state, force it to
           off if enabling
        if (backlight != null) {
            backlight.setEnabled(!enabled);
        }

        // Toggle hardkey control availability depending on navbar state
        if (homeCategory != null) {
            homeCategory.setEnabled(!enabled);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!enabled);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!enabled);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!enabled);
        }
    }

    public static void restoreKeyDisabler(Context context) {
        if (!isKeyDisablerSupported()) {
            return;
        }

        writeDisableNavkeysOption(context, Settings.System.getInt(context.getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) != 0);
    }*/

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean keysValue;
        // crDroid way of enable/disable hardware keys with navbar on
        if (preference == mMenuKeyEnabled) {
            keysValue = mMenuKeyEnabled.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KEY_MENU_ENABLED, keysValue ? 1 : 0);
            return true;
        } else if (preference == mBackKeyEnabled) {
            keysValue = mBackKeyEnabled.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KEY_BACK_ENABLED, keysValue ? 1 : 0);
            return true;
        } else if (preference == mHomeKeyEnabled) {
            keysValue = mHomeKeyEnabled.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KEY_HOME_ENABLED, keysValue ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private static boolean isKeyDisablerSupported() {
        try {
            return KeyDisabler.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    // Used for user protection in PIE and navbar 
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

        NavbarSettings getOwner() {
           return (NavbarSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_NAVIGATION_WARNING:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.navigation_bar_warning_no_navigation_present)
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.PIE_CONTROLS, 1);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_SHOW, 0);
                            getOwner().updateNavbarPreferences(false);
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
                case DLG_NAVIGATION_WARNING:
                    getOwner().mEnableNavigationBar.setChecked(true);
                    getOwner().updateNavbarPreferences(true);
                    break;
            }
        }
    }
}
