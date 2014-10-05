package com.android.settings.ldroid;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;
import com.android.settings.Utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class GeneralSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "GeneralSettings";
    private static final String RESTART_SYSTEMUI = "restart_systemui";
    private static final String SHOW_CPU_INFO_KEY = "show_cpu_info";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_LCD_DENSITY = "lcd_density";
    private static final String DENSITY_PROP = "persist.sys.lcd_density";
    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    //private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String CATEGORY_EXPANDED_DESKTOP = "expanded_desktop_category";

    private static final int DIALOG_CUSTOM_DENSITY = 101;

    //private CheckBoxPreference mExpandedDesktopNoNavbarPref;

    private ListPreference mExpandedDesktopPref;
    private Preference mRestartSystemUI;
    private CheckBoxPreference mShowCpuInfo;
    private static ListPreference mLcdDensity;

    private static Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.general_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mActivity = getActivity();

        mRestartSystemUI = findPreference(RESTART_SYSTEMUI);

        // Expanded desktop
        mExpandedDesktopPref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP);
        //mExpandedDesktopNoNavbarPref =
        //        (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);

        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
                Settings.System.EXPANDED_DESKTOP_STYLE, 0);

        // All Expanded desktop options for all devices
        mExpandedDesktopPref.setOnPreferenceChangeListener(this);
        mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
        updateExpandedDesktop(expandedDesktopValue);

        /*try {
            // Only show the navigation bar category on devices that has a navigation bar
            // unless we are forcing it via development settings
            boolean forceNavbar = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar()
                    || forceNavbar;

            if (hasNavBar) {
                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
                updateExpandedDesktop(expandedDesktopValue);
                expandedCategory.removePreference(mExpandedDesktopNoNavbarPref);
            } else {
                // Hide no-op "Status bar visible" expanded desktop mode
                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
                expandedCategory.removePreference(mExpandedDesktopPref);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }*/

	mShowCpuInfo = (CheckBoxPreference) findPreference(SHOW_CPU_INFO_KEY);

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);

        updateSettings();
    }

    private void updateSettings() {
        mLcdDensity = (ListPreference) findPreference(KEY_LCD_DENSITY);
        String current = SystemProperties.get(DENSITY_PROP,
                SystemProperties.get("ro.sf.lcd_density"));
        final ArrayList<String> array = new ArrayList<String>(
                Arrays.asList(getResources().getStringArray(R.array.lcd_density_entries)));
        if (array.contains(current)) {
            mLcdDensity.setValue(current);
        } else {
            mLcdDensity.setValue("custom");
        }
        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + current);
        mLcdDensity.setOnPreferenceChangeListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    private void writeCpuInfoOptions() {
        boolean value = mShowCpuInfo.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.SHOW_CPU, value ? 1 : 0);
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.CPUInfoService");
        if (value) {
            getActivity().startService(service);
        } else {
            getActivity().stopService(service);
        }
    }	

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mRestartSystemUI) {
            Helpers.restartSystemUI();
	} else if (preference == mShowCpuInfo) {
            writeCpuInfoOptions();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) newValue);
            updateExpandedDesktop(expandedDesktopValue);
        //if (KEY_EXPANDED_DESKTOP_NO_NAVBAR.equals(key)) {
        //    boolean value = (Boolean) objValue;
        //    updateExpandedDesktop(value ? 2 : 0);
        //}
        } else if (preference == mLcdDensity) {
            String density = (String) newValue;
            if (SystemProperties.get(DENSITY_PROP) != density) {
                if ((density).equals(getResources().getString(R.string.custom_density))) {
                    showDialogInner(DIALOG_CUSTOM_DENSITY);
                } else {
                    setDensity(Integer.parseInt(density));
                }
            }
            return true;
	}
        return false;
    }

    private void updateExpandedDesktop(int value) {
        ContentResolver cr = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);

        if (value == 0) {
            // Expanded desktop deactivated
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
            summary = R.string.expanded_desktop_disabled;
        } else if (value == 1) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_status_bar;
        } else if (value == 2) {
            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            summary = R.string.expanded_desktop_no_status_bar;
        }

        if (mExpandedDesktopPref != null && summary != -1) {
            mExpandedDesktopPref.setSummary(res.getString(summary));
        }
    }

    private static void setDensity(int density) {
        int max = mActivity.getResources().getInteger(R.integer.lcd_density_max);
        int min = mActivity.getResources().getInteger(R.integer.lcd_density_min);
        if (density < min || density > max) {
            mLcdDensity.setSummary(mActivity.getResources().getString(
                                            R.string.custom_density_summary_invalid));
        }
        SystemProperties.set(DENSITY_PROP, Integer.toString(density));
        Configuration configuration = new Configuration();
        configuration.setToDefaults();
        configuration.densityDpi = density;
        try {
            ActivityManagerNative.getDefault().updateConfiguration(configuration);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure communicating with activity manager", e);
        }
        final IWindowManager windowManagerService = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            windowManagerService.updateStatusBarNavBarHeight();
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure communicating with window manager", e);
        }
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

        GeneralSettings getOwner() {
            return (GeneralSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater factory = LayoutInflater.from(getActivity());
            int id = getArguments().getInt("id");
            switch (id) {
                case DIALOG_CUSTOM_DENSITY:
                    final View textEntryView = factory.inflate(
                            R.layout.alert_dialog_text_entry, null);
                    return new AlertDialog.Builder(getActivity())
                            .setTitle(getResources().getString(R.string.set_custom_density_title))
                            .setView(textEntryView)
                            .setPositiveButton(getResources().getString(
                                    R.string.set_custom_density_set),
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    EditText dpi = (EditText)
                                            textEntryView.findViewById(R.id.dpi_edit);
                                    Editable text = dpi.getText();
                                    dialog.dismiss();
                                    setDensity(Integer.parseInt(text.toString()));
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            })
                            .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }
}
