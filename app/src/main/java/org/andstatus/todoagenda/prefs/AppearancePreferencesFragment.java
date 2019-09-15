package org.andstatus.todoagenda.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.andstatus.todoagenda.MainActivity;
import org.andstatus.todoagenda.R;
import org.andstatus.todoagenda.util.DateUtil;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

public class AppearancePreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        showLockTimeZone(true);
        showEventEntryLayout();
        showWidgetInstanceName();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case ApplicationPreferences.PREF_EVENT_ENTRY_LAYOUT:
                showEventEntryLayout();
                break;
            case ApplicationPreferences.PREF_WIDGET_INSTANCE_NAME:
                getActivity().finish();
                startActivity(MainActivity.intentToConfigure(getActivity(), ApplicationPreferences
                        .getWidgetId(getActivity())));
                break;
            default:
                break;
        }
    }

    private void showEventEntryLayout() {
        Preference preference = findPreference(ApplicationPreferences.PREF_EVENT_ENTRY_LAYOUT);
        if (preference != null) {
            preference.setSummary(ApplicationPreferences.getEventEntryLayout(getActivity()).summaryResId);
        }
    }

    private void showWidgetInstanceName() {
        Preference preference = findPreference(ApplicationPreferences.PREF_WIDGET_INSTANCE_NAME);
        if (preference != null) {
            preference.setSummary(ApplicationPreferences.getWidgetInstanceName(getActivity()));
        }
    }

    private void showLockTimeZone(boolean setAlso) {
        CheckBoxPreference preference = findPreference(ApplicationPreferences.PREF_LOCK_TIME_ZONE);
        if (preference != null) {
            boolean isChecked = setAlso ? ApplicationPreferences.isTimeZoneLocked(getActivity()) : preference.isChecked();
            if (setAlso && preference.isChecked() != isChecked) {
                preference.setChecked(isChecked);
            }
            DateTimeZone timeZone = DateTimeZone.forID(DateUtil.validatedTimeZoneId(isChecked ?
                    ApplicationPreferences.getLockedTimeZoneId(getActivity()) : TimeZone.getDefault().getID()));
            preference.setSummary(String.format(
                    getText(isChecked ? R.string.lock_time_zone_on_desc : R.string.lock_time_zone_off_desc).toString(),
                    timeZone.getName(DateUtil.now(timeZone).getMillis()))
            );
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case ApplicationPreferences.PREF_BACKGROUND_COLOR:
                new BackgroundTransparencyDialog().show(getFragmentManager(),
                        ApplicationPreferences.PREF_BACKGROUND_COLOR);
                break;
            case ApplicationPreferences.PREF_PAST_EVENTS_BACKGROUND_COLOR:
                new BackgroundTransparencyDialog().show(getFragmentManager(),
                        ApplicationPreferences.PREF_PAST_EVENTS_BACKGROUND_COLOR);
                break;
            case ApplicationPreferences.PREF_LOCK_TIME_ZONE:
                if (preference instanceof CheckBoxPreference) {
                    CheckBoxPreference checkPref = (CheckBoxPreference) preference;
                    ApplicationPreferences.setLockedTimeZoneId(getActivity(),
                            checkPref.isChecked() ? TimeZone.getDefault().getID() : "");
                    showLockTimeZone(false);
                }
                break;
            default:
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
