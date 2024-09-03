package com.example.imageorganizer;

import static android.os.Environment.MEDIA_MOUNTED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
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
        filterButton.setOnClickListener(view -> FilterDialogHelper.showFilters(this, new FilterDialogHelper.ShowFilterAction() {
            @Override
            public void onOkButtonPressed(Dialog dialog, ChipGroup chipGroup) {
                enterFilters(dialog, chipGroup);
            }

            @Override
            public void onNegativeButtonPressed() {
                getAllImages();
            }
        }));
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

    private void enterFilters(Dialog dialog, ChipGroup chipGroup) {
        List<Integer> chipIds = chipGroup.getCheckedChipIds();
        int listSize = chipIds.size();

        String[] checkedChips = new String[listSize];

        for(int i = 0; i < listSize; i++) {
            Chip chip = dialog.findViewById(chipIds.get(i));
            checkedChips[i] = chip.getText().toString();
        }

        getImages(checkedChips);
    }

    private void getAllImages() {
        getImages(null);
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
            loadNewImages(getCurrentImagePaths());
            getAllImages();
        }
    }

    private void getImages(@Nullable String[] filters) {
        String where = null;
        String[] imageIds = null;
        int filterCount = filters != null ? filters.length : 0;

        if (filters != null && filterCount > 0) {
            String filterSelection = dbManager.buildWhereClause(TableClasses.Filter.FILTER_COL, filterCount, true);
            Cursor filterCursor = dbManager.selectFromFilterTable(new String[]{TableClasses.Filter._ID}, filterSelection, filters, null, null);
            String[] filterIds = dbManager.extractFromCursor(filterCursor, TableClasses.Filter._ID);
            if (filterCursor != null) { filterCursor.close(); }

            if (filterIds != null && filterIds.length > 0) {
                String bridgeSelection = dbManager.buildWhereClause(TableClasses.ImageFilter.FILTER_ID_COL, filterIds.length, true);
                Cursor bridgeCursor = dbManager.selectFromImageFilterTable(new String[]{TableClasses.ImageFilter.IMAGE_ID_COL}, bridgeSelection, filterIds, null, null);
                imageIds = dbManager.extractFromCursor(bridgeCursor, TableClasses.ImageFilter.IMAGE_ID_COL);
                if (bridgeCursor != null) { bridgeCursor.close(); }
            }

            if (imageIds != null && imageIds.length > 0) {
                where = dbManager.buildWhereClause(TableClasses.Image._ID, imageIds.length, true);
            } else {
                Toast.makeText(this,"No matches found; Resetting filters", Toast.LENGTH_LONG).show();
            }
        }

        Cursor imagePaths = dbManager.selectFromImagePathTable(new String[]{TableClasses.Image.PATH_COL}, where, imageIds, null, null);

        int count = imagePaths != null ? imagePaths.getCount() : 0;

        totalImages.setText("Total Images: " + count);

        images.clear();
        Collections.addAll(images, dbManager.extractFromCursor(imagePaths, TableClasses.Image.PATH_COL));

        if (imagePaths != null) { imagePaths.close(); }

        Objects.requireNonNull(recycler.getAdapter()).notifyDataSetChanged();
    }

    private String[] getCurrentImagePaths() {
        Cursor imagePaths = dbManager.selectFromImagePathTable(new String[]{TableClasses.Image.PATH_COL}, null, null, null, null);

        String[] dbData = dbManager.extractFromCursor(imagePaths, TableClasses.Image.PATH_COL);

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
            where = dbManager.buildWhereClause(MediaStore.Images.Media.DATA, pathCount, false);
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
            FilterDialogHelper.filterTextInputBox(this, str -> dbManager.insertToFilterTable(str));
        } else if (itemId == R.id.remove_filter_button) {
            //User will type in filter to be deleted to help prevent accidental deletions
            FilterDialogHelper.filterTextInputBox(this, str -> removeFilter(str));
        } else {
            return false;
        }
        return true;
    }

    private void addImage() {
        //TODO: decide logic
        Toast.makeText(getApplicationContext(), "test addImage()", Toast.LENGTH_SHORT).show();
    }

    private void removeFilter(String str) {
        String where = dbManager.buildWhereClause(TableClasses.Filter.FILTER_COL, 1, true);
        Cursor cursor = dbManager.selectFromFilterTable(new String[]{TableClasses.Filter._ID}, where, new String[]{str}, null, null);
        String[] idArr = dbManager.extractFromCursor(cursor, TableClasses.Filter._ID);
        if (cursor != null) { cursor.close(); }

        dbManager.removeFromFilter(str);
        dbManager.removeFromBridge(TableClasses.ImageFilter.FILTER_ID_COL, idArr);
    }
}