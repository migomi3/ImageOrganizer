package com.example.imageorganizer;

import static android.os.Environment.MEDIA_MOUNTED;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.List;
import java.util.Objects;


//https://www.youtube.com/watch?v=zg0_YS9PYi4
//https://www.geeksforgeeks.org/how-to-build-a-photo-viewing-application-in-android/

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private static final int PERMISSION_REQUEST_CODE = 111;

    private RecyclerView recycler;
    private ArrayList<String> images;
    private TextView totalImages;
    private SQLiteManager dbManager;
    private String[] filters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        recycler = findViewById(R.id.gallery_recycler);
        images = new ArrayList<>();
        GalleryAdaptor adaptor = new GalleryAdaptor(MainActivity.this, images);
        dbManager = new SQLiteManager(this);


        recycler.setAdapter(adaptor);

        totalImages = findViewById(R.id.total_images);

        requestPermissions();

        prepareRecyclerView();

        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(this::showMenu);

        ImageButton filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(view -> showFilters());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        //TODO: where tf are the filters coming from here?
        //TODO: fix filter logic
        List<Integer> chipIds = chipGroup.getCheckedChipIds();
        ArrayList<String> checkedChips = new ArrayList<>();
        for(Integer id : chipIds) {
            Chip chip = dialog.findViewById(id);
            checkedChips.add(chip.getText().toString());
        }
        //filters = checkedChips;
    }

    private void clearFilters() {
        filters = null;
    }

    private void generateFilters(ChipGroup chipGroup) {
        //TODO: Rewrite logic
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
            final String[] pathCol = {TableClasses.Image.PATH_COL};
            String[] dbData = prepareImages(pathCol);
            loadNewImages(dbData);
            getImages(pathCol);
            Objects.requireNonNull(recycler.getAdapter()).notifyDataSetChanged();
        }
    }

    //TODO: add filtering logic
    private void getImages(String[] pathCol) {
        Cursor imagePaths = dbManager.selectFromImagePathTable(pathCol, null, null, null, null);

        int count = imagePaths != null ? imagePaths.getCount() : 0;

        String[] dbData = new String[count];

        totalImages.setText("Total Images: " + count);

        for (int i = 0; i < count; i++) {
            imagePaths.moveToPosition(i);
            int dataIndex = imagePaths.getColumnIndex(TableClasses.Image.PATH_COL);

            images.add(imagePaths.getString(dataIndex));
        }

        if (imagePaths != null) { imagePaths.close(); }
    }

    private String[] prepareImages(String[] pathCol) { //TODO: find better method name
        Cursor imagePaths = dbManager.selectFromImagePathTable(pathCol, null, null, null, null);

        int count = imagePaths != null ? imagePaths.getCount() : 0;

        String[] dbData = new String[count];

        for (int i = 0; i < count; i++) {
            imagePaths.moveToPosition(i);
            int dataIndex = imagePaths.getColumnIndex(TableClasses.Image.PATH_COL);

            dbData[i] = imagePaths.getString(dataIndex);
        }

        if (imagePaths != null) { imagePaths.close(); }

        return dbData;
    }

    public void loadNewImages(String[] paths) {
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_TAKEN};
        final String order = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        int pathCount = paths.length;
        String where = null;
        String[] whereArgs = null;

        if (pathCount > 0) {
            where = MediaStore.Images.Media.DATA + " NOT IN (";

            for (int i = 0; i < pathCount; i++) {
                where += "?, ";
            }

            where = where.substring(0, where.length() - 2) + ")";
            whereArgs = paths;
        }

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, where, whereArgs, order);

        int count = cursor != null ? cursor.getCount() : 0;

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);

            String data = cursor.getString(dataIndex);
            String name = cursor.getString(nameIndex);
            String date = cursor.getString(dateIndex);

            dbManager.insertToImagePathTable(data, name, date);
        }

        if (cursor != null) { cursor.close(); }
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
        //TODO: decide logic
        Toast.makeText(getApplicationContext(), "test addImage()", Toast.LENGTH_SHORT).show();
    }

    private void removeFilter() {
        //TODO: Figure out how I want this thing to work and replace this code block with it
        Toast.makeText(this, "remove filter test", Toast.LENGTH_SHORT).show();
    }

    private void addFilter() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Filter");

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);

        //TODO: add logic
        builder.setPositiveButton("OK", (dialogInterface, i) -> saveFilterInput(input.getText().toString()));

        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());

        builder.show();

    }

    public String saveFilterInput(String input) {
        return input;
    }
}