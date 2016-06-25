package io.rpng.calibration.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import io.rpng.calibration.managers.CameraManager;
import io.rpng.calibration.R;
import io.rpng.calibration.utils.ImageUtils;
import io.rpng.calibration.views.AutoFitTextureView;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private static final int RESULT_SETTINGS = 1;

    private static ImageView camera2View_rgb;
    private static ImageView camera2View_gray;
    private CameraManager mCameraManager;
    private AutoFitTextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Check to see if opencv is enabled
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        // Pass to super
        super.onCreate(savedInstanceState);

        // Create our layout
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get our surfaces
        camera2View_rgb = (ImageView) findViewById(R.id.camera2_preview_rgb);
        //camera2View_gray = (ImageView) findViewById(R.id.camera2_preview_gray);
        mTextureView = (AutoFitTextureView) findViewById(R.id.camera2_texture);

        // Create the camera manager
        mCameraManager = new CameraManager(this, mTextureView);

        // Lets by default launch into the settings view
        Intent i = new Intent(this, SettingsActivity.class);
        startActivityForResult(i, RESULT_SETTINGS);

    }

    @Override
    public void onResume() {
        // Pass to our super
        super.onResume();
        // Start the background thread
        mCameraManager.startBackgroundThread();
        // Open the camera
        // This should take care of the permissions requests
        if (mTextureView.isAvailable()) {
            mCameraManager.openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mCameraManager.mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        // Stop background thread
        mCameraManager.stopBackgroundThread();
        // Close our camera, note we will get permission errors if we try to reopen
        // And we have not closed the current active camera
        mCameraManager.closeCamera();
        // Call the super
        super.onPause();
    }

    // Taken from OpenCamera project
    // URL: https://github.com/almalence/OpenCamera/blob/master/src/com/almalence/opencam/cameracontroller/Camera2Controller.java#L3455
    public final static ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader ir) {

            // Contrary to what is written in Aptina presentation acquireLatestImage is not working as described
            // Google: Also, not working as described in android docs (should work the same as acquireNextImage in
            // our case, but it is not)
            // Image im = ir.acquireLatestImage();


            // Get the next image from the queue
            Image image = ir.acquireNextImage();

            Mat mYuvMat = ImageUtils.imageToMat(image);

            // Convert from yuv
            Mat bgrMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
            Imgproc.cvtColor(mYuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_I420);

            // Convert to rgba and gray
            Mat rgbaMatOut = new Mat();
            Imgproc.cvtColor(bgrMat, rgbaMatOut, Imgproc.COLOR_BGR2RGBA, 0);
            Mat grayFrame = new Mat();
            Imgproc.cvtColor(bgrMat, grayFrame, Imgproc.COLOR_BGR2GRAY, 0);

            // Testing calibration methods
            Size mPatternSize = new Size(6,10);
            MatOfPoint2f mCorners = new MatOfPoint2f();

            Mat resizeimage = new Mat();
            Mat resizeimage2 = new Mat();
            Size sz = new Size(200,200);
            Imgproc.resize(grayFrame, resizeimage, sz );
            Imgproc.resize(rgbaMatOut, resizeimage2, sz );

            // Extract the points, and display them
            boolean mPatternWasFound = Calib3d.findChessboardCorners(resizeimage, mPatternSize, mCorners, Calib3d.CALIB_CB_FAST_CHECK);

            // If a pattern was found, draw it
            Calib3d.drawChessboardCorners(resizeimage2, mPatternSize, mCorners, mPatternWasFound);

            // Update image
            final Bitmap bitmap = Bitmap.createBitmap(resizeimage2.cols(), resizeimage2.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resizeimage2, bitmap);
            MainActivity.camera2View_rgb.setImageBitmap(bitmap);

            // Make sure we close the image
            image.close();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

                StringBuilder builder = new StringBuilder();

                builder.append("\n\n\n Username: " + sharedPrefs.getString("prefUsername", "NULL"));

                builder.append("\n Send report:" + sharedPrefs.getBoolean("prefSendReport", false));

                builder.append("\n Sync Frequency: " + sharedPrefs.getString("prefSyncFrequency", "NULL"));

                TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);

                settingsTextView.setText(builder.toString());
                break;

        }

    }
}
