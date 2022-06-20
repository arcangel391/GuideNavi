package com.example.guidenavi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private final static String TAG = "MainActivity";
    private TextToSpeech textToSpeech;

    private Mat mRgba;
    private Context context;
    private Mat mGrey;
    private Button btn_detect;
    private CameraBridgeViewBase mOpenCvCameraView;
    private objectDetectorClass objectDetectorClass;
    private MediaPlayer start;
    private MediaPlayer good;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
           switch(status){
               case LoaderCallbackInterface.SUCCESS:{
                   Log.i(TAG,"OpenCv Is Loaded");
                   mOpenCvCameraView.enableView();
               }
               default:{
                    super.onManagerConnected(status);
               }
               break;
           }
        }
    };

    public CameraActivity()
    {

        Log.i(TAG,"Instantiated new "+this.getClass());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        start = MediaPlayer.create(CameraActivity.this,R.raw.detect);
        start.start();
        good = MediaPlayer.create(CameraActivity.this,R.raw.good);
        int MY_PERMISSION_REQUEST_CAMERA=0;
        if(ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
            ==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this,new String[] {Manifest.permission.CAMERA}, MY_PERMISSION_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView= (CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        try{
            objectDetectorClass=new objectDetectorClass(CameraActivity.this,getAssets(),"ssd_mobilenet.tflite","labelmap.txt",300);
            Log.d("MainActivity","Model Loaded");
        }
        catch (IOException e){
            Log.d("MainActivity","Model not Loaded");
            e.printStackTrace();
        }




    }

    @Override
    protected void onResume() {
        super.onResume();

        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCVLoader Initialized");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.d(TAG,"OpenCVLoader is not Initialized");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this, mLoaderCallback);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
        start.stop();

    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGrey=new Mat(height,width,CvType.CV_8UC1);

    }

    public void onCameraViewStopped(){
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba = inputFrame.rgba();
        mGrey = inputFrame.gray();
        btn_detect = findViewById(R.id.btn_detect);

        Mat out = new Mat();
        out=objectDetectorClass.recognizeImage(btn_detect,CameraActivity.this,mRgba);
        return out;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        start.stop();
        good.start();
    }


}