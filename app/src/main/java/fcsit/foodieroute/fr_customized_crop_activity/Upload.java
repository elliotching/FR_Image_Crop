package fcsit.foodieroute.fr_customized_crop_activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayOutputStream;

import cz.msebera.android.httpclient.Header;


/**
 * Created by elliotching on 19-Feb-17.
 */

public class Upload {

    static RequestParams params = new RequestParams();
    private final static String FOODIE_URL_IMG_UPLOAD = "http://foodlocator.com.my/mobile/imgupload/upload.php";
    private static Context context;
    static ProgressDialog prgDialog;

    private static int retryCount = 0;
    private static boolean stopRetrying = false;

    // AsyncTask - To convert Image to String
    public static void encodeImagetoString(Context con, String imgPath, String filename) {

        context = con;
        prgDialog = new ProgressDialog(context);
        stopRetrying = false;

        new AsyncTask<String, Void, String[]>() {

            Bitmap bitmap;

            protected void onPreExecute() {
                prgDialog.setMessage("Converting Image to Binary Data");
                prgDialog.show();
            }

            @Override
            protected String[] doInBackground(String... imgPath) {

                //imgPath[0] = complete path of image
                //imgPath[1] = <filename>.jpg

                BitmapFactory.Options options = null;
                options = new BitmapFactory.Options();
//                options.inSampleSize = 3;
                bitmap = BitmapFactory.decodeFile(imgPath[0],
                        options);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                // Must compress the Image to reduce image size to make upload easy
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] byte_arr = stream.toByteArray();
                // Encode Image to String
                String encodedString = Base64.encodeToString(byte_arr, 0);
                return new String[]{encodedString, imgPath[1]};
            }

            @Override
            protected void onPostExecute(String... result) {
                prgDialog.setMessage("Calling Upload");
                // Put converted Image string into Async Http Post param
                params.put("image", result[0]);
                params.put("filename", result[1]);

                System.out.println("image StringLength: \n"+result[0].length());
                // Trigger Image upload
                triggerImageUpload("Invoking PHP");
            }
        }.execute(imgPath, filename);
    }

    private static void triggerImageUpload(String msg) {
        makeHTTPCall(msg, false);
    }



    // Make Http call to upload Image to Php server
    private static void makeHTTPCall(String msg, boolean retry) {
        prgDialog.setMessage(msg);
        final AsyncHttpClient client = new AsyncHttpClient();
        // Don't forget to change the IP address to your LAN address. Port no as well.
        client.post(FOODIE_URL_IMG_UPLOAD, params, new MyAsyncHttpRespondHandler(retry ? retryCount : 0));

//        client.setCookieStore();
        prgDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                stopRetrying = true;
                client.cancelAllRequests(true);
            }
        });
    }

    private static class MyAsyncHttpRespondHandler extends AsyncHttpResponseHandler {
        int interRetryCount;

        MyAsyncHttpRespondHandler(int c) {
            super();
            interRetryCount = c;
        }

        @Override
        public void onProgress(long bytesWritten, long totalSize) {
            super.onProgress(bytesWritten, totalSize);
            Log.d("ElliotOnProgress: ", bytesWritten+" vs "+totalSize);
        }


        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            if (statusCode == 200) {
                // Hide Progress Dialog
                String echoResult = new String(responseBody);
                prgDialog.hide();
                Toast.makeText(context, echoResult,
                        Toast.LENGTH_LONG).show();
                System.out.println("onSuccess: " + echoResult);
                retryCount = 0;
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            // Hide Progress Dialog
//                        prgDialog.hide();

            if (!stopRetrying) makeHTTPCall("Retry Uploading. (Attempt " + (++retryCount) + ")", true);
            else Toast.makeText(context , "Upload canceled." , Toast.LENGTH_LONG).show();

            // When Http response code is '404'
            if (statusCode == 404) {
                Toast.makeText(context,
                        "Requested resource not found",
                        Toast.LENGTH_LONG).show();
            }

            // When Http response code is '500'
            else if (statusCode == 500) {
                Toast.makeText(context,
                        "Something went wrong at server end",
                        Toast.LENGTH_LONG).show();
            }

            // When Http response code other than 404, 500
            else {
                Toast.makeText(
                        context,
                        "Error Occured n Most Common Error: \n" +
                                "1. Device not connected to Internet\n" +
                                "2. Web App is not deployed in App server\n" +
                                "3. App server is not running\n HTTP Status code : "
                                + statusCode, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

//    CustomMultiPartEntity j;

    public static Bitmap mark(Bitmap src, Bitmap watermark, Point location) {
        int w = src.getWidth();
        int h = src.getHeight();

        // ori image
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

//        Paint paint = new Paint();
//        paint.setColor(color);
//        paint.setAlpha(alpha);
//        paint.setTextSize(size);
//        paint.setAntiAlias(true);
//        paint.setUnderlineText(underline);
//        canvas.drawText(watermark, location.x, location.y, paint);
        canvas.drawBitmap(watermark, location.x, location.y, null);

        return result;
    }
}
