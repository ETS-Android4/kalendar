package org.andstatus.todoagenda.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.larswerkman.holocolorpicker.ColorPicker;

import org.andstatus.todoagenda.R;

public class BackgroundTransparencyDialog extends DialogFragment {
    private ColorPicker picker;
    private String prefKey;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View layout = inflater.inflate(R.layout.background_color, null);
        picker = layout.findViewById(R.id.background_color_picker);
        picker.addSVBar(layout.findViewById(R.id.background_color_svbar));
        picker.addOpacityBar(layout.findViewById(R.id.background_color_opacitybar));
        prefKey = getTag().equals(InstanceSettings.PREF_PAST_EVENTS_BACKGROUND_COLOR)
                ? InstanceSettings.PREF_PAST_EVENTS_BACKGROUND_COLOR
                : InstanceSettings.PREF_BACKGROUND_COLOR;
        int color = ApplicationPreferences.getInt(getActivity(), prefKey,
                getTag().equals(InstanceSettings.PREF_PAST_EVENTS_BACKGROUND_COLOR)
                        ? InstanceSettings.PREF_PAST_EVENTS_BACKGROUND_COLOR_DEFAULT
                        : InstanceSettings.PREF_BACKGROUND_COLOR_DEFAULT);
        // android.util.Log.v("Color", "key:" + prefKey + "; color:0x" + Integer.toString(color, 16));
        picker.setColor(color);
        picker.setOldCenterColor(color);
        return createDialog(layout);
    }

    private Dialog createDialog(View layout) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getTag().equals(InstanceSettings.PREF_PAST_EVENTS_BACKGROUND_COLOR)
                ? R.string.appearance_past_events_background_color_title
                : R.string.appearance_background_color_title);
        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> ApplicationPreferences.setInt(getActivity(), prefKey, picker.getColor()));
        return builder.create();
    }
}
