package fcsit.foodieroute.fr_customized_crop_activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.theartofdev.edmodo.cropper.CropImage.getGalleryIntents;


/**
 * USING "ARTHUR_HUB" library
 * <p>
 * https://github.com/ArthurHub/Android-Image-Cropper
 * <p>
 * dependency compile 'com.theartofdev.edmodo:android-image-cropper:2.3.+'
 */
public class MyActivity extends AppCompatActivity {

    Button mButtonChooseImage, mButtonCapture, mButtonPick, mButtonUpload;
    TextView textInfoImage;

    Context context = this;
    Activity activity = this;
    ProgressDialog prgDialog;
    String[] selectedImageFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        prgDialog = new ProgressDialog(this);
        // Set Cancelable as False
        prgDialog.setCancelable(false);

        mButtonChooseImage = (Button) findViewById(R.id.button_chooser);
        mButtonCapture = (Button) findViewById(R.id.button_capture);
        mButtonPick = (Button) findViewById(R.id.button_pick_content);
        mButtonUpload = (Button) findViewById(R.id.button_upload);
        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                CropImage.startPickImageActivity(activity);
//                Intent cc = new Intent(context , MapsActivity.class);
//                startActivity(cc);


            }
        });
        mButtonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(cameraIntent(), 1);
            }
        });

        mButtonPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(galleryIntent(), 2);
            }
        });

        mButtonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage(view);
            }
        });

        textInfoImage = (TextView) findViewById(R.id.text_image_info);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // handle result of CropImageActivity
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
            System.out.println("pick image backed.................." + resultCode);
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            startCropImageActivity(imageUri);
        }

        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            Uri imagerUri = CropImage.getPickImageResultUri(context, data);
            System.out.println("pick image backed.................." + imagerUri);
            startCropImageActivity(imagerUri);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                ((ImageView) findViewById(R.id.imageview_myactivity)).setImageURI(result.getUri());

                MyBmpInfo bmpinfo = getThumbnail(result.getUri());
                Bitmap b = bmpinfo.result;
                if(bmpinfo.warnUser) {
                    textInfoImage.setText("Warning: "+bmpinfo.warning +" ( "+b.getWidth()+" x "+b.getHeight()+" ) ");
                    textInfoImage.setTextColor(Color.parseColor("#FFFF0000"));
                }
                else textInfoImage.setText("");


                Toast.makeText(this, "Cropping successful", Toast.LENGTH_LONG).show();
                System.out.println(" size : " + b.getWidth() + " x " + b.getHeight());

                int bmpWidth = b.getWidth();
                int bmpHeight = b.getHeight();
                int logoW = (int) (bmpWidth / 5.0);
                int logoH = (int) (bmpHeight / 3.75);

                /*For testing,
                * saved in phone before upload*/
                Bitmap logo = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_12_256p_r),
                        logoW,
                        logoH,
                        false);

                int[] px = new int[logoW*logoH];
                for(int i = 0; i<px.length; i++){
                    px[i] = ContextCompat.getColor(context, R.color.c_vt_dark_cyan);
                }

                Bitmap logoBackgColor = Bitmap.createBitmap(px , logoW , logoH , Bitmap.Config.ARGB_8888);

                Bitmap createdLogo = Upload.mark(logoBackgColor,
                        logo,
                        new Point(0 , 0));

                Bitmap wtBitmap = Upload.mark(b,
                                createdLogo,
                                new Point(bmpWidth - logoW - 20 , bmpHeight - logoH - 20));

                // Save image as .jpg file in phone public "Picture" directory
                selectedImageFilePath = saveFile(wtBitmap);

                Toast.makeText(this, "Saved successfully", Toast.LENGTH_LONG);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Start crop image activity for the given image.
     */
    private void startCropImageActivity(Uri imageUri) {


        CropImage.ActivityBuilder a = CropImage.activity(imageUri);
        a.setFixAspectRatio(true);
        a.setAspectRatio(4, 3);
        a.setGuidelines(CropImageView.Guidelines.ON);
        a.setAllowCounterRotation(true);
        a.setAllowRotation(false);
        a.setMultiTouchEnabled(false);
        a.start(this, MyCrop.class);

    }

    /* In case there are images cropped too small */
    private class MyBmpInfo{
        String warning;
        Bitmap result;
        boolean warnUser;
        MyBmpInfo(Bitmap r , String w , boolean gotMsg){
            warning = w;
            result = r;
            warnUser = gotMsg;
        }
    }


    /*
    * CAUTIONS::
    * Original METHODS RETRIEVED FROM:
    * http://stackoverflow.com/questions/3879992/how-to-get-bitmap-from-an-uri
    *
    * however method has been edited for a lot!!!!
    */
    public MyBmpInfo getThumbnail(Uri uri) {
        int w = 1280;
        int h = 960;

        try {
            InputStream input = this.getContentResolver().openInputStream(uri);

            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;//optional
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();

            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }

            int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

//        double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;
//            double ratio = 1.0;

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//            bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
//            bitmapOptions.inDither = true; //optional
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//
            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();
            Log.d("elliot", "bitmap before resize: SIZE = " + bitmap.getWidth() + " x " + bitmap.getHeight());
            if (bitmap.getWidth() <= w || bitmap.getHeight() <= h) {
                return new MyBmpInfo(bitmap , "Image is too small" , true);
            } else {
                return new MyBmpInfo(Bitmap.createScaledBitmap(bitmap, w, h, true) , "" , false);
            }

        } catch (Exception e) {
            return null;
        }
    }

    private static int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) return 1;
        else {
            System.out.println("elliot: sample: " + k);
            return k;
        }
    }

    /*
    * END OF RETRIEVED CODE.
    */


    /* 1. http://stackoverflow.com/questions/649154/save-bitmap-to-location
    *  2. http://stackoverflow.com/questions/30934173/save-bitmap-to-android-default-pictures-directory
    *  3. http://stackoverflow.com/questions/37848645/how-to-get-the-default-directory-of-photo-made-by-camera-in-android
    */

    private String[] saveFile(Bitmap bmp) {
        FileOutputStream out = null;

        String filename = "foordieroute-" +
                System.currentTimeMillis() + ".jpg";
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File newFolderFromPath = new File(path.toString() + "/FoodieRoute");
        if (newFolderFromPath.exists()) {
            System.out.println("elliot+FolderExists_TRUE: " + newFolderFromPath);
        } else {
            System.out.println("elliot+FolderExists_FALSE: CREATE FOLDER (PATH), mkdir(): " + newFolderFromPath);
            newFolderFromPath.mkdir();
        }
        File filepathname = new File(newFolderFromPath, filename);
        try {
            out = new FileOutputStream(filepathname);
            System.out.println("elliot: fileoutputstream: " + out.toString());
            System.out.println("elliot: filepathname: " + filepathname.toString());
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
//                    return Uri.fromFile(getFileStreamPath(filename));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new String[]{filepathname.toString() , filename};
    }

    private Intent cameraIntent() {

        CharSequence title = context.getString(com.theartofdev.edmodo.cropper.R.string.pick_image_intent_chooser_title);
        boolean includeDocuments = false;

        PackageManager packageManager = context.getPackageManager();

        // collect all camera intents
        List<Intent> allIntents = new ArrayList<>();

        // Determine Uri of camera image to  save.
        Uri outputFileUri = CropImage.getCaptureImageOutputUri(context);

        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }
        // end of collect all camera intents.

        Intent target;
        if (allIntents.isEmpty()) {
            target = new Intent();
        } else {
            target = allIntents.get(allIntents.size() - 1);
            allIntents.remove(allIntents.size() - 1);
        }

        // Create a chooser from the main  intent
        Intent chooserIntent = Intent.createChooser(target, title);

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    private Intent galleryIntent() {

        CharSequence title = context.getString(com.theartofdev.edmodo.cropper.R.string.pick_image_intent_chooser_title);
        boolean includeDocuments = false;

        PackageManager packageManager = context.getPackageManager();

        // collect all camera intents
        List<Intent> allIntents = new ArrayList<>();

        List<Intent> galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, includeDocuments);
        if (galleryIntents.size() == 0) {
            // if no intents found for get-content try pick intent action (Huawei P9).
            galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_PICK, includeDocuments);
        }

        allIntents.addAll(galleryIntents);

        Intent target;
        if (allIntents.isEmpty()) {
            target = new Intent();
        } else {
            target = allIntents.get(allIntents.size() - 1);
            allIntents.remove(allIntents.size() - 1);
        }

        // Create a chooser from the main  intent
        Intent chooserIntent = Intent.createChooser(target, title);

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    // When Upload button is clicked
    public void uploadImage(View v) {
        // When Image is selected from Gallery
        if (selectedImageFilePath != null) {
            String imgPath = selectedImageFilePath[0];
            String filename = selectedImageFilePath[1];
            if (imgPath != null && !imgPath.isEmpty()) {

//            prgDialog.setMessage("Converting Image to Binary Data");
//            prgDialog.show();
                // Convert image to String using Base64
                Upload.encodeImagetoString(context, imgPath , filename);
                // When Image is not selected from Gallery
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "Can't found image",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    "You must select image from gallery before you try to upload",
                    Toast.LENGTH_LONG).show();
        }
    }
}
