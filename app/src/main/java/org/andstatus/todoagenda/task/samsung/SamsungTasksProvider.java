package org.andstatus.todoagenda.task.samsung;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.andstatus.todoagenda.task.AbstractTaskProvider;
import org.andstatus.todoagenda.task.TaskEvent;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

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
        String[] projection = {
                SamsungTasksContract.COLUMN_ID,
                SamsungTasksContract.COLUMN_TITLE,
                SamsungTasksContract.COLUMN_DUE_DATE
        };
        String where = getWhereClause();

        Cursor cursor = context.getContentResolver().query(SamsungTasksContract.PROVIDER_URI, projection,
                where, null, null);
        if (cursor == null) {
            return new ArrayList<>();
        }

        List<TaskEvent> tasks = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                TaskEvent task = createTask(cursor);
                if (!mKeywordsFilter.matched(task.getTitle())) {
                    tasks.add(task);
                }
            }
        } finally {
            cursor.close();
        }

        return tasks;
    }

    private String getWhereClause() {
        StringBuilder whereBuilder = new StringBuilder();
        whereBuilder.append(SamsungTasksContract.COLUMN_COMPLETE).append(EQUALS).append("0");
        whereBuilder.append(AND).append(SamsungTasksContract.COLUMN_DELETED).append(EQUALS).append("0");

        whereBuilder.append(AND_BRACKET)
                .append(SamsungTasksContract.COLUMN_DUE_DATE).append(LTE).append(mEndOfTimeRange.getMillis())
                .append(OR)
                .append(SamsungTasksContract.COLUMN_DUE_DATE).append(IS_NULL)
                .append(CLOSING_BRACKET);

        return whereBuilder.toString();
    }

    private TaskEvent createTask(Cursor cursor) {
        TaskEvent task = new SamsungTaskEvent();
        task.setId(cursor.getLong(cursor.getColumnIndex(SamsungTasksContract.COLUMN_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndex(SamsungTasksContract.COLUMN_TITLE)));

        int dueDateIdx = cursor.getColumnIndex(SamsungTasksContract.COLUMN_DUE_DATE);
        Long dueMillis = null;
        if (!cursor.isNull(dueDateIdx)) {
            dueMillis = cursor.getLong(dueDateIdx);
        }
        task.setStartDate(getDueDate(dueMillis));

        return task;
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
