package org.andstatus.todoagenda;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.andstatus.todoagenda.prefs.AllSettings;
import org.andstatus.todoagenda.prefs.InstanceSettings;
import org.andstatus.todoagenda.task.dmfs.DmfsOpenTasksContract;
import org.andstatus.todoagenda.util.CalendarIntentUtil;
import org.andstatus.todoagenda.util.DateUtil;
import org.andstatus.todoagenda.util.PermissionsUtil;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Locale;

import static android.graphics.Color.alpha;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static org.andstatus.todoagenda.Theme.themeNameToResId;
import static org.andstatus.todoagenda.util.CalendarIntentUtil.createOpenCalendarAtDayIntent;
import static org.andstatus.todoagenda.util.CalendarIntentUtil.createOpenCalendarEventPendingIntent;
import static org.andstatus.todoagenda.util.CalendarIntentUtil.createOpenCalendarPendingIntent;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setAlpha;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setColorFilter;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setImageFromAttr;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setTextColorFromAttr;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setTextSize;

public class EventAppWidgetProvider extends AppWidgetProvider {

    private static final String PACKAGE = EventAppWidgetProvider.class.getPackage().getName();
    public static final String ACTION_REFRESH = PACKAGE + ".action.REFRESH";
    private static volatile boolean receiversRegistered = false;

    public static int[] getWidgetIds(Context context) {
        return AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, EventAppWidgetProvider.class));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int widgetId : appWidgetIds) {
            AllSettings.delete(context, widgetId);
        }
    }

    @Override
    public void onUpdate(Context baseContext, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        registerReceivers(baseContext);
        for (int widgetId : appWidgetIds) {
            InstanceSettings settings = AllSettings.instanceFromId(baseContext, widgetId);
            AlarmReceiver.scheduleAlarm(settings.getHeaderThemeContext());
            RemoteViews rv = new RemoteViews(baseContext.getPackageName(), R.layout.widget);
            configureBackground(settings, rv);
            configureWidgetHeader(settings, rv);
            configureList(settings, widgetId, rv);
            appWidgetManager.updateAppWidget(widgetId, rv);
        }
    }

    private void configureBackground(InstanceSettings settings, RemoteViews rv) {
        if (settings.getShowWidgetHeader()) {
            rv.setViewVisibility(R.id.action_bar, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.action_bar, View.GONE);
        }
        int color = settings.getBackgroundColor();
        int opaqueColor = Color.rgb(red(color), green(color), blue(color));
        setColorFilter(rv, R.id.background_image, opaqueColor);
        setAlpha(rv, R.id.background_image, alpha(color));
    }

    private void configureWidgetHeader(InstanceSettings settings, RemoteViews rv) {
        configureCurrentDate(settings, rv);
        setActionIcons(settings, rv);
        configureAddEvent(settings, rv);
        configureRefresh(settings.getContext(), rv);
        configureOverflowMenu(settings, rv);
    }

    private void configureCurrentDate(InstanceSettings settings, RemoteViews rv) {
        rv.setOnClickPendingIntent(R.id.calendar_current_date, createOpenCalendarPendingIntent(settings));
        String formattedDate = DateUtil.createDateString(settings,
                DateUtil.now(settings.getTimeZone())).toUpperCase(Locale.getDefault());
        rv.setTextViewText(R.id.calendar_current_date, formattedDate);
        setTextColorFromAttr(settings.getHeaderThemeContext(), rv, R.id.calendar_current_date, R.attr.header);
    }

    private void setActionIcons(InstanceSettings settings, RemoteViews rv) {
        setImageFromAttr(settings.getHeaderThemeContext(), rv, R.id.add_event, R.attr.header_action_add_event);
        setImageFromAttr(settings.getHeaderThemeContext(), rv, R.id.refresh, R.attr.header_action_refresh);
        setImageFromAttr(settings.getHeaderThemeContext(), rv, R.id.overflow_menu, R.attr.header_action_overflow);
        int themeId = themeNameToResId(settings.getHeaderTheme());
        int alpha = 255;
        if (themeId == R.style.Theme_Calendar_Dark || themeId == R.style.Theme_Calendar_Light) {
            alpha = 154;
        }
        setAlpha(rv, R.id.add_event, alpha);
        setAlpha(rv, R.id.refresh, alpha);
        setAlpha(rv, R.id.overflow_menu, alpha);
    }

    private void configureAddEvent(InstanceSettings settings, RemoteViews rv) {
        rv.setOnClickPendingIntent(R.id.add_event, getPermittedAddEventPendingIntent(settings));
    }

    private PendingIntent getPermittedAddEventPendingIntent(InstanceSettings settings) {
        Context context = settings.getContext();
        Intent intent = PermissionsUtil.getPermittedIntent(context,
                CalendarIntentUtil.createNewEventIntent(settings.getTimeZone()));
        return isIntentAvailable(context, intent) ?
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT) :
                getEmptyPendingIntent(context);
    }

    private static PendingIntent getEmptyPendingIntent(Context context) {
        return PendingIntent.getActivity(
                context.getApplicationContext(),
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void configureRefresh(Context context, RemoteViews rv) {
        Intent intent = new Intent(context, EnvironmentChangedReceiver.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        rv.setOnClickPendingIntent(R.id.refresh, pendingIntent);
    }

    private void configureOverflowMenu(InstanceSettings settings, RemoteViews rv) {
        Intent intent = MainActivity.intentToConfigure(settings.getContext(), settings.getWidgetId());
        PendingIntent menuPendingIntent = PermissionsUtil.getPermittedPendingIntent(settings, intent);
        rv.setOnClickPendingIntent(R.id.overflow_menu, menuPendingIntent);
    }

    private static boolean isIntentAvailable(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void configureList(InstanceSettings settings, int widgetId, RemoteViews rv) {
        Intent intent = new Intent(settings.getContext(), EventWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.event_list, intent);

        boolean permissionsGranted = PermissionsUtil.arePermissionsGranted(settings.getContext());
        @IdRes int emptyViewId = R.id.empty_event_list;
        rv.setEmptyView(R.id.event_list, emptyViewId);
        rv.setTextViewText(emptyViewId, settings.getContext().getText(
                permissionsGranted ? R.string.no_upcoming_events : R.string.grant_permissions_verbose
        ));
        rv.setOnClickPendingIntent(emptyViewId, getPermittedAddEventPendingIntent(settings));
        if (permissionsGranted) {
            rv.setPendingIntentTemplate(R.id.event_list, createOpenCalendarEventPendingIntent(settings));
            rv.setOnClickFillInIntent(emptyViewId,
                    createOpenCalendarAtDayIntent(new DateTime(settings.getTimeZone())));
        }
        setTextSize(settings, rv, emptyViewId, R.dimen.event_entry_details);
        setTextColorFromAttr(settings.getEntryThemeContext(), rv, emptyViewId, R.attr.eventEntryTitle);
    }

    public static void updateEventList(Context context) {
        registerReceivers(context);
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(getWidgetIds(context), R.id.event_list);
    }

    public static void updateAllWidgets(Context context) {
        Intent intent = new Intent(context, EventAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIds(context));
        context.sendBroadcast(intent);
    }

    private static void registerReceivers(Context contextIn) {
        if (receiversRegistered) return;

        Context context = contextIn.getApplicationContext();
        EnvironmentChangedReceiver receiver = new EnvironmentChangedReceiver();

        IntentFilter providerChanged = new IntentFilter();
        providerChanged.addAction("android.intent.action.PROVIDER_CHANGED");
        providerChanged.addDataScheme("content");
        providerChanged.addDataAuthority("com.android.calendar", null);
        providerChanged.addDataAuthority(DmfsOpenTasksContract.AUTHORITY, null);
        context.registerReceiver(receiver, providerChanged);

        IntentFilter userPresent = new IntentFilter();
        userPresent.addAction("android.intent.action.USER_PRESENT");
        context.registerReceiver(receiver, userPresent);

        Log.i(EventAppWidgetProvider.class.getName(), "Registered receivers from " + contextIn.getClass().getName());
        receiversRegistered = true;
    }
}
