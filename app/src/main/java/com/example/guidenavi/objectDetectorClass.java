package com.example.guidenavi;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import java.io.IOException;

import static android.content.ContentValues.TAG;

public class objectDetectorClass {

    //load model
    private Interpreter interpreter;

    public List<String> labelList;
    public List<String> recognizedlist;
    private int INPUT_SIZE;
    private int PIXEL_SIZE=3;
    private int IMAGE_MEAN=0;
    private float IMAGE_STD=255.0f;
    //initialize gpu
    private GpuDelegate gpuDelegate;
    private int height=0;
    private int width=0;
    private  TextToSpeech textToSpeech;
    public Set<String> set = new HashSet<>();
    private MediaPlayer ding;





    objectDetectorClass(Context context,AssetManager assetManager,String modelPath,String labelPath,int inputSize) throws IOException{
        INPUT_SIZE=inputSize;

        //define gpu
        Interpreter.Options options=new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
        //loading Model
        interpreter = new Interpreter(loadModelFile(assetManager,modelPath),options);
        //load labelmap
        labelList=loadLabelList(assetManager,labelPath);



        

    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException{
        List<String> labelList=new ArrayList<>();
        BufferedReader reader=new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while((line=reader.readLine())!=null){
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException{

        AssetFileDescriptor fileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }

    public Mat recognizeImage(Button btn_detect,Context context, Mat mat_image){

        Mat rotated_mat_image=new Mat();
        Core.flip(mat_image.t(),rotated_mat_image,1);
        //convert to bitmap
        Bitmap bitmap=null;
        bitmap=Bitmap.createBitmap(rotated_mat_image.cols(),rotated_mat_image.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image,bitmap);

        height=bitmap.getHeight();
        width=bitmap.getWidth();
        ding = MediaPlayer.create(context,R.raw.warning1);

        //scale bitmap to input size
        Bitmap scaledBitmap =Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);
        //convert bitmap to bytebuffer
        ByteBuffer byteBuffer=convertBitmapToByteBuffer(scaledBitmap);
        //output
        //10=objects detected 4=coordintes in image
        //float[][][]result=new float[1][10][4];
        Object[]input=new Object[1];
        input[0]=byteBuffer;

        //treemap as output boxes/score/classes
        Map<Integer,Object> output_map=new TreeMap<>();


        float[][][]boxes=new float[1][10][4];
        float[][]scores=new float[1][10];
        float[][]classes=new float[1][10];

        output_map.put(0,boxes);
        output_map.put(1,classes);
        output_map.put(2,scores);

        //prediction
        interpreter.runForMultipleInputsOutputs(input,output_map);

        HashMap<String,String> valuess = new HashMap<>();
        Object value=output_map.get(0);
        Object Object_class=output_map.get(1);
        Object score=output_map.get(2);

        //loop in each object
        for(int i=0;i<10;i++){
            float class_value=(float) Array.get(Array.get(Object_class,0),i);
            float score_value=(float) Array.get(Array.get(score,0),i);

            if(score_value>0.65 ){
                Object box1=Array.get(Array.get(value,0),i);

                float top=(float) Array.get(box1,0)*height;
                float left=(float) Array.get(box1,1)*width;
                float bottom=(float) Array.get(box1,2)*height;
                float right=(float) Array.get(box1,3)*width;


                //rectangle                           //start                //endpoint                        //color
                Imgproc.rectangle(rotated_mat_image,new Point(left,top),new Point(right,bottom),new Scalar(255,155,155),2);
                //text in rectangle                  //name                              //startpoint                            //color                         size
                Imgproc.putText(rotated_mat_image,labelList.get((int) class_value)+"distance:"+((int)(top-100)/2)+"cm",new Point(left,top),3,1,new Scalar(255,0,0),2);

                int dis =((int)(top-100)/2);
                if(dis>0){
                    String distances = "distance of"+dis+"cm";
                    valuess.put(labelList.get((int)class_value),distances);

                }
                else{
                    dis = dis*(-1);
                    String distances = "distance of"+dis+"cm";
                    valuess.put(labelList.get((int)class_value),distances);
                }

                if((left>=95&&left<400) && (top>=225&&top<=700)){
                    ding.start();
                    ding.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            ding.start();
                        }
                    });

                }
                else {



                }

            }


        }

        btn_detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status!=TextToSpeech.ERROR){
                            textToSpeech.setLanguage(Locale.ENGLISH);

                            if(valuess.isEmpty()){
                                textToSpeech.speak("There is no object detected please retry", TextToSpeech.QUEUE_FLUSH, null);
                            }
                            else {
                                textToSpeech.speak(String.valueOf(valuess)+"in front of you", TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }

                    }
                });


            }
        });



        Core.flip(rotated_mat_image.t(),mat_image,0);

        return mat_image;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {


        ByteBuffer byteBuffer;
        int quant=0;
        int size_images=INPUT_SIZE;
        if(quant==0){
            byteBuffer=ByteBuffer.allocateDirect(1*size_images*size_images*3);
        }
        else {
            byteBuffer=ByteBuffer.allocateDirect(4*size_images*size_images*3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues=new int[size_images*size_images];
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        int pixel=0;
        for(int i=0;i<size_images;++i){
            for(int j=0;j<size_images;++j){
                final int val= intValues[pixel++];
                if(quant==0){
                    byteBuffer.put((byte) ((val>>16)&0xFF));
                    byteBuffer.put((byte) ((val>>8)&0xFF));
                    byteBuffer.put((byte) (val&0xFF));
                }
                else{
                    byteBuffer.putFloat((((val >> 16 ) & 0xFF))/255.0f);
                    byteBuffer.putFloat((((val >> 8 ) & 0xFF))/255.0f);
                    byteBuffer.putFloat((((val)& 0xFF))/255.0f);

                }
            }
        }
    return byteBuffer;
    }
}
