package com.example.imageorganizer;

import static android.content.ContentValues.TAG;
import static android.os.Environment.MEDIA_MOUNTED;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;


//https://www.youtube.com/watch?v=zg0_YS9PYi4
//https://www.geeksforgeeks.org/how-to-build-a-photo-viewing-application-in-android/
//https://developer.android.com/topic/libraries/architecture/datastore#java
//https://stackoverflow.com/questions/75443067/preferences-datastore-in-android-using-java
//https://medium.com/@deadmanapple/using-the-android-datastore-library-instead-of-sharedpreferences-in-java-d6744c348a05

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private static final int PERMISSION_REQUEST_CODE = 111;

    private RecyclerView recycler;
    private ArrayList<String> images;
    private TextView totalImages;

    RxDataStore<Preferences> dataStore;
    Preferences.Key<Set<String>> FILTER_KEY = PreferencesKeys.stringSetKey("filter_key");
    ArrayList<String> filterList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
        if (dataStoreSingleton.getDataStore() == null) {
            dataStore = new RxPreferenceDataStoreBuilder(this, "filters").build();
        } else {
            dataStore = dataStoreSingleton.getDataStore();
        }
        dataStoreSingleton.setDataStore(dataStore);

        recycler = findViewById(R.id.gallery_recycler);
        images = new ArrayList<>();
        GalleryAdaptor adaptor = new GalleryAdaptor(MainActivity.this, images);


        recycler.setAdapter(adaptor);

        totalImages = findViewById(R.id.total_images);

        requestPermissions();

        prepareRecyclerView();

        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(this::showMenu);

        ImageButton filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(view -> showFilters());

    }

    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.popup_menu);
        popup.show();
    }


    private void showFilters() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.filter_layout);

        ChipGroup filterChips = dialog.findViewById(R.id.filter_chips);

        Button clearButton = dialog.findViewById(R.id.clear_filter_button);
        Button okButton = dialog.findViewById(R.id.ok_button);
        
        clearButton.setOnClickListener(view -> {
            clearFilters();
            dialog.dismiss();
        });
        okButton.setOnClickListener(view -> {
            enterFilters(dialog, filterChips);
            dialog.dismiss();
        });

        generateFilters(filterChips);
        dialog.show();
    }

    private void enterFilters(Dialog dialog, ChipGroup chipGroup) {
        List<Integer> chipIds = chipGroup.getCheckedChipIds();
        ArrayList<String> checkedChips = new ArrayList<>();
        for(Integer id : chipIds) {
            Chip chip = dialog.findViewById(id);
            checkedChips.add(chip.getText().toString());
        }
        filterList = checkedChips;
    }

    private void clearFilters() {
        filterList.clear();
    }

    private void generateFilters(ChipGroup chipGroup) {
        Flowable<Set<String>> testSingle = dataStore.data().map(prefs -> prefs.get(FILTER_KEY)).onErrorResumeNext(throwable -> {
            Log.e(TAG, "Error reading data from DataStore", throwable);
            return Flowable.just(new HashSet<>());
        });
        testSingle.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(filterSet -> {
            for (String filterStr : filterSet) {
                Chip filterChip = new Chip(this);
                filterChip.setText(filterStr);
                filterChip.setCheckable(true);
                filterChip.setChecked(filterList.contains(filterStr));
                chipGroup.addView(filterChip);
            }
        },
        throwable -> Log.e(TAG, "generateFilters: ", throwable));

    }

    private void requestPermissions() {
        if (checkPermission()) {
            Toast.makeText(this, "Permissions granted..", Toast.LENGTH_SHORT).show();
            loadImages();
        } else {
            requestPermission();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void prepareRecyclerView() {

        GridLayoutManager manager = new GridLayoutManager(MainActivity.this, 4);

        recycler.setLayoutManager(manager);
    }

    private void loadImages() {
        boolean SDCard = Environment.getExternalStorageState().equals(MEDIA_MOUNTED);
        if (SDCard) {
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
            final String order = MediaStore.Images.Media.DATE_TAKEN + " DESC";

            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, order);

            int count = cursor != null ? cursor.getCount() : 0;

            totalImages.setText("Total Images: " + count);

            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                int colIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                images.add(cursor.getString(colIndex));
            }

            Objects.requireNonNull(recycler.getAdapter()).notifyDataSetChanged();
            if (cursor != null) { cursor.close(); }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] Permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, Permissions, grantResults);
        // we are checking the permission code.
        if (requestCode == PERMISSION_REQUEST_CODE) {// in this case we are checking if the permissions are accepted or not.
            if (grantResults.length > 0) {
                boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (storageAccepted) {
                    // if the permissions are accepted we are displaying a toast message
                    // and calling a method to get image path.
                    Toast.makeText(this, "Permissions Granted..", Toast.LENGTH_SHORT).show();
                    loadImages();
                } else {
                    // if permissions are denied we are closing the app and displaying the toast message.
                    Toast.makeText(this, "Permissions denied, Permissions are required to use the app..",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_image_button) {
            addImage();
        } else if (itemId == R.id.add_filter_button) {
            addFilter();
        } else if (itemId == R.id.remove_filter_button) {
            removeFilter();
        } else {
            return false;
        }
        return true;
    }

    private void addImage() {
        //TODO: write code
        Toast.makeText(getApplicationContext(), "test addImage()", Toast.LENGTH_SHORT).show();
    }

    private void removeFilter() {
        //TODO: Figure out how I want this thing to work and replace this code block with it
        dataStore.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.remove(FILTER_KEY);
            return Single.just(mutablePreferences);
        });
        Toast.makeText(this, "remove filter test", Toast.LENGTH_SHORT).show();
    }

    private void addFilter() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Filter");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);

        builder.setPositiveButton("OK", (dialogInterface, i) -> dataStore.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            String inputStr = input.getText().toString();
            Set<String> filter_set;

            boolean setExists = mutablePreferences.get(FILTER_KEY) != null;

            if(setExists) { filter_set = new HashSet<>(mutablePreferences.get(FILTER_KEY)); }
            else { filter_set = new HashSet<>(); }

            filter_set.add(inputStr);

            mutablePreferences.set(FILTER_KEY, filter_set);
            return Single.just(mutablePreferences);
        }));

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();

    }
}