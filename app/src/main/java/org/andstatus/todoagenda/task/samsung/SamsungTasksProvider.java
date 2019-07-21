package org.andstatus.todoagenda.task.samsung;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.andstatus.todoagenda.R;
import org.andstatus.todoagenda.calendar.CalendarQueryResult;
import org.andstatus.todoagenda.calendar.CalendarQueryResultsStorage;
import org.andstatus.todoagenda.prefs.EventSource;
import org.andstatus.todoagenda.task.AbstractTaskProvider;
import org.andstatus.todoagenda.task.TaskEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SamsungTasksProvider extends AbstractTaskProvider {

    public SamsungTasksProvider(Context context, int widgetId) {
        super(context, widgetId);
    }

    @Override
    public List<TaskEvent> getTasks() {
        initialiseParameters();
        return queryTasks();
    }

    private List<TaskEvent> queryTasks() {
        Uri uri = SamsungTasksContract.Tasks.PROVIDER_URI;
        String[] projection = {
                SamsungTasksContract.Tasks.COLUMN_ID,
                SamsungTasksContract.Tasks.COLUMN_TITLE,
                SamsungTasksContract.Tasks.COLUMN_DUE_DATE,
                SamsungTasksContract.Tasks.COLUMN_COLOR,
                SamsungTasksContract.Tasks.COLUMN_LIST_ID,
        };
        String where = getWhereClause();

        CalendarQueryResult result = new CalendarQueryResult(getSettings(), uri, projection, where, null, null);

        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(uri, projection, where, null, null);
        } catch (IllegalArgumentException e) {
            cursor = null;
        }
        if (cursor == null) {
            return new ArrayList<>();
        }

        List<TaskEvent> tasks = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                if (CalendarQueryResultsStorage.getNeedToStoreResults()) {
                    result.addRow(cursor);
                }

                TaskEvent task = createTask(cursor);
                if (!mKeywordsFilter.matched(task.getTitle())) {
                    tasks.add(task);
                }
            }
        } finally {
            cursor.close();
        }

        CalendarQueryResultsStorage.storeTask(result);

        return tasks;
    }

    private String getWhereClause() {
        StringBuilder whereBuilder = new StringBuilder();
        whereBuilder.append(SamsungTasksContract.Tasks.COLUMN_COMPLETE).append(EQUALS).append("0");
        whereBuilder.append(AND).append(SamsungTasksContract.Tasks.COLUMN_DELETED).append(EQUALS).append("0");

        whereBuilder.append(AND_BRACKET)
                .append(SamsungTasksContract.Tasks.COLUMN_DUE_DATE).append(LTE).append(mEndOfTimeRange.getMillis())
                .append(OR)
                .append(SamsungTasksContract.Tasks.COLUMN_DUE_DATE).append(IS_NULL)
                .append(CLOSING_BRACKET);

        Set<String> taskLists = getSettings().getActiveTaskLists();
        if (!taskLists.isEmpty()) {
            whereBuilder.append(AND);
            whereBuilder.append(SamsungTasksContract.Tasks.COLUMN_LIST_ID);
            whereBuilder.append(" IN ( ");
            whereBuilder.append(TextUtils.join(",", taskLists));
            whereBuilder.append(CLOSING_BRACKET);
        }

        return whereBuilder.toString();
    }

    private TaskEvent createTask(Cursor cursor) {
        TaskEvent task = new SamsungTaskEvent();
        task.setId(cursor.getLong(cursor.getColumnIndex(SamsungTasksContract.Tasks.COLUMN_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndex(SamsungTasksContract.Tasks.COLUMN_TITLE)));

        int dueDateIdx = cursor.getColumnIndex(SamsungTasksContract.Tasks.COLUMN_DUE_DATE);
        Long dueMillis = null;
        if (!cursor.isNull(dueDateIdx)) {
            dueMillis = cursor.getLong(dueDateIdx);
        }
        task.setTaskDate(getTaskDate(dueMillis, null));

        task.setColor(getColor(cursor, cursor.getColumnIndex(SamsungTasksContract.Tasks.COLUMN_COLOR),
                cursor.getInt(cursor.getColumnIndex(SamsungTasksContract.Tasks.COLUMN_LIST_ID))));

        return task;
    }

    @Override
    public Collection<EventSource> getTaskLists() {
        ArrayList<EventSource> eventSources = new ArrayList<>();

        String[] projection = {
                SamsungTasksContract.TaskLists.COLUMN_ID,
                SamsungTasksContract.TaskLists.COLUMN_NAME,
                SamsungTasksContract.TaskLists.COLUMN_COLOR,
        };
        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(SamsungTasksContract.TaskLists.PROVIDER_URI, projection, null, null, null);
        } catch (IllegalArgumentException e) {
            cursor = null;
        }
        if (cursor == null) {
            return eventSources;
        }

        String taskListName = context.getResources().getString(R.string.task_prefs);
        int idIdx = cursor.getColumnIndex(SamsungTasksContract.TaskLists.COLUMN_ID);
        int nameIdx = cursor.getColumnIndex(SamsungTasksContract.TaskLists.COLUMN_NAME);
        int colorIdx = cursor.getColumnIndex(SamsungTasksContract.TaskLists.COLUMN_COLOR);
        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(idIdx);
                EventSource eventSource = new EventSource(id, taskListName,
                        cursor.getString(nameIdx), getColor(cursor, colorIdx, id));
                eventSources.add(eventSource);
            }
        } finally {
            cursor.close();
        }

        return eventSources;
    }

    private int getColor(Cursor cursor, int colorIdx, int accountId) {
        if (!cursor.isNull(colorIdx)) {
            return getAsOpaque(cursor.getInt(colorIdx));
        } else {
            int[] fixedColors = context.getResources().getIntArray(R.array.task_list_colors);
            int arrayIdx = accountId % fixedColors.length;
            return fixedColors[arrayIdx];
        }
    }

    @Override
    public boolean hasPermission() {
        return true;
    }

    @Override
    public void requestPermission(Activity activity) {
        // Requires just android.permission.READ_CALENDAR, which is expected to be granted already
    }
}
