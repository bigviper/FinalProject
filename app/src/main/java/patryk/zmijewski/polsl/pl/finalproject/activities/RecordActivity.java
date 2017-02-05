package patryk.zmijewski.polsl.pl.finalproject.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.connection.EEGPower;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacpp.opencv_imgproc;

import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import patryk.zmijewski.polsl.pl.finalproject.R;
import patryk.zmijewski.polsl.pl.finalproject.model.sensor.SensorConnector;
import patryk.zmijewski.polsl.pl.finalproject.model.settings.RecordSettings;
import patryk.zmijewski.polsl.pl.finalproject.view.SensorPreviewAdapter;

public class RecordActivity extends Activity implements OnClickListener {

    private final static String CLASS_LABEL = "RecordActivity";
    private final static String LOG_TAG = CLASS_LABEL;

    private String ffmpeg_link;

    long startTime = 0;
    boolean recording = false;

    private FFmpegFrameRecorder recorder;

    private boolean isPreviewOn = false;

    private int sampleAudioRateInHz = 44100;
    private int imageWidth = 1280;
    private int imageHeight = 720;
    private final int image2Height = 145;
    private final int imageDimsMultiplied = imageWidth*imageHeight;
    private final int image2DimsMultiplied = imageWidth*image2Height;
    private int frameRate = 30;

    /* audio data getting thread */
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    volatile boolean runAudioThread = true;

    /* video data getting thread */
    private Camera cameraDevice;
    private CameraView cameraView;

    //private RelativeLayout screen;
    //private FrameLayout sensorPreview;
    //private EditText    data1;
    //private Bitmap      sensorPreviewBitmap;
    private GridView sensorPreview;
    private Frame yuvImage = null;

    /* layout setting */
    private Button btnRecorderControl;

    /* The number of seconds in the continuous record loop (or 0 to disable loop). */
    final int RECORD_LENGTH = 0;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;

    private final SensorConnector sensorConnector = SensorConnector.getInstance();
    private final RecordSettings recordSettings = RecordSettings.getInstance();
    private final int[] visibleReadings = recordSettings.getActiveSensorReadings();

    private boolean isSensorConnected = false;

    private SensorPreviewAdapter EEGSensorAdapter = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        BluetoothAdapter btAdapter = null;

        sensorConnector.setContext(this);


        Intent intent = getIntent();
        BluetoothDevice btDevice = intent.getParcelableExtra("connectedBluetoothDevice");
        String address = intent.getStringExtra("connectedBluetoothDeviceAddress");

        try {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btAdapter == null || !btAdapter.isEnabled()) {
                Toast.makeText(
                        this,
                        "Please enable your Bluetooth and re-run this program !",
                        Toast.LENGTH_LONG).show();
                finish();
//				return;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.i(LOG_TAG, "error:" + e.getMessage());
            return;
        }



        setContentView(R.layout.activity_camera);
       sensorConnector.setmBluetoothAdapter(btAdapter);
        if(btDevice!=null){
            sensorConnector.setmBluetoothDevice(btDevice);
            sensorConnector.setAddress(address);
            sensorConnector.start();
            isSensorConnected = true;
        }


        initLayout();

       /* if(!sensorConnector.isConnected()) {
            sensorConnector.start();
        }
        isSensorConnected = true;*/

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorConnector.stop();
        recording = false;

        if (cameraView != null) {
            cameraView.stopPreview();
        }

        if(cameraDevice != null) {
            cameraDevice.stopPreview();
            cameraDevice.release();
            cameraDevice = null;
        }
    }



    private void initLayout() {
        RelativeLayout topLayout = (RelativeLayout) findViewById(R.id.camera_preview);

       /* if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }*/


        cameraDevice = Camera.open();
        /////////////Comment
        Camera.Parameters parameters = cameraDevice.getParameters();

        /*List<int[]> frameRates = parameters.getSupportedPreviewFpsRange();
        List<Camera.Size> sizes =  parameters.getSupportedPreviewSizes();
        Camera.Size prevSize = parameters.getPreviewSize();*/

        parameters.setPreviewFpsRange(30000,30000);
        parameters.setPreviewSize(imageWidth,imageHeight);
        List<Integer> formats = parameters.getSupportedPreviewFormats();
        parameters.setPreviewFormat(formats.get(0));
        cameraDevice.setParameters(parameters);
        //////////////
        Log.i(LOG_TAG, "cameara open");
        cameraView = new CameraView(this, cameraDevice);
        topLayout.addView(cameraView);
        Log.i(LOG_TAG, "cameara preview start: OK");

        btnRecorderControl = (Button) findViewById(R.id.button_capture);
        btnRecorderControl.setText("Start");
        btnRecorderControl.setOnClickListener(this);
        btnRecorderControl.bringToFront();

        ImageView connectionIcon =(ImageView) findViewById(R.id.connectionStatus);
        connectionIcon.bringToFront();

        //Working with activity_camera.xml
        //GridView sensorGrid = (GridView)findViewById(R.id.sensor_preview_grid);
        //sensorGrid.bringToFront();
        //uncomment it please
        initSensorPreview();

        //Working with activity_camera_v2.xml

        /*sensorPreview = (FrameLayout) findViewById(R.id.sensor_preview);
        sensorPreview.bringToFront();
        data1 = (EditText) findViewById(R.id.editText3);*/
    }

    private void initSensorPreview(){
        //In this method we have to work in accordance to mapping in RecordSettings.activeSensorReadings
        sensorPreview = (GridView)findViewById(R.id.sensor_preview_grid);
        EEGSensorAdapter = new SensorPreviewAdapter(this,this.recordSettings.getActiveSensorReadings());
        sensorPreview.setAdapter(EEGSensorAdapter);
        sensorPreview.bringToFront();

    }


    public void updateLayout() {

        TextView modifiedScore;
        EEGPower newScores;
        if(sensorConnector.getPower()!=null){
            newScores = sensorConnector.getPower();
            for(int i =0; i < visibleReadings.length;i++){
                switch(visibleReadings[i]){
                    case (-1):{
                        break;
                    }
                    case(0): {
                        modifiedScore = (TextView) findViewById(R.id.delta_score);
                        modifiedScore.setText(Integer.toString(newScores.delta));
                        break;
                    }

                    case(1): {
                        modifiedScore = (TextView) findViewById(R.id.theta_score);
                        modifiedScore.setText(Integer.toString(newScores.theta));
                        break;
                    }

                    case(2): {
                        modifiedScore = (TextView) findViewById(R.id.low_alpha_score);
                        modifiedScore.setText(Integer.toString(newScores.lowAlpha));
                        break;
                    }

                    case(3): {
                        modifiedScore = (TextView) findViewById(R.id.high_alpha_score);
                        modifiedScore.setText(Integer.toString(newScores.highAlpha));
                        break;
                    }

                    case(4): {
                        modifiedScore = (TextView) findViewById(R.id.low_beta_score);
                        modifiedScore.setText(Integer.toString(newScores.lowBeta));
                        break;
                    }

                    case(5): {
                        modifiedScore = (TextView) findViewById(R.id.high_beta_score);
                        modifiedScore.setText(Integer.toString(newScores.highBeta));
                        break;
                    }

                    case(6): {
                        modifiedScore = (TextView) findViewById(R.id.low_gamma_score);
                        modifiedScore.setText(Integer.toString(newScores.lowGamma));
                        break;
                    }

                    case(7): {
                        modifiedScore = (TextView) findViewById(R.id.medium_gamma_score);
                        modifiedScore.setText(Integer.toString(newScores.middleGamma));
                        break;
                    }

                    case(8): {
                        modifiedScore = (TextView) findViewById(R.id.attention_score);
                        modifiedScore.setText(Integer.toString(sensorConnector.getCurrentAttention()));
                        //modifiedScore.setText(Integer.toString(sensorConnector.));
                        break;
                    }

                    case(9): {
                        modifiedScore = (TextView) findViewById(R.id.meditation_score);
                        modifiedScore.setText(Integer.toString(sensorConnector.getCurrentMeditation()));
                        //modifiedScore.setText(Integer.toString(newScores.delta));
                        break;
                    }
                }
            }
        }

    }

    //---------------------------------------
    // initialize ffmpeg_recorder
    //---------------------------------------
    private void initRecorder() {

        Log.w(LOG_TAG,"init recorder");

        if (RECORD_LENGTH > 0) {
            imagesIndex = 0;
            images = new Frame[RECORD_LENGTH * frameRate];
            timestamps = new long[images.length];
            for (int i = 0; i < images.length; i++) {
                images[i] = new Frame(imageWidth, imageHeight, Frame.DEPTH_BYTE, 4);
                timestamps[i] = -1;
            }
        } else if (yuvImage == null) {
            yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_BYTE, 4);
            Log.i(LOG_TAG, "create yuvImage");
        }

        Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
        recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth, imageHeight, 1);
        recorder.setFormat("mp4");
        recorder.setSampleRate(sampleAudioRateInHz);
        // Set in the surface changed method
        recorder.setFrameRate(frameRate);

        Log.i(LOG_TAG, "recorder initialize success");

        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
        runAudioThread = true;
    }

    private void initFileName(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "FinalProject");

        if(     recordSettings.getFileName()==null ||
                recordSettings.getFileName().isEmpty() ||
                recordSettings.getFileName().equals(R.string.insert_filename)) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());


            ffmpeg_link = mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4";
        }
        else{
            ffmpeg_link = mediaStorageDir.getPath()+ File.separator +recordSettings.getFileName()+".mp4";
        }
    }

    public void startRecording() {
        initFileName();
        initRecorder();

        try {
            recorder.start();
            startTime = System.currentTimeMillis();
            recording = true;
            audioThread.start();

        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {

        runAudioThread = false;
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            // reset interrupt to be nice
            Thread.currentThread().interrupt();
            return;
        }
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && recording) {
            if (RECORD_LENGTH > 0) {
                Log.v(LOG_TAG,"Writing frames");
                try {
                    int firstIndex = imagesIndex % samples.length;
                    int lastIndex = (imagesIndex - 1) % images.length;
                    if (imagesIndex <= images.length) {
                        firstIndex = 0;
                        lastIndex = imagesIndex - 1;
                    }
                    if ((startTime = timestamps[lastIndex] - RECORD_LENGTH * 1000000L) < 0) {
                        startTime = 0;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += images.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        long t = timestamps[i % timestamps.length] - startTime;
                        if (t >= 0) {
                            if (t > recorder.getTimestamp()) {
                                recorder.setTimestamp(t);
                            }
                            recorder.record(images[i % images.length]);
                        }
                    }

                    firstIndex = samplesIndex % samples.length;
                    lastIndex = (samplesIndex - 1) % samples.length;
                    if (samplesIndex <= samples.length) {
                        firstIndex = 0;
                        lastIndex = samplesIndex - 1;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += samples.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        recorder.recordSamples(samples[i % samples.length]);
                    }
                } catch (FFmpegFrameRecorder.Exception e) {
                    Log.v(LOG_TAG,e.getMessage());
                    e.printStackTrace();
                }
            }

            recording = false;
            Log.v(LOG_TAG,"Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recording) {
                stopRecording();
            }

            finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }



    private CameraHandlerThread mThread = null;


    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            if (RECORD_LENGTH > 0) {
                samplesIndex = 0;
                samples = new ShortBuffer[RECORD_LENGTH * sampleAudioRateInHz * 2 / bufferSize + 1];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = ShortBuffer.allocate(bufferSize);
                }
            } else {
                audioData = ShortBuffer.allocate(bufferSize);
            }

            Log.d(LOG_TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                if (RECORD_LENGTH > 0) {
                    audioData = samples[samplesIndex++ % samples.length];
                    audioData.position(0).limit(0);
                }
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);

                    if (recording) {
                        if (RECORD_LENGTH <= 0) try {
                            recorder.recordSamples(audioData);
                            //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(LOG_TAG,e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(LOG_TAG,"AudioThread Finished, release audioRecord");

            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(LOG_TAG,"audioRecord released");
            }
        }
    }

    //---------------------------------------------
    // camera thread, gets and encodes video data
    //---------------------------------------------
    class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        private RenderScript rs;
        private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
        private Type.Builder yuvType, rgbaType;
        private Allocation in, out;

        private CallbackHandlerThread callbackThreads = null;
        private int openThreads;

        private int[] callbackBitmapBuffer;


        public CameraView(Context context, Camera camera) {
            super(context);
            Log.w("camera", "camera view");
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(CameraView.this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewCallback(CameraView.this);
            openThreads = 0;
            //callbackThreads = new CallbackHandlerThread[2];
            callbackBitmapBuffer = new int[imageHeight*imageWidth];
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            rs = RenderScript.create(this.getContext());
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

            try {
                stopPreview();
                mCamera.setPreviewDisplay(holder);
            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            stopPreview();

            Camera.Parameters camParams = mCamera.getParameters();
            List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
            // Sort the list in ascending order
            Collections.sort(sizes, new Comparator<Camera.Size>() {

                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            // Pick the first preview size that is equal or bigger, or pick the last (biggest) option if we cannot
            // reach the initial settings of imageWidth/imageHeight.
            for (int i = 0; i < sizes.size(); i++) {
                if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                    imageWidth = sizes.get(i).width;
                    imageHeight = sizes.get(i).height;
                    Log.v(LOG_TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
                    break;
                }
            }
            camParams.setPreviewSize(imageWidth, imageHeight);

            Log.v(LOG_TAG, "Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

            camParams.setPreviewFrameRate(frameRate);
            Log.v(LOG_TAG, "Preview Framerate: " + camParams.getPreviewFrameRate());

            mCamera.setParameters(camParams);

            // Set the holder (which might have changed) again
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(CameraView.this);
                startPreview();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not set preview display in surfaceChanged");
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mHolder.addCallback(null);
                mCamera.setPreviewCallback(null);
            } catch (RuntimeException e) {
                // The camera has probably just been released, ignore.
            }
        }

        public void startPreview() {
            if (!isPreviewOn && mCamera != null) {
                isPreviewOn = true;
                mCamera.startPreview();
            }
        }

        public void stopPreview() {
            if (isPreviewOn && mCamera != null) {
                isPreviewOn = false;
                mCamera.stopPreview();
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            /*if (callbackThreads == null) {
                callbackThreads = new CallbackHandlerThread(data);
            }

            synchronized (callbackThreads) {

                callbackThreads.processFrame();
            }*/
            opencv_core.Mat res =new opencv_core.Mat();

            /*Frame previewPicture = new Frame(imageWidth, imageHeight+imageHeight/2, Frame.DEPTH_UBYTE, 1);
            ((ByteBuffer)previewPicture.image[0].position(0)).put(data);

            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            opencv_core.Mat previewPictureYuv = converter.convert(previewPicture);*/

            opencv_core.Mat mYuv = new opencv_core.Mat(imageHeight+imageHeight/2, imageWidth, opencv_core.CV_8UC1);
             mYuv.data().put(data);
            //mYuv.put(new opencv_core.Mat(0,0,data));

            opencv_imgproc.cvtColor(mYuv,res,opencv_imgproc.COLOR_YUV420sp2RGBA);
            //COLOR_YUV2RGB_I420


            //GPUImageNativeLibrary.YUVtoARBG(data,imageWidth,imageHeight,callbackBitmapBuffer);


            ByteBuffer pictureByteBuffer = res.data().asByteBuffer();
            /*ByteBuffer pictureByteBuffer = ByteBuffer.allocate(imageWidth*imageHeight*4);
            IntBuffer pictureIntBuffer = pictureByteBuffer.asIntBuffer();
            pictureIntBuffer.put(callbackBitmapBuffer);*/


            Bitmap returnedBitmap = Bitmap.createBitmap(sensorPreview.getWidth(), sensorPreview.getHeight(),Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(returnedBitmap);

            Drawable bgDrawable =sensorPreview.getBackground();
            if (bgDrawable!=null)
                //has background drawable, then draw it on the canvas
                bgDrawable.draw(canvas);
            else
                //does not have background drawable, then draw white background on the canvas
                canvas.drawColor(Color.GRAY);
            sensorPreview.draw(canvas);



            Bitmap sensorPreviewBitmap = Bitmap.createScaledBitmap(returnedBitmap,imageWidth,image2Height,false);

            int size = sensorPreviewBitmap.getRowBytes() * sensorPreviewBitmap.getHeight();
            ByteBuffer sensorBuffer = ByteBuffer.allocate(size);
            sensorPreviewBitmap.copyPixelsToBuffer(sensorBuffer);
            sensorPreviewBitmap.recycle();

            pictureByteBuffer.position((imageDimsMultiplied-image2DimsMultiplied)*4);
            sensorBuffer.rewind();
            pictureByteBuffer.put(sensorBuffer.array(),0,sensorBuffer.array().length);
            pictureByteBuffer.rewind();

            if (RECORD_LENGTH > 0) {
                int i = imagesIndex++ % images.length;
                yuvImage = images[i];
                timestamps[i] = 1000 * (System.currentTimeMillis() - startTime);
            }


            /* get video data */
            if (yuvImage != null && recording) {
                ((ByteBuffer)yuvImage.image[0].position(0)).put(pictureByteBuffer);

                if (RECORD_LENGTH <= 0) try {
                    Log.v(LOG_TAG,"Writing Frame");
                    long t = 1000 * (System.currentTimeMillis() - startTime);
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }
                    recorder.record(yuvImage, avutil.AV_PIX_FMT_RGBA);
                } catch (FFmpegFrameRecorder.Exception e) {
                    Log.v(LOG_TAG,e.getMessage());
                    e.printStackTrace();
                }
            }

        }

        private class CallbackHandlerThread extends HandlerThread {
            Handler mHandler = null;
            byte[] data = null;

            CallbackHandlerThread(byte[] inputData) {
                super("CallbackHandlerThread");
                start();
                data = inputData;
                mHandler = new Handler(getLooper());
            }

            synchronized void notifyFrameProcessed() {
                notify();
            }

            void processFrame() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processFrameAction(data);

                    }
                });

            }

            private void processFrameAction(byte[] data) {
                if(isSensorConnected) {
                    RecordActivity.this.updateLayout();
                }
                if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    startTime = System.currentTimeMillis();
                    return;
                }
                if (yuvType == null)
                {
                    yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                    in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                    rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(imageWidth).setY(imageHeight);
                    out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
                }

                in.copyFrom(data);

                yuvToRgbIntrinsic.setInput(in);
                yuvToRgbIntrinsic.forEach(out);

                Bitmap bmpout = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
                out.copyTo(bmpout);
                ByteBuffer pictureBuffer =ByteBuffer.allocate(imageDimsMultiplied*4);
                bmpout.copyPixelsToBuffer(pictureBuffer);



                Bitmap returnedBitmap = Bitmap.createBitmap(sensorPreview.getWidth(), sensorPreview.getHeight(),Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(returnedBitmap);

                Drawable bgDrawable =sensorPreview.getBackground();
                if (bgDrawable!=null)
                    //has background drawable, then draw it on the canvas
                    bgDrawable.draw(canvas);
                else
                    //does not have background drawable, then draw white background on the canvas
                    canvas.drawColor(Color.GRAY);


                sensorPreview.draw(canvas);
                Bitmap sensorPreviewBitmap = Bitmap.createScaledBitmap(returnedBitmap,imageWidth,image2Height,false);

                int size = sensorPreviewBitmap.getRowBytes() * sensorPreviewBitmap.getHeight();
                ByteBuffer sensorBuffer = ByteBuffer.allocate(size);
                sensorPreviewBitmap.copyPixelsToBuffer(sensorBuffer);
                sensorPreviewBitmap.recycle();

                pictureBuffer.position((imageDimsMultiplied-image2DimsMultiplied)*4-4);
                sensorBuffer.rewind();
                pictureBuffer.put(sensorBuffer.array(),0,sensorBuffer.array().length);
                pictureBuffer.rewind();


                if (RECORD_LENGTH > 0) {
                    int i = imagesIndex++ % images.length;
                    yuvImage = images[i];
                    timestamps[i] = 1000 * (System.currentTimeMillis() - startTime);
                }


            /* get video data */
                if (yuvImage != null && recording) {
                    ((ByteBuffer)yuvImage.image[0].position(0)).put(pictureBuffer);

                    if (RECORD_LENGTH <= 0) try {
                        Log.v(LOG_TAG,"Writing Frame");
                        long t = 1000 * (System.currentTimeMillis() - startTime);
                        if (t > recorder.getTimestamp()) {
                            recorder.setTimestamp(t);
                        }
                        recorder.record(yuvImage, avutil.AV_PIX_FMT_RGBA);
                    } catch (FFmpegFrameRecorder.Exception e) {
                        Log.v(LOG_TAG,e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }





    @Override
    public void onClick(View v) {
        if (!recording) {
            startRecording();
            Log.w(LOG_TAG, "Start Button Pushed");
            btnRecorderControl.setText("Stop");
        } else {
            // This will trigger the audio recording loop to stop and then set isRecorderStart = false;
            stopRecording();
            Log.w(LOG_TAG, "Stop Button Pushed");
            btnRecorderControl.setText("Start");
        }
    }



    private class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    oldOpenCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            }
            catch (InterruptedException e) {
                Log.w(LOG_TAG, "wait was interrupted");
            }
        }

        private void oldOpenCamera() {
            try {
                cameraDevice = Camera.open();
            }
            catch (RuntimeException e) {
                Log.e(LOG_TAG, "failed to open front camera");
            }
        }
    }

}