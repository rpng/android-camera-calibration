package io.rpng.calibration;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private static ImageView camera2View;
    private CameraHandler mCameraHandler;
    private AutoFitTextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Get our surfaces
        camera2View = (ImageView) findViewById(R.id.camera2_preview);
        mTextureView = (AutoFitTextureView) findViewById(R.id.camera2_texture);

        // Create the camera manager
        mCameraHandler = new CameraHandler(this, mTextureView);

    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraHandler.startBackgroundThread();
        if (mTextureView.isAvailable()) {
            mCameraHandler.openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mCameraHandler.mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        //closeCamera();
        mCameraHandler.stopBackgroundThread();
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

            /*
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Get the YUV planes, and combine them into a single data byte array
            Image.Plane Y = image.getPlanes()[0];
            Image.Plane U = image.getPlanes()[1];
            Image.Plane V = image.getPlanes()[2];

            int Yb = Y.getBuffer().remaining();
            int Ub = U.getBuffer().remaining();
            int Vb = V.getBuffer().remaining();

            byte[] data = new byte[Yb + Ub + Vb];

            Y.getBuffer().get(data, 0, Yb);
            U.getBuffer().get(data, Yb, Ub);
            V.getBuffer().get(data, Yb + Ub, Vb);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 50, out);
            byte[] imageBytes = out.toByteArray();

            Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            MainActivity.camera2View.setImageBitmap(bmp);
            */

            Mat bgrMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
            Imgproc.cvtColor(mYuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_I420);

            // Convert to rgba
            Mat rgbaMatOut = new Mat();
            Mat grayFrame = new Mat();
            Imgproc.cvtColor(bgrMat, rgbaMatOut, Imgproc.COLOR_BGR2RGBA, 0);
            Imgproc.cvtColor(bgrMat, grayFrame, Imgproc.COLOR_BGR2GRAY, 0);

            // Testing calibration methods
            Size mPatternSize = new Size(4,3);
            MatOfPoint2f mCorners = new MatOfPoint2f();

            // Extract the points, and display them
            boolean mPatternWasFound = Calib3d.findCirclesGrid(grayFrame, mPatternSize, mCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
            Calib3d.drawChessboardCorners(rgbaMatOut, mPatternSize, mCorners, mPatternWasFound);

            // Update image
            final Bitmap bitmap = Bitmap.createBitmap(bgrMat.cols(), bgrMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgbaMatOut, bitmap);
            MainActivity.camera2View.setImageBitmap(bitmap);

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
