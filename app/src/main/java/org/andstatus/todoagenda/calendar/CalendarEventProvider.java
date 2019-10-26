package org.andstatus.todoagenda.calendar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;
import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.NonNull;

import org.andstatus.todoagenda.prefs.EventSource;
import org.andstatus.todoagenda.provider.EventProvider;
import org.andstatus.todoagenda.provider.QueryResult;
import org.andstatus.todoagenda.provider.QueryResultsStorage;
import org.andstatus.todoagenda.util.CalendarIntentUtil;
import org.andstatus.todoagenda.util.DateUtil;
import org.andstatus.todoagenda.util.PermissionsUtil;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CalendarEventProvider extends EventProvider {
    public static final String EVENT_SORT_ORDER = "startDay ASC, allDay DESC, begin ASC ";
    private static final String EVENT_SELECTION = Instances.SELF_ATTENDEE_STATUS + "!="
            + Attendees.ATTENDEE_STATUS_DECLINED;
    private static final String[] CALENDARS_PROJECTION = new String[]{Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME, Calendars.CALENDAR_COLOR,
            Calendars.ACCOUNT_NAME};


    public CalendarEventProvider(Context context, int widgetId) {
        super(context, widgetId);
    }

    public List<CalendarEvent> getEvents() {
        initialiseParameters();
        if (!PermissionsUtil.arePermissionsGranted(context)) {
            return new ArrayList<>();
        }
        List<CalendarEvent> eventList = getTimeFilteredEventList();
        if (getSettings().getShowOnlyClosestInstanceOfRecurringEvent()) {
            filterShowOnlyClosestInstanceOfRecurringEvent(eventList);
        }
        return eventList;
    }

    private void filterShowOnlyClosestInstanceOfRecurringEvent(@NonNull List<CalendarEvent> eventList) {
        SparseArray<CalendarEvent> eventIds = new SparseArray<>();
        List<CalendarEvent> toDelete = new ArrayList<>();
        for (CalendarEvent event : eventList) {
            CalendarEvent otherEvent = eventIds.get(event.getEventId());
            if (otherEvent == null) {
                eventIds.put(event.getEventId(), event);
            } else if (Math.abs(event.getStartDate().getMillis() -
                    DateUtil.now(zone).getMillis()) <
                    Math.abs(otherEvent.getStartDate().getMillis() -
                            DateUtil.now(zone).getMillis())) {
                toDelete.add(otherEvent);
                eventIds.put(event.getEventId(), event);
            } else {
                toDelete.add(event);
            }
        }
        eventList.removeAll(toDelete);
    }

    public DateTime getEndOfTimeRange() {
        return mEndOfTimeRange;
    }

    public DateTime getStartOfTimeRange() {
        return mStartOfTimeRange;
    }

    private List<CalendarEvent> getTimeFilteredEventList() {
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, getStartOfTimeRangeForQuery(mStartOfTimeRange));
        ContentUris.appendId(builder, mEndOfTimeRange.getMillis());
        List<CalendarEvent> eventList = queryList(builder.build(), getCalendarSelection());
        // Above filters are not exactly correct for AllDay events: for them that filter
        // time should be moved by a time zone... (i.e. by several hours)
        // This is why we need to do additional filtering after querying a Content Provider:
        for (Iterator<CalendarEvent> it = eventList.iterator(); it.hasNext(); ) {
            CalendarEvent event = it.next();
            if (!event.getEndDate().isAfter(mStartOfTimeRange)
                    || !mEndOfTimeRange.isAfter(event.getStartDate())) {
                // We remove using Iterator to avoid ConcurrentModificationException
                it.remove();
            }
        }
        return eventList;
    }

    private long getStartOfTimeRangeForQuery(DateTime startOfTimeRange) {
        int offset = zone.getOffset(startOfTimeRange);
        if (offset >= 0) {
            return startOfTimeRange.getMillis();
        } else {
            return startOfTimeRange.getMillis() + offset;
        }
    }

    private String getCalendarSelection() {
        Set<String> activeCalendars = getSettings().getActiveCalendars();
        StringBuilder stringBuilder = new StringBuilder(EVENT_SELECTION);
        if (!activeCalendars.isEmpty()) {
            stringBuilder.append(AND_BRACKET);
            Iterator<String> iterator = activeCalendars.iterator();
            while (iterator.hasNext()) {
                String calendarId = iterator.next();
                stringBuilder.append(Instances.CALENDAR_ID);
                stringBuilder.append(EQUALS);
                stringBuilder.append(calendarId);
                if (iterator.hasNext()) {
                    stringBuilder.append(OR);
                }
            }
            stringBuilder.append(CLOSING_BRACKET);
        }
        return stringBuilder.toString();
    }

    private List<CalendarEvent> queryList(Uri uri, String selection) {
        List<CalendarEvent> eventList = new ArrayList<>();
        QueryResult result = new QueryResult(getSettings(), uri, getProjection(),
                selection, null, EVENT_SORT_ORDER);
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, getProjection(),
                    selection, null, EVENT_SORT_ORDER);
            if (cursor != null) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    if (QueryResultsStorage.getNeedToStoreResults()) {
                        result.addRow(cursor);
                    }
                    CalendarEvent event = createCalendarEvent(cursor);
                    if (!eventList.contains(event) && !mKeywordsFilter.matched(event.getTitle())) {
                        eventList.add(event);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(this.getClass().getSimpleName(), "Failed to queryList uri:" + uri + ", selection:" + selection, e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        QueryResultsStorage.storeCalendar(result);
        return eventList;
    }

    public static String[] getProjection() {
        List<String> columnNames = new ArrayList<>();
        columnNames.add(Instances.EVENT_ID);
        columnNames.add(Instances.TITLE);
        columnNames.add(Instances.BEGIN);
        columnNames.add(Instances.END);
        columnNames.add(Instances.ALL_DAY);
        columnNames.add(Instances.EVENT_LOCATION);
        columnNames.add(Instances.HAS_ALARM);
        columnNames.add(Instances.RRULE);
        columnNames.add(Instances.DISPLAY_COLOR);
        return columnNames.toArray(new String[0]);
    }

    private CalendarEvent createCalendarEvent(Cursor cursor) {
        boolean allDay = cursor.getInt(cursor.getColumnIndex(Instances.ALL_DAY)) > 0;
        CalendarEvent event = new CalendarEvent(context, widgetId, zone, allDay);
        event.setEventId(cursor.getInt(cursor.getColumnIndex(Instances.EVENT_ID)));
        event.setTitle(cursor.getString(cursor.getColumnIndex(Instances.TITLE)));
        event.setStartMillis(cursor.getLong(cursor.getColumnIndex(Instances.BEGIN)));
        event.setEndMillis(cursor.getLong(cursor.getColumnIndex(Instances.END)));
        event.setLocation(cursor.getString(cursor.getColumnIndex(Instances.EVENT_LOCATION)));
        event.setAlarmActive(cursor.getInt(cursor.getColumnIndex(Instances.HAS_ALARM)) > 0);
        event.setRecurring(cursor.getString(cursor.getColumnIndex(Instances.RRULE)) != null);
        event.setColor(getAsOpaque(getEventColor(cursor)));
        return event;
    }

    private int getEventColor(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Instances.DISPLAY_COLOR));
    }

    public List<EventSource> getCalendars() {
        List<EventSource> eventSources = new ArrayList<>();

        Cursor cursor = createCalendarsCursor();
        if (cursor == null) {
            return eventSources;
        }

        int idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID);
        int nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        int summaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME);
        int colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            EventSource source = new EventSource(cursor.getInt(idIdx), cursor.getString(nameIdx),
                    cursor.getString(summaryIdx), cursor.getInt(colorIdx));
            eventSources.add(source);
        }
        return eventSources;
    }

    private Cursor createCalendarsCursor() {
        Uri.Builder builder = Calendars.CONTENT_URI.buildUpon();
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.query(builder.build(), CALENDARS_PROJECTION, null, null, null);
    }

    public Intent createOpenCalendarEventIntent(CalendarEvent event) {
        Intent intent = CalendarIntentUtil.createCalendarIntent();
        intent.setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getEventId()));
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getStartMillis());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getEndMillis());
        return intent;
    }
}
