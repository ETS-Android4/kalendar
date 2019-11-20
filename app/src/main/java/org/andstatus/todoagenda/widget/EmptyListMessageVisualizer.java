package org.andstatus.todoagenda.widget;

import android.app.PendingIntent;
import android.widget.RemoteViews;
import androidx.annotation.StringRes;
import androidx.arch.core.util.Function;

import org.andstatus.todoagenda.EventRemoteViewsFactory;
import org.andstatus.todoagenda.R;
import org.andstatus.todoagenda.prefs.InstanceSettings;
import org.andstatus.todoagenda.util.PermissionsUtil;

import static org.andstatus.todoagenda.util.RemoteViewsUtil.setBackgroundColor;
import static org.andstatus.todoagenda.util.RemoteViewsUtil.setTextSize;

public class EmptyListMessageVisualizer {
    private InstanceSettings settings;

    public EmptyListMessageVisualizer(InstanceSettings settings) {
        this.settings = settings;
    }

    public RemoteViews getView(Type type) {
        RemoteViews rv = new RemoteViews(settings.getContext().getPackageName(), R.layout.item_empty_list);

        rv.setTextViewText(R.id.event_entry, settings.getContext().getString(type.message));
        setTextSize(settings, rv, R.id.event_entry, R.dimen.event_entry_title);

        rv.setTextColor(R.id.event_entry, settings.getEventColor());
        setBackgroundColor(rv, R.id.event_entry, settings.getBackgroundColor());

        rv.setOnClickPendingIntent(R.id.event_entry, type.getIntent(settings));

        return rv;
    }

    public enum Type {
        EMPTY(R.string.no_events_to_show, EventRemoteViewsFactory::getPermittedAddEventPendingIntent),
        NO_PERMISSIONS(R.string.grant_permissions_verbose, PermissionsUtil::getNoPermissionsPendingIntent);

        @StringRes private int message;
        private Function<InstanceSettings, PendingIntent> intentSupplier;

        Type(@StringRes int message, Function<InstanceSettings, PendingIntent> intentSupplier) {
            this.message = message;
            this.intentSupplier = intentSupplier;
        }

        private PendingIntent getIntent(InstanceSettings settings) {
            return intentSupplier.apply(settings);
        }
    }
}
