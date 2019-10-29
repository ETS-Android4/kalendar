package org.andstatus.todoagenda.prefs;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import org.andstatus.todoagenda.task.TaskProvider;

import java.util.Collection;
import java.util.Set;

public class TaskListsPreferencesFragment extends AbstractEventSourcesPreferencesFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Set<String> fetchInitialActiveSources() {
        return instanceSettings.getActiveTaskLists();
    }

    @Override
    protected Collection<EventSource> fetchAvailableSources() {
        Intent intent = getActivity().getIntent();
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        return new TaskProvider(getActivity(), widgetId).getTaskLists(instanceSettings.getTaskSource());
    }

    @Override
    protected void storeSelectedSources(Set<String> selectedSources) {
        instanceSettings.setActiveTaskLists(selectedSources);
    }
}
