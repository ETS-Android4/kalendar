package org.andstatus.todoagenda.task.dmfs;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.andstatus.todoagenda.calendar.CalendarQueryResult;
import org.andstatus.todoagenda.calendar.CalendarQueryResultsStorage;
import org.andstatus.todoagenda.prefs.EventSource;
import org.andstatus.todoagenda.task.AbstractTaskProvider;
import org.andstatus.todoagenda.task.TaskEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DmfsOpenTasksProvider extends AbstractTaskProvider {

    public DmfsOpenTasksProvider(Context context, int widgetId) {
        super(context, widgetId);
    }

    @Override
    public List<TaskEvent> getTasks() {
        if (!hasPermission()) {
            return new ArrayList<>();
        }

        initialiseParameters();

        return queryTasks();
    }

    private List<TaskEvent> queryTasks() {
        Uri uri = DmfsOpenTasksContract.Tasks.PROVIDER_URI;
        String[] projection = {
                DmfsOpenTasksContract.Tasks.COLUMN_ID,
                DmfsOpenTasksContract.Tasks.COLUMN_TITLE,
                DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE,
                DmfsOpenTasksContract.Tasks.COLUMN_START_DATE,
                DmfsOpenTasksContract.Tasks.COLUMN_COLOR,
        };
        String where = getWhereClause();

        CalendarQueryResult result = new CalendarQueryResult(getSettings(), uri, projection, where, null, null);

        Cursor cursor = context.getContentResolver().query(uri, projection, where, null, null);
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

        whereBuilder.append(DmfsOpenTasksContract.Tasks.COLUMN_STATUS).append(NOT_EQUALS).append(DmfsOpenTasksContract.Tasks.STATUS_COMPLETED);

        // @formatter:off
        whereBuilder.append(AND_BRACKET)
                .append(DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE).append(LTE).append(mEndOfTimeRange.getMillis())
                .append(OR)
                    .append(OPEN_BRACKET)
                        .append(DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE).append(IS_NULL)
                        .append(AND_BRACKET)
                            .append(DmfsOpenTasksContract.Tasks.COLUMN_START_DATE).append(LTE).append(mEndOfTimeRange.getMillis())
                            .append(OR)
                            .append(DmfsOpenTasksContract.Tasks.COLUMN_START_DATE).append(IS_NULL)
                        .append(CLOSING_BRACKET)
                    .append(CLOSING_BRACKET)
                .append(CLOSING_BRACKET);
        // @formatter:on

        Set<String> taskLists = getSettings().getActiveTaskLists();
        if (!taskLists.isEmpty()) {
            whereBuilder.append(AND);
            whereBuilder.append(DmfsOpenTasksContract.Tasks.COLUMN_LIST_ID);
            whereBuilder.append(" IN ( ");
            whereBuilder.append(TextUtils.join(",", taskLists));
            whereBuilder.append(CLOSING_BRACKET);
        }

        return whereBuilder.toString();
    }

    private TaskEvent createTask(Cursor cursor) {
        TaskEvent task = new DmfsOpenTasksEvent();
        task.setId(cursor.getLong(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_TITLE)));

        int dueDateIdx = cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE);
        Long dueMillis = null;
        if (!cursor.isNull(dueDateIdx)) {
            dueMillis = cursor.getLong(dueDateIdx);
        }
        int startDateIdx = cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_START_DATE);
        Long startMillis = null;
        if (!cursor.isNull(startDateIdx)) {
            startMillis = cursor.getLong(startDateIdx);
        }
        task.setTaskDate(getTaskDate(dueMillis, startMillis));

        task.setColor(getAsOpaque(cursor.getInt(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_COLOR))));

        return task;
    }

    @Override
    public Collection<EventSource> getTaskLists() {
        ArrayList<EventSource> eventSources = new ArrayList<>();

        String[] projection = {
                DmfsOpenTasksContract.TaskLists.COLUMN_ID,
                DmfsOpenTasksContract.TaskLists.COLUMN_NAME,
                DmfsOpenTasksContract.TaskLists.COLUMN_COLOR,
                DmfsOpenTasksContract.TaskLists.COLUMN_ACCOUNT_NAME,
        };
        Cursor cursor = context.getContentResolver().query(DmfsOpenTasksContract.TaskLists.PROVIDER_URI, projection, null, null, null);
        if (cursor == null) {
            return eventSources;
        }

        int idIdx = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_ID);
        int nameIdx = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_NAME);
        int colorIdx = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_COLOR);
        int accountIdx = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_ACCOUNT_NAME);
        try {
            while (cursor.moveToNext()) {
                EventSource eventSource = new EventSource(cursor.getInt(idIdx), cursor.getString(nameIdx),
                        cursor.getString(accountIdx), cursor.getInt(colorIdx));
                eventSources.add(eventSource);
            }
        } finally {
            cursor.close();
        }

        return eventSources;
    }

    @Override
    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, DmfsOpenTasksContract.PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{DmfsOpenTasksContract.PERMISSION}, 1);
    }
}
