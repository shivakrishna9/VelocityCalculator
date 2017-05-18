package ch.jiikuy.velocitycalculator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;


public class CalculateActivity extends AppCompatActivity {
    int selections = 0;
    Double[] values = new Double[4];
    Bitmap[] bms = new Bitmap[2];
    Bitmap bmOverlay;
    ImageView imageView;
    long endOfVideo = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate);

        // Get the URI of the video
        Intent intent = getIntent();
        final Uri videoUri = intent.getParcelableExtra(MainActivity.EXTRA_VIDEO);
        // Get the ImageView
        imageView = (ImageView) findViewById(R.id.imageView);
        final AlertDialog.Builder loadingDialogBuilder = new AlertDialog.Builder(CalculateActivity.this);
        // Show loading dialog
        loadingDialogBuilder.setMessage(getString(R.string.text_loading))
                .setCancelable(false);
        final AlertDialog loadingDialog = loadingDialogBuilder.create();
        loadingDialog.show();
        Runnable runnable = new Runnable() {
            public void run() {
                // Construct MediaMetadataRetriever
                final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    // Get the first and the last frame of the video
                    retriever.setDataSource(CalculateActivity.this, videoUri);
                    bms[0] = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
                    endOfVideo = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
                    Log.i("Duration:", String.valueOf(endOfVideo));
                    bms[1] = retriever.getFrameAtTime(endOfVideo);
                    while(bms[1] == null ) {
                        bms[1] = retriever.getFrameAtTime(endOfVideo-=10000);
                    }
                    // Calculate Pixel/Centimeter from the calibration width and calibration distance
                    CalibrationActivity.PX_PER_CM = Double.parseDouble(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)) / CalibrationActivity.calibrationWidth * CalibrationActivity.calibrationDistance;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingDialog.cancel();
                            imageView.requestFocus();
                            // Overlay first and last frame
                            overlay(bms[0], bms [1]);
                            // Set the ImageView
                            imageView.setImageBitmap(bmOverlay);
                            // Set the listener of the ImageView
                            imageView.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    double bmWidth = bms[0].getWidth();
                                    double imWidth = imageView.getWidth();
                                    double width = bmWidth / imWidth;
                                    if (selections < 2 && event.getAction() == MotionEvent.ACTION_DOWN) {
                                        selections++;
                                        if (selections == 1) {
                                            values[0] = event.getX() * width;
                                            values[1] = event.getY() * width;
                                        } else if (selections == 2) {
                                            values[2] = event.getX() * width;
                                            values[3] = event.getY() * width;
                                        }
                                        Canvas canvas = new Canvas(bmOverlay);
                                        Paint paint = new Paint();
                                        paint.setColor(Color.RED);
                                        canvas.drawCircle(event.getX() * (float)width, event.getY() * (float)width, 25, paint);
                                        imageView.setImageBitmap(bmOverlay);
                                        Log.i("Coordinates " + selections, event.getX() * width + " " + event.getY() * width);
                                    }
                                    return true;
                                }
                            });
                        }
                    });
                }catch(Exception e) {
                    Log.e("Error:", e.toString());
                    e.printStackTrace();
                }
            }
        };

        // Start Thread
        Thread loadImages = new Thread(runnable);
        loadImages.start();
    }

    private void overlay(Bitmap bm1, Bitmap bm2) {
        // Combine bitmaps
        bmOverlay = Bitmap.createBitmap(bm1.getWidth(), bm1.getHeight(), bm1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        Paint paint = new Paint();
        paint.setAlpha(128);
        canvas.drawBitmap(bm1, new Matrix(), null);
        canvas.drawBitmap(bm2, 0, 0, paint);
    }
    public void selectCoordinates(View view) {
        EditText distanceEditText = (EditText)findViewById(R.id.editText);
        if(selections == 2 && !distanceEditText.getText().toString().isEmpty()) {
            // Get distance on combined image in Pixels
            int distanceOnVideo = (int) Math.sqrt(Math.pow(values[0] - values[2], 2) + Math.pow(values[1] - values[3], 2));
            Log.i(getString(R.string.text_distance), String.valueOf(distanceOnVideo));
            // Calculate speed in m/s
            double speedMetersPerSecond = 1 / ((CalibrationActivity.PX_PER_CM / Double.parseDouble(distanceEditText.getText().toString())) / distanceOnVideo) / (endOfVideo / 1000000) / 100;
            AlertDialog.Builder distanceSelectDialogBuilder = new AlertDialog.Builder(CalculateActivity.this);
            distanceSelectDialogBuilder.setMessage(getString(R.string.text_speed) + ":" + String.valueOf(speedMetersPerSecond) + "m/s");
            AlertDialog distanceSelectDialog = distanceSelectDialogBuilder.create();
            distanceSelectDialog.show();
        }else {
            // Display alert
            AlertDialog.Builder infoDialogBuilder = new AlertDialog.Builder(CalculateActivity.this);
            infoDialogBuilder.setMessage(getString(R.string.text_notCompleted));
            AlertDialog distanceSelectDialog = infoDialogBuilder.create();
            distanceSelectDialog.show();
        }
    }

}