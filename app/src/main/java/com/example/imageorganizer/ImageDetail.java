package com.example.imageorganizer;


import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class ImageDetail extends AppCompatActivity {

    String imgPath;
    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private SQLiteManager dbManager;
    private String[] imageIdArr;

    // on below line we are defining our scale factor.
    private float mScaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);
        dbManager = new SQLiteManager(this);

        // on below line getting data which we have passed from our adapter class.
        imgPath = getIntent().getStringExtra("imgPath");

        // initializing our image view.
        imageView = findViewById(R.id.idIVImage);

        // on below line we are initializing our scale gesture detector for zoom in and out for our image.
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        // on below line we are getting our image file from its path.
        File imgFile = new File(imgPath);

        // if the file exists then we are loading that image in our image view.
        if (imgFile.exists()) {
            Picasso.get().load(imgFile).placeholder(R.drawable.ic_launcher_background).into(imageView);
        }

        String where = dbManager.buildWhereClause(TableClasses.Image.PATH_COL, 1, true);
        Cursor imageCursor = dbManager.selectFromImagePathTable(new String[]{TableClasses.Image._ID}, where, new String[]{imgPath}, null, null);
        imageIdArr = dbManager.extractFromCursor(imageCursor, TableClasses.Image._ID);
        if (imageCursor != null) { imageCursor.close(); }

        loadChips();
    }

    private void loadChips() {
        ChipGroup chipgroup = findViewById(R.id.imageDetailChipGroup);

        String[] filterArr = dbManager.getFiltersFromImageId(imageIdArr);

        if (filterArr != null){
            for (String tag : filterArr) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chipgroup.addView(chip);
            }
        }

        Chip chip = new Chip(this);
        chip.setText("+");
        chip.setOnClickListener(view -> FilterDialogHelper.showFilters(this, new FilterDialogHelper.FilterAction() {
            @Override
            public void onOkButtonPressed(Dialog dialog, ChipGroup chipGroup) {
                buildBridges(dialog, chipGroup);
            }

            @Override
            public void negativeButtonPressed() {

            }
        }));
        chipgroup.addView(chip);
    }

    private void buildBridges(Dialog dialog, ChipGroup chipgroup) {
        long imgId = Long.parseLong(imageIdArr[0]);
        List<Integer> filterList = chipgroup.getCheckedChipIds();
        int listSize = filterList.size();
        String[] filterArr = new String[listSize];

        for (int i = 0; i < listSize; i++) {
            Chip chip = dialog.findViewById(filterList.get(i));
            filterArr[i] = chip.getText().toString();
        }

        String where = dbManager.buildWhereClause(TableClasses.Filter.FILTER_COL, listSize, true);
        Cursor cursor = dbManager.selectFromFilterTable(new String[]{TableClasses.Filter._ID}, where, filterArr, null, null);
        String[] filterIdArr = dbManager.extractFromCursor(cursor, TableClasses.Filter._ID);
        if (cursor != null) { cursor.close(); }

        for (String FilterId : filterIdArr) {
            long id = Long.parseLong(FilterId);
            dbManager.insertToImageFilterTable(imgId, id);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // inside on touch event method we are calling on
        // touch event method and passing our motion event to it.
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        // on below line we are creating a class for our scale
        // listener and  extending it with gesture listener.
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            // inside on scale method we are setting scale
            // for our image in our image view.
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

            // on below line we are setting
            // scale x and scale y to our image view.
            imageView.setScaleX(mScaleFactor);
            imageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}