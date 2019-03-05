package tk.bungeefan.vokabel_shamader16;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import java.util.List;

public class VocableAdapter<T> extends ArrayAdapter {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SharedPreferences prefs;

    public VocableAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }
}
