package com.yorku.mstew.camera2videoimage20;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)


    //firstly we want to make the window sticky. We acheive this by making system flags
    //Making the window sticky

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }//checked
    }

    //This is our attribute section.Note:this list will increase as we progress through the tutorial. We will create all members in this section:
    //private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    // private HandlerThread mBackgroundHandlerThread;
    //private Handler mBackgroundHandler;
    private Size mPreviewSize;
    //private static final int REQUEST_CAMERA_PERMISSION_RESULT=0;
    //private CaptureRequest.Builder mCaptureRequestBuilder;

    // under here is my previous code. lets see if it works?
//nope so lets restart
    //create surface texture listener
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();

            // Toast.makeText(getApplicationContext(), "Texture is available", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    //checked
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            //Toast.makeText(this, "we done", Toast.LENGTH_LONG).show();
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    //checked
//Creating the camera device
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            Toast.makeText(getApplicationContext(), "Camera Connected", Toast.LENGTH_SHORT).show();

            if(mIsRecording){
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                startRecord();
                mMediaRecorder.start();
            }else{
                startPreview();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }

    };
    //checked
    //Getting Camera Id
    //comment
    private String mCameraId;
    private int mTotalRotation;

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;

                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "Video app needs access to camera", Toast.LENGTH_SHORT).show();
                    }

                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    //close the camera
    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    //Creating a background thread
    private HandlerThread mBackgroundHandlerThread;

    private Handler mBackgroundHandler;

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Cam2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());

    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Adjusting orientation for calculating preview size
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);


    }

    private static int sensorDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;

    }


    //setting preview size dimensions
    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs.getHeight());

        }

    }

    //pt8
    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];

        }
    }

    //onCreate was here since the start
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mMediaRecorder = new MediaRecorder();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createVideoFolder();
        mMediaRecorder=new MediaRecorder();
        mTextureView = (TextureView) findViewById(R.id.textureView);

        mRecordImageButton = (ImageButton) findViewById(R.id.VideoButton);

        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording) {
                    mIsRecording = false;
                    mRecordImageButton.setImageResource(R.mipmap.vidpiconline);
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    startPreview();
                } else {
                    mIsRecording=true;
                    checkWriteStoragePermission();

                }
            }

        });
    }

    //pt9
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "App will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mIsRecording = true;
                mRecordImageButton.setImageResource(R.mipmap.vidpiconline);
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "App needs to save video to run", Toast.LENGTH_SHORT).show();

            }
        }
    }


    private CaptureRequest.Builder mCaptureRequestBuilder;

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(), "Unable to set up camera preview", Toast.LENGTH_SHORT).show();
                        }

                    }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();


    }

    //Here we are going to create a video busy button
    private ImageButton mRecordImageButton;
    private boolean mIsRecording = false;
    //Setting up storage
    private File mVideoFolder;
    private String mVideoFileName;

    //creating the video folder
    private void createVideoFolder() {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mVideoFolder = new File(movieFile, "Camera2_Video_Image");
        //check to see if the folder is already created
        if (!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();

        }

    }

//now we have to call the videoFolder onCreate

    private File createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //there are two types of SimpleDateFormat and Date()
        String prepend = "VIDEO_" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;

    }

    //create and Initialize REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;


    //Now we have to make this marshmellow compatable
    private void checkWriteStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(this, "M version", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mIsRecording = true;
                mRecordImageButton.setImageResource(R.mipmap.vidpicbusy);
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaRecorder.start();
            } else {
                Toast.makeText(this, "Permission to write is not granted", Toast.LENGTH_SHORT).show();
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();


                }
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else {
            mIsRecording = true;
            mRecordImageButton.setImageResource(R.mipmap.vidpicbusy);

            try {
                createVideoFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            startRecord();
            mMediaRecorder.start();

        }

    }

    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;

    private void setupMediaRecorder() throws IOException {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }
    //checked

    //Capturing and Saving Videos
    private void startRecord() {
        try {
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface),
                    new CameraCaptureSession.StateCallback() {


                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}





