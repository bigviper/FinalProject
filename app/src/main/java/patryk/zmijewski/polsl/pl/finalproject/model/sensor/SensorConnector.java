package patryk.zmijewski.polsl.pl.finalproject.model.sensor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.neurosky.AlgoSdk.NskAlgoDataType;
import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.AlgoSdk.NskAlgoType;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import java.lang.reflect.Method;

import patryk.zmijewski.polsl.pl.finalproject.R;
import patryk.zmijewski.polsl.pl.finalproject.model.bluetooth.Utils;

import static java.security.AccessController.getContext;

/**
 * Class holding a connection between a device and cellphone's bluetooth adapter.
 * Singleton class, because cell's adapter can connect to only one device at once.
 * Created by Patryk Żmijewski on 26.12.2016.
 */

public class SensorConnector {

    /**
     * Field necessary to use when we want to modify connection status
     */
    private Context context;

    private ImageView  connectionStatusIcon;
    /**
     * TAG used in logcat
     */
    private static final String TAG = SensorConnector.class.getSimpleName();

    /**
     * Instance of the class, we will operate on it with the use of getInstance() method.
     */
    private static SensorConnector instance = new SensorConnector();

    /**
     * Field responsible for sending the data from the sensor to the program.
     */
    private TgStreamReader tgStreamReader;

    /**
     * Field performing the calculations on the data delivered from the sensor.
     * Currently blink detection, attention and meditation algorithms are used.
     */
    private NskAlgoSdk nskAlgoSdk;

    /**
     * Field representing cell's bluetooth adapter.
     */
    private BluetoothAdapter mBluetoothAdapter;

    /**
     * Filed representing bluetooth module in EEG sensor.
     */
    private BluetoothDevice mBluetoothDevice;

    /**
     * Field holding the adress of the device.
     */
    private String address = null;

    /**
     * Field containing current state of the TgStreamReader
     */
    private int currentState = 0;

    /**
     * The  filed holding the amount of bad packets obtained form the sensor.
     */
    private int badPacketCount = 0;

    /**
     * Code of poor signal, passed by the EEG sensor.
     */
    private int poorSignal;

    /**
     * Strure holding information about current brain waves' values.
     */
    private EEGPower power;

    /**
     * Field holding raw data, probably needed to caltcuate Meditation and Attention
     */
    private int raw;

    private int currentAttention;

    private int currentMeditation;

    boolean isConnected;


    /**
     * Private constructor of the class, so that we are sure, that no tow connections can exist.
     */
    private SensorConnector() {
    }

    /**
     * Getter of the instance
     * @return singleton instance of the class
     */
    public static SensorConnector getInstance() {
        return instance;
    }

    /**
     * Getter of the device stream reader.
     * @return object that allows to get the raw data from the sensor.
     */
    public TgStreamReader getTgStreamReader() {
        return tgStreamReader;
    }

    /**
     * Getter of the mBluetoothAdapter.
     * @return object representing cell's bluetooth adapter.
     */
    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    /**
     * Allows to set used bluetooth adapter.
     * @param mBluetoothAdapter - cell's bluetoothd adapter.
     */
    public void setmBluetoothAdapter(BluetoothAdapter mBluetoothAdapter) {
        this.mBluetoothAdapter = mBluetoothAdapter;
    }

    /**
     * Getter of mBluetoothDevice.
     * @return the device that is currently connected to cell's bluetooth adapter.
     */
    public BluetoothDevice getmBluetoothDevice() {
        return mBluetoothDevice;
    }

    /**
     * Setter of mBluetoothDevice.
     * @param mBluetoothDevice device that is currently connected to cell's bluetooth adapter.
     */
    public void setmBluetoothDevice(BluetoothDevice mBluetoothDevice) {
        this.mBluetoothDevice = mBluetoothDevice;
    }

    /**
     * Getter of the addres.
     * @return address of the bluetooth device.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Setter of the address.
     * @param address address of the bluetooth device.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Value of bad packets read during operation
     * @return count of bad packets
     */
    public int getBadPacketCount() {
        return badPacketCount;
    }

    /**
     * Getter of poorSignal field
     * @return code of poor signal
     */
    public int getPoorSignal() {
        return poorSignal;
    }

    /**
     * Getter of power field
     * @return power containing all bwainwaves' data
     */
    public EEGPower getPower() {
        return power;
    }

    /**
     * Getter of raw filed
     * @return int containing raw ralues necessary to compute Attention and Meditation
     */
    public int getRaw() {
        return raw;
    }

    private void changeIconToActive(){
        connectionStatusIcon  = (ImageView) ((Activity)context).findViewById(R.id.connectionStatus);
        connectionStatusIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.connected_v1));
    }

    private void changeIconToNoSignal(){
        connectionStatusIcon  = (ImageView) ((Activity)context).findViewById(R.id.connectionStatus);
        connectionStatusIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.nosignal_v1));
    }

    private void changeIconToConnecting(){
        int i = 0;
        connectionStatusIcon  = (ImageView) ((Activity)context).findViewById(R.id.connectionStatus);
        while(true){
            switch(i){
                case(0):{
                    connectionStatusIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.connecting1_v1));
                    i+=1;
                    break;
                }

                case(1):{
                    connectionStatusIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.connecting2_v1));
                    i+=1;
                    break;
                }

                case(2): {
                    connectionStatusIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.connecting3_v1));
                    i=0;
                    break;
                }
            }
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                Log.w(TAG,"Thread sleep exception!");
                e.printStackTrace();
            }

        }
    }

    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            currentState  = connectionStates;
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTED:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToActive();
                        }
                    });
                    //tgStreamReader.start();
                    //showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToActive();
                        }
                    });
                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToConnecting();
                        }
                    });
                    //get data time out
                    break;
                case ConnectionStates.STATE_COMPLETE:
                    //read file complete
                    break;
                case ConnectionStates.STATE_STOPPED:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToConnecting();
                        }
                    });
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToNoSignal();
                        }
                    });
                    isConnected=false;
                    break;
                case ConnectionStates.STATE_ERROR:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToNoSignal();
                        }
                    });
                    isConnected = false;
                    Log.d(TAG,"Connect error, Please try again!");
                    break;
                case ConnectionStates.STATE_FAILED:
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeIconToNoSignal();
                        }
                    });
                    Log.d(TAG,"Connect failed, Please try again!");
                    break;
            }
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_STATE;
            msg.arg1 = connectionStates;
            LinkDetectedHandler.sendMessage(msg);


        }

        @Override
        public void onRecordFail(int a) {
            Log.e(TAG,"onRecordFail: " +a);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            badPacketCount ++;
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = MSG_UPDATE_BAD_PACKET;
            msg.arg1 = badPacketCount;
            LinkDetectedHandler.sendMessage(msg);

        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            Message msg = LinkDetectedHandler.obtainMessage();
            msg.what = datatype;
            msg.arg1 = data;
            msg.obj = obj;
            LinkDetectedHandler.sendMessage(msg);
        }

    };

    private boolean isPressing = false;
    private static final int MSG_UPDATE_BAD_PACKET = 1001;
    private static final int MSG_UPDATE_STATE = 1002;
    private static final int MSG_CONNECT = 1003;
    private boolean isReadFilter = false;



    private Handler LinkDetectedHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1234:
                    tgStreamReader.MWM15_getFilterType();
                    isReadFilter = true;
                    Log.d(TAG,"MWM15_getFilterType ");

                    break;
                case 1235:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_60HZ);
                    Log.d(TAG,"MWM15_setFilter  60HZ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;
                case 1236:
                    tgStreamReader.MWM15_setFilterType(MindDataType.FilterType.FILTER_50HZ);
                    Log.d(TAG,"MWM15_SetFilter 50HZ ");
                    LinkDetectedHandler.sendEmptyMessageDelayed(1237, 1000);
                    break;

                case 1237:
                    tgStreamReader.MWM15_getFilterType();
                    Log.d(TAG,"MWM15_getFilterType ");

                    break;


                case MindDataType.CODE_FILTER_TYPE:
                    Log.d(TAG,"CODE_FILTER_TYPE: " + msg.arg1 + "  isReadFilter: " + isReadFilter);
                    if(isReadFilter){
                        isReadFilter = false;
                        if(msg.arg1 == MindDataType.FilterType.FILTER_50HZ.getValue()){
                            LinkDetectedHandler.sendEmptyMessageDelayed(1235, 1000);
                        }else if(msg.arg1 == MindDataType.FilterType.FILTER_60HZ.getValue()){
                            LinkDetectedHandler.sendEmptyMessageDelayed(1236, 1000);
                        }else{
                            Log.e(TAG,"Error filter type");
                        }
                    }

                    break;



                case MindDataType.CODE_RAW:

                    //PZ 20161226
                    //msg.arg1;
                    //raw = msg.arg1;
                    break;
                case MindDataType.CODE_MEDITATION:
                    short medValue[] = {(short)msg.arg1};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_MED.value, medValue, 1);
                    Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);
                    break;
                case MindDataType.CODE_ATTENTION:
                    short attValue[] = {(short)msg.arg1};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_ATT.value, attValue, 1);
                    Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
                    break;
                case MindDataType.CODE_EEGPOWER:
                    power = (EEGPower)msg.obj;
                    if(!power.isValidate()){
                        power=null;

                    }
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    poorSignal = msg.arg1;
                    Log.d(TAG, "poorSignal:" + poorSignal);
                    break;
                case MSG_UPDATE_BAD_PACKET:
                    //msg.arg1;
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };


    public void reset(){
        badPacketCount=0;

    }

    public void start(){
        if(address == null || address.isEmpty()) {
            isConnected = false;
            return;
        }

        nskAlgoSdk = new NskAlgoSdk();
        int algoTypes =0;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_MED.value;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_ATT.value;



        if(nskAlgoSdk.NskAlgoInit(algoTypes, Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS).getAbsolutePath())==0) {

            nskAlgoSdk.NskAlgoStart(false);
        }


        nskAlgoSdk.setOnAttAlgoIndexListener(new NskAlgoSdk.OnAttAlgoIndexListener() {
            @Override
            public void onAttAlgoIndex(int i) {
                currentAttention=i;
            }
        });

        nskAlgoSdk.setOnMedAlgoIndexListener(new NskAlgoSdk.OnMedAlgoIndexListener() {
            @Override
            public void onMedAlgoIndex(int i) {
                currentMeditation=i;
            }
        });

        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);
        //bindToDevice(remoteDevice);
        tgStreamReader = createStreamReader(remoteDevice);
        tgStreamReader.connectAndStart();
        isConnected = true;



    }

    public void stop() {
        if(nskAlgoSdk != null) {
            nskAlgoSdk.NskAlgoStop();
            nskAlgoSdk.NskAlgoUninit();
        }

        if(tgStreamReader != null){
            tgStreamReader.stop();
            tgStreamReader.close();//if there is not stop cmd, please call close() or the data will accumulate
            tgStreamReader = null;
        }
        isConnected = false;
    }

    public TgStreamReader createStreamReader(BluetoothDevice bd){

        if(tgStreamReader == null){
            // Example of constructor public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
            tgStreamReader = new TgStreamReader(bd,callback);
            tgStreamReader.startLog();
        }else{
            // (1) Demo of changeBluetoothDevice
            tgStreamReader.changeBluetoothDevice(bd);

            // (4) Demo of setTgStreamHandler, you can change the data handler by this function
            tgStreamReader.setTgStreamHandler(callback);
        }
        return tgStreamReader;
    }

    /**
     * Check whether the given device is bonded, if not, bond it
     * @param bd
     */
    public void bindToDevice(BluetoothDevice bd){
        int ispaired = 0;
        if(bd.getBondState() != BluetoothDevice.BOND_BONDED){
            //ispaired = remoteDevice.createBond();
            try {
                //Set pin
                if(Utils.autoBond(bd.getClass(), bd, "0000")){
                    ispaired += 1;
                }
                //bind to device
                if(Utils.createBond(bd.getClass(), bd)){
                    ispaired += 2;
                }
                Method createCancelMethod=BluetoothDevice.class.getMethod("cancelBondProcess");
                boolean bool=(Boolean)createCancelMethod.invoke(bd);
                Log.d(TAG,"bool="+bool);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, " paire device Exception:    " + e.toString());
            }
        }
        Log.d(TAG, " ispaired:    " + ispaired);

    }

    public NskAlgoSdk getNskAlgoSdk() {
        return nskAlgoSdk;
    }

    public int getCurrentAttention() {
        return currentAttention;
    }

    public int getCurrentMeditation() {
        return currentMeditation;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
