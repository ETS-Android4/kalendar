package org.andstatus.todoagenda;

import android.test.InstrumentationTestCase;

import org.andstatus.todoagenda.calendar.MockCalendarContentProvider;
import org.andstatus.todoagenda.prefs.InstanceSettingsTestHelper;
import org.andstatus.todoagenda.provider.QueryRow;
import org.andstatus.todoagenda.util.DateUtil;
import org.andstatus.todoagenda.util.TestHelpers;
import org.andstatus.todoagenda.widget.CalendarEntry;
import org.andstatus.todoagenda.widget.LastEntry;
import org.andstatus.todoagenda.widget.WidgetEntry;
import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

/**
 * @author yvolk@yurivolkov.com
 */
public class RecurringEventsTest extends InstrumentationTestCase {

    private static final String TAG = RecurringEventsTest.class.getSimpleName();

    private MockCalendarContentProvider provider = null;
    private EventRemoteViewsFactory factory = null;
    private int eventId = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        provider = MockCalendarContentProvider.getContentProvider(this);
        factory = new EventRemoteViewsFactory(provider.getContext(), provider.getWidgetId());
        assertTrue(factory.getWidgetEntries().get(0) instanceof LastEntry);
        eventId = 0;
    }

    @Override
    protected void tearDown() throws Exception {
        provider.tearDown();
        super.tearDown();
    }

    /**
     * @see <a href="https://github.com/plusonelabs/calendar-widget/issues/191">Issue 191</a> and
     * <a href="https://github.com/plusonelabs/calendar-widget/issues/46">Issue 46</a>
     */
    public void testShowRecurringEvents() {
        generateEventInstances();
        assertEquals("Entries: " + factory.getWidgetEntries().size(), 15, countCalendarEntries());

        InstanceSettingsTestHelper settingsHelper = new InstanceSettingsTestHelper(provider.getContext(),
                provider.getWidgetId());
        settingsHelper.setShowOnlyClosestInstanceOfRecurringEvent(true);

        generateEventInstances();
        assertEquals("Entries: " + factory.getWidgetEntries().size(), 1, countCalendarEntries());
    }

    int countCalendarEntries() {
        int count = 0;
        for (WidgetEntry widgetEntry : factory.getWidgetEntries()) {
            if (CalendarEntry.class.isAssignableFrom(widgetEntry.getClass())) {
                count++;
            }
        }
        return count;
    }

    void generateEventInstances() {
        TestHelpers.forceReload(factory);
        provider.clear();
        DateTime date = DateUtil.now(provider.getSettings().getTimeZone()).withTimeAtStartOfDay();
        long millis = date.getMillis() + TimeUnit.HOURS.toMillis(10);
        eventId++;
        for (int ind = 0; ind < 15; ind++) {
            millis += TimeUnit.DAYS.toMillis(1);
            provider.addRow(new QueryRow().setEventId(eventId).setTitle("Work each day")
                    .setBegin(millis).setEnd(millis + TimeUnit.HOURS.toMillis(9)));
        }
        factory.onDataSetChanged();
        factory.logWidgetEntries(TAG);
    }
}
