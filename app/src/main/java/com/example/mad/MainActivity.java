package com.example.mad;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGES_REQUEST_CODE = 1;
    private ArrayList<Uri> imageUris = new ArrayList<>();
    private ImageView photo1, photo2, photo3, photo4;

    Button btnMergePhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMergePhotos = findViewById(R.id.button3);

        photo1 = findViewById(R.id.photo1);
        photo2 = findViewById(R.id.photo2);
        photo3 = findViewById(R.id.photo3);
        photo4 = findViewById(R.id.photo4);

        Button btnPickPhotos = findViewById(R.id.btnPickPhotos);
        btnPickPhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check permission and call action (e.g., pick images from the gallery)
                pickImagesFromGallery();
            }
        });

        // Handle merge action
        btnMergePhotos.setOnClickListener(v -> {
            if (imageUris.size() > 0) {
                mergeImages();
            } else {
                Toast.makeText(MainActivity.this, "No images selected to merge", Toast.LENGTH_SHORT).show();
            }
        });





    }

    private void pickImagesFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_IMAGES_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                imageUris.add(imageUri);
            }
            loadImagesIntoViews();
        } else {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImagesIntoViews() {
        if (imageUris.size() > 0) {
            Glide.with(this).load(imageUris.get(0)).into(photo1);
        }
        if (imageUris.size() > 1) {
            Glide.with(this).load(imageUris.get(1)).into(photo2);
        }
        if (imageUris.size() > 2) {
            Glide.with(this).load(imageUris.get(2)).into(photo3);
        }
        if (imageUris.size() > 3) {
            Glide.with(this).load(imageUris.get(3)).into(photo4);
        }
    }

    private void mergeImages() {
        if (imageUris.size() == 0) {
            Toast.makeText(this, "No images selected to merge", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set the canvas dimensions based on the maximum image dimensions or desired size
        int canvasWidth = 800;  // Adjust width
        int canvasHeight = 800; // Adjust height

        // Create a white background for the canvas
        Bitmap mergedImage = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mergedImage);
        canvas.drawColor(Color.WHITE); // Fill the canvas with white color

        Paint paint = new Paint();

        int numImages = imageUris.size();
        int halfWidth = canvasWidth / 2;
        int halfHeight = canvasHeight / 2;

        try {
            for (int i = 0; i < numImages; i++) {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUris.get(i));

                // Calculate aspect ratio of the image
                int originalWidth = bitmap.getWidth();
                int originalHeight = bitmap.getHeight();
                float aspectRatio = (float) originalWidth / originalHeight;

                // Calculate new dimensions while maintaining the aspect ratio
                int scaledWidth, scaledHeight;
                if (numImages <= 2) {
                    // Fit one or two images in the entire canvas
                    if (originalWidth > originalHeight) {
                        scaledWidth = canvasWidth;
                        scaledHeight = (int) (canvasWidth / aspectRatio);
                    } else {
                        scaledHeight = canvasHeight;
                        scaledWidth = (int) (canvasHeight * aspectRatio);
                    }
                } else {
                    // Fit 3 or 4 images in a half-width/half-height grid
                    if (originalWidth > originalHeight) {
                        scaledWidth = halfWidth;
                        scaledHeight = (int) (halfWidth / aspectRatio);
                    } else {
                        scaledHeight = halfHeight;
                        scaledWidth = (int) (halfHeight * aspectRatio);
                    }
                }

                // Scale the image while keeping the aspect ratio
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false);

                // Calculate the position (center the image if smaller than the slot)
                int left = (i % 2 == 0) ? 0 : halfWidth;
                int top = (i < 2) ? 0 : halfHeight;

                if (numImages > 2) {
                    // Adjust left and top to center the image within the space
                    left += (halfWidth - scaledWidth) / 2;
                    top += (halfHeight - scaledHeight) / 2;
                } else {
                    // For one or two images, center them in the entire canvas
                    left = (canvasWidth - scaledWidth) / 2;
                    top = (canvasHeight - scaledHeight) / 2;
                }

                // Draw the image onto the canvas
                canvas.drawBitmap(scaledBitmap, left, top, paint);
            }

            // Save the merged image
            String savedImagePath = saveImageToGallery(mergedImage);

            if (savedImagePath != null) {
                Toast.makeText(this, "Images merged and saved successfully", Toast.LENGTH_SHORT).show();
                openSavedImage(savedImagePath);
            } else {
                Toast.makeText(this, "Failed to save the image", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to merge images", Toast.LENGTH_SHORT).show();
        }
    }



    private String saveImageToGallery(Bitmap bitmap) {
        String savedImagePath = null;
        String imageFileName = "merged_image_" + System.currentTimeMillis() + ".jpg";

        try {
            // Save the image using MediaStore (API level 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CollageApp");
                values.put(MediaStore.Images.Media.IS_PENDING, true);
                values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

                Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri imageUri = getContentResolver().insert(collection, values);

                if (imageUri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    }
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    getContentResolver().update(imageUri, values, null, null);
                    savedImagePath = imageUri.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedImagePath;
    }

    private void openSavedImage(String imagePath) {
        Uri imageUri = Uri.parse(imagePath);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(imageUri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

}
