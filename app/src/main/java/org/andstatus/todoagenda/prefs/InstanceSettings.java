package org.andstatus.todoagenda.prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;

import org.andstatus.todoagenda.EndedSomeTimeAgo;
import org.andstatus.todoagenda.util.DateUtil;
import org.andstatus.todoagenda.widget.EventEntryLayout;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.andstatus.todoagenda.Theme.themeNameToResId;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_ABBREVIATE_DATES;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_ABBREVIATE_DATES_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_ACTIVE_CALENDARS;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_ACTIVE_TASK_LISTS;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_BACKGROUND_COLOR;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_BACKGROUND_COLOR_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_DATE_FORMAT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_DATE_FORMAT_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_DAY_HEADER_ALIGNMENT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_DAY_HEADER_ALIGNMENT_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_ENTRY_THEME;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_ENTRY_THEME_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_EVENTS_ENDED;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_EVENT_ENTRY_LAYOUT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_EVENT_RANGE;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_EVENT_RANGE_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_FILL_ALL_DAY;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_FILL_ALL_DAY_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_HEADER_THEME;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_HEADER_THEME_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_HIDE_BASED_ON_KEYWORDS;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_INDICATE_ALERTS;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_INDICATE_RECURRING;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_LOCKED_TIME_ZONE_ID;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_MULTILINE_TITLE;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_MULTILINE_TITLE_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_PAST_EVENTS_BACKGROUND_COLOR;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_PAST_EVENTS_BACKGROUND_COLOR_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_DAYS_WITHOUT_EVENTS;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_DAY_HEADERS;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_END_TIME;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_END_TIME_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_LOCATION;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_LOCATION_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_ONLY_CLOSEST_INSTANCE_OF_RECURRING_EVENT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_PAST_EVENTS_WITH_DEFAULT_COLOR;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_SHOW_WIDGET_HEADER;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_TASK_SOURCE;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_TASK_SOURCE_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_TEXT_SIZE_SCALE;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_TEXT_SIZE_SCALE_DEFAULT;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_WIDGET_ID;
import static org.andstatus.todoagenda.prefs.ApplicationPreferences.PREF_WIDGET_INSTANCE_NAME;
import static org.andstatus.todoagenda.prefs.SettingsStorage.saveJson;

/**
 * Loaded settings of one Widget
 *
 * @author yvolk@yurivolkov.com
 */
public class InstanceSettings {
    private final Context context;
    private volatile ContextThemeWrapper entryThemeContext = null;
    private volatile ContextThemeWrapper headerThemeContext = null;

    final int widgetId;
    private final String widgetInstanceName;
    private Set<String> activeCalendars = Collections.emptySet();
    private int eventRange = Integer.valueOf(PREF_EVENT_RANGE_DEFAULT);
    private EndedSomeTimeAgo eventsEnded = EndedSomeTimeAgo.NONE;
    private boolean fillAllDayEvents = PREF_FILL_ALL_DAY_DEFAULT;
    private String hideBasedOnKeywords = "";
    private int pastEventsBackgroundColor = PREF_PAST_EVENTS_BACKGROUND_COLOR_DEFAULT;
    private boolean showDaysWithoutEvents = false;
    private boolean showDayHeaders = true;
    private boolean showPastEventsWithDefaultColor = false;
    private boolean showEndTime = PREF_SHOW_END_TIME_DEFAULT;
    private boolean showLocation = PREF_SHOW_LOCATION_DEFAULT;
    private String dateFormat = PREF_DATE_FORMAT_DEFAULT;
    private boolean abbreviateDates = PREF_ABBREVIATE_DATES_DEFAULT;
    private String lockedTimeZoneId = "";
    private EventEntryLayout eventEntryLayout = EventEntryLayout.DEFAULT;
    private boolean titleMultiline = PREF_MULTILINE_TITLE_DEFAULT;
    private boolean showOnlyClosestInstanceOfRecurringEvent = false;
    private boolean indicateAlerts = true;
    private boolean indicateRecurring = false;
    private String entryTheme = PREF_ENTRY_THEME_DEFAULT;
    private String headerTheme = PREF_HEADER_THEME_DEFAULT;
    private boolean showWidgetHeader = true;
    private int backgroundColor = PREF_BACKGROUND_COLOR_DEFAULT;
    private String textSizeScale = PREF_TEXT_SIZE_SCALE_DEFAULT;
    private String dayHeaderAlignment = PREF_DAY_HEADER_ALIGNMENT_DEFAULT;
    private String taskSource = PREF_TASK_SOURCE_DEFAULT;
    private Set<String> activeTaskLists = Collections.emptySet();

    public static InstanceSettings fromJson(Context context, JSONObject json) throws JSONException {
        InstanceSettings settings = new InstanceSettings(context, json.optInt(PREF_WIDGET_ID),
                json.optString(PREF_WIDGET_INSTANCE_NAME));
        if (settings.widgetId == 0) {
            return settings;
        }
        if (json.has(PREF_ACTIVE_CALENDARS)) {
            settings.activeCalendars = jsonArray2StringSet(json.getJSONArray(PREF_ACTIVE_CALENDARS));
        }
        if (json.has(PREF_EVENT_RANGE)) {
            settings.eventRange = json.getInt(PREF_EVENT_RANGE);
        }
        if (json.has(PREF_EVENTS_ENDED)) {
            settings.eventsEnded = EndedSomeTimeAgo.fromValue(json.getString(PREF_EVENTS_ENDED));
        }
        if (json.has(PREF_FILL_ALL_DAY)) {
            settings.fillAllDayEvents = json.getBoolean(PREF_FILL_ALL_DAY);
        }
        if (json.has(PREF_HIDE_BASED_ON_KEYWORDS)) {
            settings.hideBasedOnKeywords = json.getString(PREF_HIDE_BASED_ON_KEYWORDS);
        }
        if (json.has(PREF_PAST_EVENTS_BACKGROUND_COLOR)) {
            settings.pastEventsBackgroundColor = json.getInt(PREF_PAST_EVENTS_BACKGROUND_COLOR);
        }
        if (json.has(PREF_SHOW_DAYS_WITHOUT_EVENTS)) {
            settings.showDaysWithoutEvents = json.getBoolean(PREF_SHOW_DAYS_WITHOUT_EVENTS);
        }
        if (json.has(PREF_SHOW_DAY_HEADERS)) {
            settings.showDayHeaders = json.getBoolean(PREF_SHOW_DAY_HEADERS);
        }
        if (json.has(PREF_SHOW_PAST_EVENTS_WITH_DEFAULT_COLOR)) {
            settings.showPastEventsWithDefaultColor = json.getBoolean(PREF_SHOW_PAST_EVENTS_WITH_DEFAULT_COLOR);
        }
        if (json.has(PREF_SHOW_END_TIME)) {
            settings.showEndTime = json.getBoolean(PREF_SHOW_END_TIME);
        }
        if (json.has(PREF_SHOW_LOCATION)) {
            settings.showLocation = json.getBoolean(PREF_SHOW_LOCATION);
        }
        if (json.has(PREF_DATE_FORMAT)) {
            settings.dateFormat = json.getString(PREF_DATE_FORMAT);
        }
        if (json.has(PREF_ABBREVIATE_DATES)) {
            settings.abbreviateDates = json.getBoolean(PREF_ABBREVIATE_DATES);
        }
        if (json.has(PREF_LOCKED_TIME_ZONE_ID)) {
            settings.setLockedTimeZoneId(json.getString(PREF_LOCKED_TIME_ZONE_ID));
        }
        if (json.has(PREF_EVENT_ENTRY_LAYOUT)) {
            settings.eventEntryLayout = EventEntryLayout.fromValue(json.getString(PREF_EVENT_ENTRY_LAYOUT));
        }
        if (json.has(PREF_MULTILINE_TITLE)) {
            settings.titleMultiline = json.getBoolean(PREF_MULTILINE_TITLE);
        }
        if (json.has(PREF_SHOW_ONLY_CLOSEST_INSTANCE_OF_RECURRING_EVENT)) {
            settings.showOnlyClosestInstanceOfRecurringEvent = json.getBoolean(
                    PREF_SHOW_ONLY_CLOSEST_INSTANCE_OF_RECURRING_EVENT);
        }
        if (json.has(PREF_INDICATE_ALERTS)) {
            settings.indicateAlerts = json.getBoolean(PREF_INDICATE_ALERTS);
        }
        if (json.has(PREF_INDICATE_RECURRING)) {
            settings.indicateRecurring = json.getBoolean(PREF_INDICATE_RECURRING);
        }
        if (json.has(PREF_ENTRY_THEME)) {
            settings.entryTheme = json.getString(PREF_ENTRY_THEME);
        }
        if (json.has(PREF_HEADER_THEME)) {
            settings.headerTheme = json.getString(PREF_HEADER_THEME);
        }
        if (json.has(PREF_SHOW_WIDGET_HEADER)) {
            settings.showWidgetHeader = json.getBoolean(PREF_SHOW_WIDGET_HEADER);
        }
        if (json.has(PREF_BACKGROUND_COLOR)) {
            settings.backgroundColor = json.getInt(PREF_BACKGROUND_COLOR);
        }
        if (json.has(PREF_TEXT_SIZE_SCALE)) {
            settings.textSizeScale = json.getString(PREF_TEXT_SIZE_SCALE);
        }
        if (json.has(PREF_DAY_HEADER_ALIGNMENT)) {
            settings.dayHeaderAlignment = json.getString(PREF_DAY_HEADER_ALIGNMENT);
        }
        if (json.has(PREF_TASK_SOURCE)) {
            settings.taskSource = json.getString(PREF_TASK_SOURCE);
        }
        if (json.has(PREF_ACTIVE_TASK_LISTS)) {
            settings.activeTaskLists = jsonArray2StringSet(json.getJSONArray(PREF_ACTIVE_TASK_LISTS));
        }
        return settings;
    }

    private static Set<String> jsonArray2StringSet(JSONArray jsonArray) {
        Set<String> set = new HashSet<>();
        for (int index = 0; index < jsonArray.length(); index++) {
            String value = jsonArray.optString(index);
            if (value != null) {
                set.add(value);
            }
        }
        return set;
    }

    static InstanceSettings fromApplicationPreferences(Context context, int widgetId) {
        InstanceSettings settings = new InstanceSettings(context, widgetId,
                ApplicationPreferences.getString(context, PREF_WIDGET_INSTANCE_NAME,
                        ApplicationPreferences.getString(context, PREF_WIDGET_INSTANCE_NAME, "")));
        settings.activeCalendars = ApplicationPreferences.getActiveCalendars(context);
        settings.eventRange = ApplicationPreferences.getEventRange(context);
        settings.eventsEnded = ApplicationPreferences.getEventsEnded(context);
        settings.fillAllDayEvents = ApplicationPreferences.getFillAllDayEvents(context);
        settings.hideBasedOnKeywords = ApplicationPreferences.getHideBasedOnKeywords(context);
        settings.pastEventsBackgroundColor = ApplicationPreferences.getPastEventsBackgroundColor(context);
        settings.showDaysWithoutEvents = ApplicationPreferences.getShowDaysWithoutEvents(context);
        settings.showDayHeaders = ApplicationPreferences.getShowDayHeaders(context);
        settings.showPastEventsWithDefaultColor = ApplicationPreferences.getShowPastEventsWithDefaultColor(context);
        settings.showEndTime = ApplicationPreferences.getShowEndTime(context);
        settings.showLocation = ApplicationPreferences.getShowLocation(context);
        settings.dateFormat = ApplicationPreferences.getDateFormat(context);
        settings.abbreviateDates = ApplicationPreferences.getAbbreviateDates(context);
        settings.setLockedTimeZoneId(ApplicationPreferences.getLockedTimeZoneId(context));
        settings.eventEntryLayout = ApplicationPreferences.getEventEntryLayout(context);
        settings.titleMultiline = ApplicationPreferences.isTitleMultiline(context);
        settings.showOnlyClosestInstanceOfRecurringEvent = ApplicationPreferences
                .getShowOnlyClosestInstanceOfRecurringEvent(context);
        settings.indicateAlerts = ApplicationPreferences.getBoolean(context, PREF_INDICATE_ALERTS, true);
        settings.indicateRecurring = ApplicationPreferences.getBoolean(context, PREF_INDICATE_RECURRING, false);
        settings.entryTheme = ApplicationPreferences.getString(context, PREF_ENTRY_THEME, PREF_ENTRY_THEME_DEFAULT);
        settings.headerTheme = ApplicationPreferences.getString(context, PREF_HEADER_THEME, PREF_HEADER_THEME_DEFAULT);
        settings.showWidgetHeader = ApplicationPreferences.getBoolean(context, PREF_SHOW_WIDGET_HEADER, true);
        settings.backgroundColor = ApplicationPreferences.getInt(context, PREF_BACKGROUND_COLOR,
                PREF_BACKGROUND_COLOR_DEFAULT);
        settings.textSizeScale = ApplicationPreferences.getString(context, PREF_TEXT_SIZE_SCALE,
                PREF_TEXT_SIZE_SCALE_DEFAULT);
        settings.dayHeaderAlignment = ApplicationPreferences.getString(context, PREF_DAY_HEADER_ALIGNMENT,
                PREF_DAY_HEADER_ALIGNMENT_DEFAULT);
        settings.taskSource = ApplicationPreferences.getTaskSource(context);
        settings.activeTaskLists = ApplicationPreferences.getActiveTaskLists(context);
        return settings;
    }

    @NonNull
    private static String getStorageKey(int widgetId) {
        return "instanceSettings" + widgetId;
    }

    InstanceSettings(Context context, int widgetId, String proposedInstanceName) {
        this.context = context;
        this.widgetId = widgetId;
        this.widgetInstanceName = AllSettings.uniqueInstanceName(context, widgetId, proposedInstanceName);
    }

    void save() {
        try {
            saveJson(context, getStorageKey(widgetId), toJson());
        } catch (IOException e) {
            Log.e("save", toString(), e);
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(PREF_WIDGET_ID, widgetId);
            json.put(PREF_WIDGET_INSTANCE_NAME, widgetInstanceName);
            json.put(PREF_ACTIVE_CALENDARS, new JSONArray(activeCalendars));
            json.put(PREF_EVENT_RANGE, eventRange);
            json.put(PREF_EVENTS_ENDED, eventsEnded.save());
            json.put(PREF_FILL_ALL_DAY, fillAllDayEvents);
            json.put(PREF_HIDE_BASED_ON_KEYWORDS, hideBasedOnKeywords);
            json.put(PREF_PAST_EVENTS_BACKGROUND_COLOR, pastEventsBackgroundColor);
            json.put(PREF_SHOW_DAYS_WITHOUT_EVENTS, showDaysWithoutEvents);
            json.put(PREF_SHOW_DAY_HEADERS, showDayHeaders);
            json.put(PREF_SHOW_PAST_EVENTS_WITH_DEFAULT_COLOR, showPastEventsWithDefaultColor);
            json.put(PREF_SHOW_END_TIME, showEndTime);
            json.put(PREF_SHOW_LOCATION, showLocation);
            json.put(PREF_DATE_FORMAT, dateFormat);
            json.put(PREF_ABBREVIATE_DATES, abbreviateDates);
            json.put(PREF_LOCKED_TIME_ZONE_ID, lockedTimeZoneId);
            json.put(PREF_EVENT_ENTRY_LAYOUT, eventEntryLayout.value);
            json.put(PREF_MULTILINE_TITLE, titleMultiline);
            json.put(PREF_SHOW_ONLY_CLOSEST_INSTANCE_OF_RECURRING_EVENT, showOnlyClosestInstanceOfRecurringEvent);
            json.put(PREF_INDICATE_ALERTS, indicateAlerts);
            json.put(PREF_INDICATE_RECURRING, indicateRecurring);
            json.put(PREF_ENTRY_THEME, entryTheme);
            json.put(PREF_HEADER_THEME, headerTheme);
            json.put(PREF_SHOW_WIDGET_HEADER, showWidgetHeader);
            json.put(PREF_BACKGROUND_COLOR, backgroundColor);
            json.put(PREF_TEXT_SIZE_SCALE, textSizeScale);
            json.put(PREF_DAY_HEADER_ALIGNMENT, dayHeaderAlignment);
            json.put(PREF_TASK_SOURCE, taskSource);
            json.put(PREF_ACTIVE_TASK_LISTS, new JSONArray(activeTaskLists));
        } catch (JSONException e) {
            throw new RuntimeException("Saving settings to JSON", e);
        }
        return json;
    }

    public Context getContext() {
        return context;
    }

    public int getWidgetId() {
        return widgetId;
    }

    public String getWidgetInstanceName() {
        return widgetInstanceName;
    }

    public Set<String> getActiveCalendars() {
        return activeCalendars;
    }

    public int getEventRange() {
        return eventRange;
    }

    public EndedSomeTimeAgo getEventsEnded() {
        return eventsEnded;
    }

    public boolean getFillAllDayEvents() {
        return fillAllDayEvents;
    }

    public String getHideBasedOnKeywords() {
        return hideBasedOnKeywords;
    }

    public int getPastEventsBackgroundColor() {
        return pastEventsBackgroundColor;
    }

    public boolean getShowDaysWithoutEvents() {
        return showDaysWithoutEvents;
    }

    public boolean getShowDayHeaders() {
        return showDayHeaders;
    }

    public boolean getShowPastEventsWithDefaultColor() {
        return showPastEventsWithDefaultColor;
    }

    public boolean getShowEndTime() {
        return showEndTime;
    }

    public boolean getShowLocation() {
        return showLocation;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public boolean getAbbreviateDates() {
        return abbreviateDates;
    }

    private void setLockedTimeZoneId(String lockedTimeZoneId) {
        this.lockedTimeZoneId = DateUtil.validatedTimeZoneId(lockedTimeZoneId);
    }

    public String getLockedTimeZoneId() {
        return lockedTimeZoneId;
    }

    public boolean isTimeZoneLocked() {
        return !TextUtils.isEmpty(lockedTimeZoneId);
    }

    public DateTimeZone getTimeZone() {
        return DateTimeZone.forID(DateUtil.validatedTimeZoneId(
                isTimeZoneLocked() ? lockedTimeZoneId : TimeZone.getDefault().getID()));
    }

    public EventEntryLayout getEventEntryLayout() {
        return eventEntryLayout;
    }

    public boolean isTitleMultiline() {
        return titleMultiline;
    }

    public boolean getShowOnlyClosestInstanceOfRecurringEvent() {
        return showOnlyClosestInstanceOfRecurringEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceSettings settings = (InstanceSettings) o;
        return toJson().toString().equals(settings.toJson().toString());
    }

    @Override
    public int hashCode() {
        return toJson().toString().hashCode();
    }

    public boolean getIndicateAlerts() {
        return indicateAlerts;
    }

    public boolean getIndicateRecurring() {
        return indicateRecurring;
    }

    public String getHeaderTheme() {
        return headerTheme;
    }

    public ContextThemeWrapper getHeaderThemeContext() {
        if (headerThemeContext == null) {
            headerThemeContext = new ContextThemeWrapper(context, themeNameToResId(headerTheme));
        }
        return headerThemeContext;
    }

    public String getEntryTheme() {
        return entryTheme;
    }

    public ContextThemeWrapper getEntryThemeContext() {
        if (entryThemeContext == null) {
            entryThemeContext = new ContextThemeWrapper(context, themeNameToResId(entryTheme));
        }
        return entryThemeContext;
    }

    public boolean getShowWidgetHeader() {
        return showWidgetHeader;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public String getTextSizeScale() {
        return textSizeScale;
    }

    public String getDayHeaderAlignment() {
        return dayHeaderAlignment;
    }

    public String getTaskSource() {
        return taskSource;
    }

    public Set<String> getActiveTaskLists() {
        return activeTaskLists;
    }
}
