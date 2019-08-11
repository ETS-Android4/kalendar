package org.andstatus.todoagenda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.andstatus.todoagenda.prefs.AllSettings;

public class EnvironmentChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(this.getClass().getName(), "Received intent: " + intent);
        AllSettings.ensureLoadedFromFiles(context, false);
        String action = intent == null
                ? ""
                : (intent.getAction() == null ? "" : intent.getAction());
        switch (action) {
            case Intent.ACTION_LOCALE_CHANGED:
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_DATE_CHANGED:
            case Intent.ACTION_TIMEZONE_CHANGED:
                EventAppWidgetProvider.updateWidgetsWithData(context);
                break;
            default:
                EventAppWidgetProvider.updateEventList(context);
                break;
        }
    }
}
