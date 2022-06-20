package com.example.guidenavi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.media.MediaPlayer;

import org.opencv.android.OpenCVLoader;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    MediaPlayer good;
    Handler handler = new Handler();
    final static int DELAY = 500;
    public TextToSpeech textToSpeech;


    static{
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity","openCV is Loaded");
        }
        else{
            Log.d("MainActivity","openCV is not Loaded");
        }
    }
private ImageView cameraview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        good = MediaPlayer.create(HomeActivity.this, R.raw.good);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==textToSpeech.SUCCESS);
                int lang = textToSpeech.setLanguage(Locale.ENGLISH);
            }
        });
        cameraview=findViewById(R.id.cameraView);
        playAudioWithDelay();
        cameraview.setOnClickListener(new View.OnClickListener() {



            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this,CameraActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));


            }
        });

    }
    public void playAudioWithDelay(){
       handler.postDelayed(new Runnable() {
           @Override
           public void run() {
               good.start();
           }
       },DELAY);

    }

    @Override
    protected void onPause() {
        super.onPause();
        good.stop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        good.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        good.start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        good.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        good.start();
    }
}