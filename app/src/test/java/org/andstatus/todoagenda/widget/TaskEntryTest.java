package org.andstatus.todoagenda.widget;

import org.andstatus.todoagenda.task.TaskEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TaskEntryTest {
    @Test
    public void isCurrent_forTodayTask_returnsTrue() {
        TaskEvent task = new TaskEvent();
        task.setZone(DateTimeZone.getDefault());
        task.setDates(DateTime.now().withTimeAtStartOfDay().plusHours(5).getMillis(), null);
        TaskEntry taskEntry = TaskEntry.fromEvent(task);

        assertThat(taskEntry.isCurrent()).isTrue();
    }

    @Test
    public void isCurrent_forFutureTask_returnsFalse() {
        TaskEvent task = new TaskEvent();
        task.setZone(DateTimeZone.getDefault());
        task.setDates(DateTime.now().plusDays(2).getMillis(), null);
        TaskEntry taskEntry = TaskEntry.fromEvent(task);

        assertThat(taskEntry.isCurrent()).isFalse();
    }
}
