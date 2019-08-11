package org.andstatus.todoagenda;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;

import org.andstatus.todoagenda.prefs.ApplicationPreferences;
import org.andstatus.todoagenda.prefs.TaskPreferencesFragment;
import org.andstatus.todoagenda.util.PermissionsUtil;

import java.util.List;

public class WidgetConfigurationActivity extends PreferenceActivity {

    private static final String PREFERENCES_PACKAGE_NAME = "org.andstatus.todoagenda.prefs";

    private int widgetId = 0;
    private Fragment currentFragment;

    @NonNull
    public static Intent intentToStartMe(Context context, int widgetId) {
        Intent intent = new Intent(context, WidgetConfigurationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP + Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return intent;
    }

    @Override
    protected void onPause() {
        super.onPause();
        ApplicationPreferences.save(this, widgetId);
        EventAppWidgetProvider.updateWidgetWithData(this, widgetId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        restartIfNeeded();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prepareForNewIntent(getIntent());
        super.onCreate(savedInstanceState);
    }

    private void prepareForNewIntent(Intent newIntent) {
        int newWidgetId = newIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        if (newWidgetId == 0) {
            newWidgetId = ApplicationPreferences.getWidgetId(this);
        }
        Intent restartIntent = null;
        if (newWidgetId == 0 || !PermissionsUtil.arePermissionsGranted(this)) {
            restartIntent = MainActivity.intentToStartMe(this);
        } else if (widgetId != 0 && widgetId != newWidgetId) {
            restartIntent = MainActivity.intentToConfigure(this, newWidgetId);
        } else if (widgetId == 0) {
            widgetId = newWidgetId;
            ApplicationPreferences.startEditing(this, widgetId);
        }
        if (restartIntent != null) {
            widgetId = 0;
            startActivity(restartIntent);
            finish();
        }
    }

    private void restartIfNeeded() {
        if (widgetId != ApplicationPreferences.getWidgetId(this) || !PermissionsUtil.arePermissionsGranted(this)) {
            widgetId = 0;
            startActivity(MainActivity.intentToStartMe(this));
            finish();
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_header, target);
        setTitle(ApplicationPreferences.getWidgetInstanceName(this));
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected boolean isValidFragment(String fragmentName) {
        if (fragmentName.startsWith(PREFERENCES_PACKAGE_NAME)) {
            return true;
        }
        return super.isValidFragment(fragmentName);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        this.currentFragment = fragment;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (currentFragment instanceof TaskPreferencesFragment) {
            ((TaskPreferencesFragment) currentFragment).gotPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
