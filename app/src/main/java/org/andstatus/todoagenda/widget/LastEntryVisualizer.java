package org.andstatus.todoagenda.widget;

import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import org.andstatus.todoagenda.R;
import org.andstatus.todoagenda.util.PermissionsUtil;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

import static org.andstatus.todoagenda.EventRemoteViewsFactory.getPermittedAddEventPendingIntent;
import static org.andstatus.todoagenda.util.CalendarIntentUtil.createOpenCalendarAtDayIntent;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setTextSize;

/**
 * @author yvolk@yurivolkov.com
 */
public class LastEntryVisualizer extends WidgetEntryVisualizer<LastEntry> {
    public LastEntryVisualizer(Context context, int widgetId) {
        super(context, widgetId);
    }

    @Override
    public RemoteViews getRemoteViews(WidgetEntry eventEntry, int position) {
        LastEntry entry = (LastEntry) eventEntry;
        Log.d(this.getClass().getSimpleName(), "lastEntry: " + entry.type);
        RemoteViews rv = new RemoteViews(getContext().getPackageName(), entry.type.layoutId);

        int viewId = R.id.event_entry;
        switch (entry.type) {
            case EMPTY:
            case NOT_LOADED:
                boolean permissionsGranted = PermissionsUtil.arePermissionsGranted(getSettings().getContext());
                if (permissionsGranted) {
                    rv.setOnClickFillInIntent(viewId,
                            createOpenCalendarAtDayIntent(new DateTime(getSettings().getTimeZone())));
                } else {
                    rv.setOnClickPendingIntent(viewId, getPermittedAddEventPendingIntent(getSettings()));
                }
                break;
            default:
                rv.setOnClickPendingIntent(viewId, getPermittedAddEventPendingIntent(getSettings()));
                break;
        }
        if (entry.type == LastEntry.LastEntryType.EMPTY && getSettings().noPastEvents()) {
            rv.setTextViewText(viewId, getContext().getText(R.string.no_upcoming_events));
        }
        setTextSize(getSettings(), rv, viewId, R.dimen.event_entry_title);
        setEventTitleColor(rv, viewId, entry);
        return rv;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public List<LastEntry> getEventEntries() {
        return Collections.emptyList();
    }

    @Override
    public Class<? extends LastEntry> getSupportedEventEntryType() {
        return LastEntry.class;
    }
}
