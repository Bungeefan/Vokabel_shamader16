package tk.bungeefan.vokabel_shamader16;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FILE_NAME = "180a_vokabel.csv";
    private Map<String, List<String>> de_en = new HashMap<>();
    private Map<String, List<String>> en_de = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private Spinner spinner;
    private SearchView searchView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        assertPreferencesInFile();
        ListView listView = findViewById(R.id.listView);
        registerForContextMenu(listView);
        if (existsData()) {
            try {
                readData(openFileInput(FILE_NAME));
            } catch (FileNotFoundException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        } else {
            readData(getInputStreamForAsset());
        }
        listView.setAdapter(adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1));

        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.getFilter().filter(s);
                return false;
            }
        });

        spinner = findViewById(R.id.spinner);
        spinner.setSelection(prefs.getInt("sort_preferences", 0));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("sort_preferences", position).apply();
                sortData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sortData();
    }

    private void assertPreferencesInFile() {
        try {
            String versionKey = "longVersionCode";
            long currentVersion = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .getLongVersionCode();
            long lastStoredVersion = prefs.getLong(versionKey, -1);
            if (lastStoredVersion == currentVersion) return;
            prefs.edit()
                    .putLong(versionKey, currentVersion)
                    .putInt("sort_preferences", prefs.getInt("sort_preferences", 0))
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private View getAddDialog() {
        final View vDialog = getLayoutInflater().inflate(R.layout.dialog, null);

        EditText firstField = vDialog.findViewById(R.id.firstField);
        EditText secondField = vDialog.findViewById(R.id.secondField);
        (prefs.getInt("sort_preferences", 0) == 0 ? firstField : secondField).setHint("Deutsch");
        (prefs.getInt("sort_preferences", 0) == 1 ? firstField : secondField).setHint("Englisch");

        return vDialog;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View vDialog = getAddDialog();
        switch (item.getItemId()) {
            case R.id.addItem:
                new AlertDialog.Builder(this)
                        .setTitle("Neue Vokabel")
                        .setView(vDialog)
                        .setPositiveButton("Hinzufügen", (dialog, which) -> {
                            EditText firstField = vDialog.findViewById(R.id.firstField);
                            EditText secondField = vDialog.findViewById(R.id.secondField);
                            if (prefs.getInt("sort_preferences", 0) == 0) {
                                addVocab(firstField, secondField);
                            } else {
                                addVocab(secondField, firstField);
                            }
                            sortData();
                        })
                        .setNegativeButton("Abbrechen", null)
                        .show();
                break;
            case R.id.saveItem:
                writeData();
                Snackbar.make(findViewById(android.R.id.content), "Vokabeln wurden gespeichert!", Snackbar.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addVocab(EditText germanText, EditText englishText) {
        addVocab(germanText.getText().toString(), englishText.getText().toString());
    }

    private void addVocab(String germanWord, String englishWord) {
        addVocab(de_en, germanWord, englishWord);
        addVocab(en_de, englishWord, germanWord);
    }

    private void addVocab(Map<String, List<String>> map, String firstWord, String secondWord) {
        List<String> list;
        if (map.containsKey(firstWord)) {
            list = map.get(firstWord);
        } else {
            list = new ArrayList<>();
        }
        list.add(secondWord);
        map.put(firstWord, list);
    }

    private void sortData() {
        adapter.clear();
        for (Map.Entry<String, List<String>> entry : (spinner.getSelectedItemPosition() == 0 ? de_en : en_de).entrySet()) {
            adapter.add(entry.getKey() + ": " + String.join(", ", entry.getValue()));
        }
        adapter.sort(String::compareTo);
        adapter.getFilter().filter(null);
    }

    private boolean existsData() {
        return getApplicationContext().getFileStreamPath(FILE_NAME).exists();
    }

    private void readData(InputStream inputStream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            de_en.clear();
            en_de.clear();
            while (in.ready()) {
                String[] split = in.readLine().split(";");
                if (split.length >= 2) {
                    String germanWord = split[0];
                    String englishWord = split[1];

                    addVocab(germanWord, englishWord);
                }
            }
        } catch (IOException exp) {
            Log.d(TAG, Log.getStackTraceString(exp));
        }
    }

    private void writeData() {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(openFileOutput(FILE_NAME, MODE_PRIVATE)))) {
            for (Map.Entry<String, List<String>> entry : de_en.entrySet()) {
                for (String string : entry.getValue()) {
                    out.println(entry.getKey() + ";" + string);
                }
            }
            out.println();
        } catch (FileNotFoundException exp) {
            Log.d(TAG, Log.getStackTraceString(exp));
        }
    }

    private InputStream getInputStreamForAsset() {
        try {
            return getAssets().open(FILE_NAME);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return null;
        }
    }
}
