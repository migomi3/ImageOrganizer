package com.example.imageorganizer;

import static android.os.Environment.MEDIA_MOUNTED;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

//https://www.youtube.com/watch?v=zg0_YS9PYi4
//https://www.geeksforgeeks.org/how-to-build-a-photo-viewing-application-in-android/

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 111;

    private RecyclerView recycler;
    private ArrayList<String> images;
    private GalleryAdaptor adaptor;

    TextView totalImages;
    Button addImageButton;
    //Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recycler = findViewById(R.id.gallery_recycler);
        images = new ArrayList<>();
        adaptor = new GalleryAdaptor(MainActivity.this, images);


        recycler.setAdapter(adaptor);

        totalImages = findViewById(R.id.total_images);

        requestPermissions();

        prepareRecyclerView();

        addImageButton = findViewById(R.id.add_image_button);
        addImageButton.setOnClickListener(view -> Toast.makeText(view.getContext(), "testing button", Toast.LENGTH_LONG).show());

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

            totalImages.setText(totalImages.getText().toString().substring(0, 13) + count);

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
                    Toast.makeText(this, "Permissions denied, Permissions are required to use the app..", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}